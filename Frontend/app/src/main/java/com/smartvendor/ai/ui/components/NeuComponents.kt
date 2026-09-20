package com.smartvendor.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvendor.ai.ui.theme.*

/**
 * Modifier that draws a crisp, solid Neubrutalist offset shadow.
 */
fun Modifier.neuShadow(
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp,
    shadowColor: Color = NeuBlack,
    cornerRadius: Dp = 12.dp
): Modifier = this.drawBehind {
    val cornerPx = cornerRadius.toPx()
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerPx, cornerPx)
    )
}

/**
 * Neubrutalist Card with solid black border and hard offset drop shadow.
 */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeuSurface,
    borderColor: Color = NeuBlack,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .neuShadow(
                offsetX = shadowOffset,
                offsetY = shadowOffset,
                shadowColor = NeuBlack,
                cornerRadius = cornerRadius
            )
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .then(clickModifier)
    ) {
        content()
    }
}

/**
 * Neubrutalist Action Button with solid ink border, offset shadow and press animation.
 */
@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeuBlue,
    textColor: Color = Color.White,
    borderColor: Color = NeuBlack,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shadowOffset: Dp = 4.dp,
    cornerRadius: Dp = 10.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(cornerRadius)

    val currentShadow = if (isPressed) 1.dp else shadowOffset
    val currentOffset = if (isPressed) 2.dp else 0.dp

    Box(
        modifier = modifier
            .offset(x = currentOffset, y = currentOffset)
            .neuShadow(
                offsetX = currentShadow,
                offsetY = currentShadow,
                shadowColor = NeuBlack,
                cornerRadius = cornerRadius
            )
            .clip(shape)
            .background(if (enabled) backgroundColor else Color.LightGray)
            .border(BorderStroke(2.5.dp, borderColor), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) textColor else Color.DarkGray,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

/**
 * Neubrutalist Pill Badge with solid black outline and bold uppercase text.
 */
@Composable
fun NeuBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeuYellow,
    textColor: Color = NeuBlack,
    borderColor: Color = NeuBlack,
    shadowOffset: Dp = 2.dp
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .neuShadow(offsetX = shadowOffset, offsetY = shadowOffset, shadowColor = NeuBlack, cornerRadius = 8.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.8.dp, borderColor), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}
