package app.expressiveclock.installer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import app.expressiveclock.installer.Presentation.Phase

@Composable internal fun ProgressEmblem(state:UiSnapshot,accent:Color,diameter:Dp) {
    val motion=LocalMotionEnabled.current
    val actual=Presentation.percent(state.progress)
    val determinate=state.phase==Phase.DOWNLOADING && actual>=0
    val progress by animateFloatAsState(if(determinate)actual/100f else 0f,
        if(motion)spring(1f,500f) else snap(),label="actualDownloadProgress")
    val working=state.phase in listOf(Phase.CONNECTING,Phase.DOWNLOADING,Phase.VERIFYING,Phase.INSTALLING)
    val rotation:State<Float> = if(working && !determinate && motion) {
        val infinite=rememberInfiniteTransition(label="workingIndicator")
        infinite.animateFloat(0f,360f,infiniteRepeatable(tween(if(state.phase==Phase.VERIFYING)1800 else 2400,easing=LinearEasing)),label="orbit")
    } else rememberUpdatedState(0f)
    val entrance=remember { Animatable(1f) }
    LaunchedEffect(state.phase,motion) {
        if(motion) {entrance.snapTo(.93f);entrance.animateTo(1f,spring(.68f,380f))} else entrance.snapTo(1f)
    }
    Box(Modifier.size(diameter).then(if(determinate)Modifier.semantics {
        progressBarRangeInfo=ProgressBarRangeInfo(actual.toFloat(),0f..100f)
        contentDescription="Download progress"
    } else Modifier),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val angle=rotation.value // Draw-time read: the orbit does not recompose the card each frame.
            val stroke=5.dp.toPx();val inset=stroke/2+4.dp.toPx()
            val top=Offset(inset,inset);val circle=Size(size.width-2*inset,size.height-2*inset)
            drawArc(Color.White.copy(alpha=.09f),-90f,360f,false,top,circle,style=Stroke(stroke,cap=StrokeCap.Round))
            if(determinate) drawArc(accent,-90f,360f*progress.coerceIn(0f,1f),false,top,circle,style=Stroke(stroke,cap=StrokeCap.Round))
            else if(working) {
                if(state.phase==Phase.VERIFYING) {
                    // Clock’s segmented dial language, rotated as an indeterminate verification indicator.
                    repeat(12){i->drawArc(accent.copy(alpha=.15f+.75f*i/11f),angle+i*30f,12f,false,top,circle,style=Stroke(stroke,cap=StrokeCap.Round))}
                } else drawArc(accent,angle-90f,80f,false,top,circle,style=Stroke(stroke,cap=StrokeCap.Round))
            } else if(state.phase==Phase.COMPLETED || state.phase==Phase.READY_TO_INSTALL) {
                drawArc(accent,-90f,360f,false,top,circle,style=Stroke(stroke,cap=StrokeCap.Round))
            }
        }
        Box(Modifier.graphicsLayer {scaleX=entrance.value;scaleY=entrance.value},contentAlignment=Alignment.Center) {
            if(determinate) RollingPercentage(actual,accent)
            else Glyph(state.phase,if(state.phase==Phase.FAILED)Color(0xFFFFB4AB) else accent,Modifier.size(diameter*.38f))
        }
    }
}

/** Clock TickerCell / Island MechanicalDigit: stable measured cells, 38ms cadence, 380/.68 spring
 * and 3.5dp progressive blur. Values jump directly to latest real percentage, never invented steps. */
@Composable private fun RollingPercentage(percent:Int,color:Color) {
    val motion=LocalMotionEnabled.current
    val style=MaterialTheme.typography.displayMedium.copy(fontSize=42.sp,lineHeight=52.sp,fontWeight=FontWeight(400),fontFeatureSettings="tnum")
    val measure=rememberTextMeasurer()
    val metrics=measure.measure("0",style)
    val width=with(LocalDensity.current){metrics.size.width.toDp()}
    val cellHeight=with(LocalDensity.current){metrics.size.height.toDp()}+16.dp
    val value=percent.toString().padStart(3,' ')
    val blankCells=value.takeWhile {it==' '}.length
    val centering by animateFloatAsState(-blankCells*width.value/2f,
        if(motion)spring(.86f,420f) else snap(),label="numberOpticalCenter")
    val pixels=LocalDensity.current.density
    Row(Modifier.graphicsLayer {translationX=centering*pixels}.clearAndSetSemantics {testTag="percentageDigits"},verticalAlignment=Alignment.CenterVertically) {
        value.forEachIndexed { index,char->
            var shown by remember { mutableStateOf(char) }
            LaunchedEffect(char,motion) {if(motion)delay((2-index)*38L);shown=char}
            AnimatedContent(shown,modifier=Modifier.width(width).height(cellHeight).clipToBounds(),
                transitionSpec={
                    if(motion && initialState!=' ' && targetState!=' ')
                        (slideInVertically(spring(.68f,380f)){it}+fadeIn(tween(90))) togetherWith
                            (slideOutVertically(tween(180,easing=FastOutSlowInEasing)){-it}+fadeOut(tween(70)))
                    else EnterTransition.None togetherWith ExitTransition.None
                },contentAlignment=Alignment.Center,label="progressDigit$index") { digit->
                val blur by transition.animateFloat(transitionSpec={if(motion)tween(180) else snap()},label="digitMotionBlur") {
                    if(it==EnterExitState.Visible)0f else 3.5f
                }
                Text(digit.toString(),style=style,color=color,modifier=Modifier.blur(blur.dp).padding(vertical=4.dp))
            }
        }
        Text("%",style=MaterialTheme.typography.titleLarge,color=color,modifier=Modifier.padding(start=2.dp))
    }
}

/** Same rounded route/tray glyph as the helper launcher, plus simple Clock-style check/shield.
 * Drawn as vector paths at any density; no emoji, external artwork or icon font. */
@Composable internal fun Glyph(phase:Phase,color:Color,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val k=size.minDimension/48f
        val stroke=Stroke(3f*k,cap=StrokeCap.Round,join=StrokeJoin.Round)
        fun path(draw:Path.()->Unit){drawPath(Path().apply(draw),color,style=stroke)}
        when(phase) {
            Phase.COMPLETED,Phase.READY_TO_INSTALL->path {moveTo(10*k,25*k);lineTo(20*k,35*k);lineTo(38*k,14*k)}
            Phase.FAILED->{path {moveTo(24*k,10*k);lineTo(24*k,27*k)};drawCircle(color,2*k,Offset(24*k,36*k))}
            Phase.VERIFYING->path {moveTo(24*k,5*k);lineTo(40*k,12*k);lineTo(38*k,29*k);quadraticTo(35*k,39*k,24*k,44*k);quadraticTo(13*k,39*k,10*k,29*k);lineTo(8*k,12*k);close();moveTo(16*k,24*k);lineTo(22*k,30*k);lineTo(32*k,19*k)}
            Phase.CONNECTING->{drawCircle(color,8*k,Offset(17*k,19*k),style=stroke);drawCircle(color,8*k,Offset(31*k,29*k),style=stroke)}
            Phase.INSTALLING->path {moveTo(12*k,32*k);lineTo(12*k,39*k);lineTo(36*k,39*k);lineTo(36*k,32*k);moveTo(24*k,30*k);lineTo(24*k,9*k);moveTo(16*k,17*k);lineTo(24*k,9*k);lineTo(32*k,17*k)}
            else->path {moveTo(24*k,8*k);lineTo(24*k,30*k);moveTo(16*k,22*k);lineTo(24*k,30*k);lineTo(32*k,22*k);moveTo(12*k,33*k);lineTo(12*k,40*k);lineTo(36*k,40*k);lineTo(36*k,33*k)}
        }
    }
}
