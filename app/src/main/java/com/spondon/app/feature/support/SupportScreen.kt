package com.spondon.app.feature.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.spondon.app.R
import com.spondon.app.core.ui.i18n.S
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.ui.theme.SoftRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    navController: NavController,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val s = S.strings
    val state by viewModel.state.collectAsState()
    val devInfo = state.developerInfo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.supportDeveloper, fontWeight = FontWeight.Bold) },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // ─── Logo (dark-mode safe) ──────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // White backing circle so the PNG is always visible
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Spondon Logo",
                        modifier = Modifier.size(90.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ─── Developer Profile Photo ─────────────────
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = BloodRed,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(16.dp))
            } else {
                // Profile photo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BloodRed.copy(alpha = 0.3f),
                                    SoftRose.copy(alpha = 0.3f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (devInfo.profilePhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = devInfo.profilePhotoUrl,
                            contentDescription = devInfo.name,
                            modifier = Modifier
                                .size(102.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = BloodRed.copy(alpha = 0.5f),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Developer name
                if (devInfo.name.isNotEmpty()) {
                    Text(
                        text = devInfo.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                // Subtitle
                if (devInfo.subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = devInfo.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ─── Social Links Row ────────────────────────
                val socialLinks = buildList {
                    if (devInfo.facebook.isNotEmpty()) add("facebook" to devInfo.facebook)
                    if (devInfo.whatsapp.isNotEmpty()) add("whatsapp" to devInfo.whatsapp)
                    if (devInfo.instagram.isNotEmpty()) add("instagram" to devInfo.instagram)
                    if (devInfo.linkedin.isNotEmpty()) add("linkedin" to devInfo.linkedin)
                    if (devInfo.github.isNotEmpty()) add("github" to devInfo.github)
                    if (devInfo.twitter.isNotEmpty()) add("twitter" to devInfo.twitter)
                }

                if (socialLinks.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            socialLinks.forEachIndexed { index, (platform, url) ->
                                SocialIconButton(
                                    platform = platform,
                                    url = url,
                                    onClick = {
                                        try {
                                            val resolvedUrl = when (platform) {
                                                "whatsapp" -> {
                                                    if (url.startsWith("http")) url
                                                    else "https://wa.me/${url.replace("+", "").replace(" ", "")}"
                                                }
                                                else -> url
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (_: Exception) { }
                                    },
                                )
                                if (index < socialLinks.lastIndex) {
                                    Spacer(Modifier.width(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ─── Donate / Support Button ─────────────────
            Button(
                onClick = {
                    try {
                        val url = devInfo.supportUrl.ifEmpty { "https://www.supportkori.com/arshad" }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // No browser available
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BloodRed),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    Icons.Filled.VolunteerActivism,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = s.supportButtonText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ─── Footer note ─────────────────────────────
            Text(
                text = s.supportFooter,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 48.dp),
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SocialIconButton(
    platform: String,
    url: String,
    onClick: () -> Unit,
) {
    val (label, color) = when (platform) {
        "facebook" -> "Facebook" to Color(0xFF1877F2)
        "whatsapp" -> "WhatsApp" to Color(0xFF25D366)
        "instagram" -> "Instagram" to Color(0xFFE4405F)
        "linkedin" -> "LinkedIn" to Color(0xFF0A66C2)
        "github" -> "GitHub" to Color(0xFF333333)
        "twitter" -> "X" to Color(0xFF1DA1F2)
        else -> platform to BloodRed
    }

    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
        )
    }
}
