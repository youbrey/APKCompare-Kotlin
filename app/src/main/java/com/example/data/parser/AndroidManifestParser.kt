package com.example.data.parser

import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidManifestParser {

    data class ParsedManifest(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdkVersion: Int,
        val targetSdkVersion: Int,
        val permissions: List<String>,
        val activitiesCount: Int,
        val servicesCount: Int,
        val receiversCount: Int,
        val providersCount: Int
    )

    fun parseManifest(manifestBytes: ByteArray): ParsedManifest {
        var pkgName = "com.unknown.app"
        var vName = "1.0.0"
        var vCode = 1L
        var minSdk = 21
        var targetSdk = 34
        val permissions = mutableSetOf<String>()
        var activities = 0
        var services = 0
        var receivers = 0
        var providers = 0

        try {
            val strings = extractStringsFromBinaryXml(manifestBytes)

            for (str in strings) {
                when {
                    str.startsWith("android.permission.") -> {
                        permissions.add(str)
                    }
                    str.contains("permission") && str.contains(".") -> {
                        permissions.add(str)
                    }
                    str.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")) -> {
                        if (pkgName == "com.unknown.app" && !str.startsWith("android.") && !str.startsWith("java.") && !str.startsWith("kotlin.")) {
                            pkgName = str
                        }
                    }
                    str.matches(Regex("^\\d+\\.\\d+(\\.\\d+)?(-[a-zA-Z0-9]+)?$")) -> {
                        vName = str
                    }
                }
            }

            // Simple heuristic component search in strings
            activities = strings.count { it.endsWith("Activity") || it.contains(".ui.") }
            services = strings.count { it.endsWith("Service") }
            receivers = strings.count { it.endsWith("Receiver") }
            providers = strings.count { it.endsWith("Provider") }

            if (activities == 0) activities = 1

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedManifest(
            packageName = pkgName,
            versionName = vName,
            versionCode = vCode,
            minSdkVersion = minSdk,
            targetSdkVersion = targetSdk,
            permissions = permissions.toList().sorted(),
            activitiesCount = activities,
            servicesCount = services,
            receiversCount = receivers,
            providersCount = providers
        )
    }

    private fun extractStringsFromBinaryXml(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        if (bytes.size < 32) return strings

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.short.toInt() and 0xFFFF
        if (magic != 0x0003) { // Chunk type XML
            return strings
        }

        // Header size & Chunk size
        buffer.position(8)

        // Read chunks looking for String Pool (chunk type 0x0001)
        while (buffer.hasRemaining() && buffer.position() + 8 <= bytes.size) {
            val chunkType = buffer.short.toInt() and 0xFFFF
            val headerSize = buffer.short.toInt() and 0xFFFF
            val chunkSize = buffer.int

            val startPos = buffer.position() - 8
            if (chunkSize <= 0 || startPos + chunkSize > bytes.size) break

            if (chunkType == 0x0001) { // RES_STRING_POOL_TYPE
                val stringCountRaw = buffer.int
                val styleCount = buffer.int
                val flags = buffer.int
                val stringsStart = buffer.int
                val isUtf8 = (flags and (1 shl 8)) != 0

                // Sanity bound: each string offset entry is 4 bytes, so stringCount can never
                // legitimately exceed (chunkSize / 4). Without this check, a malformed or
                // corrupted AndroidManifest.xml could set stringCount to a huge/garbage value
                // and IntArray(stringCount) would attempt a giant single allocation - the same
                // class of bug that caused the OOM in DexParser's downstream report. Clamp
                // instead of trusting the raw header field.
                val maxPlausibleCount = (chunkSize / 4).coerceAtLeast(0)
                if (stringCountRaw < 0 || stringCountRaw > maxPlausibleCount) {
                    break
                }
                val stringCount = stringCountRaw

                val stringOffsets = IntArray(stringCount)
                for (i in 0 until stringCount) {
                    if (buffer.position() + 4 <= bytes.size) {
                        stringOffsets[i] = buffer.int
                    }
                }

                val poolOffset = startPos + stringsStart
                for (i in 0 until stringCount) {
                    val strPos = poolOffset + stringOffsets[i]
                    if (strPos in 0 until bytes.size) {
                        buffer.position(strPos)
                        val str = if (isUtf8) readUtf8String(buffer) else readUtf16String(buffer)
                        if (str.isNotBlank()) {
                            strings.add(str.trim())
                        }
                    }
                }
                break
            } else {
                buffer.position(startPos + chunkSize)
            }
        }
        return strings
    }

    private fun readUtf8String(buffer: ByteBuffer): String {
        try {
            readLeb128(buffer) // char count
            val byteCount = readLeb128(buffer) // byte count
            if (byteCount <= 0 || buffer.remaining() < byteCount) return ""
            val strBytes = ByteArray(byteCount)
            buffer.get(strBytes)
            return String(strBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun readUtf16String(buffer: ByteBuffer): String {
        try {
            val charCount = buffer.short.toInt() and 0xFFFF
            if (charCount <= 0 || buffer.remaining() < charCount * 2) return ""
            val chars = CharArray(charCount)
            for (i in 0 until charCount) {
                chars[i] = buffer.char
            }
            return String(chars)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun readLeb128(buffer: ByteBuffer): Int {
        var result = 0
        var shift = 0
        while (buffer.hasRemaining()) {
            val b = buffer.get().toInt()
            result = result or ((b and 0x7f) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        return result
    }
}
