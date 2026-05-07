package com.spondon.app.feature.superadmin.auth

import androidx.compose.animation.AnimatedVisibility
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
fun SuperAdminLoginScreen(
    navController: NavController,
    viewModel: SuperAdminAuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Navigate to SA Dashboard on successful login
    LaunchedEffect(state.isLoginComplete) {
        if (state.isLoginComplete) {
            navController.navigate("sa_dashboard") {
                popUpTo("sa_login") { inclusive = true }
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

            // Shield icon
            Icon(
                Icons.Outlined.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SAGold,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "SuperAdmin Access",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )

            Text(
                "3-Factor Authentication Required",
                style = MaterialTheme.typography.bodySmall,
                color = SAGold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // Login card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SADarkCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Factor 1: Email
                    Text(
                        "Factor 1 — Identity",
                        style = MaterialTheme.typography.labelSmall,
                        color = SAGold.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))
                    SALoginTextField(
                        value = state.email,
                        onValueChange = viewModel::updateEmail,
                        label = "Email Address",
                        icon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Factor 2: Password
                    Text(
                        "Factor 2 — Password",
                        style = MaterialTheme.typography.labelSmall,
                        color = SAGold.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))
                    SALoginTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = "Password",
                        icon = Icons.Outlined.Lock,
                        isPassword = true,
                        passwordVisible = state.passwordVisible,
                        onToggleVisibility = viewModel::togglePasswordVisibility,
                    )

                    Spacer(Modifier.height(16.dp))

                    // Factor 3: Passphrase
                    Text(
                        "Factor 3 — Secret Passphrase",
                        style = MaterialTheme.typography.labelSmall,
                        color = SAGold.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.height(6.dp))
                    SALoginTextField(
                        value = state.passphrase,
                        onValueChange = viewModel::updatePassphrase,
                        label = "Secret Passphrase",
                        icon = Icons.Outlined.Key,
                        isPassword = true,
                        passwordVisible = state.passphraseVisible,
                        onToggleVisibility = viewModel::togglePassphraseVisibility,
                    )

                    Spacer(Modifier.height(20.dp))

                    // Failed attempts warning
                    if (state.failedAttempts > 0 && state.failedAttempts < 3) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF3D2E15),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "⚠️ ${3 - state.failedAttempts} attempt(s) remaining before lockout",
                                modifier = Modifier.padding(10.dp),
                                color = SAGold,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Error
                    AnimatedVisibility(visible = state.error != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
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

                    // Login Button
                    val isLocked = state.lockoutEndTime > System.currentTimeMillis()
                    Button(
                        onClick = viewModel::login,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isLoading && !isLocked,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SAGold,
                            contentColor = SADark,
                            disabledContainerColor = SAGold.copy(alpha = 0.3f),
                        ),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SADark,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.Login, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isLocked) "Locked" else "Authenticate",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Security notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.03f),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Outlined.Security,
                        null,
                        Modifier.size(14.dp),
                        tint = SAGold.copy(alpha = 0.3f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "This is a restricted access point. All login attempts " +
                            "are logged. 3 failed attempts triggers a 10-minute lockout.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp,
                        ),
                        color = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SALoginTextField(
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
