package com.pixelguard.applock.ui.logs

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.pixelguard.applock.data.model.IntruderRecord
import com.pixelguard.applock.data.repository.FakeAppLockRepository
import com.pixelguard.applock.ui.main.MainViewModel
import com.pixelguard.applock.ui.theme.PixelAppLockTheme
import com.pixelguard.applock.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-420dpi")
class IntruderLogsScreenUiTest {

    @get:Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var fakeRepository: FakeAppLockRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeAppLockRepository(initialHasPin = true)
        viewModel = MainViewModel(fakeRepository)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun emptyState_displaysNoIntrudersDetectedBanner() {
        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Intruder Selfies").assertExists()
        composeTestRule.onNodeWithText("No Break-In Attempts").assertExists()
        composeTestRule.onNodeWithText("When someone enters an incorrect passcode, their photo will appear here.").assertExists()
    }

    @Test
    fun recordsList_displaysAttemptedAppName() {
        fakeRepository.setIntruderRecords(
            listOf(
                IntruderRecord(
                    id = "rec-1",
                    timestamp = 1700000000000L,
                    targetPackageName = "com.pixelguard.bank",
                    targetAppName = "Pixel Bank",
                    photoPath = "/nonexistent/path.jpg"
                )
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Attempted: Pixel Bank").assertExists()
    }

    @Test
    fun clearAllButton_showsConfirmationDialogAndClearsLogs() {
        fakeRepository.setIntruderRecords(
            listOf(
                IntruderRecord(
                    id = "rec-1",
                    timestamp = 1700000000000L,
                    targetPackageName = "com.pixelguard.bank",
                    targetAppName = "Pixel Bank",
                    photoPath = "/nonexistent/path.jpg"
                )
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Clear All").performClick()

        composeTestRule.onNodeWithText("Clear All Logs?").assertExists()
        composeTestRule.onNodeWithText("Delete All").performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.onNodeWithText("No Break-In Attempts").assertExists()
    }

    @Test
    fun backButton_callsNavigateBackCallback() {
        var backCalled = false

        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertThat(backCalled).isTrue()
    }

    @Test
    fun singleRecordDelete_removesTargetRecord() {
        fakeRepository.setIntruderRecords(
            listOf(
                IntruderRecord(
                    id = "rec-1",
                    timestamp = 1700000000000L,
                    targetPackageName = "com.pixelguard.bank",
                    targetAppName = "Pixel Bank",
                    photoPath = "/nonexistent/bank.jpg"
                ),
                IntruderRecord(
                    id = "rec-2",
                    timestamp = 1700000010000L,
                    targetPackageName = "com.android.chrome",
                    targetAppName = "Chrome",
                    photoPath = "/nonexistent/chrome.jpg"
                )
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Attempted: Pixel Bank").assertExists()
        composeTestRule.onNodeWithText("Attempted: Chrome").assertExists()

        // Delete first record
        val deleteButtons = composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Delete"))
        deleteButtons[0].performClick()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.onNodeWithText("Attempted: Pixel Bank").assertDoesNotExist()
        composeTestRule.onNodeWithText("Attempted: Chrome").assertExists()
    }

    @Test
    fun photoPreviewDialog_opensAndDismisses() {
        fakeRepository.setIntruderRecords(
            listOf(
                IntruderRecord(
                    id = "rec-1",
                    timestamp = 1700000000000L,
                    targetPackageName = "com.pixelguard.bank",
                    targetAppName = "Pixel Bank",
                    photoPath = "/nonexistent/bank.jpg"
                )
            )
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.setContent {
            PixelAppLockTheme {
                IntruderLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Tap the intruder record card to open photo preview dialog
        composeTestRule.onNodeWithText("Attempted: Pixel Bank").performClick()

        // Check dialog close button and dismiss
        composeTestRule.onNodeWithText("Close").assertExists()
        composeTestRule.onNodeWithText("Close").performClick()

        composeTestRule.onNodeWithText("Close").assertDoesNotExist()
    }
}
