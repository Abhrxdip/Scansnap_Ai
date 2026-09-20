package com.smartvendor.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSuccessScreen(
    billId: String,
    onNavigateHome: () -> Unit
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
            // Success Icon Bubble
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .neuShadow(4.dp, 4.dp, NeuBlack, 45.dp)
                    .clip(CircleShape)
                    .background(NeuGreen)
                    .border(BorderStroke(3.dp, NeuBlack), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Success",
                    tint = NeuBlack,
                    modifier = Modifier.size(50.dp)
                )
            }

            Text(
                text = "Bill Generated\nSuccessfully!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = NeuBlack,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuSurface,
                shadowOffset = 5.dp,
                cornerRadius = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "INVOICE REFERENCE",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeuGray
                    )
                    NeuBadge(
                        text = billId,
                        backgroundColor = NeuYellow,
                        textColor = NeuBlack,
                        shadowOffset = 2.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The transaction has been recorded in your local database and the inventory updated.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NeuGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NeuButton(
                text = "📤 Share Digital Receipt",
                onClick = { /* Share receipt action */ },
                backgroundColor = NeuYellow,
                textColor = NeuBlack,
                modifier = Modifier.fillMaxWidth(),
                shadowOffset = 3.dp,
                cornerRadius = 12.dp
            )

            NeuButton(
                text = "Return to Dashboard →",
                onClick = onNavigateHome,
                backgroundColor = NeuBlue,
                textColor = NeuWhite,
                modifier = Modifier.fillMaxWidth(),
                shadowOffset = 4.dp,
                cornerRadius = 12.dp
            )
        }
    }
}
