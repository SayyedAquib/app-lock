package com.pixelguard.applock.ui.onboarding

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.pixelguard.applock.data.repository.FakeAppLockRepository
import com.pixelguard.applock.ui.main.MainViewModel
import com.pixelguard.applock.ui.main.PermissionStatus
import com.pixelguard.applock.ui.theme.PixelAppLockTheme
import com.pixelguard.applock.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-420dpi")
class OnboardingScreenUiTest {

    @get:Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var fakeRepository: FakeAppLockRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeAppLockRepository(initialHasPin = false)
        viewModel = MainViewModel(fakeRepository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun initialState_displaysPermissionCardsAndRequiredBadges() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = false,
                hasAccessibilityPermission = false,
                hasNotificationPermission = false
            )
        )

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Pixel Guard").assertExists()
        composeTestRule.onNodeWithText("Display Over Other Apps").assertDoesNotExist()
        composeTestRule.onNodeWithText("App Detection").assertExists()
        composeTestRule.onNodeWithText("Hide Notification Details").assertExists()
        composeTestRule.onNodeWithText("Turn On App Detection").assertExists()
    }

    @Test
    fun whenPermissionsGranted_andNoPin_showsCreateMasterPinCard() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = true,
                hasAccessibilityPermission = true,
                hasNotificationPermission = false
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Step 2: Create Passcode").assertExists()
        composeTestRule.onNodeWithText("Enter 4-Digit Passcode").assertExists()
        composeTestRule.onNodeWithText("Confirm 4-Digit Passcode").assertExists()
        composeTestRule.onNodeWithText("Save Passcode & Finish").assertExists()
    }

    @Test
    fun pinCreation_buttonDisabledUntilBothFieldsHaveFourDigits() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = true,
                hasAccessibilityPermission = true,
                hasNotificationPermission = false
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        val submitButton = composeTestRule.onNodeWithText("Save Passcode & Finish")
        submitButton.assertExists()
        submitButton.assertIsNotEnabled()

        // Type only 2 digits in first field
        composeTestRule.onNodeWithText("Enter 4-Digit Passcode").performScrollTo().performTextInput("12")
        submitButton.assertIsNotEnabled()

        // Complete first field with 4 digits, but confirm is empty
        composeTestRule.onNodeWithText("Enter 4-Digit Passcode").performScrollTo().performTextInput("34")
        submitButton.assertIsNotEnabled()

        // Type 4 digits in confirm
        composeTestRule.onNodeWithText("Confirm 4-Digit Passcode").performScrollTo().performTextInput("1234")
        submitButton.assertIsEnabled()
    }

    @Test
    fun pinMismatch_displaysErrorMessage() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = true,
                hasAccessibilityPermission = true,
                hasNotificationPermission = false
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Enter 4-Digit Passcode").performScrollTo().performTextInput("1234")
        composeTestRule.onNodeWithText("Confirm 4-Digit Passcode").performScrollTo().performTextInput("5678")

        composeTestRule.onNodeWithText("Save Passcode & Finish").performScrollTo().performClick()

        composeTestRule.onNodeWithText("Passcodes do not match").assertExists()
        assertThat(fakeRepository.storedPin).isNull()
    }

    @Test
    fun pinMatch_savesMasterPinAndCallsOnComplete() {
        var onCompleteCalled = false
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = true,
                hasAccessibilityPermission = true,
                hasNotificationPermission = false
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = { onCompleteCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Enter 4-Digit Passcode").performScrollTo().performTextInput("8942")
        composeTestRule.onNodeWithText("Confirm 4-Digit Passcode").performScrollTo().performTextInput("8942")

        composeTestRule.onNodeWithText("Save Passcode & Finish").performScrollTo().performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(fakeRepository.storedPin).isEqualTo("8942")
        assertThat(onCompleteCalled).isTrue()
    }

    @Test
    fun skipOptionalNotificationPermission_clickingLaterHidesLaterButton() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = false,
                hasAccessibilityPermission = false,
                hasNotificationPermission = false
            )
        )

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        val laterButton = composeTestRule.onNodeWithText("Later")
        laterButton.assertExists()
        laterButton.performClick()

        // After skipping, "Later" is replaced with an outlined "Enable" button
        composeTestRule.onNodeWithText("Later").assertDoesNotExist()
    }

    @Test
    fun whenAllPermissionsGranted_andPinAlreadyConfigured_displaysContinueButton() {
        var onCompleteCalled = false
        viewModel.setMasterPin("1234")
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = true,
                hasAccessibilityPermission = true,
                hasNotificationPermission = false
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = { onCompleteCalled = true }
                )
            }
        }

        val continueBtn = composeTestRule.onNodeWithText("Get Started")
        continueBtn.assertExists()
        continueBtn.performClick()
        assertThat(onCompleteCalled).isTrue()
    }

    @Test
    fun whenAccessibilityPermissionNotGranted_displaysGrantAccessibilityPermission() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = false,
                hasAccessibilityPermission = false,
                hasNotificationPermission = false
            )
        )

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Turn On App Detection").assertExists()
    }

    @Test
    fun grantRequiredPermissionsButton_isEnabledAndClickable() {
        viewModel.setPermissionStatusForTest(
            PermissionStatus(
                hasOverlayPermission = false,
                hasAccessibilityPermission = false,
                hasNotificationPermission = false
            )
        )

        composeTestRule.setContent {
            PixelAppLockTheme {
                OnboardingScreen(
                    viewModel = viewModel,
                    onComplete = {}
                )
            }
        }

        val button = composeTestRule.onNodeWithText("Turn On App Detection")
        button.assertExists()
        button.assertIsEnabled()
    }
}
