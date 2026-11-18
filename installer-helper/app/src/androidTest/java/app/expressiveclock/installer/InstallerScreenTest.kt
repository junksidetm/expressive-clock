package app.expressiveclock.installer

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import app.expressiveclock.installer.Presentation.Phase

/** Isolated UI states only: no Work singleton, credentials, network or PackageInstaller calls. */
@RunWith(AndroidJUnit4::class)
class InstallerScreenTest {
    @get:Rule val ui=createComposeRule()
    private class Actions:InstallerUi.Actions {
        var installs=0;var retries=0;var cancels=0;var connects=0
        override fun install(){installs++};override fun retry(){retries++};override fun cancel(){cancels++}
        override fun connect(client:String){connects++};override fun authorize(){};override fun disconnect(){}
    }
    private fun show(state:UiSnapshot,actions:Actions=Actions(),dark:Boolean=false,fontScale:Float=1f) {
        ui.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,fontScale)) {
                InstallerTheme(dark) { CompositionLocalProvider(LocalMotionEnabled provides false) {
                    InstallerScreen(state,"ClockPublicClientFixture",actions)
                } }
            }
        }
    }
    @Test fun connectedReadyIsCompactAndSettingsAreHidden(){
        show(UiSnapshot(connected=true))
        ui.onNodeWithText("You're all set").assertIsDisplayed()
        ui.onNodeWithText("GitHub connected").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("GitHub App client ID").assertDoesNotExist()
        ui.onNodeWithText("Install update").assertDoesNotExist()
        ui.onNodeWithContentDescription("Connection settings").performClick()
        ui.onNodeWithText("GitHub App client ID").performScrollTo().assertIsDisplayed()
    }
    @Test fun disconnectedHasOneConnectAction(){
        val a=Actions();show(UiSnapshot(),a)
        ui.onAllNodesWithText("Connect GitHub").assertCountEquals(1)
        ui.onNodeWithText("Connect GitHub").performScrollTo().performClick()
        ui.runOnIdle {assertEquals(1,a.connects)}
    }
    @Test fun downloadHasRealProgressAndOnlyCancel(){
        val a=Actions();show(UiSnapshot(Phase.DOWNLOADING,connected=true,busy=true,hasRequest=true,progress=37),a)
        ui.onNodeWithText("Downloading your build").assertIsDisplayed()
        ui.onNodeWithContentDescription("Download progress").assert(SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
            androidx.compose.ui.semantics.ProgressBarRangeInfo(37f,0f..100f)))
        ui.onNodeWithText("Install update").assertDoesNotExist()
        ui.onNodeWithText("Cancel").performScrollTo().performClick()
        ui.runOnIdle {assertEquals(1,a.cancels)}
    }
    @Test fun verificationDoesNotClaimCompletion(){
        show(UiSnapshot(Phase.VERIFYING,connected=true,busy=true,hasRequest=true))
        ui.onNodeWithText("Checking your APK").assertIsDisplayed()
        ui.onNodeWithText("Installation complete").assertDoesNotExist()
        ui.onNodeWithContentDescription("Download progress").assertDoesNotExist()
    }
    @Test fun readyToInstallInvokesOnlyProvidedCallback(){
        val a=Actions();show(UiSnapshot(Phase.READY_TO_INSTALL,connected=true,ready=true,hasRequest=true),a)
        ui.onNodeWithText("Install update").performScrollTo().performClick()
        ui.runOnIdle {assertEquals(1,a.installs)}
    }
    @Test fun failedBuildCanRetry(){
        val a=Actions();show(UiSnapshot(Phase.FAILED,"Connection interrupted. Try again.",true,false,false,true),a)
        ui.onNodeWithText("Retry download").performScrollTo().performClick()
        ui.runOnIdle {assertEquals(1,a.retries)}
    }
    @Test fun darkLargeFontsRemainScrollable(){
        show(UiSnapshot(Phase.COMPLETED,connected=true),dark=true,fontScale=2f)
        ui.onNodeWithText("Installation complete").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("GitHub connected").performScrollTo().assertIsDisplayed()
    }
    @Test fun everyPhaseCanTransitionWithoutNetworkOrInstaller(){
        var state by mutableStateOf(UiSnapshot(connected=true))
        ui.setContent {InstallerTheme {CompositionLocalProvider(LocalMotionEnabled provides false) {InstallerScreen(state,"approved",Actions())}}}
        for(phase in Phase.entries) {
            ui.runOnIdle {state=UiSnapshot(phase,connected=true,busy=phase in listOf(Phase.CONNECTING,Phase.DOWNLOADING,Phase.VERIFYING,Phase.INSTALLING),progress=62)}
            ui.onNodeWithText(title(phase,true)).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun semanticTextContrastInBothSchemes(){
        for(c in listOf(InstallerLight,InstallerDark)) {
            val a=c.onSurface.luminance();val b=c.background.luminance()
            assertTrue((maxOf(a,b)+.05f)/(minOf(a,b)+.05f)>=4.5f)
        }
    }
    @Test fun rapidAnimatedStateChangesSettleOnLatestState(){
        var state by mutableStateOf(UiSnapshot(connected=true))
        ui.setContent {InstallerTheme {InstallerScreen(state,"approved",Actions())}}
        ui.mainClock.autoAdvance=false
        ui.runOnIdle {state=UiSnapshot(Phase.DOWNLOADING,connected=true,busy=true,hasRequest=true,progress=12)}
        ui.mainClock.advanceTimeBy(60)
        ui.runOnIdle {state=UiSnapshot(Phase.VERIFYING,connected=true,busy=true,hasRequest=true)}
        ui.mainClock.advanceTimeBy(60)
        ui.runOnIdle {state=UiSnapshot(Phase.READY_TO_INSTALL,connected=true,ready=true,hasRequest=true)}
        ui.mainClock.advanceTimeBy(1500)
        ui.onNodeWithText("Ready to install").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Install update").assertIsEnabled()
    }
    @Test fun captureIsolatedLightAndDarkDesign(){
        var dark by mutableStateOf(false)
        var state by mutableStateOf(UiSnapshot(connected=true))
        ui.setContent {InstallerTheme(dark) {CompositionLocalProvider(LocalMotionEnabled provides false) {InstallerScreen(state,"approved",Actions())}}}
        fun capture(name:String){
            ui.onNodeWithText("Clock Installer").performScrollTo()
            ui.waitForIdle()
            val image=ui.onRoot().captureToImage().asAndroidBitmap()
            val path=File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,name)
            path.outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
        capture("installer-ready-light.png")
        ui.runOnIdle {dark=true}
        capture("installer-ready-dark.png")
        ui.runOnIdle {state=UiSnapshot(Phase.DOWNLOADING,connected=true,busy=true,hasRequest=true,progress=37)}
        capture("installer-download-dark.png")
    }
    @Test fun largeDownloadDigitsHaveRoomForScaledLineHeight(){
        var requiredHeight=0.dp
        ui.setContent {
            val base=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density,2f)) {
                val density=LocalDensity.current
                SideEffect {requiredHeight=with(density){52.sp.toDp()}+8.dp}
                InstallerTheme {CompositionLocalProvider(LocalMotionEnabled provides false) {
                    InstallerScreen(UiSnapshot(Phase.DOWNLOADING,connected=true,busy=true,hasRequest=true,progress=100),"approved",Actions())
                }}
            }
        }
        ui.onNodeWithTag("percentageDigits",useUnmergedTree=true).performScrollTo()
        val viewport=ui.onNodeWithTag("percentageDigits",useUnmergedTree=true).getUnclippedBoundsInRoot()
        assertTrue("Digit viewport must contain its scaled line box plus padding",viewport.bottom-viewport.top>=requiredHeight)
        ui.onNodeWithText("Cancel").performScrollTo().assertIsDisplayed()
    }
}
