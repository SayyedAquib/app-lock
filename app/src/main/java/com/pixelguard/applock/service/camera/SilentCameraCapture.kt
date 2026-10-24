package com.pixelguard.applock.service.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SilentCameraCapture(private val context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var watchdogRunnable: Runnable? = null

    @SuppressLint("MissingPermission")
    fun captureIntruderPhoto(onPhotoCaptured: (File) -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return

        val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        } ?: return

        startBackgroundThread()

        val watchdog = Runnable {
            Log.w(TAG, "Intruder photo capture timed out after 4000ms; forcing hardware cleanup")
            cleanup()
        }
        watchdogRunnable = watchdog
        backgroundHandler?.postDelayed(watchdog, 4000L)

        val intruderDir = File(context.filesDir, "intruders").apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(intruderDir, "intruder_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.enc")

        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    try {
                        val cryptoManager = com.pixelguard.applock.AppLockApplication.instance.cryptoManager
                        val encryptedBytes = cryptoManager.encryptToFileBytes(bytes)
                        FileOutputStream(outputFile).use { it.write(encryptedBytes) }
                        onPhotoCaptured(outputFile)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save encrypted intruder selfie: ${e.message}")
                    } finally {
                        cleanup()
                    }
                }
            }, backgroundHandler)
        }

        try {
            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera, outputFile)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cleanup()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cleanup()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening front camera: ${e.message}")
            cleanup()
        }
    }

    private fun createCaptureSession(camera: CameraDevice, outputFile: File) {
        val surface = imageReader?.surface ?: return
        try {
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.JPEG_ORIENTATION, 270) // Typical front-cam orientation for portrait
            }

            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        session.capture(captureRequestBuilder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to trigger capture request: ${e.message}")
                        cleanup()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cleanup()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Session creation failed: ${e.message}")
            cleanup()
        }
    }

    private fun startBackgroundThread() {
        handlerThread = HandlerThread("CameraBackgroundThread").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun cleanup() {
        try {
            watchdogRunnable?.let { backgroundHandler?.removeCallbacks(it) }
            watchdogRunnable = null
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error during camera cleanup: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SilentCameraCapture"
    }
}
