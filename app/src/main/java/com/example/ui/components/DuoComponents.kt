package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DuoBorder
import com.example.ui.theme.DuoGreen
import com.example.ui.theme.DuoGreenDark
import com.example.ui.theme.DuoInk
import com.example.ui.theme.DuoSurface1
import com.example.ui.theme.DuoSurface2

@Composable
fun DuoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DuoGreen,
    shadowColor: Color = DuoGreenDark,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    testTag: String = "",
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val offsetBy by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        label = "button_press_offset"
    )
    val shadowHeight = if (isPressed && enabled) 0.dp else 4.dp

    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    com.example.audio.DuoSoundPlayer.playClick()
                    onClick()
                }
            )
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(
                    color = if (enabled) shadowColor else DuoSurface2,
                    shape = RoundedCornerShape(14.dp)
                )
        )

        // Button body
        Row(
            modifier = Modifier
                .offset(y = offsetBy)
                .background(
                    color = if (enabled) backgroundColor else DuoSurface1,
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    BorderStroke(2.dp, if (enabled) Color.Transparent else DuoBorder),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun DuoProgressBar(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    fillColor: Color = DuoGreen,
    backgroundColor: Color = DuoSurface2,
    height: Dp = 16.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor, shape = RoundedCornerShape(99.dp))
            .border(BorderStroke(2.dp, DuoBorder), shape = RoundedCornerShape(99.dp))
            .clip(RoundedCornerShape(99.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .fillMaxHeight()
                .background(fillColor)
        )
    }
}

@Composable
fun DuoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = DuoBorder,
    shadowColor: Color = DuoBorder,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .testTag(testTag)
            .padding(bottom = 4.dp) // Leave space for shadow
    ) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(shadowColor, shape = RoundedCornerShape(16.dp))
        )

        // Main Surface
        Column(
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                .border(BorderStroke(2.dp, borderColor), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

// Ensure ColumnScope is available for DuoCard
typealias ColumnScope = androidx.compose.foundation.layout.ColumnScope

@Composable
fun DuoDumbbellLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(120.dp)) {
        val w = size.width
        val h = size.height

        // 1. Draw central steel bar
        // Gray bar
        drawRoundRect(
            color = Color(0xFFBDBDBD),
            topLeft = Offset(w * 0.2f, h * 0.43f),
            size = Size(w * 0.6f, h * 0.14f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Draw steel bar highlights
        drawRoundRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(w * 0.2f, h * 0.43f),
            size = Size(w * 0.6f, h * 0.05f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // 2. Left Plate shadow (Duo Green Dark: #58A700)
        drawRoundRect(
            color = Color(0xFF58A700),
            topLeft = Offset(w * 0.05f, h * 0.22f),
            size = Size(w * 0.25f, h * 0.6f),
            cornerRadius = CornerRadius(24f, 24f)
        )
        // Left Plate (Duo Green: #58CC02)
        drawRoundRect(
            color = Color(0xFF58CC02),
            topLeft = Offset(w * 0.05f, h * 0.18f),
            size = Size(w * 0.25f, h * 0.6f),
            cornerRadius = CornerRadius(24f, 24f)
        )

        // 3. Right Plate shadow (Duo Green Dark: #58A700)
        drawRoundRect(
            color = Color(0xFF58A700),
            topLeft = Offset(w * 0.7f, h * 0.22f),
            size = Size(w * 0.25f, h * 0.6f),
            cornerRadius = CornerRadius(24f, 24f)
        )
        // Right Plate (Duo Green: #58CC02)
        drawRoundRect(
            color = Color(0xFF58CC02),
            topLeft = Offset(w * 0.7f, h * 0.18f),
            size = Size(w * 0.25f, h * 0.6f),
            cornerRadius = CornerRadius(24f, 24f)
        )

        // 4. Owl Eyes!
        // Left Eye (outer white)
        drawCircle(
            color = Color.White,
            radius = w * 0.09f,
            center = Offset(w * 0.175f, h * 0.44f)
        )
        // Left pupil (black)
        drawCircle(
            color = Color(0xFF3C3C3C),
            radius = w * 0.045f,
            center = Offset(w * 0.175f, h * 0.44f)
        )
        // Left pupil highlight
        drawCircle(
            color = Color.White,
            radius = w * 0.015f,
            center = Offset(w * 0.16f, h * 0.42f)
        )

        // Right Eye (outer white)
        drawCircle(
            color = Color.White,
            radius = w * 0.09f,
            center = Offset(w * 0.825f, h * 0.44f)
        )
        // Right pupil (black)
        drawCircle(
            color = Color(0xFF3C3C3C),
            radius = w * 0.045f,
            center = Offset(w * 0.825f, h * 0.44f)
        )
        // Right pupil highlight
        drawCircle(
            color = Color.White,
            radius = w * 0.015f,
            center = Offset(w * 0.81f, h * 0.42f)
        )

        // 5. Owl Beak (Orange: #FF9600) on the central bar
        val beakPath = Path().apply {
            moveTo(w * 0.5f - w * 0.04f, h * 0.47f) // left
            lineTo(w * 0.5f + w * 0.04f, h * 0.47f) // right
            lineTo(w * 0.5f, h * 0.56f) // bottom point
            close()
        }
        drawPath(
            path = beakPath,
            color = Color(0xFFFF9600)
        )
    }
}
