package com.example.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.ApkComparisonReport
import com.example.data.model.RiskLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportExporter(private val context: Context) {

    fun exportReportToPdf(report: ApkComparisonReport): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42) // Slate 900
        }
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.rgb(100, 116, 139) // Slate 500
        }
        val sectionPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(2, 132, 199) // Cyan 600
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            color = Color.rgb(30, 41, 59) // Slate 800
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(15, 23, 42)
        }

        var yPos = 40f
        val leftMargin = 36f
        val rightMargin = 559f

        // Top Banner background
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(0f, 0f, 595f, 75f, paint)

        // Header Title
        canvas.drawText("LAPORAN AUDIT & PERBANDINGAN APK", leftMargin, 35f, titlePaint)

        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date(report.timestamp))
        canvas.drawText("Dibuat pada: $dateStr | ID Laporan: ${report.id.take(8)}", leftMargin, 55f, subTitlePaint)

        yPos = 95f

        // Risk Level Badge Box
        val badgeColor = when (report.overallRiskLevel) {
            RiskLevel.LOW -> Color.rgb(16, 185, 129) // Emerald
            RiskLevel.MEDIUM -> Color.rgb(245, 158, 11) // Amber
            RiskLevel.HIGH -> Color.rgb(239, 68, 68) // Rose
            RiskLevel.CRITICAL -> Color.rgb(185, 28, 28) // Dark Red
        }

        paint.color = badgeColor
        canvas.drawRoundRect(leftMargin, yPos, leftMargin + 180f, yPos + 36f, 8f, 8f, paint)

        val badgeTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }
        canvas.drawText("RISIKO: ${report.overallRiskLevel.name} (${report.riskScore}/100)", leftMargin + 12f, yPos + 22f, badgeTextPaint)

        // Metadata Comparison Box
        yPos += 50f
        canvas.drawText("1. RINGKASAN METADATA & HASH APK", leftMargin, yPos, sectionPaint)
        yPos += 15f

        // Draw Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(leftMargin, yPos, rightMargin, yPos + 20f, paint)
        canvas.drawText("Atribut", leftMargin + 8f, yPos + 14f, boldTextPaint)
        canvas.drawText("APK 1 (Basis)", leftMargin + 160f, yPos + 14f, boldTextPaint)
        canvas.drawText("APK 2 (Audit / Perbandingan)", leftMargin + 350f, yPos + 14f, boldTextPaint)

        yPos += 20f

        val metadataRows = listOf(
            Triple("Nama File", report.apk1.fileName, report.apk2.fileName),
            Triple("Package Name", report.apk1.packageName, report.apk2.packageName),
            Triple("Versi App", "${report.apk1.versionName} (${report.apk1.versionCode})", "${report.apk2.versionName} (${report.apk2.versionCode})"),
            Triple("Ukuran File", formatSize(report.apk1.fileSize), formatSize(report.apk2.fileSize)),
            Triple("Min / Target SDK", "API ${report.apk1.minSdkVersion} / ${report.apk1.targetSdkVersion}", "API ${report.apk2.minSdkVersion} / ${report.apk2.targetSdkVersion}"),
            Triple("Status Signature", if (report.signatureMatches) "SERUPA (MATCH)" else "BERBEDA (REPACKAGED)", if (report.signatureMatches) "SERUPA (MATCH)" else "BERBEDA (REPACKAGED)"),
            Triple("SHA-256 Hash", report.apk1.hashes.sha256.take(16) + "...", report.apk2.hashes.sha256.take(16) + "...")
        )

        for (row in metadataRows) {
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(leftMargin, yPos, rightMargin, yPos + 18f, paint)

            paint.color = Color.rgb(203, 213, 225)
            canvas.drawLine(leftMargin, yPos + 18f, rightMargin, yPos + 18f, paint)

            canvas.drawText(row.first, leftMargin + 8f, yPos + 13f, textPaint)
            canvas.drawText(truncate(row.second, 26), leftMargin + 160f, yPos + 13f, textPaint)
            canvas.drawText(truncate(row.third, 26), leftMargin + 350f, yPos + 13f, textPaint)

            yPos += 18f
        }

        // DEX Code & Method Diffs
        yPos += 20f
        canvas.drawText("2. ANALISIS BINER DEX & KODE TINGKAT METHOD", leftMargin, yPos, sectionPaint)
        yPos += 15f

        val dexSummary = "Total Kelas Berubah: ${report.classDiffs.size} | Kelas Ditambahkan: ${report.classDiffs.count { it.diffType == com.example.data.model.DiffType.ADDED }} | Kelas Dihapus: ${report.classDiffs.count { it.diffType == com.example.data.model.DiffType.REMOVED }}"
        canvas.drawText(dexSummary, leftMargin, yPos, textPaint)
        yPos += 15f

        var count = 0
        for (diff in report.classDiffs.take(6)) {
            val statusStr = when (diff.diffType) {
                com.example.data.model.DiffType.ADDED -> "[+ DITAMBAHKAN]"
                com.example.data.model.DiffType.REMOVED -> "[- DIHAPUS]"
                com.example.data.model.DiffType.MODIFIED -> "[~ DIMODIFIKASI]"
                com.example.data.model.DiffType.UNCHANGED -> "[UNCHANGED]"
            }
            canvas.drawText("$statusStr ${diff.className}", leftMargin + 10f, yPos, boldTextPaint)
            yPos += 14f

            for (m in diff.methodDiffs.take(3)) {
                val mType = if (m.diffType == com.example.data.model.DiffType.ADDED) "+ " else if (m.diffType == com.example.data.model.DiffType.REMOVED) "- " else "~ "
                val mSec = if (m.method2?.isSecuritySensitive == true || m.method1?.isSecuritySensitive == true) " [SECURITY SENSITIVE]" else ""
                canvas.drawText("   $mType Method: ${m.methodName}${m.descriptor}$mSec", leftMargin + 15f, yPos, subTitlePaint)
                yPos += 12f
            }
            count++
            if (yPos > 720f) break
        }

        // Added Permissions Section
        if (report.addedPermissions.isNotEmpty() && yPos < 750f) {
            yPos += 15f
            canvas.drawText("3. IZIN SISTEM DITAMBAHKAN (${report.addedPermissions.size})", leftMargin, yPos, sectionPaint)
            yPos += 14f
            for (p in report.addedPermissions.take(4)) {
                canvas.drawText("• $p", leftMargin + 10f, yPos, textPaint)
                yPos += 12f
            }
        }

        // Footer
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(leftMargin, 800f, rightMargin, 800f, paint)
        canvas.drawText("APK Comparator & Security Audit Engine — Halaman 1 dari 1", leftMargin, 815f, subTitlePaint)

        pdfDocument.finishPage(page)

        return try {
            val exportDir = File(context.cacheDir, "reports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val pdfFile = File(exportDir, "APK_Diff_Report_${report.id.take(6)}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) "%.2f MB".format(mb) else "%.1f KB".format(kb)
    }

    private fun truncate(str: String, maxLen: Int): String {
        return if (str.length > maxLen) str.take(maxLen - 3) + "..." else str
    }
}
