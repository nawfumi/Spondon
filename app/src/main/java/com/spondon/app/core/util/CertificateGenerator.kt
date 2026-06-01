package com.spondon.app.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates modern, minimalistic PDF certificates for blood donors.
 * Uses Android's built-in PdfDocument API — no third-party dependencies.
 */
object CertificateGenerator {

    private const val PAGE_WIDTH = 842  // A4 landscape width in points
    private const val PAGE_HEIGHT = 595 // A4 landscape height in points

    data class CertificateData(
        val donorName: String,
        val bloodGroup: String,
        val totalDonations: Int,
        val lastDonationDate: Date?,
    )

    /**
     * Generates a PDF certificate and saves it to the Downloads folder.
     * @return The file path of the saved certificate, or null on failure.
     */
    fun generateCertificate(context: Context, data: CertificateData): String? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)

        drawCertificate(page.canvas, data)

        document.finishPage(page)

        val fileName = "Spondon_Certificate_${data.donorName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"

        return try {
            val filePath = savePdf(context, document, fileName)
            document.close()
            filePath
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun drawCertificate(canvas: Canvas, data: CertificateData) {
        val width = PAGE_WIDTH.toFloat()
        val height = PAGE_HEIGHT.toFloat()

        // ─── Background ──────────────────────────────────────
        val bgPaint = Paint().apply {
            color = Color.rgb(250, 250, 252)
        }
        canvas.drawRect(0f, 0f, width, height, bgPaint)

        // ─── Watermark blood drops ───────────────────────────
        drawWatermarkDrops(canvas, width, height)

        // ─── Top accent bar ──────────────────────────────────
        val accentPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width, 0f,
                Color.rgb(229, 57, 53), // BloodRed
                Color.rgb(198, 40, 40), // DarkRose
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, width, 8f, accentPaint)

        // ─── Bottom accent bar ───────────────────────────────
        canvas.drawRect(0f, height - 8f, width, height, accentPaint)

        // ─── Left accent stripe ──────────────────────────────
        val leftStripePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height,
                Color.rgb(229, 57, 53),
                Color.rgb(198, 40, 40),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, 6f, height, leftStripePaint)

        // ─── Border frame ────────────────────────────────────
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.rgb(229, 57, 53)
            strokeWidth = 1.5f
            alpha = 40
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, borderPaint)

        // Inner decorative border
        val innerBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.rgb(229, 57, 53)
            strokeWidth = 0.5f
            alpha = 20
        }
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, innerBorderPaint)

        // ─── "CERTIFICATE" label ─────────────────────────────
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.35f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CERTIFICATE OF APPRECIATION", width / 2, 70f, labelPaint)

        // ─── Decorative line under label ─────────────────────
        val linePaint = Paint().apply {
            color = Color.rgb(229, 57, 53)
            strokeWidth = 1f
            alpha = 60
        }
        canvas.drawLine(width / 2 - 100f, 80f, width / 2 + 100f, 80f, linePaint)

        // ─── Title ───────────────────────────────────────────
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33)
            textSize = 36f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Blood Donation", width / 2, 130f, titlePaint)

        val titleBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            textSize = 36f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Certificate", width / 2, 170f, titleBoldPaint)

        // ─── "This certifies that" ──────────────────────────
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(117, 117, 117)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This certifies that", width / 2, 210f, bodyPaint)

        // ─── Donor Name ──────────────────────────────────────
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33)
            textSize = 32f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.donorName, width / 2, 255f, namePaint)

        // ─── Underline ───────────────────────────────────────
        val nameWidth = namePaint.measureText(data.donorName)
        val underlinePaint = Paint().apply {
            shader = LinearGradient(
                width / 2 - nameWidth / 2, 0f,
                width / 2 + nameWidth / 2, 0f,
                Color.rgb(229, 57, 53),
                Color.rgb(198, 40, 40),
                Shader.TileMode.CLAMP,
            )
            strokeWidth = 2f
        }
        canvas.drawLine(
            width / 2 - nameWidth / 2 - 20f, 265f,
            width / 2 + nameWidth / 2 + 20f, 265f,
            underlinePaint,
        )

        // ─── Description ─────────────────────────────────────
        val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(97, 97, 97)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "has generously donated blood and helped save lives through the Spondon community.",
            width / 2, 300f, descPaint,
        )

        // ─── Stats cards ─────────────────────────────────────
        val statsY = 340f
        val cardWidth = 180f
        val cardHeight = 80f
        val cardGap = 40f
        val totalCardsWidth = cardWidth * 3 + cardGap * 2
        val startX = (width - totalCardsWidth) / 2

        // Card 1: Total Donations
        drawStatCard(
            canvas, startX, statsY, cardWidth, cardHeight,
            "${data.totalDonations}", "Total Donations",
            Color.rgb(229, 57, 53),
        )

        // Card 2: Blood Group
        drawStatCard(
            canvas, startX + cardWidth + cardGap, statsY, cardWidth, cardHeight,
            data.bloodGroup, "Blood Group",
            Color.rgb(198, 40, 40),
        )

        // Card 3: Last Donation
        val lastDonation = data.lastDonationDate?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
        } ?: "N/A"
        drawStatCard(
            canvas, startX + (cardWidth + cardGap) * 2, statsY, cardWidth, cardHeight,
            lastDonation, "Last Donation",
            Color.rgb(183, 28, 28),
        )

        // ─── Decorative separator ────────────────────────────
        val sepY = statsY + cardHeight + 30f
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            alpha = 80
        }
        val dotSpacing = 8f
        val dotsCount = 15
        val dotsWidth = (dotsCount - 1) * dotSpacing
        val dotsStartX = (width - dotsWidth) / 2
        for (i in 0 until dotsCount) {
            canvas.drawCircle(dotsStartX + i * dotSpacing, sepY, 1.5f, dotPaint)
        }

        // ─── Thank you message ───────────────────────────────
        val thankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "\"Every drop counts. Thank you for being a hero.\"",
            width / 2, sepY + 30f, thankPaint,
        )

        // ─── Footer ─────────────────────────────────────────
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(158, 158, 158)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        canvas.drawText(
            "Issued by Spondon Blood Donation Community  •  ${dateFormat.format(Date())}",
            width / 2, height - 40f, footerPaint,
        )

        // ─── App branding ────────────────────────────────────
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            textSize = 18f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            alpha = 25
        }
        canvas.drawText("SPONDON", width / 2, height - 55f, brandPaint)
    }

    private fun drawStatCard(
        canvas: Canvas,
        x: Float, y: Float,
        w: Float, h: Float,
        value: String,
        label: String,
        accentColor: Int,
    ) {
        // Card background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 255, 255)
            setShadowLayer(4f, 0f, 2f, Color.argb(20, 0, 0, 0))
        }
        canvas.drawRoundRect(x, y, x + w, y + h, 10f, 10f, cardPaint)

        // Top accent line
        val topLinePaint = Paint().apply {
            color = accentColor
        }
        canvas.drawRoundRect(x, y, x + w, y + 3f, 10f, 10f, topLinePaint)

        // Value text
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, x + w / 2, y + h / 2 + 2f, valuePaint)

        // Label text
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(158, 158, 158)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText(label.uppercase(Locale.getDefault()), x + w / 2, y + h - 12f, labelPaint)
    }

    /**
     * Draws subtle blood drop watermark shapes in the background.
     */
    private fun drawWatermarkDrops(canvas: Canvas, width: Float, height: Float) {
        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 57, 53)
            alpha = 8
            style = Paint.Style.FILL
        }

        // Several large drops at different positions
        val drops = listOf(
            Triple(width * 0.1f, height * 0.3f, 60f),
            Triple(width * 0.85f, height * 0.25f, 50f),
            Triple(width * 0.15f, height * 0.75f, 45f),
            Triple(width * 0.9f, height * 0.7f, 55f),
            Triple(width * 0.5f, height * 0.85f, 40f),
        )

        for ((cx, cy, size) in drops) {
            drawBloodDrop(canvas, cx, cy, size, dropPaint)
        }
    }

    private fun drawBloodDrop(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            // Blood drop shape: a circle at the bottom with a pointed top
            moveTo(cx, cy - size * 1.2f) // tip
            cubicTo(
                cx + size * 0.6f, cy - size * 0.3f,
                cx + size * 0.7f, cy + size * 0.3f,
                cx, cy + size * 0.7f,
            )
            cubicTo(
                cx - size * 0.7f, cy + size * 0.3f,
                cx - size * 0.6f, cy - size * 0.3f,
                cx, cy - size * 1.2f,
            )
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun savePdf(context: Context, document: PdfDocument, fileName: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues,
            ) ?: return null

            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return null
            outputStream.use { document.writeTo(it) }
            uri.toString()
        } else {
            // Fallback for older Android
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            file.absolutePath
        }
    }
}
