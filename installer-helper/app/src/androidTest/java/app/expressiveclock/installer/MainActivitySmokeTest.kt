package app.expressiveclock.installer

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launch only; no authentication/network/installation action. Optional known-session fixture. */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun launcherPreservesConnectionAndDoesNotStartAnInstall(){
        ui.onNodeWithText("Clock Installer").assertIsDisplayed()
        ui.onNodeWithContentDescription("Connection settings").performScrollTo().assertIsDisplayed()
        if(InstrumentationRegistry.getArguments().getString("expectedConnected")=="true") {
            ui.onNodeWithText("GitHub connected").assertIsDisplayed()
        }
        ui.onNodeWithText("Downloading your build").assertDoesNotExist()
        ui.onNodeWithText("Over to Android").assertDoesNotExist()
        ui.onNodeWithText("Installation complete").assertDoesNotExist()
    }
}
