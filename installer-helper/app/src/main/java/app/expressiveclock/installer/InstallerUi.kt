package app.expressiveclock.installer

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import app.expressiveclock.installer.Presentation.Phase
import app.expressiveclock.installer.Presentation.Action

/** Non-sensitive presentation snapshot. No credentials, URLs or verification decisions enter UI. */
@Immutable
data class UiSnapshot(val phase:Phase=Phase.READY,val detail:String="",val connected:Boolean=false,
    val busy:Boolean=false,val ready:Boolean=false,val hasRequest:Boolean=false,val progress:Int=-1,val code:String="",val metadata:String="")

class InstallerUi(private val activity:ComponentActivity,private val actions:Actions,initialClient:String) {
    interface Actions {
        fun connect(client:String)
        fun authorize()
        fun retry()
        fun install()
        fun cancel()
        fun disconnect()
    }
    private var current by mutableStateOf(UiSnapshot())
    init {
        activity.setContent {
            val context=LocalContext.current
            val preferences=remember { context.getSharedPreferences("appearance",0) }
            var wallpaper by rememberSaveable { mutableStateOf(preferences.getBoolean("wallpaper",false)) }
            val dark=isSystemInDarkTheme()
            LaunchedEffect(dark) {
                val bars=if(dark)SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    else SystemBarStyle.light(AndroidColor.TRANSPARENT,AndroidColor.TRANSPARENT)
                activity.enableEdgeToEdge(statusBarStyle=bars,navigationBarStyle=bars)
            }
            InstallerTheme(dark,wallpaper) {
                InstallerScreen(current,initialClient,actions,wallpaper) {
                    wallpaper=it;preferences.edit().putBoolean("wallpaper",it).apply()
                }
            }
        }
    }
    fun update(snapshot:UiSnapshot){current=snapshot}
}

@Composable
internal fun InstallerScreen(state:UiSnapshot,initialClient:String,actions:InstallerUi.Actions,
    wallpaper:Boolean=false,onWallpaper:(Boolean)->Unit={}) {
    val color=MaterialTheme.colorScheme
    val density=LocalDensity.current
    var advanced by rememberSaveable { mutableStateOf(initialClient.isBlank()) }
    var client by rememberSaveable { mutableStateOf(initialClient) }
    Surface(color=color.background,modifier=Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            val wide=Presentation.twoColumns(maxWidth.value,density.fontScale)
            val compact=maxHeight<600.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=if(wide)36.dp else 24.dp)
                .padding(top=if(compact)20.dp else 28.dp,bottom=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Column(Modifier.widthIn(max=1040.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(if(compact)20.dp else 28.dp)) {
                    Header(wide,compact)
                    if(wide) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.Top) {
                            StatusCard(state,compact,Modifier.weight(1.3f),actions,client)
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                                ConnectionCard(state.connected,state.busy,state.phase==Phase.CONNECTING,client,{client=it},advanced,{advanced=!advanced},actions,wallpaper,onWallpaper)
                                TrustNote()
                            }
                        }
                    } else {
                        StatusCard(state,compact,Modifier.fillMaxWidth(),actions,client)
                        ConnectionCard(state.connected,state.busy,state.phase==Phase.CONNECTING,client,{client=it},advanced,{advanced=!advanced},actions,wallpaper,onWallpaper)
                        TrustNote()
                    }
                }
            }
        }
    }
}

@Composable private fun Header(wide:Boolean,compact:Boolean) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                Glyph(Phase.READY,MaterialTheme.colorScheme.onSecondaryContainer,Modifier.size(20.dp))
            }
            Text("EXPRESSIVE CLOCK",style=MaterialTheme.typography.labelMedium.copy(letterSpacing=1.2.sp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Clock Installer",style=if(wide)MaterialTheme.typography.displayMedium else if(compact)MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall)
        Text("Your latest Expressive Clock builds, ready to install.",style=MaterialTheme.typography.bodyLarge,
            color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.widthIn(max=520.dp))
    }
}

@Composable private fun StatusCard(state:UiSnapshot,compact:Boolean,modifier:Modifier,actions:InstallerUi.Actions,client:String) {
    val shape=RoundedCornerShape(36.dp)
    val motion=LocalMotionEnabled.current
    val colors=MaterialTheme.colorScheme
    val dark=colors.background.luminance()<.5f
    val accent=if(dark)colors.primary else colors.primaryContainer
    val onAccent=if(dark)colors.onPrimary else colors.onPrimaryContainer
    val primary=Presentation.primary(state.phase,state.connected,state.busy,state.ready,state.hasRequest,state.code.isNotEmpty())
    Column(modifier.shadow(10.dp,shape,ambientColor=Color.Black.copy(alpha=.2f),spotColor=Color.Black.copy(alpha=.2f))
        .clip(shape).studioCharcoal()
        .border(1.dp,Color.White.copy(alpha=.09f),shape).expressiveTouch(shape)
        .animateContentSize(if(motion)spring(.86f,380f) else snap()).padding(if(compact)24.dp else 28.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Row(Modifier.background(Color.White.copy(alpha=.07f),CircleShape).padding(horizontal=12.dp,vertical=7.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(6.dp).background(if(state.phase==Phase.FAILED)Color(0xFFFFB4AB) else accent,CircleShape))
                Text(phaseLabel(state.phase),style=MaterialTheme.typography.labelMedium,color=Color(0xFFE0E7E8))
            }
            if(LocalDensity.current.fontScale<=1.2f) Text("DEBUG BUILDS",style=MaterialTheme.typography.labelSmall,color=Color(0xFFBFC8CA))
        }
        BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
            val ideal=(if(compact)112.dp else 148.dp)*LocalDensity.current.fontScale.coerceAtLeast(1f)
            ProgressEmblem(state,accent,ideal.coerceAtMost(maxWidth))
        }
        AnimatedContent(targetState=statusTitle(state) to description(state.phase,state),transitionSpec={
            if(motion) (fadeIn(tween(180))+slideInVertically(spring(.82f,380f)){it/8}) togetherWith fadeOut(tween(100))
            else EnterTransition.None togetherWith ExitTransition.None
        },label="statusContent") { (title,detail) ->
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp),
                modifier=Modifier.fillMaxWidth().semantics(mergeDescendants=true) {liveRegion=LiveRegionMode.Polite}) {
                Text(title,style=MaterialTheme.typography.headlineMedium,color=Color.White,textAlign=TextAlign.Center)
                Text(detail,style=MaterialTheme.typography.bodyLarge,color=Color(0xFFBFC8CA),textAlign=TextAlign.Center)
            }
        }
        if(state.metadata.isNotBlank()) Text(state.metadata,style=MaterialTheme.typography.bodySmall,
            color=Color(0xFFD6E2E5),textAlign=TextAlign.Center)
        if(state.phase==Phase.CONNECTING && state.code.isNotEmpty()) {
            SelectionContainerCompat(state.code)
        }
        if(primary!=Action.NONE && primary!=Action.CONNECT) {
            ExpressiveButton(actionLabel(primary),accent,onAccent,Modifier.fillMaxWidth()) {
                when(primary){Action.AUTHORIZE->actions.authorize();Action.RETRY->actions.retry();Action.INSTALL->actions.install();else->Unit}
            }
        }
        if(state.busy) {
            TextButton(onClick=actions::cancel,colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFE0E7E8)),modifier=Modifier.heightIn(min=48.dp)) {Text("Cancel",style=MaterialTheme.typography.labelLarge)}
        } else if(state.phase==Phase.READY || state.phase==Phase.COMPLETED) {
            Text(if(state.phase==Phase.COMPLETED)"Confirmed by Android" else "Verified before installation. Yours to confirm.",
                style=MaterialTheme.typography.labelMedium,color=Color(0xFFBFC8CA),textAlign=TextAlign.Center)
        }
    }
}

@Composable private fun SelectionContainerCompat(code:String) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(code,style=MaterialTheme.typography.headlineSmall.copy(letterSpacing=2.sp),color=Color.White,
            modifier=Modifier.background(Color.White.copy(alpha=.08f),RoundedCornerShape(18.dp)).padding(horizontal=20.dp,vertical=12.dp)
                .semantics {contentDescription="GitHub authorization code $code"})
    }
}

@Composable private fun ConnectionCard(connected:Boolean,busy:Boolean,connecting:Boolean,client:String,onClient:(String)->Unit,advanced:Boolean,onAdvanced:()->Unit,
    actions:InstallerUi.Actions,wallpaper:Boolean,onWallpaper:(Boolean)->Unit) {
    val c=MaterialTheme.colorScheme;val motion=LocalMotionEnabled.current
    val shape=RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(c.surfaceContainer).border(1.dp,c.outlineVariant.copy(alpha=.35f),shape)
        .animateContentSize(if(motion)spring(.86f,380f) else snap()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(c.secondaryContainer,CircleShape),contentAlignment=Alignment.Center) {
                Glyph(if(connected)Phase.COMPLETED else Phase.CONNECTING,c.onSecondaryContainer,Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(if(connected)"GitHub connected" else "Connect your builds",style=MaterialTheme.typography.titleMedium)
                Text(if(connected)"samuelbeckman / expressive-clock" else "Secure access to your development builds.",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)
            }
            Box(Modifier.size(48.dp).expressiveTouch(CircleShape,onClick=onAdvanced).semantics {
                contentDescription="Connection settings";stateDescription=if(advanced)"Expanded" else "Collapsed"
            },contentAlignment=Alignment.Center) {
                Text(if(advanced)"−" else "⋯",style=MaterialTheme.typography.headlineSmall,color=c.onSurfaceVariant)
            }
        }
        if(!connected && !connecting) {
            Text(if(client.isNotBlank())"Sign in once. Your account stays on your device." else "Add your approved GitHub App client ID in connection settings.",
                style=MaterialTheme.typography.bodyMedium,color=c.onSurfaceVariant)
            ExpressiveButton(if(busy)"Please wait…" else "Connect GitHub",c.primary,c.onPrimary,Modifier.fillMaxWidth(),!busy && client.isNotBlank()) {actions.connect(client.trim())}
        }
        AnimatedVisibility(advanced,enter=if(motion)expandVertically(spring(.86f,380f))+fadeIn() else EnterTransition.None,
            exit=if(motion)shrinkVertically(tween(180))+fadeOut() else ExitTransition.None) {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color=c.outlineVariant.copy(alpha=.45f))
                Text("Connection settings",style=MaterialTheme.typography.titleSmall)
                OutlinedTextField(value=client,onValueChange=onClient,label={Text("GitHub App client ID")},
                    supportingText={Text("Public identifier, never a token or secret.")},singleLine=true,enabled=!busy,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth())
                if(connected) TextButton(onClick=actions::disconnect,modifier=Modifier.heightIn(min=48.dp)) {Text("Disconnect GitHub")}
                if(android.os.Build.VERSION.SDK_INT>=31) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Wallpaper accent",style=MaterialTheme.typography.bodyMedium,modifier=Modifier.weight(1f))
                    Switch(checked=wallpaper,onCheckedChange=onWallpaper,modifier=Modifier.semantics {contentDescription="Use wallpaper accent"})
                }
                Text("Only the approved repository is used. Downloads are checked before Android asks you to install.",style=MaterialTheme.typography.bodySmall,color=c.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun TrustNote() {
    Text("Authenticated downloads  ·  On-device checks\nYou always confirm installation in Android.",
        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,
        modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp))
}

@Composable internal fun ExpressiveButton(label:String,background:Color,foreground:Color,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit) {
    val shape=RoundedCornerShape(28.dp)
    Box(modifier.heightIn(min=56.dp).expressiveTouch(shape,enabled,onClick)
        .background(if(enabled)background else background.copy(alpha=.15f)).padding(horizontal=24.dp,vertical=16.dp),contentAlignment=Alignment.Center) {
        Text(label,style=MaterialTheme.typography.titleMedium,color=if(enabled)foreground else MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
    }
}

internal fun phaseLabel(phase:Phase)=when(phase){
    Phase.READY->"Ready";Phase.CONNECTING->"Connecting";Phase.DOWNLOADING->"Downloading";Phase.VERIFYING->"Verifying"
    Phase.READY_TO_INSTALL->"Verified";Phase.INSTALLING->"Installing";Phase.COMPLETED->"Complete";Phase.FAILED->"Needs attention"
}
internal fun statusTitle(state:UiSnapshot)=if(state.phase==Phase.READY && state.connected && state.hasRequest)
    "Ready when you are" else title(state.phase,state.connected)
internal fun title(phase:Phase,connected:Boolean)=when(phase){
    Phase.READY->if(connected)"You're all set" else "Your builds, connected"
    Phase.CONNECTING->"A secure connection";Phase.DOWNLOADING->"Downloading your build";Phase.VERIFYING->"Checking your APK"
    Phase.READY_TO_INSTALL->"Ready to install";Phase.INSTALLING->"Over to Android";Phase.COMPLETED->"Installation complete";Phase.FAILED->"Let's try that again"
}
internal fun description(phase:Phase,s:UiSnapshot)=when(phase){
    Phase.READY->if(s.connected && s.hasRequest)"Your selected build can be checked again whenever you’re ready." else if(s.connected)"Your next Expressive Clock build is just a notification away." else "Connect GitHub to receive your Expressive Clock builds."
    Phase.CONNECTING->if(s.code.isEmpty())"Preparing your GitHub sign-in." else "Enter this code on GitHub to approve read-only access."
    Phase.DOWNLOADING->"An authenticated download, straight from the approved Clock workflow."
    Phase.VERIFYING->"Checking the build, integrity and signing certificate."
    Phase.READY_TO_INSTALL->"Your build passed verification. Android will ask you to confirm the update."
    Phase.INSTALLING->"Follow Android's installation prompt. You stay in control."
    Phase.COMPLETED->"Your Expressive Clock debug build has been updated."
    Phase.FAILED->s.detail.ifBlank {"The request couldn't be completed. Please try again."}
}
private fun actionLabel(action:Action)=when(action){Action.AUTHORIZE->"Open GitHub authorization";Action.RETRY->"Retry download";Action.INSTALL->"Install update";else->"Connect GitHub"}
