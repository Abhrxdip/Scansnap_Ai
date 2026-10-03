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
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    val quickSuggestions = listOf(
        "🍜 Is Maggi available?",
        "🔍 Where else can I find Maggi?",
        "🥛 What is the price of Amul milk?",
        "🍪 Show me biscuits"
    )

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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeuBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "ScanSnap Copilot",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = NeuBlack
                                )
                                NeuBadge(
                                    text = "AI",
                                    backgroundColor = NeuYellow,
                                    textColor = NeuBlack
                                )
                            }
                            Text(
                                text = "Deterministic Store & Inventory AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NeuGray
                            )
                        }
                    }
                }
            }
        },
        containerColor = NeuBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Quick Suggestion Chips Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    val cleanText = suggestion.substringAfter(" ")
                    Box(
                        modifier = Modifier
                            .neuShadow(2.dp, 2.dp, NeuBlack, 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeuSurface)
                            .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.sendMessage(cleanText)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = suggestion,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NeuBlack
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = NeuBlack.copy(alpha = 0.2f))

            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = true
            ) {
                items(uiState.messages.reversed(), key = { it.id }) { message ->
                    ChatMessageItem(message = message)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = NeuBlue,
                    trackColor = NeuSurface
                )
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE4E6)),
                    border = BorderStroke(1.5.dp, NeuRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = NeuRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        NeuButton(
                            text = "Retry",
                            onClick = { viewModel.retryLastMessage() },
                            backgroundColor = NeuYellow,
                            textColor = NeuBlack,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }

            // Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeuSurface)
                    .border(BorderStroke(2.dp, NeuBlack))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Ask stock, price, or find elsewhere...",
                                fontSize = 13.sp,
                                color = NeuGray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeuBlack,
                            unfocusedBorderColor = NeuBlack.copy(alpha = 0.5f),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !uiState.isLoading,
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (inputText.isNotBlank() && !uiState.isLoading) NeuGreen else NeuGray.copy(alpha = 0.3f))
                            .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                            .clickable(enabled = !uiState.isLoading && inputText.isNotBlank()) {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText.trim())
                                    inputText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = NeuBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .neuShadow(1.dp, 1.dp, NeuBlack, 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeuYellow)
                    .border(BorderStroke(1.5.dp, NeuBlack), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🤖", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .neuShadow(2.dp, 2.dp, NeuBlack, 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUser) NeuBlue else NeuSurface)
                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else NeuBlack,
                fontWeight = if (isUser) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
