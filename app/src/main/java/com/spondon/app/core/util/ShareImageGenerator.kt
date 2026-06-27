package com.spondon.app.core.util

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.spondon.app.R
import com.spondon.app.core.domain.model.BloodRequest
import com.spondon.app.core.domain.model.Urgency
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation

/**
 * Generates an app-themed share card as a Bitmap and saves it to cache
 * so it can be shared via FileProvider.
 */
object ShareImageGenerator {

    // ── Brand palette ────────────────────────────────────────
    private const val COLOR_BLOOD_RED = 0xFFC0152A.toInt()
    private const val COLOR_DARK_ROSE = 0xFF8B0F1F.toInt()
    private const val COLOR_SOFT_ROSE = 0xFFE63950.toInt()
    private const val COLOR_BG_DARK = 0xFF111111.toInt()
    private const val COLOR_SURFACE = 0xFF1E1E1E.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFFEEEEEE.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFFAAAAAA.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_CRITICAL = 0xFFFF1744.toInt()
    private const val COLOR_MODERATE = 0xFFFF9100.toInt()
    private const val COLOR_NORMAL = 0xFF78909C.toInt()

    // Card dimensions (px) – produces a nice 1080×1920-ish portrait card
    private const val WIDTH = 1080
    private const val HEIGHT = 1500
    private const val PADDING = 80f
    private const val CORNER = 48f

    fun generate(context: Context, request: BloodRequest): File {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawHeader(canvas, context)
        drawBloodGroupBadge(canvas, request)
        drawRequestInfo(canvas, request)
        drawFooter(canvas)

        // Save to cache
        val dir = File(context.cacheDir, "share_images")
        dir.mkdirs()
        val file = File(dir, "spondon_request_${request.id.take(8)}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    fun getShareUri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    // ─────────────────────────────────────────────────────────
    // Drawing helpers
    // ─────────────────────────────────────────────────────────

    private fun drawBackground(canvas: Canvas) {
        // Dark gradient bg
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, HEIGHT.toFloat(),
                COLOR_BG_DARK, 0xFF0A0A0A.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Subtle red glow at top
        val glowPaint = Paint().apply {
            shader = RadialGradient(
                WIDTH / 2f, 120f, 500f,
                (COLOR_BLOOD_RED and 0x00FFFFFF) or 0x30000000,
                0x00000000,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(WIDTH / 2f, 120f, 500f, glowPaint)

        // Card surface with rounded rect
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SURFACE
        }
        val cardRect = RectF(PADDING, 180f, WIDTH - PADDING, HEIGHT - PADDING)
        canvas.drawRoundRect(cardRect, CORNER, CORNER, cardPaint)

        // Red accent strip at top of card
        val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                PADDING, 180f, WIDTH - PADDING, 180f,
                COLOR_BLOOD_RED, COLOR_DARK_ROSE,
                Shader.TileMode.CLAMP,
            )
        }
        val stripPath = Path().apply {
            addRoundRect(
                RectF(PADDING, 180f, WIDTH - PADDING, 200f),
                floatArrayOf(CORNER, CORNER, CORNER, CORNER, 0f, 0f, 0f, 0f),
                Path.Direction.CW,
            )
        }
        canvas.drawPath(stripPath, stripPaint)
    }

    private fun drawHeader(canvas: Canvas, context: Context) {
        // App logo
        try {
            val logoBitmap = ContextCompat.getDrawable(context, R.drawable.logo)?.toBitmap(80, 90)
            if (logoBitmap != null) {
                canvas.drawBitmap(logoBitmap, PADDING + 40f, 50f, null)
            }
        } catch (_: Exception) { /* logo optional */ }

        // App name
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("SPONDON", PADDING + 140f, 105f, namePaint)

        // Tagline
        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SOFT_ROSE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Every Drop Counts", PADDING + 142f, 140f, tagPaint)
    }

    private fun drawBloodGroupBadge(canvas: Canvas, request: BloodRequest) {
        val cx = WIDTH / 2f
        val cy = 360f
        val radius = 110f

        // Red circle
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                COLOR_BLOOD_RED, COLOR_DARK_ROSE,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_SOFT_ROSE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(cx, cy, radius + 4f, borderPaint)

        // Blood group text
        val bgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_WHITE
            textSize = if (request.bloodGroup.length > 3) 70f else 80f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(request.bloodGroup, cx, cy + 28f, bgPaint)

        // "BLOOD NEEDED" text below badge
        val neededPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLOOD_RED
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("BLOOD NEEDED", cx, cy + radius + 60f, neededPaint)
    }

    private fun drawRequestInfo(canvas: Canvas, request: BloodRequest) {
        var y = 580f
        val left = PADDING + 60f
        val maxWidth = WIDTH - 2 * (PADDING + 60f)

        // Urgency badge
        val urgencyColor = when (request.urgency) {
            Urgency.CRITICAL -> COLOR_CRITICAL
            Urgency.MODERATE -> COLOR_MODERATE
            Urgency.NORMAL -> COLOR_NORMAL
        }
        val urgencyText = when (request.urgency) {
            Urgency.CRITICAL -> "⚠️ CRITICAL"
            Urgency.MODERATE -> "⚠️ URGENT"
            Urgency.NORMAL -> "NORMAL"
        }

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = urgencyColor and 0x00FFFFFF or 0x33000000 }
        val badgeTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = urgencyColor
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val badgeWidth = badgeTextPaint.measureText(urgencyText) + 48f
        val badgeRect = RectF(left, y, left + badgeWidth, y + 52f)
        canvas.drawRoundRect(badgeRect, 26f, 26f, badgePaint)
        canvas.drawText(urgencyText, left + 24f, y + 38f, badgeTextPaint)
        y += 80f

        // Divider
        val divPaint = Paint().apply { color = 0x33FFFFFF }
        canvas.drawRect(left, y, left + maxWidth, y + 2f, divPaint)
        y += 30f

        // Info rows
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val rows = mutableListOf<Pair<String, String>>()
        rows.add("🏥" to "Hospital: ${request.hospital}")
        if (request.address.isNotBlank()) rows.add("📍" to "Address: ${request.address}")
        rows.add("💪" to "Units Needed: ${request.unitsNeeded}")
        request.donationDateTime?.let { rows.add("📅" to "Date: ${dateFormat.format(it)}") }
        if (!request.patientName.isNullOrBlank()) rows.add("👤" to "Patient: ${request.patientName}")
        if (request.patientCondition.isNotBlank()) rows.add("📋" to "Condition: ${request.patientCondition}")
        if (request.contactNumber.isNotBlank()) rows.add("📞" to "Contact: ${request.contactNumber}")
        if (request.requesterName.isNotBlank()) rows.add("🙏" to "Requested by: ${request.requesterName}")
        if (request.communityName.isNotBlank()) rows.add("👥" to "Community: ${request.communityName}")

        val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        for ((emoji, text) in rows) {
            canvas.drawText(emoji, left, y + 36f, emojiPaint)
            // Use StaticLayout for potential text wrapping
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, labelPaint, maxWidth.toInt() - 60)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(6f, 1f)
                .build()
            canvas.withTranslation(left + 50f, y + 6f) {
                layout.draw(this)
            }
            y += (layout.height + 20f).coerceAtLeast(58f)
        }

        // Bottom divider
        y += 10f
        canvas.drawRect(left, y, left + maxWidth, y + 2f, divPaint)
        y += 30f

        // Call to action
        val ctaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLOOD_RED
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Please help save a life! ❤️", WIDTH / 2f, y + 38f, ctaPaint)
    }

    private fun drawFooter(canvas: Canvas) {
        val y = HEIGHT - PADDING - 40f
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_SECONDARY
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Shared via Spondon (স্পন্দন) — Blood Donation App", WIDTH / 2f, y, footerPaint)
    }
}
