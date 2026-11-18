package app.expressiveclock.installer

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.Role

/** Clock Glass.kt's .95 press / 420,.62 release spring; Island's 160/280ms concentrated bloom.
 * Passive pointer observation leaves clicks, scrolling and accessibility activation in control. */
internal fun Modifier.expressiveTouch(shape:Shape,enabled:Boolean=true,onClick:(()->Unit)?=null):Modifier=composed {
    val motion=LocalMotionEnabled.current
    val interactions=remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    var finger by remember { mutableStateOf<Offset?>(null) }
    var last by remember { mutableStateOf<Offset?>(null) }
    val down=enabled && (pressed || finger!=null)
    val scale by animateFloatAsState(if(down && onClick!=null) .96f else 1f,
        if(!motion)snap() else if(down)tween(90) else spring(0.62f,420f),label="pressDetent")
    val alpha by animateFloatAsState(if(down)1f else 0f,
        if(motion)tween(if(down)160 else 280) else snap(),label="touchLight")
    this.graphicsLayer { scaleX=scale;scaleY=scale }.clip(shape)
        .pointerInput(enabled) {
            try {
                awaitPointerEventScope {
                    while(true){
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        finger=if(enabled)event.changes.firstOrNull { it.pressed }?.position else null
                        if(finger!=null)last=finger
                    }
                }
            } finally {finger=null}
        }.then(if(onClick!=null) Modifier.clickable(interactionSource=interactions,indication=null,
            enabled=enabled,role=Role.Button,onClick=onClick) else Modifier)
        .drawWithContent {
            drawContent()
            if(alpha>.001f) drawRect(Brush.radialGradient(
                listOf(Color.White.copy(alpha=.22f*alpha),Color(0xFF80D8FF).copy(alpha=.10f*alpha),Color.Transparent),
                center=finger?:last?:center,radius=(size.minDimension*.58f).coerceAtLeast(1f)))
        }
}

/** Exact studio-light stops/illumination from Island NotificationCharcoalSurface.kt. */
internal fun Modifier.studioCharcoal()=drawWithCache {
    val base=Brush.verticalGradient(0f to Color(0xFF25292B),.35f to Color(0xFF1B1F21),
        .72f to Color(0xFF111517),1f to Color(0xFF0B0E10),endY=size.height.coerceAtLeast(1f))
    val illumination=Brush.radialGradient(listOf(Color.White.copy(alpha=.025f),Color.Transparent),
        center=Offset(size.width*.46f,-size.height*.32f),radius=size.maxDimension.coerceAtLeast(1f)*.95f)
    onDrawBehind {drawRect(base);drawRect(illumination)}
}
