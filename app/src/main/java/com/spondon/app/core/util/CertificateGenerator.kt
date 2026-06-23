package com.spondon.app.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.BlurMaskFilter
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
import androidx.core.net.toUri

object CertificateGenerator {

    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595

    private val Crimson = Color.rgb(192, 16, 42)
    private val CrimsonDeep = Color.rgb(140, 10, 31)
    private val CrimsonSoft = Color.rgb(232, 83, 107)
    private val Ink = Color.rgb(34, 20, 22)
    private val Gold = Color.rgb(201, 150, 46)
    private val GoldLight = Color.rgb(240, 205, 122)
    private val GoldDark = Color.rgb(156, 113, 30)
    private val Paper = Color.rgb(255, 253, 251)

    private const val OUTER_MARGIN = 28f
    private const val OUTER_STROKE = 4f
    private const val BORDER_GAP = 10f
    private const val INNER_STROKE = 1.5f

    data class CertificateData(
        val donorName: String,
        val bloodGroup: String,
        val totalDonations: Int,
        val lastDonationDate: Date?,
        val hospitalName: String,
        val signatoryName: String,
        val appSlogan: String = "Every Drop Counts",
        val locationAddress: String = "",
    )

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
            if (filePath != null) {
                showCertificateNotification(context, filePath)
            }
            filePath
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun showCertificateNotification(context: Context, filePath: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "certificate_downloads"

        val channel = android.app.NotificationChannel(
            channelId,
            "Certificate Downloads",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for downloaded certificates"
        }
        manager.createNotificationChannel(channel)

        val uri = if (filePath.startsWith("content://")) {
            filePath.toUri()
        } else {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                File(filePath)
            )
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            filePath.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.spondon.app.R.mipmap.ic_spondon_round)
            .setContentTitle("Certificate Saved")
            .setContentText("Tap to view your certificate")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(filePath.hashCode(), notification)
    }

    private fun drawCertificate(canvas: Canvas, data: CertificateData) {
        val w = PAGE_WIDTH.toFloat()
        val h = PAGE_HEIGHT.toFloat()

        val bgPaint = Paint().apply { color = Paper }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        drawBackgroundDecor(canvas, w, h)
        drawDoubleBorder(canvas, w, h)
        drawBrandRow(canvas, w)
        drawHeadline(canvas, w)
        drawBody(canvas, w, data)
        drawFooter(canvas, w, h, data)
    }

    private fun drawDoubleBorder(canvas: Canvas, w: Float, h: Float) {
        val p = Paint().apply {
            style = Paint.Style.STROKE
            color = Crimson
        }

        p.strokeWidth = OUTER_STROKE
        canvas.drawRect(OUTER_MARGIN, OUTER_MARGIN, w - OUTER_MARGIN, h - OUTER_MARGIN, p)

        val inset = OUTER_MARGIN + OUTER_STROKE + BORDER_GAP
        p.strokeWidth = INNER_STROKE
        canvas.drawRect(inset, inset, w - inset, h - inset, p)
    }

    private fun drawBackgroundDecor(canvas: Canvas, w: Float, h: Float) {
        val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Crimson
            alpha = 30
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.NORMAL)
        }

        val cycleWidth = 160f
        val amp = 24f
        val count = (w / cycleWidth).toInt() + 2

        fun drawEcg(yBase: Float) {
            val path = Path()
            for (i in 0 until count) {
                val x0 = i * cycleWidth
                path.moveTo(x0, yBase)
                path.lineTo(x0 + 60f, yBase)
                path.lineTo(x0 + 72f, yBase)
                path.lineTo(x0 + 80f, yBase - amp)
                path.lineTo(x0 + 92f, yBase + amp)
                path.lineTo(x0 + 100f, yBase)
                path.lineTo(x0 + cycleWidth, yBase)
            }
            canvas.drawPath(path, blurPaint)
        }

        drawEcg(yBase = 28f)
        drawEcg(yBase = h - 28f)

        val dropBlurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Crimson
            alpha = 16
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }

        drawFaintDrop(canvas, 35f, 20f, 70f, dropBlurPaint)
        drawFaintDrop(canvas, w - 35f, h - 20f, 70f, dropBlurPaint)
        drawFaintDrop(canvas, 50f, h - 25f, 45f, dropBlurPaint)
        drawFaintDrop(canvas, w - 50f, 25f, 50f, dropBlurPaint)
    }

    private fun drawFaintDrop(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(cx, cy - size * 0.6f)
            cubicTo(
                cx + size * 0.5f, cy - size * 0.15f,
                cx + size * 0.55f, cy + size * 0.35f,
                cx, cy + size * 0.6f,
            )
            cubicTo(
                cx - size * 0.55f, cy + size * 0.35f,
                cx - size * 0.5f, cy - size * 0.15f,
                cx, cy - size * 0.6f,
            )
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBrandRow(canvas: Canvas, w: Float) {
        val dropSize = 22f
        val dropCx = w / 2f - 150f
        val dropCy = 55f

        val dropPath = Path().apply {
            moveTo(dropCx, dropCy - dropSize * 0.7f)
            cubicTo(
                dropCx + dropSize * 0.5f, dropCy - dropSize * 0.2f,
                dropCx + dropSize * 0.55f, dropCy + dropSize * 0.3f,
                dropCx, dropCy + dropSize * 0.55f,
            )
            cubicTo(
                dropCx - dropSize * 0.55f, dropCy + dropSize * 0.3f,
                dropCx - dropSize * 0.5f, dropCy - dropSize * 0.2f,
                dropCx, dropCy - dropSize * 0.7f,
            )
            close()
        }

        val dropFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                dropCx, dropCy - dropSize, dropCx, dropCy + dropSize,
                intArrayOf(CrimsonSoft, Crimson, CrimsonDeep),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(dropPath, dropFill)

        val sPath = Path().apply {
            moveTo(dropCx + dropSize * 0.35f, dropCy - dropSize * 0.3f)
            cubicTo(
                dropCx - dropSize * 0.05f, dropCy - dropSize * 0.45f,
                dropCx - dropSize * 0.25f, dropCy - dropSize * 0.1f,
                dropCx - dropSize * 0.05f, dropCy + dropSize * 0.05f,
            )
            cubicTo(
                dropCx + dropSize * 0.15f, dropCy + dropSize * 0.2f,
                dropCx + dropSize * 0.3f, dropCy + dropSize * 0.3f,
                dropCx + dropSize * 0.05f, dropCy + dropSize * 0.45f,
            )
        }
        val sPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Paper
            style = Paint.Style.STROKE
            strokeWidth = dropSize * 0.07f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(sPath, sPaint)

        val spondonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CrimsonDeep
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            letterSpacing = 0.12f
        }
        canvas.drawText("SPONDON", dropCx + 40f, dropCy + 6f, spondonPaint)

        val bengaliPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CrimsonSoft
            textSize = 13f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("স্পন্দন", dropCx + 115f, dropCy + 20f, bengaliPaint)
    }

    private fun drawHeadline(canvas: Canvas, w: Float) {
        val certPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Crimson
            textSize = 52f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CERTIFICATE", w / 2f, 112f, certPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink
            alpha = 180
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText("OF BLOOD DONATION", w / 2f, 142f, subPaint)
    }

    private fun drawBody(canvas: Canvas, w: Float, data: CertificateData) {
        val cx = w / 2f
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink
            alpha = 200
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("This certificate is presented to", cx, 190f, bodyPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CrimsonDeep
            textSize = 44f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.donorName, cx, 252f, namePaint)

        val nw = namePaint.measureText(data.donorName)
        val ulPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 169, 169)
            strokeWidth = 1f
        }
        canvas.drawLine(cx - nw / 2f - 8f, 262f, cx + nw / 2f + 8f, 262f, ulPaint)

        val donationDateStr = data.lastDonationDate?.let {
            SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(it)
        } ?: "N/A"

        val bodySmallLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink; alpha = 200; textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val bodySmallCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink; alpha = 200; textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val accentLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Crimson; textSize = 14f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        canvas.drawText("in recognition of your generous contribution as a blood donor on", cx, 302f, bodySmallCenter)

        val dateSeg = "$donationDateStr, at "
        val hospitalSeg = data.hospitalName
        val combinedW = accentLeft.measureText(dateSeg) + accentLeft.measureText(hospitalSeg)
        val dateStartX = cx - combinedW / 2f
        canvas.drawText(dateSeg, dateStartX, 328f, accentLeft)
        canvas.drawText(hospitalSeg, dateStartX + accentLeft.measureText(dateSeg), 328f, accentLeft)

        canvas.drawText("Your selfless act has helped save lives and brought hope", cx, 365f, bodySmallCenter)

        val thanksPrefix = "to those in need. Thank you for being a "
        val thanksBold = "true hero"
        val thanksSuffix = "."
        val thanksTotalW = bodySmallLeft.measureText(thanksPrefix) + accentLeft.measureText(thanksBold) + bodySmallLeft.measureText(thanksSuffix)
        val thanksStartX = cx - thanksTotalW / 2f
        canvas.drawText(thanksPrefix, thanksStartX, 390f, bodySmallLeft)
        canvas.drawText(thanksBold, thanksStartX + bodySmallLeft.measureText(thanksPrefix), 390f, accentLeft)
        canvas.drawText(thanksSuffix, thanksStartX + bodySmallLeft.measureText(thanksPrefix) + accentLeft.measureText(thanksBold), 390f, bodySmallLeft)
    }

    private fun drawFooter(canvas: Canvas, w: Float, h: Float, data: CertificateData) {
        val leftCx = w * 0.25f
        val rightCx = w * 0.75f
        val fy = h - 145f

        val medalSize = 50f
        drawMedalSeal(canvas, leftCx, fy, medalSize)

        val donationDateStr = data.lastDonationDate?.let {
            SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(it)
        } ?: "N/A"

        val smallSans = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink
            alpha = 160
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val semiBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val dateLineY = fy + medalSize + 20f
        val dateLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(201, 185, 185)
            strokeWidth = 1f
        }
        canvas.drawLine(leftCx - 60f, dateLineY, leftCx + 60f, dateLineY, dateLinePaint)

        val dateTextY = dateLineY + 16f
        canvas.drawText(donationDateStr, leftCx, dateTextY, semiBold)

        canvas.drawText("Date of Donation", leftCx, dateTextY + 18f, smallSans)

        if (data.locationAddress.isNotBlank()) {
            canvas.drawText(data.locationAddress, leftCx, dateTextY + 38f, smallSans)
        }

        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Ink
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Spondon App", rightCx, fy + 6f, appNamePaint)

        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CrimsonSoft
            textSize = 12f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.appSlogan, rightCx, fy + 26f, sloganPaint)

        val sigLineY = fy + 42f
        canvas.drawLine(rightCx - 70f, sigLineY, rightCx + 70f, sigLineY, dateLinePaint)

        val signatoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Crimson
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.signatoryName, rightCx, sigLineY + 18f, signatoryPaint)
    }

    private fun drawMedalSeal(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CrimsonDeep
        }
        val ribbonPath = Path().apply {
            moveTo(cx - size * 0.4f, cy + size * 0.25f)
            lineTo(cx - size * 0.55f, cy + size * 1.2f)
            lineTo(cx, cy + size * 0.8f)
            lineTo(cx + size * 0.55f, cy + size * 1.2f)
            lineTo(cx + size * 0.4f, cy + size * 0.25f)
            close()
        }
        canvas.drawPath(ribbonPath, ribbonPaint)

        val starPath = Path()
        val points = 12
        for (i in 0 until points * 2) {
            val angle = (Math.PI * 2 * i / (points * 2)).toFloat()
            val radius = if (i % 2 == 0) size * 0.5f else size * 0.4f
            val x = cx + radius * kotlin.math.cos(angle)
            val y = cy + radius * kotlin.math.sin(angle)
            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        }
        starPath.close()

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx - size, cy - size, cx + size, cy + size,
                intArrayOf(GoldLight, Gold),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(starPath, starPaint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GoldLight
        }
        canvas.drawCircle(cx, cy, size * 0.36f, circlePaint)

        val borderCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GoldDark
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawCircle(cx, cy, size * 0.28f, borderCirclePaint)
    }

    private fun savePdf(context: Context, document: PdfDocument, fileName: String): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
