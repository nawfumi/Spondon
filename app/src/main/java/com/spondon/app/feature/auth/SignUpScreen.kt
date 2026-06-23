package com.spondon.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.AuthOverlayState
import com.spondon.app.core.ui.components.AuthStateOverlay
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.components.SpondonButton
import com.spondon.app.core.ui.components.SpondonTextField
import com.spondon.app.core.ui.components.StepProgressBar
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.delay

@Composable
fun SignUpScreen(
    navController: NavController,
    onGoogleSignIn: () -> Unit = {},
    viewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // ── Entrance animations ──
    val headerAlpha = remember { Animatable(0f) }
    val formAlpha = remember { Animatable(0f) }
    val formOffset = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400))
        formAlpha.animateTo(1f, tween(450))
        formOffset.animateTo(0f, tween(450, easing = EaseOutCubic))
    }

    // ── Auth overlay state ──
    var overlayState by remember { mutableStateOf(AuthOverlayState.HIDDEN) }
    var overlayMessage by remember { mutableStateOf("") }

    // Track loading
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) overlayState = AuthOverlayState.LOADING
    }

    // Track error
    LaunchedEffect(state.error) {
        if (state.error != null && !state.isLoading) {
            overlayMessage = state.error ?: "Sign up failed"
            overlayState = AuthOverlayState.ERROR
        }
    }

    // ── One-shot navigation events (prevents crash-on-reopen) ──
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToProfileSetup -> {
                    overlayState = AuthOverlayState.SUCCESS
                    delay(800)
                    navController.navigate(Routes.DonorProfileSetup.route) {
                        // DonorProfileSetup is INSIDE auth_flow, so just pop current screen
                        popUpTo(Routes.SignUp.route) { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToHome -> {
                    overlayState = AuthOverlayState.SUCCESS
                    delay(1000)
                    navController.navigate(Routes.Home.route) {
                        popUpTo("auth_flow") { inclusive = true }
                    }
                }
                else -> {} // Other events handled by other screens
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        BloodRed.copy(alpha = 0.02f),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── Header with entrance animation ──
            Column(modifier = Modifier.alpha(headerAlpha.value)) {
                // Step progress
                StepProgressBar(
                    currentStep = 0,
                    totalSteps = 3,
                    stepLabels = listOf("Basic Info", "Health Profile", "Location"),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Title with blood drop accent
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BloodDropLoader(size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Create Account",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Step 1 of 3 — Basic Information",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Form with slide-up animation ──
            Column(
                modifier = Modifier
                    .alpha(formAlpha.value)
                    .offset(y = formOffset.value.dp),
            ) {
                // Full Name
                SpondonTextField(
                    value = state.fullName,
                    onValueChange = { viewModel.updateFullName(it) },
                    label = "Full Name",
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Number
                SpondonTextField(
                    value = state.phone,
                    onValueChange = { viewModel.updatePhone(it) },
                    label = "Phone Number",
                    leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.phone.isNotEmpty() && state.phone.length < 11,
                    errorMessage = if (state.phone.isNotEmpty() && state.phone.length < 11) "Enter a valid phone number" else null,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email
                SpondonTextField(
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = "Email Address",
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.email.isNotEmpty() && !state.email.contains("@"),
                    errorMessage = if (state.email.isNotEmpty() && !state.email.contains("@")) "Enter a valid email" else null,
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                SpondonTextField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = "Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                            Icon(
                                if (state.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                )

                // Password strength meter
                AnimatedVisibility(
                    visible = state.password.isNotEmpty(),
                    enter = expandVertically(animationSpec = tween(200, easing = LinearEasing)) + fadeIn(tween(200, easing = LinearEasing)),
                    exit = shrinkVertically(animationSpec = tween(150, easing = LinearEasing)) + fadeOut(tween(150, easing = LinearEasing)),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthMeter(viewModel.getPasswordStrength())
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm Password
                SpondonTextField(
                    value = state.confirmPassword,
                    onValueChange = { viewModel.updateConfirmPassword(it) },
                    label = "Confirm Password",
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                            Icon(
                                if (state.confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    visualTransformation = if (state.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword,
                    errorMessage = if (state.confirmPassword.isNotEmpty() && state.password != state.confirmPassword) "Passwords do not match" else null,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Google Sign-In card ──
                OutlinedCard(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = BloodRed,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continue with Google",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Terms of Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.agreedToTerms,
                        onCheckedChange = { viewModel.toggleTermsAgreement() },
                        colors = CheckboxDefaults.colors(checkedColor = BloodRed),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val annotatedText = buildAnnotatedString {
                        append("I agree to the ")
                        pushStringAnnotation("terms", "terms")
                        withStyle(SpanStyle(color = BloodRed, fontWeight = FontWeight.SemiBold)) {
                            append("Terms of Service")
                        }
                        pop()
                        append(" and ")
                        pushStringAnnotation("privacy", "privacy")
                        withStyle(SpanStyle(color = BloodRed, fontWeight = FontWeight.SemiBold)) {
                            append("Privacy Policy")
                        }
                        pop()
                    }
                    ClickableText(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        ),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations("terms", offset, offset)
                                .firstOrNull()?.let { navController.navigate(Routes.TermsOfService.route) }
                            annotatedText.getStringAnnotations("privacy", offset, offset)
                                .firstOrNull()?.let { navController.navigate(Routes.PrivacyPolicy.route) }
                        },
                    )
                }

                // Inline error
                AnimatedVisibility(
                    visible = state.error != null && overlayState == AuthOverlayState.HIDDEN,
                    enter = slideInVertically(animationSpec = tween(200, easing = LinearEasing), initialOffsetY = { -20 }) + fadeIn(tween(200, easing = LinearEasing)),
                    exit = slideOutVertically(animationSpec = tween(150, easing = LinearEasing), targetOffsetY = { -20 }) + fadeOut(tween(150, easing = LinearEasing)),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = UrgencyCritical.copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = UrgencyCritical,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.error ?: "",
                                color = UrgencyCritical,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue button
                SpondonButton(
                    text = "Continue",
                    onClick = {
                        navController.navigate(Routes.DonorProfileSetup.route)
                    },
                    enabled = viewModel.isStep1Valid() && !state.isLoading,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Already have an account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                    TextButton(onClick = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.SignUp.route) { inclusive = true }
                        }
                    }) {
                        Text("Log In", color = BloodRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Blood-themed auth overlay ──
        AuthStateOverlay(
            state = overlayState,
            loadingText = "Creating account...",
            successText = "Account created! 🩸",
            errorText = overlayMessage,
            onDismiss = {
                overlayState = AuthOverlayState.HIDDEN
                viewModel.clearError()
            },
        )

        // Dismiss error overlay on tap
        if (overlayState == AuthOverlayState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        overlayState = AuthOverlayState.HIDDEN
                        viewModel.clearError()
                    },
            )
        }
    }
}

// ── Password Strength Meter ──

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> UrgencyCritical
            PasswordStrength.FAIR -> UrgencyModerate
            PasswordStrength.STRONG -> AvailableGreen
        },
        animationSpec = tween(300),
        label = "strength_color",
    )
    val fraction by animateFloatAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> 0.33f
            PasswordStrength.FAIR -> 0.66f
            PasswordStrength.STRONG -> 1f
        },
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "strength_fraction",
    )
    val label = when (strength) {
        PasswordStrength.WEAK -> "Weak"
        PasswordStrength.FAIR -> "Fair"
        PasswordStrength.STRONG -> "Strong"
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}