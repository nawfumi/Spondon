package com.spondon.app.feature.superadmin.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

private val SAGold = Color(0xFFFFD700)
private val SADark = Color(0xFF0D0D0D)
private val SADarkCard = Color(0xFF1A1A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminRegisterScreen(
    navController: NavController,
    viewModel: SuperAdminAuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate to SA Dashboard on successful registration
    LaunchedEffect(state.registrationComplete) {
        if (state.registrationComplete) {
            navController.navigate("sa_dashboard") {
                popUpTo("sa_register") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SADark, Color(0xFF16213E)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Crown icon
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SAGold,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "SuperAdmin Registration",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )

            Text(
                "One-time setup • This screen will self-destruct",
                style = MaterialTheme.typography.bodySmall,
                color = SAGold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Registration card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SADarkCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Email
                    SATextField(
                        value = state.email,
                        onValueChange = viewModel::updateEmail,
                        label = "Email Address",
                        icon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Password
                    SATextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = "Password (min 8 chars)",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = state.passwordVisible,
                        onToggleVisibility = viewModel::togglePasswordVisibility,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Secret Passphrase
                    SATextField(
                        value = state.passphrase,
                        onValueChange = viewModel::updatePassphrase,
                        label = "Secret Passphrase",
                        icon = Icons.Outlined.Key,
                        isPassword = true,
                        passwordVisible = state.passphraseVisible,
                        onToggleVisibility = viewModel::togglePassphraseVisibility,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "⚠️ Store this passphrase securely. It cannot be recovered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SAGold.copy(alpha = 0.5f),
                    )

                    Spacer(Modifier.height(20.dp))

                    // Error
                    AnimatedVisibility(visible = state.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D1515),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                state.error ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Register Button
                    Button(
                        onClick = viewModel::register,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SAGold,
                            contentColor = SADark,
                        ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SADark,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.Shield, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Initialize SuperAdmin",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SATextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.5f)) },
        leadingIcon = {
            Icon(icon, null, tint = SAGold.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (isPassword && onToggleVisibility != null) {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null,
                        tint = Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            cursorColor = SAGold,
            focusedBorderColor = SAGold.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(10.dp),
    )
}
