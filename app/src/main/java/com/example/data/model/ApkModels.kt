package com.example.data.model

import java.io.Serializable

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class DiffType {
    ADDED, REMOVED, MODIFIED, UNCHANGED
}

data class MethodInfo(
    val name: String,
    val descriptor: String, // e.g. (ILjava/lang/String;)V
    val returnType: String,
    val parameterTypes: List<String>,
    val accessFlags: String,
    val isSecuritySensitive: Boolean = false,
    val securityCategory: String? = null // e.g. "Crypto", "Network", "Reflection", "Permission Check"
) : Serializable

data class ClassInfo(
    val className: String, // e.g. com.example.MainActivity
    val packageName: String,
    val simpleName: String,
    val isInterface: Boolean = false,
    val superClass: String? = null,
    val methods: List<MethodInfo> = emptyList(),
    val methodCount: Int = methods.size,
    val fieldCount: Int = 0
) : Serializable

data class MethodDiff(
    val methodName: String,
    val descriptor: String,
    val diffType: DiffType,
    val method1: MethodInfo? = null,
    val method2: MethodInfo? = null,
    val changeDescription: String? = null
) : Serializable

data class ClassDiff(
    val className: String,
    val packageName: String,
    val diffType: DiffType,
    // NOTE: class1/class2 (full ClassInfo with nested method lists) were removed on purpose.
    // They were never read by ReportScreen/HomeScreen/PdfReportExporter (verified) and were
    // duplicating every method's data 2-3x on top of apk1.classes/apk2.classes, which was the
    // root cause of the OOM during JSON serialization in HistoryRepository.saveReport().
    val methodDiffs: List<MethodDiff> = emptyList(),
    val addedMethodsCount: Int = 0,
    val removedMethodsCount: Int = 0,
    val modifiedMethodsCount: Int = 0
) : Serializable

data class ResourceItem(
    val path: String,
    val type: String, // "drawable", "layout", "values", "raw", "asset", "native_so", "other"
    val sizeBytes: Long
) : Serializable

data class ResourceDiff(
    val path: String,
    val type: String,
    val diffType: DiffType,
    val size1: Long = 0,
    val size2: Long = 0,
    val sizeDelta: Long = size2 - size1
) : Serializable

data class LibraryItem(
    val name: String,
    val category: String, // "Networking", "Database", "UI/Compose", "Security/Crypto", "Analytics", "Native C++"
    val detectedFrom: String // "Package Prefix" or "lib/*.so"
) : Serializable

data class LibraryDiff(
    val name: String,
    val category: String,
    val diffType: DiffType,
    val details: String
) : Serializable

data class CertificateInfo(
    val subject: String = "Unknown",
    val issuer: String = "Unknown",
    val serialNumber: String = "Unknown",
    val validFrom: String = "Unknown",
    val validUntil: String = "Unknown",
    val sigAlgName: String = "Unknown",
    val signingSchemes: String = "v1 / v2"
) : Serializable

data class SignatureHashes(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val certInfo: CertificateInfo = CertificateInfo()
) : Serializable

data class ApkMetadata(
    val fileName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val fileSize: Long,
    val permissions: List<String>,
    val activitiesCount: Int,
    val servicesCount: Int,
    val receiversCount: Int,
    val providersCount: Int,
    val hashes: SignatureHashes,
    val classes: List<ClassInfo>,
    val resources: List<ResourceItem>,
    val libraries: List<LibraryItem>,
    val dexFileCount: Int
) : Serializable

data class SecurityAlert(
    val title: String,
    val description: String,
    val severity: RiskLevel
) : Serializable

data class ApkComparisonReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val apk1: ApkMetadata,
    val apk2: ApkMetadata,
    val overallRiskLevel: RiskLevel,
    val riskScore: Int, // 0 to 100
    val securityAlerts: List<SecurityAlert>,
    val sizeDeltaBytes: Long = apk2.fileSize - apk1.fileSize,
    val classDiffs: List<ClassDiff>,
    val resourceDiffs: List<ResourceDiff>,
    val libraryDiffs: List<LibraryDiff>,
    val addedPermissions: List<String>,
    val removedPermissions: List<String>,
    val signatureMatches: Boolean,
    val summaryText: String
) : Serializable
