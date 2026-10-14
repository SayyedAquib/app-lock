package com.pixelguard.applock.data.model

data class IntruderRecord(
    val id: String,
    val timestamp: Long,
    val targetPackageName: String,
    val targetAppName: String,
    val photoPath: String
)
