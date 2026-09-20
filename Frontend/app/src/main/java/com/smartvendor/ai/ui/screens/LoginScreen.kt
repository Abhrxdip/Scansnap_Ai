package com.smartvendor.ai.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartvendor.ai.R
import com.smartvendor.ai.ui.components.NeuBadge
import com.smartvendor.ai.ui.components.NeuButton
import com.smartvendor.ai.ui.components.NeuCard
import com.smartvendor.ai.ui.components.neuShadow
import com.smartvendor.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Badge
            NeuBadge(
                text = "⚡ NEXT-GEN AI POS",
                backgroundColor = NeuYellow,
                textColor = NeuBlack,
                shadowOffset = 2.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SmartVendor AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = NeuBlack
            )
            Text(
                text = stringResource(id = R.string.tagline),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeuGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Neubrutalist Card
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeuSurface,
                shadowOffset = 6.dp,
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab Selector: Login / Register
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.authMode == AuthMode.LOGIN) NeuYellow else NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAuthMode(AuthMode.LOGIN) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeuBlack
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .neuShadow(2.dp, 2.dp, NeuBlack, 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.authMode == AuthMode.REGISTER) NeuBlue else NeuSurface)
                                .border(BorderStroke(2.dp, NeuBlack), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAuthMode(AuthMode.REGISTER) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = if (uiState.authMode == AuthMode.REGISTER) NeuWhite else NeuBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    AnimatedContent(
                        targetState = uiState.authMode,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "AuthModeSwitch"
                    ) { mode ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            // Name field (Register only)
                            if (mode == AuthMode.REGISTER) {
                                OutlinedTextField(
                                    value = uiState.nameInput,
                                    onValueChange = { viewModel.onNameChanged(it) },
                                    label = { Text("Store / Full Name", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeuBlack) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Email
                            OutlinedTextField(
                                value = uiState.emailInput,
                                onValueChange = { viewModel.onEmailChanged(it) },
                                label = { Text("Email Address", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeuBlack) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Password
                            OutlinedTextField(
                                value = uiState.passwordInput,
                                onValueChange = { viewModel.onPasswordChanged(it) },
                                label = { Text("Password", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeuBlack) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = NeuBlack
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Confirm Password (Register only)
                            if (mode == AuthMode.REGISTER) {
                                OutlinedTextField(
                                    value = uiState.confirmPasswordInput,
                                    onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                                    label = { Text("Confirm Password", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeuBlack) },
                                    trailingIcon = {
                                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = NeuBlack
                                            )
                                        }
                                    },
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Forgot password (Login only)
                            if (mode == AuthMode.LOGIN) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        color = NeuBlue,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .clickable { viewModel.openForgotPasswordDialog() }
                                            .padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Primary CTA Button
                            NeuButton(
                                text = if (uiState.isLoading) "Processing..." else if (mode == AuthMode.LOGIN) "Login →" else "Create Account →",
                                onClick = {
                                    if (mode == AuthMode.LOGIN) {
                                        viewModel.performLogin(onLoginSuccess)
                                    } else {
                                        viewModel.performRegister(onLoginSuccess)
                                    }
                                },
                                enabled = !uiState.isLoading,
                                backgroundColor = if (mode == AuthMode.LOGIN) NeuBlue else NeuGreen,
                                textColor = if (mode == AuthMode.LOGIN) NeuWhite else NeuBlack,
                                modifier = Modifier.fillMaxWidth(),
                                shadowOffset = 4.dp,
                                cornerRadius = 12.dp
                            )
                        }
                    }

                    // Error Notification inline
                    if (uiState.errorMessage != null) {
                        NeuCard(
                            backgroundColor = NeuRed,
                            shadowOffset = 3.dp,
                            cornerRadius = 10.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = NeuWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    color = NeuWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.clearError() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (uiState.showResetPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = uiState.emailInput,
            onDismiss = { viewModel.closeForgotPasswordDialog() },
            onSend = { email -> viewModel.sendPasswordReset(email) }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(BorderStroke(3.dp, NeuBlack), RoundedCornerShape(16.dp))
            .neuShadow(6.dp, 6.dp, NeuBlack, 16.dp),
        title = {
            Text(
                text = "Reset Password",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = NeuBlack
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your email address to receive a password reset link.",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = NeuGray
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NeuBlack) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NeuButton(
                text = "Send Link",
                onClick = { onSend(email) },
                backgroundColor = NeuBlue,
                textColor = NeuWhite,
                shadowOffset = 2.dp,
                cornerRadius = 8.dp
            )
        },
        dismissButton = {
            NeuButton(
                text = "Cancel",
                onClick = onDismiss,
                backgroundColor = NeuSurface,
                textColor = NeuBlack,
                shadowOffset = 2.dp,
                cornerRadius = 8.dp
            )
        }
    )
}
