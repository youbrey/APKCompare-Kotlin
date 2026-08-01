package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApkComparisonReport
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onOpenReport: () -> Unit
) {
    val context = LocalContext.current

    var apk1Uri by remember { mutableStateOf<Uri?>(null) }
    var apk1Name by remember { mutableStateOf<String?>(null) }

    var apk2Uri by remember { mutableStateOf<Uri?>(null) }
    var apk2Name by remember { mutableStateOf<String?>(null) }

    val launcherApk1 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            apk1Uri = uri
            apk1Name = getFileNameFromUri(context, uri) ?: "APK_1_Base.apk"
        }
    }

    val launcherApk2 = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            apk2Uri = uri
            apk2Name = getFileNameFromUri(context, uri) ?: "APK_2_Audit.apk"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyanAccent,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = Slate950,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "APK Comparator",
                                color = Slate100,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "DEX & Security Audit Engine",
                                color = CyanGlow,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Hero Card Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perbandingan 2 File APK",
                                color = Slate100,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih 2 file APK untuk menganalisis perbedaan kelas DEX tingkat method, resources, pustaka native, dan status hash signature.",
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // APK Selector 1
                        ApkSelectorCard(
                            label = "APK 1 (File Basis / Versi Lama)",
                            fileName = apk1Name,
                            icon = Icons.Outlined.Android,
                            accentColor = CyanBright,
                            onSelect = { launcherApk1.launch("*/*") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // APK Selector 2
                        ApkSelectorCard(
                            label = "APK 2 (File Audit / Versi Baru)",
                            fileName = apk2Name,
                            icon = Icons.Outlined.Build,
                            accentColor = EmeraldAdded,
                            onSelect = { launcherApk2.launch("*/*") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Run Comparison Button
                        Button(
                            onClick = {
                                if (apk1Uri != null && apk2Uri != null && apk1Name != null && apk2Name != null) {
                                    viewModel.compareUserApks(context, apk1Uri!!, apk1Name!!, apk2Uri!!, apk2Name!!)
                                    onOpenReport()
                                }
                            },
                            enabled = apk1Uri != null && apk2Uri != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                disabledContainerColor = Slate800
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mulai Analisis Perbandingan APK",
                                color = if (apk1Uri != null && apk2Uri != null) Slate950 else Slate600,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Demo Sample Button
                        OutlinedButton(
                            onClick = {
                                viewModel.loadSampleDemoReport()
                                onOpenReport()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = CyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ Uji Coba Laporan Sampel Audit (v1.0 vs v1.2)",
                                color = CyanGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Quick Active Comparison Summary if available
            if (uiState.currentReport != null) {
                item {
                    Text(
                        text = "HASIL TERAKHIR DIPROSES",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    HistoryReportCard(
                        report = uiState.currentReport,
                        onClick = onOpenReport,
                        onDelete = null
                    )
                }
            }

            // Local History Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RIWAYAT ANALISIS PERBANDINGAN LOKAL",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (uiState.historyList.isNotEmpty()) {
                        Text(
                            text = "${uiState.historyList.size} Laporan",
                            color = CyanGlow,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (uiState.historyList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum Ada Riwayat Laporan",
                                color = Slate200,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pilih file APK di atas atau jalankan Uji Coba Laporan Sampel untuk melihat perbandingan secara otomatis.",
                                color = Slate400,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.historyList, key = { it.id }) { report ->
                    HistoryReportCard(
                        report = report,
                        onClick = {
                            viewModel.loadReportFromHistory(report)
                            onOpenReport()
                        },
                        onDelete = {
                            viewModel.deleteHistoryReport(report.id)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ApkSelectorCard(
    label: String,
    fileName: String?,
    icon: ImageVector,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (fileName != null) accentColor else Slate700),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = fileName ?: "Klik untuk memilih file .apk",
                    color = if (fileName != null) Slate100 else CyanGlow,
                    fontSize = 13.sp,
                    fontWeight = if (fileName != null) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (fileName != null) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = if (fileName != null) EmeraldAdded else Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HistoryReportCard(
    report: ApkComparisonReport,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiskBadge(riskLevel = report.overallRiskLevel, score = report.riskScore)

                val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(report.timestamp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = RoseRemoved,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.apk1.fileName,
                    color = Slate200,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " vs ",
                    color = CyanGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = report.apk2.fileName,
                    color = Slate200,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📦 Kelas: ${report.classDiffs.size} berubah",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "🛡️ Izin: +${report.addedPermissions.size}",
                    color = if (report.addedPermissions.isNotEmpty()) RoseRemoved else Slate400,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (report.signatureMatches) "🔒 Signature Match" else "⚠️ Signature Mismatch",
                    color = if (report.signatureMatches) EmeraldAdded else RoseRemoved,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) cursor.getString(nameIndex) else null
            } else null
        }
        name ?: uri.lastPathSegment
    } catch (e: Exception) {
        uri.lastPathSegment
    }
}
