package com.example.ui.screens

import android.content.Intent
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val report = uiState.currentReport ?: return

    // Show method detail dialog if selected
    if (uiState.selectedMethodForDetail != null) {
        MethodDetailDialog(
            methodDiff = uiState.selectedMethodForDetail,
            onDismiss = { viewModel.selectMethodForDetail(null) }
        )
    }

    // Trigger PDF share if generated
    LaunchedEffect(uiState.pdfExportFile) {
        val pdfFile = uiState.pdfExportFile
        if (pdfFile != null) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF"))
            } catch (e: Exception) {
                // Fallback direct intent
            } finally {
                viewModel.clearPdfFile()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Laporan Perbandingan APK",
                            color = Slate100,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${report.apk1.fileName} vs ${report.apk2.fileName}",
                            color = Slate400,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate100)
                    }
                },
                actions = {
                    // Export PDF
                    IconButton(onClick = { viewModel.exportPdf(context) }) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = CyanGlow)
                    }
                    // Share summary text
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Audit APK - ${report.apk1.fileName}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                🔍 LAPORAN PERBANDINGAN APK
                                File Basis: ${report.apk1.fileName}
                                File Audit: ${report.apk2.fileName}
                                Tingkat Risiko: ${report.overallRiskLevel.name} (${report.riskScore}/100)
                                Signature Match: ${if (report.signatureMatches) "SERUPA" else "BERBEDA (REPACKAGED)"}
                                Kelas Berubah: ${report.classDiffs.size}
                                Izin Ditambahkan: ${report.addedPermissions.size}
                                
                                Dibuat dengan APK Comparator & DEX Audit Engine.
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Ringkasan Audit"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Slate100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Slate900,
                contentColor = CyanBright,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = DarkCardBorder) }
            ) {
                val tabs = listOf(
                    Pair("📊 Ringkasan", 0),
                    Pair("🧬 DEX & Code Diff", 1),
                    Pair("📦 Resource & Manifest", 2),
                    Pair("📚 Pustaka & Native", 3),
                    Pair("🔐 Signature & Hash", 4)
                )

                tabs.forEach { (label, index) ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (uiState.selectedTab == index) CyanGlow else Slate400
                            )
                        }
                    )
                }
            }

            // Tab Contents
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    0 -> OverviewTab(report = report)
                    1 -> DexCodeDiffTab(
                        report = report,
                        searchQuery = uiState.searchQuery,
                        activeFilter = uiState.activeFilter,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setFilter(it) },
                        onSelectMethod = { viewModel.selectMethodForDetail(it) }
                    )
                    2 -> ResourceManifestTab(report = report)
                    3 -> LibrariesTab(report = report)
                    4 -> SignaturesTab(report = report)
                }
            }
        }
    }
}

@Composable
fun OverviewTab(report: ApkComparisonReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Risk Header
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PENILAIAN KEAMANAN & RISIKO",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        RiskBadge(riskLevel = report.overallRiskLevel, score = report.riskScore)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = report.summaryText,
                        color = Slate200,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (report.securityAlerts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PERINGATAN KEAMANAN UTAMA:",
                            color = RoseRemoved,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        for (alert in report.securityAlerts) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF321014),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RoseRemoved.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RoseRemoved,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = alert.title,
                                            color = RoseLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = alert.description,
                                            color = Slate200,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metrics 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "Ukuran Delta",
                        value = formatSizeDelta(report.sizeDeltaBytes),
                        subtitle = "${formatSize(report.apk1.fileSize)} ➔ ${formatSize(report.apk2.fileSize)}",
                        icon = Icons.Default.Storage,
                        accentColor = if (report.sizeDeltaBytes > 0) AmberModified else EmeraldAdded,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Kelas DEX",
                        value = "${report.classDiffs.size} Berubah",
                        subtitle = "+${report.classDiffs.count { it.diffType == DiffType.ADDED }} | -${report.classDiffs.count { it.diffType == DiffType.REMOVED }}",
                        icon = Icons.Default.Code,
                        accentColor = CyanBright,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        title = "Izin Ditambahkan",
                        value = "+${report.addedPermissions.size} Izin",
                        subtitle = if (report.addedPermissions.isNotEmpty()) report.addedPermissions.first().take(22) + "..." else "Tidak ada izin baru",
                        icon = Icons.Default.Security,
                        accentColor = if (report.addedPermissions.isNotEmpty()) RoseRemoved else EmeraldAdded,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Hash Signature",
                        value = if (report.signatureMatches) "MATCH" else "MISMATCH",
                        subtitle = if (report.signatureMatches) "Sertifikat Identik" else "Potensi Repackaged!",
                        icon = if (report.signatureMatches) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                        accentColor = if (report.signatureMatches) EmeraldAdded else RoseRemoved,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick File Comparison Summary Table
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INFO LENGKAP KEDUA APK",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InfoRow("Package Name", report.apk1.packageName, report.apk2.packageName)
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Versi App", "${report.apk1.versionName} (${report.apk1.versionCode})", "${report.apk2.versionName} (${report.apk2.versionCode})")
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Min / Target SDK", "API ${report.apk1.minSdkVersion} / ${report.apk1.targetSdkVersion}", "API ${report.apk2.minSdkVersion} / ${report.apk2.targetSdkVersion}")
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Jumlah File DEX", "${report.apk1.dexFileCount} file DEX", "${report.apk2.dexFileCount} file DEX")
                }
            }
        }
    }
}

@Composable
fun DexCodeDiffTab(
    report: ApkComparisonReport,
    searchQuery: String,
    activeFilter: String,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSelectMethod: (MethodDiff) -> Unit
) {
    val filteredClasses = remember(report.classDiffs, searchQuery, activeFilter) {
        report.classDiffs.filter { cls ->
            val matchesSearch = searchQuery.isBlank() ||
                    cls.className.contains(searchQuery, ignoreCase = true) ||
                    cls.methodDiffs.any { it.methodName.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (activeFilter) {
                "ADDED" -> cls.diffType == DiffType.ADDED
                "REMOVED" -> cls.diffType == DiffType.REMOVED
                "MODIFIED" -> cls.diffType == DiffType.MODIFIED
                "SECURITY" -> cls.methodDiffs.any { it.method2?.isSecuritySensitive == true || it.method1?.isSecuritySensitive == true }
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari nama kelas atau method...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanGlow) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100
                ),
                singleLine = true
            )
        }

        // Filter chips row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Pair("Semua (${report.classDiffs.size})", "ALL"),
                    Pair("Ditambahkan (+)", "ADDED"),
                    Pair("Dihapus (-)", "REMOVED"),
                    Pair("Dimodifikasi (~)", "MODIFIED"),
                    Pair("🔒 Keamanan", "SECURITY")
                )

                filters.forEach { (label, key) ->
                    val isSelected = activeFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(key) },
                        label = { Text(text = label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Slate950,
                            containerColor = DarkCard,
                            labelColor = Slate400
                        )
                    )
                }
            }
        }

        if (filteredClasses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada kelas DEX yang cocok dengan kriteria pencarian.",
                        color = Slate400,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredClasses, key = { it.className }) { classDiff ->
                ClassDiffExpandableCard(classDiff = classDiff, onSelectMethod = onSelectMethod)
            }
        }
    }
}

@Composable
fun ClassDiffExpandableCard(
    classDiff: ClassDiff,
    onSelectMethod: (MethodDiff) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiffTypeBadge(diffType = classDiff.diffType)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = classDiff.className,
                        color = Slate100,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${classDiff.methodDiffs.size} perubahan method | Paket: ${classDiff.packageName}",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyanGlow
                )
            }

            if (expanded && classDiff.methodDiffs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "METHOD TERINTEGRASI DI DALAM KELAS INI:",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (mDiff in classDiff.methodDiffs) {
                        val methodInfo = mDiff.method2 ?: mDiff.method1
                        val isSec = methodInfo?.isSecuritySensitive == true

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSec) CyanAccent.copy(alpha = 0.6f) else Slate700
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMethod(mDiff) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val mSymbol = when (mDiff.diffType) {
                                    DiffType.ADDED -> "+"
                                    DiffType.REMOVED -> "-"
                                    DiffType.MODIFIED -> "~"
                                    DiffType.UNCHANGED -> "="
                                }
                                val mColor = when (mDiff.diffType) {
                                    DiffType.ADDED -> EmeraldAdded
                                    DiffType.REMOVED -> RoseRemoved
                                    DiffType.MODIFIED -> AmberModified
                                    DiffType.UNCHANGED -> Slate400
                                }

                                Text(
                                    text = mSymbol,
                                    color = mColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = mDiff.methodName,
                                            color = Slate100,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isSec) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🔒 ${methodInfo?.securityCategory ?: "Keamanan"}",
                                                color = CyanGlow,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = mDiff.descriptor,
                                        color = Slate400,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceManifestTab(report: ApkComparisonReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Permissions Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PERUBAHAN IZIN MANIFEST (PERMISSIONS)",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (report.addedPermissions.isEmpty() && report.removedPermissions.isEmpty()) {
                        Text(text = "Tidak ada perubahan izin manifest antara APK 1 dan APK 2.", color = Slate200, fontSize = 12.sp)
                    } else {
                        if (report.addedPermissions.isNotEmpty()) {
                            Text(text = "IZIN DITAMBAHKAN (+${report.addedPermissions.size}):", color = RoseRemoved, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            for (p in report.addedPermissions) {
                                Text(
                                    text = "+ $p",
                                    color = RoseLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        if (report.removedPermissions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "IZIN DIHAPUS (-${report.removedPermissions.size}):", color = EmeraldAdded, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            for (p in report.removedPermissions) {
                                Text(
                                    text = "- $p",
                                    color = EmeraldLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Component Count Comparison
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "KOMPONEN MANIFEST (ACTIVITIES / SERVICES / RECEIVERS)",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InfoRow("Activities", "${report.apk1.activitiesCount}", "${report.apk2.activitiesCount}")
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Services", "${report.apk1.servicesCount}", "${report.apk2.servicesCount}")
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Broadcast Receivers", "${report.apk1.receiversCount}", "${report.apk2.receiversCount}")
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Content Providers", "${report.apk1.providersCount}", "${report.apk2.providersCount}")
                }
            }
        }

        // Resource Diffs List
        item {
            Text(
                text = "PERUBAHAN ASET & RESOURCE (${report.resourceDiffs.size} DITEMUKAN)",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        items(report.resourceDiffs.take(15)) { rDiff ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiffTypeBadge(diffType = rDiff.diffType)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rDiff.path,
                            color = Slate100,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Kategori: ${rDiff.type} | Delta: ${formatSizeDelta(rDiff.sizeDelta)}",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibrariesTab(report: ApkComparisonReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "PERBANDINGAN PUSTAKA / SDK PIHAK KETIGA & BINER NATIVE",
                color = Slate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (report.libraryDiffs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCard,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tidak ada penambahan atau pengurangan SDK/Library terdeteksi.",
                            color = Slate200,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(report.libraryDiffs) { lDiff ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiffTypeBadge(diffType = lDiff.diffType)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lDiff.name,
                                color = Slate100,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kategori: ${lDiff.category}",
                                color = CyanGlow,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lDiff.details,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SignaturesTab(report: ApkComparisonReport) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Signature Match Status Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (report.signatureMatches) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (report.signatureMatches) EmeraldAdded else RoseRemoved)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (report.signatureMatches) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                        contentDescription = null,
                        tint = if (report.signatureMatches) EmeraldAdded else RoseRemoved,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (report.signatureMatches) "SER TIFIKAT SIGNATURE DENGAN KUNCI COCOK (MATCH)" else "PERINGATAN: SIGNATURE BERBEDA (MISMATCH / REPACKAGED)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (report.signatureMatches) "Kedua APK ditandatangani dengan kunci sertifikat pengembang yang persis sama." else "Sertifikat kunci penandatanganan APK berbeda! Berpotensi hasil repackaging tak resmi.",
                            color = Slate100,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Hash Table Comparison
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PERBANDINGAN HASH KRIPTOGRAFI FILE APK",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    HashRow("MD5 (APK 1)", report.apk1.hashes.md5)
                    Spacer(modifier = Modifier.height(6.dp))
                    HashRow("MD5 (APK 2)", report.apk2.hashes.md5)

                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    HashRow("SHA-1 (APK 1)", report.apk1.hashes.sha1)
                    Spacer(modifier = Modifier.height(6.dp))
                    HashRow("SHA-1 (APK 2)", report.apk2.hashes.sha1)

                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 10.dp))

                    HashRow("SHA-256 (APK 1)", report.apk1.hashes.sha256)
                    Spacer(modifier = Modifier.height(6.dp))
                    HashRow("SHA-256 (APK 2)", report.apk2.hashes.sha256)
                }
            }
        }

        // X.509 Certificate Details
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DETAIL SERTIFIKAT X.509",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    InfoRow("Subject Certificate", report.apk1.hashes.certInfo.subject, report.apk2.hashes.certInfo.subject)
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Issuer Root", report.apk1.hashes.certInfo.issuer, report.apk2.hashes.certInfo.issuer)
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Serial Number", report.apk1.hashes.certInfo.serialNumber, report.apk2.hashes.certInfo.serialNumber)
                    HorizontalDivider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 6.dp))
                    InfoRow("Skema Penandatanganan", report.apk1.hashes.certInfo.signingSchemes, report.apk2.hashes.certInfo.signingSchemes)
                }
            }
        }
    }
}

@Composable
fun HashRow(label: String, hashValue: String) {
    Column {
        Text(text = label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            text = hashValue,
            color = CyanGlow,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .background(Slate900, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun InfoRow(title: String, val1: String, val2: String) {
    Column {
        Text(text = title.uppercase(), color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "APK 1: $val1",
                color = Slate200,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "APK 2: $val2",
                color = Slate200,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "%.2f MB".format(mb) else "%.1f KB".format(kb)
}

private fun formatSizeDelta(bytes: Long): String {
    val sign = if (bytes > 0) "+" else ""
    return "$sign${formatSize(bytes)}"
}
