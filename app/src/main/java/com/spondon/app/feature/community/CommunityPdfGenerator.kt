package com.spondon.app.feature.community

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sorting options for the PDF member list.
 */
enum class PdfSortOption(val label: String) {
    ALPHABETICAL("Alphabetical (A-Z)"),
    BLOOD_GROUP("Blood Group"),
    SERIAL_ID("Serial ID"),
    TIME("Registration Time"),
}

/**
 * Generates a PDF member list for a community.
 * Uses Android's native PdfDocument API (no third-party dependencies).
 *
 * Columns: Serial (#), ID (if serial enabled), Name, Blood Group,
 *          Total Donations, Last Donated
 */
class CommunityPdfGenerator {

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_LEFT = 36f
        private const val MARGIN_RIGHT = 36f
        private const val MARGIN_TOP = 36f
        private const val MARGIN_BOTTOM = 40f
        private const val ROW_HEIGHT = 22f
        private const val HEADER_HEIGHT = 80f
    }

    /**
     * Data class for a single table row.
     */
    data class MemberRow(
        val index: Int,
        val serialId: String,
        val name: String,
        val bloodGroup: String,
        val totalDonations: Int,
        val lastDonation: String,
    )

    /**
     * Generate the PDF and return the file.
     */
    fun generate(
        context: Context,
        communityName: String,
        members: List<User>,
        community: Community,
        serials: Map<String, String>,
        sortOption: PdfSortOption = PdfSortOption.ALPHABETICAL,
    ): File {
        val isSerialEnabled = community.isSerialEnabled
        val rows = toSortedRows(members, community, serials, sortOption)

        val document = PdfDocument()
        val usableWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

        // Define column widths
        val columns = buildColumns(isSerialEnabled, usableWidth)

        // Calculate rows per page (accounting for header on first page)
        val contentAreaFirstPage = PAGE_HEIGHT - MARGIN_TOP - HEADER_HEIGHT - MARGIN_BOTTOM - ROW_HEIGHT // table header
        val contentAreaOtherPages = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM - ROW_HEIGHT // table header
        val rowsFirstPage = (contentAreaFirstPage / ROW_HEIGHT).toInt()
        val rowsOtherPages = (contentAreaOtherPages / ROW_HEIGHT).toInt()

        var pageIndex = 0
        var rowIndex = 0
        val totalRows = rows.size

        while (rowIndex < totalRows || pageIndex == 0) {
            pageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y: Float

            if (pageIndex == 1) {
                // Draw branded header on first page
                y = drawHeader(canvas, communityName, members.size, isSerialEnabled, sortOption)
            } else {
                y = MARGIN_TOP
            }

            // Draw table header
            y = drawTableHeader(canvas, columns, y)

            // Draw rows
            val maxRows = if (pageIndex == 1) rowsFirstPage else rowsOtherPages
            var rowsOnPage = 0
            while (rowIndex < totalRows && rowsOnPage < maxRows) {
                val row = rows[rowIndex]
                y = drawTableRow(canvas, columns, row, y, rowIndex % 2 == 1, isSerialEnabled)
                rowIndex++
                rowsOnPage++
            }

            // Footer
            drawFooter(canvas, pageIndex)

            document.finishPage(page)
        }

        // Write to cache directory
        val dir = File(context.cacheDir, "pdf_exports")
        if (!dir.exists()) dir.mkdirs()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedName = communityName.replace(Regex("[^A-Za-z0-9_\\- ]"), "").take(30).trim()
        val file = File(dir, "${sanitizedName}_Members_$dateStr.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    /**
     * Convert members to sorted rows based on the selected sort option.
     */
    private fun toSortedRows(
        members: List<User>,
        community: Community,
        serials: Map<String, String>,
        sortOption: PdfSortOption,
    ): List<MemberRow> {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val unsortedRows = members.map { user ->
            val serial = serials[user.uid] ?: ""
            MemberRow(
                index = 0, // Will be set after sorting
                serialId = serial,
                name = user.name.ifEmpty { "Unknown" },
                bloodGroup = user.bloodGroup.ifEmpty { "—" },
                totalDonations = user.totalDonations,
                lastDonation = user.lastDonationDate?.let { dateFormat.format(it) } ?: "—",
            ) to user // pair with User for sorting by createdAt
        }

        val sorted = when (sortOption) {
            PdfSortOption.ALPHABETICAL -> unsortedRows.sortedBy { it.first.name.lowercase() }

            PdfSortOption.BLOOD_GROUP -> unsortedRows.sortedWith(
                compareBy<Pair<MemberRow, User>> { bloodGroupOrder(it.first.bloodGroup) }
                    .thenBy { it.first.name.lowercase() },
            )

            PdfSortOption.SERIAL_ID -> unsortedRows.sortedWith(
                compareBy<Pair<MemberRow, User>> { it.first.serialId.ifEmpty { "zzz" } }
                    .thenBy { it.first.name.lowercase() },
            )

            PdfSortOption.TIME -> unsortedRows.sortedBy { it.second.createdAt?.time ?: Long.MAX_VALUE }
        }

        return sorted.mapIndexed { idx, pair ->
            pair.first.copy(index = idx + 1)
        }
    }

    /**
     * Returns an ordering value so blood groups sort in a natural order.
     */
    private fun bloodGroupOrder(bg: String): Int {
        return when (bg.trim().uppercase()) {
            "A+" -> 0
            "A-" -> 1
            "B+" -> 2
            "B-" -> 3
            "O+" -> 4
            "O-" -> 5
            "AB+" -> 6
            "AB-" -> 7
            else -> 8
        }
    }

    // ─── Column definitions ──────────────────────────────────────

    data class Column(val header: String, val width: Float, val align: Paint.Align)

    private fun buildColumns(isSerialEnabled: Boolean, usableWidth: Float): List<Column> {
        return if (isSerialEnabled) {
            listOf(
                Column("#", usableWidth * 0.06f, Paint.Align.CENTER),
                Column("ID", usableWidth * 0.14f, Paint.Align.LEFT),
                Column("Name", usableWidth * 0.28f, Paint.Align.LEFT),
                Column("Blood", usableWidth * 0.12f, Paint.Align.CENTER),
                Column("Donations", usableWidth * 0.16f, Paint.Align.CENTER),
                Column("Last Donated", usableWidth * 0.24f, Paint.Align.LEFT),
            )
        } else {
            listOf(
                Column("#", usableWidth * 0.07f, Paint.Align.CENTER),
                Column("Name", usableWidth * 0.32f, Paint.Align.LEFT),
                Column("Blood", usableWidth * 0.13f, Paint.Align.CENTER),
                Column("Donations", usableWidth * 0.18f, Paint.Align.CENTER),
                Column("Last Donated", usableWidth * 0.30f, Paint.Align.LEFT),
            )
        }
    }

    // ─── Drawing helpers ─────────────────────────────────────────

    private val titlePaint = Paint().apply {
        color = Color.parseColor("#C62828") // BloodRed
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 10f
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.WHITE
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private val headerBgPaint = Paint().apply {
        color = Color.parseColor("#C62828")
        style = Paint.Style.FILL
    }

    private val cellPaint = Paint().apply {
        color = Color.parseColor("#212121")
        textSize = 9f
        isAntiAlias = true
    }

    private val stripePaint = Paint().apply {
        color = Color.parseColor("#FFF3F3")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 0.5f
    }

    private val footerPaint = Paint().apply {
        color = Color.GRAY
        textSize = 8f
        isAntiAlias = true
    }

    private fun drawHeader(
        canvas: Canvas,
        communityName: String,
        memberCount: Int,
        isSerialEnabled: Boolean,
        sortOption: PdfSortOption,
    ): Float {
        var y = MARGIN_TOP + 20f

        // Community name
        canvas.drawText(communityName, MARGIN_LEFT, y, titlePaint)
        y += 16f

        // Member count and date
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Members: $memberCount  •  Generated: $dateStr", MARGIN_LEFT, y, subtitlePaint)
        y += 12f

        // Sort order
        canvas.drawText("Sorted by: ${sortOption.label}", MARGIN_LEFT, y, subtitlePaint)
        y += 10f

        if (isSerialEnabled) {
            canvas.drawText("Serial IDs: Enabled", MARGIN_LEFT, y, subtitlePaint)
            y += 10f
        }

        // Divider line
        y += 6f
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint)
        y += 10f

        return y
    }

    private fun drawTableHeader(canvas: Canvas, columns: List<Column>, startY: Float): Float {
        // Background
        canvas.drawRect(
            MARGIN_LEFT, startY,
            PAGE_WIDTH - MARGIN_RIGHT, startY + ROW_HEIGHT,
            headerBgPaint,
        )

        var x = MARGIN_LEFT
        columns.forEach { col ->
            val textX = when (col.align) {
                Paint.Align.CENTER -> x + col.width / 2f
                Paint.Align.RIGHT -> x + col.width - 4f
                else -> x + 4f
            }
            headerPaint.textAlign = col.align
            canvas.drawText(col.header, textX, startY + 15f, headerPaint)
            x += col.width
        }

        return startY + ROW_HEIGHT
    }

    private fun drawTableRow(
        canvas: Canvas,
        columns: List<Column>,
        row: MemberRow,
        startY: Float,
        isStripe: Boolean,
        isSerialEnabled: Boolean,
    ): Float {
        if (isStripe) {
            canvas.drawRect(
                MARGIN_LEFT, startY,
                PAGE_WIDTH - MARGIN_RIGHT, startY + ROW_HEIGHT,
                stripePaint,
            )
        }

        // Bottom line
        canvas.drawLine(
            MARGIN_LEFT, startY + ROW_HEIGHT,
            PAGE_WIDTH - MARGIN_RIGHT, startY + ROW_HEIGHT,
            linePaint,
        )

        val values = if (isSerialEnabled) {
            // With serial ID column
            listOf(
                "${row.index}",
                row.serialId.ifEmpty { "—" },
                row.name,
                row.bloodGroup,
                row.totalDonations.toString(),
                row.lastDonation,
            )
        } else {
            // Without serial ID column
            listOf(
                "${row.index}",
                row.name,
                row.bloodGroup,
                row.totalDonations.toString(),
                row.lastDonation,
            )
        }

        var x = MARGIN_LEFT
        columns.forEachIndexed { idx, col ->
            val value = values.getOrElse(idx) { "" }
            val textX = when (col.align) {
                Paint.Align.CENTER -> x + col.width / 2f
                Paint.Align.RIGHT -> x + col.width - 4f
                else -> x + 4f
            }
            cellPaint.textAlign = col.align
            // Truncate long text
            val truncated = if (value.length > 25) value.take(22) + "…" else value
            canvas.drawText(truncated, textX, startY + 15f, cellPaint)
            x += col.width
        }

        return startY + ROW_HEIGHT
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val text = "Page $pageNumber  •  Spondon"
        footerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
    }
}
