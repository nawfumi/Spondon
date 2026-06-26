package com.spondon.app.feature.request

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.ui.theme.BloodRed
import com.spondon.app.core.util.ShareImageGenerator
import com.spondon.app.core.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    request: BloodRequest,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val shareText = remember(request) { ShareUtils.buildShareText(request) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                )
            }

            Text(
                text = "Share Blood Request",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${request.bloodGroup} blood needed at ${request.hospital}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // ── Share Options ──────────────────────────────────

            val shareOptions = remember {
                listOf(
                    ShareOption("Share as Image", Icons.Outlined.Image, BloodRed, ShareType.IMAGE),
                    ShareOption("Share as Text",
                        Icons.AutoMirrored.Outlined.TextSnippet, Color(0xFF4CAF50), ShareType.TEXT),
                    ShareOption("Copy Text", Icons.Outlined.ContentCopy, Color(0xFF2196F3), ShareType.COPY),
                    ShareOption("More Options", Icons.Outlined.Share, Color(0xFF9C27B0), ShareType.SYSTEM_CHOOSER),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(shareOptions) { option ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    handleShare(context, option.type, request, shareText)
                                    sheetState.hide()
                                    onDismiss()
                                }
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(option.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = option.label,
                                tint = option.color,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 13.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Hint text
            Text(
                text = "\"Share as Image\" creates a branded card with all request info — great for Facebook, Instagram Stories, and more.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Models ──────────────────────────────────────────────────

private enum class ShareType { IMAGE, TEXT, COPY, SYSTEM_CHOOSER }

private data class ShareOption(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val type: ShareType,
)

// ─── Share Logic ─────────────────────────────────────────────

private suspend fun handleShare(
    context: Context,
    type: ShareType,
    request: BloodRequest,
    shareText: String,
) {
    when (type) {
        ShareType.IMAGE -> shareAsImage(context, request, shareText)
        ShareType.TEXT -> shareAsText(context, shareText)
        ShareType.COPY -> copyToClipboard(context, shareText)
        ShareType.SYSTEM_CHOOSER -> shareViaSystemChooser(context, request, shareText)
    }
}

/**
 * Generates an app-themed card image and shares it via the system share sheet.
 * The image + text are sent together so apps like Facebook, Messenger,
 * WhatsApp, Instagram etc. can pick whichever they support.
 */
private suspend fun shareAsImage(context: Context, request: BloodRequest, shareText: String) {
    try {
        val file = withContext(Dispatchers.IO) {
            ShareImageGenerator.generate(context, request)
        }
        val uri = ShareImageGenerator.getShareUri(context, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Blood Request"))
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to create share image", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareAsText(context: Context, shareText: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share Blood Request"))
}

/**
 * System chooser with both image + text so apps that support images get the card,
 * and text-only apps get the text.
 */
private suspend fun shareViaSystemChooser(context: Context, request: BloodRequest, shareText: String) {
    try {
        val file = withContext(Dispatchers.IO) {
            ShareImageGenerator.generate(context, request)
        }
        val uri = ShareImageGenerator.getShareUri(context, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Blood Request"))
    } catch (_: Exception) {
        // Fall back to text-only
        shareAsText(context, shareText)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Blood Request", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
