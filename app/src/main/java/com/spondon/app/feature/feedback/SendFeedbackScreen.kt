package com.spondon.app.feature.feedback

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SoftRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendFeedbackScreen(
    navController: NavController,
    viewModel: SendFeedbackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val s = S.strings

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.sendFeedback, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AnimatedContent(
            targetState = state.sent,
            transitionSpec = {
                fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
            },
            label = "feedback_sent",
        ) { isSent ->
            if (isSent) {
                // ─── Success State ───────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp),
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = BloodRed,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            s.feedbackSentTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            s.feedbackSentMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp),
                        ) {
                            Text(s.back, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // ─── Form State ──────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Description card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SoftRose.copy(alpha = 0.15f),
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.Info, null,
                                modifier = Modifier.size(18.dp),
                                tint = BloodRed.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                s.feedbackInfoText,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            )
                        }
                    }

                    // ─── Feedback Type Selector ──────────────
                    Text(
                        s.feedbackTypeLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = BloodRed.copy(alpha = 0.8f),
                    )

                    val feedbackTypes = listOf(
                        Triple("BUG", Icons.Outlined.BugReport, s.feedbackTypeBug),
                        Triple("FEATURE", Icons.Outlined.Lightbulb, s.feedbackTypeFeature),
                        Triple("COMPLAINT", Icons.Outlined.Report, s.feedbackTypeComplaint),
                        Triple("OTHER", Icons.Outlined.QuestionMark, s.feedbackTypeOther),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        feedbackTypes.forEach { (type, icon, label) ->
                            FeedbackTypeChip(
                                icon = icon,
                                label = label,
                                selected = state.feedbackType == type,
                                onClick = { viewModel.setType(type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // ─── Body ────────────────────────────────
                    Text(
                        s.feedbackBodyLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = BloodRed.copy(alpha = 0.8f),
                    )

                    OutlinedTextField(
                        value = state.body,
                        onValueChange = viewModel::setBody,
                        placeholder = {
                            Text(
                                s.feedbackBodyPlaceholder,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        minLines = 6,
                        maxLines = 12,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BloodRed.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            cursorColor = BloodRed,
                        ),
                    )

                    // Character count
                    Text(
                        "${state.body.length} / 1000",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.body.length > 1000) Color.Red
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.align(Alignment.End),
                    )

                    Spacer(Modifier.height(4.dp))

                    // ─── Submit Button ───────────────────────
                    Button(
                        onClick = { viewModel.submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !state.isSending && state.body.isNotBlank() && state.body.length <= 1000,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(s.feedbackSending, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Outlined.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(s.feedbackSubmit, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ─── Auto-attached info note ─────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                Icons.Outlined.PhoneAndroid, null,
                                Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                s.feedbackAutoInfo,
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedbackTypeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) BloodRed.copy(alpha = 0.1f)
    else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) BloodRed
    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = if (selected) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = contentColor)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 10.sp,
                ),
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
