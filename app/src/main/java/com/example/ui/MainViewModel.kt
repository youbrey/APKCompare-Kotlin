package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.PdfReportExporter
import com.example.data.local.HistoryRepository
import com.example.data.model.ApkComparisonReport
import com.example.data.model.MethodDiff
import com.example.data.parser.ApkAnalyzer
import com.example.data.sample.SampleApkProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MainUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val currentReport: ApkComparisonReport? = null,
    val historyList: List<ApkComparisonReport> = emptyList(),
    val selectedTab: Int = 0, // 0: Overview, 1: DEX Code Diffs, 2: Resources & Manifest, 3: Libraries, 4: Signatures & Hashes
    val searchQuery: String = "",
    val activeFilter: String = "ALL", // "ALL", "ADDED", "REMOVED", "MODIFIED", "SECURITY"
    val selectedMethodForDetail: MethodDiff? = null,
    val pdfExportFile: File? = null,
    val userNotice: String? = null
)

class MainViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val analyzer = ApkAnalyzer()

    init {
        // Observe local database history
        viewModelScope.launch {
            repository.historyList.collect { list ->
                _uiState.update { it.copy(historyList = list) }
            }
        }
    }

    fun loadSampleDemoReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Memuat sampel audit APK v1.0 vs v1.2...") }
            withContext(Dispatchers.Default) {
                val report = SampleApkProvider.createSampleReport()
                repository.saveReport(report)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentReport = report,
                        selectedTab = 0,
                        userNotice = "Sampel audit APK berhasil dimuat!"
                    )
                }
            }
        }
    }

    fun compareUserApks(
        context: Context,
        uri1: Uri,
        fileName1: String,
        uri2: Uri,
        fileName2: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Menganalisis file APK 1 ($fileName1)...") }

            try {
                val report = withContext(Dispatchers.IO) {
                    val size1 = getFileSize(context, uri1)
                    val stream1 = context.contentResolver.openInputStream(uri1) ?: throw IllegalStateException("Gagal membuka APK 1")
                    val apk1 = analyzer.analyzeApk(stream1, fileName1, size1)
                    stream1.close()

                    _uiState.update { it.copy(loadingMessage = "Menganalisis file APK 2 ($fileName2)...") }
                    val size2 = getFileSize(context, uri2)
                    val stream2 = context.contentResolver.openInputStream(uri2) ?: throw IllegalStateException("Gagal membuka APK 2")
                    val apk2 = analyzer.analyzeApk(stream2, fileName2, size2)
                    stream2.close()

                    _uiState.update { it.copy(loadingMessage = "Membandingkan struktur DEX dan signature...") }
                    val resultReport = analyzer.compareApks(apk1, apk2)
                    repository.saveReport(resultReport)
                    resultReport
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentReport = report,
                        selectedTab = 0,
                        userNotice = "Analisis perbandingan APK selesai!"
                    )
                }
            } catch (e: OutOfMemoryError) {
                // Must be caught separately from Exception below: OutOfMemoryError extends
                // java.lang.Error, not Exception, so "catch (e: Exception)" alone never catches
                // it and the app would force-close instead of showing this message gracefully.
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNotice = "Gagal menganalisis APK: memori tidak cukup untuk memproses file sebesar ini. Coba tutup aplikasi lain lalu ulangi."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userNotice = "Gagal menganalisis APK: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun exportPdf(context: Context) {
        val report = uiState.value.currentReport ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Membuat dokumen PDF laporan...") }
            val file = withContext(Dispatchers.IO) {
                val exporter = PdfReportExporter(context)
                exporter.exportReportToPdf(report)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    pdfExportFile = file,
                    userNotice = if (file != null) "PDF berhasil diekspor ke: ${file.name}" else "Gagal mengekspor PDF"
                )
            }
        }
    }

    fun loadReportFromHistory(report: ApkComparisonReport) {
        _uiState.update {
            it.copy(
                currentReport = report,
                selectedTab = 0
            )
        }
    }

    fun deleteHistoryReport(id: String) {
        viewModelScope.launch {
            repository.deleteReport(id)
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun selectMethodForDetail(methodDiff: MethodDiff?) {
        _uiState.update { it.copy(selectedMethodForDetail = methodDiff) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun clearPdfFile() {
        _uiState.update { it.copy(pdfExportFile = null) }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val size = if (cursor.moveToFirst() && sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                if (size > 0) size else 10_000_000L
            } ?: 10_000_000L
        } catch (e: Exception) {
            10_000_000L
        }
    }
}

class MainViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}
