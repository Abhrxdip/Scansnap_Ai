package com.smartvendor.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.ui.components.BarChart
import com.smartvendor.ai.ui.components.PieChart
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuSurface)
                    .border(BorderStroke(2.5.dp, NeuBlack))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                .clickable { onNavigateBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeuBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Sales Intelligence",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = NeuBlack
                            )
                            Text(
                                text = "Live Business Analytics & Trends",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }

                    NeuBadge(
                        text = "AI METRICS",
                        backgroundColor = NeuPurple,
                        textColor = NeuBlack
                    )
                }
            }
        },
        containerColor = NeuBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Range Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf("Today", "Yesterday", "Last 7 Days", "Last 30 Days")) { range ->
                            val isSelected = uiState.selectedTimeRange == range
                            Box(
                                modifier = Modifier
                                    .neuShadow(
                                        offsetX = if (isSelected) 3.dp else 1.5.dp,
                                        offsetY = if (isSelected) 3.dp else 1.5.dp,
                                        cornerRadius = 8.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeuBlue else NeuSurface)
                                    .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectTimeRange(range) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = range,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else NeuBlack
                                )
                            }
                        }
                    }
                }

                // 2x2 Metric KPI Overview Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Total Revenue",
                            value = "₹${"%.2f".format(uiState.totalRevenue)}",
                            icon = Icons.Outlined.Payments,
                            badgeColor = NeuGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Completed Bills",
                            value = "${uiState.totalTransactions}",
                            icon = Icons.Outlined.ReceiptLong,
                            badgeColor = NeuYellow,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Avg Ticket Size",
                            value = "₹${"%.2f".format(uiState.averageBillValue)}",
                            icon = Icons.Outlined.TrendingUp,
                            badgeColor = NeuCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Top Performer",
                            value = uiState.bestSellingProduct.ifBlank { "Maggi" },
                            icon = Icons.Outlined.Star,
                            badgeColor = NeuPink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Smart AI Inventory Restock Recommendations
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NeuSurface,
                        shadowOffset = 4.dp,
                        cornerRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🧠", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "AI Restock Recommendations",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = NeuBlack
                                        )
                                        Text(
                                            text = "Based on sales velocity & checkout depletion",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = NeuGray
                                        )
                                    }
                                }
                                NeuBadge(text = "AUTOPILOT", backgroundColor = NeuPurple, textColor = NeuBlack)
                            }

                            if (uiState.stockRecommendations.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    uiState.stockRecommendations.forEach { rec ->
                                        NeuCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            backgroundColor = when (rec.urgencyLevel) {
                                                "HIGH" -> Color(0xFFFFE4E6)
                                                "MEDIUM" -> Color(0xFFFEF3C7)
                                                else -> NeuBackground
                                            },
                                            shadowOffset = 2.dp,
                                            cornerRadius = 10.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = rec.productName.replaceFirstChar { it.uppercase() },
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 15.sp,
                                                        color = NeuBlack
                                                    )
                                                    NeuBadge(
                                                        text = rec.salesVelocity,
                                                        backgroundColor = when (rec.urgencyLevel) {
                                                            "HIGH" -> NeuRed
                                                            "MEDIUM" -> NeuYellow
                                                            else -> NeuBlue
                                                        },
                                                        textColor = if (rec.urgencyLevel == "HIGH") Color.White else NeuBlack,
                                                        shadowOffset = 1.dp
                                                    )
                                                }

                                                Text(
                                                    text = "Peak: ${rec.peakWindow}  •  Stock Left: ${rec.currentStock}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (rec.currentStock <= 5) NeuRed else NeuBlack
                                                )

                                                Text(
                                                    text = rec.reasoning,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = NeuGray,
                                                    lineHeight = 15.sp
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    NeuButton(
                                                        text = "+${rec.recommendedReorder} Restock",
                                                        onClick = { viewModel.restockProduct(rec.productId, rec.recommendedReorder) },
                                                        backgroundColor = NeuGreen,
                                                        textColor = NeuBlack,
                                                        shadowOffset = 2.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "🎉", fontSize = 18.sp)
                                    Text(
                                        text = "All products have healthy inventory levels!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = NeuBlack
                                    )
                                }
                            }
                        }
                    }
                }

                // Cross-Vendor Market Intelligence Section
                if (uiState.marketTrends.isNotEmpty()) {
                    item {
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeuSurface,
                            shadowOffset = 4.dp,
                            cornerRadius = 14.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(text = "🌐", fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = "Retail Market Trends",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = NeuBlack
                                            )
                                            Text(
                                                text = "FMCG opportunities across micro-retailers",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = NeuGray
                                            )
                                        }
                                    }
                                    NeuBadge(text = "HOT DEMAND", backgroundColor = NeuYellow, textColor = NeuBlack)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    uiState.marketTrends.forEach { trend ->
                                        NeuCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            backgroundColor = NeuBackground,
                                            shadowOffset = 2.dp,
                                            cornerRadius = 10.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = trend.title,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 14.sp,
                                                        color = NeuBlack,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    NeuBadge(
                                                        text = trend.badgeLabel,
                                                        backgroundColor = if (trend.actionType == "ADD_PRODUCT") NeuYellow else NeuPurple,
                                                        textColor = NeuBlack,
                                                        shadowOffset = 1.dp
                                                    )
                                                }

                                                Text(
                                                    text = trend.description,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = NeuGray,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Revenue Trend Chart Card
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NeuSurface,
                        shadowOffset = 4.dp,
                        cornerRadius = 14.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Revenue Velocity (₹)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = NeuBlack
                                )
                                NeuBadge(text = "DAILY CHART", backgroundColor = NeuGreen, textColor = NeuBlack)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            BarChart(dataPoints = uiState.revenueDataPoints)
                        }
                    }
                }

                // Category Distribution Chart Card
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = NeuSurface,
                        shadowOffset = 4.dp,
                        cornerRadius = 14.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Category Distribution",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = NeuBlack
                                )
                                NeuBadge(text = "BREAKDOWN", backgroundColor = NeuCyan, textColor = NeuBlack)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            PieChart(categoryData = uiState.categoryDistribution)
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NeuBlue
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    NeuCard(
        modifier = modifier.height(115.dp),
        backgroundColor = NeuSurface,
        shadowOffset = 3.dp,
        cornerRadius = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NeuGray
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = NeuBlack, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlack,
                maxLines = 1
            )
        }
    }
}
