package com.smartvendor.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NeuBadge(
                text = "🚀 INTELLIGENT RETAIL OS",
                backgroundColor = NeuYellow,
                textColor = NeuBlack,
                shadowOffset = 2.dp
            )

            Text(
                text = "Welcome to\nSmartVendor AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NeuBlack,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )

            Text(
                text = "Empowering retail stores with camera-driven AI checkout, real-time inventory, and automated billing.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = NeuGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Feature Highlights
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuSurface,
                shadowOffset = 5.dp,
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRow(
                        icon = Icons.Default.CameraAlt,
                        iconBg = NeuYellow,
                        title = "Visual AI Recognition",
                        desc = "Instantly detect multiple products simultaneously with CameraX"
                    )
                    FeatureRow(
                        icon = Icons.Default.Inventory,
                        iconBg = NeuGreen,
                        title = "Smart Inventory Tracking",
                        desc = "Automatic stock depletion and urgent restocking alarms"
                    )
                    FeatureRow(
                        icon = Icons.Default.Analytics,
                        iconBg = NeuPurple,
                        title = "AI Sales Intelligence",
                        desc = "Real-time revenue metrics, profit charts, and replenishment tips"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            NeuButton(
                text = "Get Started Now →",
                onClick = onFinishOnboarding,
                backgroundColor = NeuBlue,
                textColor = NeuWhite,
                modifier = Modifier.fillMaxWidth(),
                shadowOffset = 4.dp,
                cornerRadius = 14.dp
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NeuBlack, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = NeuBlack
            )
            Text(
                text = desc,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = NeuGray
            )
        }
    }
}
