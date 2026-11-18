package app.expressiveclock.installer

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import android.os.Build

// Clock Concept palette: teal tonal relationships; Island's neutral page/surface hierarchy.
internal val InstallerLight = lightColorScheme(
    primary=Color(0xFF006874), onPrimary=Color.White,
    primaryContainer=Color(0xFF97F0FF), onPrimaryContainer=Color(0xFF001F24),
    secondaryContainer=Color(0xFFCDE7EC), onSecondaryContainer=Color(0xFF051F24),
    background=Color(0xFFFAFAFC), onBackground=Color(0xFF171D1E),
    surface=Color(0xFFFAFAFC), onSurface=Color(0xFF171D1E),
    surfaceContainer=Color(0xFFEFF4F5), surfaceContainerHigh=Color(0xFFE3E9EA),
    onSurfaceVariant=Color(0xFF3F484A), outlineVariant=Color(0xFFBFC8CA),
)
internal val InstallerDark = darkColorScheme(
    primary=Color(0xFF4FD8EB), onPrimary=Color(0xFF00363D),
    primaryContainer=Color(0xFF004F58), onPrimaryContainer=Color(0xFF97F0FF),
    secondaryContainer=Color(0xFF334B50), onSecondaryContainer=Color(0xFFCDE7EC),
    background=Color(0xFF101416), onBackground=Color(0xFFE0E7E8),
    surface=Color(0xFF101416), onSurface=Color(0xFFE0E7E8),
    surfaceContainer=Color(0xFF1A2021), surfaceContainerHigh=Color(0xFF242A2B),
    onSurfaceVariant=Color(0xFFBFC8CA), outlineVariant=Color(0xFF3F484A),
)

// Adapted from Clock Type.kt: real variable weights + explicit optical size, no Roboto tracking.
private fun text(size:Int,height:Int,weight:Int=400): TextStyle = TextStyle(
    fontFamily=FontFamily(listOf(400,500,600,700).map { w ->
        Font(R.font.google_sans_flex,FontWeight(w),variationSettings=FontVariation.Settings(
            FontVariation.weight(w),FontVariation.Setting("opsz",size.toFloat().coerceIn(6f,144f))))
    }), fontSize=size.sp,lineHeight=height.sp,fontWeight=FontWeight(weight),letterSpacing=0.sp,
)
private val InstallerTypography=Typography(
    displayLarge=text(57,64),displayMedium=text(45,52,500),displaySmall=text(36,44,500),
    headlineLarge=text(32,40,500),headlineMedium=text(28,36,500),headlineSmall=text(24,32,500),
    titleLarge=text(22,28,500),titleMedium=text(16,24,500),titleSmall=text(14,20,500),
    bodyLarge=text(16,24),bodyMedium=text(14,20),bodySmall=text(12,16),
    labelLarge=text(14,20,500),labelMedium=text(12,16,500),labelSmall=text(11,16,500),
    displayLargeEmphasized=text(57,64,500),displayMediumEmphasized=text(45,52,500),displaySmallEmphasized=text(36,44,500),
    headlineLargeEmphasized=text(32,40,500),headlineMediumEmphasized=text(28,36,500),headlineSmallEmphasized=text(24,32,500),
    titleLargeEmphasized=text(22,28,500),titleMediumEmphasized=text(16,24,600),titleSmallEmphasized=text(14,20,600),
    bodyLargeEmphasized=text(16,24,500),bodyMediumEmphasized=text(14,20,500),bodySmallEmphasized=text(12,16,500),
    labelLargeEmphasized=text(14,20,600),labelMediumEmphasized=text(12,16,600),labelSmallEmphasized=text(11,16,600),
)
internal val LocalMotionEnabled=staticCompositionLocalOf { true }

@Composable
internal fun rememberMotionEnabled():Boolean {
    val context=LocalContext.current
    var enabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(context) {
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange:Boolean){ enabled=ValueAnimator.areAnimatorsEnabled() }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

@Composable
internal fun InstallerTheme(dark:Boolean=isSystemInDarkTheme(), wallpaper:Boolean=false,content:@Composable ()->Unit) {
    val base=if(dark)InstallerDark else InstallerLight
    val context=LocalContext.current
    val colors=if(wallpaper && Build.VERSION.SDK_INT>=31) {
        val dynamic=if(dark)dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // Wallpaper changes accents only, preserving the family’s neutral surfaces and hierarchy.
        base.copy(primary=dynamic.primary,onPrimary=dynamic.onPrimary,
            primaryContainer=dynamic.primaryContainer,onPrimaryContainer=dynamic.onPrimaryContainer)
    } else base
    CompositionLocalProvider(LocalMotionEnabled provides rememberMotionEnabled()) {
        MaterialExpressiveTheme(colorScheme=colors,typography=InstallerTypography,
            motionScheme=MotionScheme.expressive(),content=content)
    }
}
