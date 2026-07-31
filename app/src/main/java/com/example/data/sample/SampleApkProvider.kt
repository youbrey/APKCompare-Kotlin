package com.example.data.sample

import com.example.data.model.*
import com.example.data.parser.ApkAnalyzer

object SampleApkProvider {

    fun createSampleReport(): ApkComparisonReport {
        // Base APK v1.0
        val apk1 = ApkMetadata(
            fileName = "E-Banking-v1.0.0.apk",
            packageName = "com.securebank.mobile",
            versionName = "1.0.0",
            versionCode = 100,
            minSdkVersion = 24,
            targetSdkVersion = 33,
            fileSize = 18_450_000L, // ~18.4 MB
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.USE_BIOMETRIC",
                "android.permission.VIBRATE"
            ),
            activitiesCount = 12,
            servicesCount = 3,
            receiversCount = 2,
            providersCount = 1,
            hashes = SignatureHashes(
                md5 = "e2fc714c4727ee9395f324cd2e7f331f",
                sha1 = "2b4f92d2e1c9a8b7f6e5d4c3b2a1098877665544",
                sha256 = "8f9e0d1c2b3a4f5e6d7c8b9a0f1e2d3c4b5a6f7e8d9c0b1a2f3e4d5c6b7a8f9e",
                certInfo = CertificateInfo(
                    subject = "CN=SecureBank Mobile, OU=Security Engineering, O=SecureBank Inc, C=ID",
                    issuer = "CN=SecureBank Mobile Root CA, O=SecureBank Inc, C=ID",
                    serialNumber = "7F8A2B9C0D1E2F3A",
                    validFrom = "2024-01-01 00:00:00 GMT",
                    validUntil = "2049-01-01 00:00:00 GMT",
                    sigAlgName = "SHA256withRSA",
                    signingSchemes = "v1 / v2 / v3"
                )
            ),
            classes = listOf(
                ClassInfo(
                    className = "com.securebank.mobile.ui.login.LoginActivity",
                    packageName = "com.securebank.mobile.ui.login",
                    simpleName = "LoginActivity",
                    methods = listOf(
                        MethodInfo("onCreate", "(Landroid/os/Bundle;)V", "void", listOf("Bundle"), "public"),
                        MethodInfo("authenticateUser", "(Ljava/string;Ljava/string;)Z", "boolean", listOf("String", "String"), "private", isSecuritySensitive = true, securityCategory = "Kredensial & Autentikasi")
                    )
                ),
                ClassInfo(
                    className = "com.securebank.mobile.crypto.AesCryptoManager",
                    packageName = "com.securebank.mobile.crypto",
                    simpleName = "AesCryptoManager",
                    methods = listOf(
                        MethodInfo("encryptData", "([B[B)[B", "byte[]", listOf("byte[]", "byte[]"), "public static", isSecuritySensitive = true, securityCategory = "Kriptografi & Enkripsi"),
                        MethodInfo("decryptData", "([B[B)[B", "byte[]", listOf("byte[]", "byte[]"), "public static", isSecuritySensitive = true, securityCategory = "Kriptografi & Enkripsi")
                    )
                ),
                ClassInfo(
                    className = "com.securebank.mobile.network.ApiClient",
                    packageName = "com.securebank.mobile.network",
                    simpleName = "ApiClient",
                    methods = listOf(
                        MethodInfo("executeTransaction", "(Lcom/securebank/TransactionRequest;)Lcom/securebank/TransactionResponse;", "TransactionResponse", listOf("TransactionRequest"), "public", isSecuritySensitive = true, securityCategory = "Jaringan & API")
                    )
                )
            ),
            resources = listOf(
                ResourceItem("res/drawable/ic_logo.png", "drawable", 124000),
                ResourceItem("res/layout/activity_login.xml", "layout", 18500),
                ResourceItem("res/values/strings.xml", "values", 42000),
                ResourceItem("lib/arm64-v8a/libbankcrypto.so", "native_so", 1250000)
            ),
            libraries = listOf(
                LibraryItem("Retrofit", "Networking", "Package Prefix"),
                LibraryItem("OkHttp", "Networking", "Package Prefix"),
                LibraryItem("AndroidX Room ORM", "Database", "Package Prefix"),
                LibraryItem("libbankcrypto.so", "Security / Crypto", "lib/*.so")
            ),
            dexFileCount = 2
        )

        // Updated Audit APK v1.2 (contains added permissions, modified crypto, repackaged signature alert)
        val apk2 = ApkMetadata(
            fileName = "E-Banking-v1.2.0-Audit.apk",
            packageName = "com.securebank.mobile",
            versionName = "1.2.0",
            versionCode = 120,
            minSdkVersion = 24,
            targetSdkVersion = 34,
            fileSize = 21_890_000L, // ~21.8 MB
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.USE_BIOMETRIC",
                "android.permission.VIBRATE",
                "android.permission.READ_SMS", // Added dangerous
                "android.permission.CAMERA", // Added dangerous
                "android.permission.POST_NOTIFICATIONS"
            ),
            activitiesCount = 15,
            servicesCount = 4,
            receiversCount = 3,
            providersCount = 1,
            hashes = SignatureHashes(
                md5 = "7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d",
                sha1 = "11223344556677889900aabbccddeeff12345678",
                sha256 = "aa11bb22cc33dd44ee55ff667788990011223344556677889900aabbccddeeff", // Different SHA256 (Repackaged!)
                certInfo = CertificateInfo(
                    subject = "CN=Unknown Developer, OU=Debug, O=ThirdParty, C=US",
                    issuer = "CN=Unknown Developer Root",
                    serialNumber = "1234567890ABCDEF",
                    validFrom = "2025-01-01 00:00:00 GMT",
                    validUntil = "2050-01-01 00:00:00 GMT",
                    sigAlgName = "SHA256withRSA",
                    signingSchemes = "v1 / v2"
                )
            ),
            classes = listOf(
                ClassInfo(
                    className = "com.securebank.mobile.ui.login.LoginActivity",
                    packageName = "com.securebank.mobile.ui.login",
                    simpleName = "LoginActivity",
                    methods = listOf(
                        MethodInfo("onCreate", "(Landroid/os/Bundle;)V", "void", listOf("Bundle"), "public"),
                        MethodInfo("authenticateUser", "(Ljava/string;Ljava/string;)Z", "boolean", listOf("String", "String"), "private", isSecuritySensitive = true, securityCategory = "Kredensial & Autentikasi"),
                        MethodInfo("interceptSmsOtp", "(Ljava/string;)V", "void", listOf("String"), "public static", isSecuritySensitive = true, securityCategory = "Akses Sensor & Data Sensitif") // Added method!
                    )
                ),
                ClassInfo(
                    className = "com.securebank.mobile.crypto.AesCryptoManager",
                    packageName = "com.securebank.mobile.crypto",
                    simpleName = "AesCryptoManager",
                    methods = listOf(
                        MethodInfo("encryptData", "([B[B)[B", "byte[]", listOf("byte[]", "byte[]"), "public static", isSecuritySensitive = true, securityCategory = "Kriptografi & Enkripsi"),
                        MethodInfo("decryptData", "([B[B)[B", "byte[]", listOf("byte[]", "byte[]"), "public static", isSecuritySensitive = true, securityCategory = "Kriptografi & Enkripsi"),
                        MethodInfo("bypassSslCheck", "()V", "void", emptyList(), "public static", isSecuritySensitive = true, securityCategory = "Jaringan & API") // Dangerous added method!
                    )
                ),
                ClassInfo(
                    className = "com.securebank.mobile.security.RootChecker", // Added class!
                    packageName = "com.securebank.mobile.security",
                    simpleName = "RootChecker",
                    methods = listOf(
                        MethodInfo("isDeviceRooted", "()Z", "boolean", emptyList(), "public", isSecuritySensitive = true, securityCategory = "Integritas & Anti-Root"),
                        MethodInfo("detectHookingFramework", "()Z", "boolean", emptyList(), "public", isSecuritySensitive = true, securityCategory = "Integritas & Anti-Root")
                    )
                )
            ),
            resources = listOf(
                ResourceItem("res/drawable/ic_logo.png", "drawable", 124000),
                ResourceItem("res/drawable/bg_scanner.xml", "drawable", 45000), // Added res
                ResourceItem("res/layout/activity_login.xml", "layout", 21000), // Modified
                ResourceItem("res/layout/activity_scanner.xml", "layout", 16000), // Added
                ResourceItem("lib/arm64-v8a/libbankcrypto.so", "native_so", 1250000),
                ResourceItem("lib/arm64-v8a/libanalytics_native.so", "native_so", 890000) // Added native lib!
            ),
            libraries = listOf(
                LibraryItem("Retrofit", "Networking", "Package Prefix"),
                LibraryItem("OkHttp", "Networking", "Package Prefix"),
                LibraryItem("AndroidX Room ORM", "Database", "Package Prefix"),
                LibraryItem("libbankcrypto.so", "Security / Crypto", "lib/*.so"),
                LibraryItem("libanalytics_native.so", "Native C++ Binary", "lib/*.so"), // Added
                LibraryItem("Adjust Attribution", "Analytics", "Package Prefix") // Added
            ),
            dexFileCount = 3
        )

        val analyzer = ApkAnalyzer()
        return analyzer.compareApks(apk1, apk2)
    }
}
