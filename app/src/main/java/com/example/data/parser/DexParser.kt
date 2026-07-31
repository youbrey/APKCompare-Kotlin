package com.example.data.parser

import com.example.data.model.ClassInfo
import com.example.data.model.MethodInfo
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DexParser {

    fun parseDex(dexBytes: ByteArray): List<ClassInfo> {
        if (dexBytes.size < 0x70) return emptyList()

        val buffer = ByteBuffer.wrap(dexBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Validate Magic
        val magic = ByteArray(8)
        buffer.get(magic)
        val magicStr = String(magic)
        if (!magicStr.startsWith("dex\n")) {
            return emptyList()
        }

        try {
            buffer.position(0x38)
            val stringIdsSize = buffer.int
            val stringIdsOff = buffer.int
            val typeIdsSize = buffer.int
            val typeIdsOff = buffer.int
            val protoIdsSize = buffer.int
            val protoIdsOff = buffer.int
            val fieldIdsSize = buffer.int
            val fieldIdsOff = buffer.int
            val methodIdsSize = buffer.int
            val methodIdsOff = buffer.int
            val classDefsSize = buffer.int
            val classDefsOff = buffer.int

            // 1. Read String Table
            val strings = ArrayList<String>(stringIdsSize)
            for (i in 0 until stringIdsSize) {
                val off = buffer.getInt(stringIdsOff + i * 4)
                strings.add(readMutf8String(buffer, off))
            }

            // 2. Read Type Table (class descriptors)
            val types = ArrayList<String>(typeIdsSize)
            for (i in 0 until typeIdsSize) {
                val stringIdx = buffer.getInt(typeIdsOff + i * 4)
                if (stringIdx in strings.indices) {
                    types.add(strings[stringIdx])
                } else {
                    types.add("Lunknown/Type;")
                }
            }

            // 3. Read Proto Table (prototypes)
            data class Proto(val returnType: String, val paramTypes: List<String>, val shorty: String)
            val protos = ArrayList<Proto>(protoIdsSize)
            for (i in 0 until protoIdsSize) {
                val protoPos = protoIdsOff + i * 12
                val shortyIdx = buffer.getInt(protoPos)
                val returnTypeIdx = buffer.getInt(protoPos + 4)
                val parametersOff = buffer.getInt(protoPos + 8)

                val retType = if (returnTypeIdx in types.indices) types[returnTypeIdx] else "V"
                val paramList = mutableListOf<String>()
                if (parametersOff > 0) {
                    buffer.position(parametersOff)
                    val paramSize = buffer.int
                    for (p in 0 until paramSize) {
                        val typeIdx = buffer.short.toInt() and 0xFFFF
                        if (typeIdx in types.indices) {
                            paramList.add(types[typeIdx])
                        }
                    }
                }
                val shortyStr = if (shortyIdx in strings.indices) strings[shortyIdx] else ""
                protos.add(Proto(formatType(retType), paramList.map { formatType(it) }, shortyStr))
            }

            // 4. Read Method IDs
            data class MethodId(val classType: String, val proto: Proto, val name: String)
            val methodIds = ArrayList<MethodId>(methodIdsSize)
            for (i in 0 until methodIdsSize) {
                val methodPos = methodIdsOff + i * 8
                val classIdx = buffer.getShort(methodPos).toInt() and 0xFFFF
                val protoIdx = buffer.getShort(methodPos + 2).toInt() and 0xFFFF
                val nameIdx = buffer.getInt(methodPos + 4)

                val classType = if (classIdx in types.indices) formatType(types[classIdx]) else "UnknownClass"
                val proto = if (protoIdx in protos.indices) protos[protoIdx] else Proto("void", emptyList(), "")
                val name = if (nameIdx in strings.indices) strings[nameIdx] else "unknownMethod"
                methodIds.add(MethodId(classType, proto, name))
            }

            // Group method IDs by Class
            val methodsByClass = HashMap<String, MutableList<MethodInfo>>()
            for (m in methodIds) {
                val descriptor = "(${m.proto.paramTypes.joinToString(",")})${m.proto.returnType}"
                val isSec = isSecuritySensitiveMethod(m.name, m.classType)
                val secCategory = getSecurityCategory(m.name, m.classType)
                val info = MethodInfo(
                    name = m.name,
                    descriptor = descriptor,
                    returnType = m.proto.returnType,
                    parameterTypes = m.proto.paramTypes,
                    accessFlags = "public",
                    isSecuritySensitive = isSec,
                    securityCategory = secCategory
                )
                methodsByClass.getOrPut(m.classType) { mutableListOf() }.add(info)
            }

            // 5. Read Class Defs
            val classes = mutableListOf<ClassInfo>()
            for (i in 0 until classDefsSize) {
                val classDefPos = classDefsOff + i * 32
                val classIdx = buffer.getInt(classDefPos)
                val accessFlagsInt = buffer.getInt(classDefPos + 4)
                val superClassIdx = buffer.getInt(classDefPos + 8)

                val rawType = if (classIdx in types.indices) types[classIdx] else continue
                val className = formatType(rawType)
                val superClassName = if (superClassIdx in types.indices) formatType(types[superClassIdx]) else null

                val pkgName = extractPackageName(className)
                val simpleName = extractSimpleName(className)

                val classMethods = methodsByClass[className] ?: emptyList()

                classes.add(
                    ClassInfo(
                        className = className,
                        packageName = pkgName,
                        simpleName = simpleName,
                        isInterface = (accessFlagsInt and 0x0200) != 0,
                        superClass = superClassName,
                        methods = classMethods,
                        methodCount = classMethods.size
                    )
                )
            }

            return classes
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun readMutf8String(buffer: ByteBuffer, offset: Int): String {
        buffer.position(offset)
        readLeb128(buffer) // string length
        val bytes = mutableListOf<Byte>()
        while (buffer.hasRemaining()) {
            val b = buffer.get()
            if (b == 0.toByte()) break
            bytes.add(b)
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
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

    private fun formatType(typeDescriptor: String): String {
        return when {
            typeDescriptor.startsWith("L") && typeDescriptor.endsWith(";") ->
                typeDescriptor.substring(1, typeDescriptor.length - 1).replace('/', '.')
            typeDescriptor.startsWith("[") ->
                formatType(typeDescriptor.substring(1)) + "[]"
            typeDescriptor == "Z" -> "boolean"
            typeDescriptor == "B" -> "byte"
            typeDescriptor == "S" -> "short"
            typeDescriptor == "C" -> "char"
            typeDescriptor == "I" -> "int"
            typeDescriptor == "J" -> "long"
            typeDescriptor == "F" -> "float"
            typeDescriptor == "D" -> "double"
            typeDescriptor == "V" -> "void"
            else -> typeDescriptor
        }
    }

    private fun extractPackageName(fullClassName: String): String {
        val lastDot = fullClassName.lastIndexOf('.')
        return if (lastDot > 0) fullClassName.substring(0, lastDot) else "default"
    }

    private fun extractSimpleName(fullClassName: String): String {
        val lastDot = fullClassName.lastIndexOf('.')
        return if (lastDot > 0) fullClassName.substring(lastDot + 1) else fullClassName
    }

    private fun isSecuritySensitiveMethod(name: String, className: String): Boolean {
        val combined = "$className.$name".lowercase()
        val keywords = listOf(
            "cipher", "secretkey", "encrypt", "decrypt", "hash", "md5", "sha256", "rsa", "aes",
            "sslcontext", "trustmanager", "httpurlconnection", "retrofit", "okhttp",
            "camera", "location", "telephony", "sms", "root", "su", "dexclassloader",
            "reflection", "getidentifier", "keystore", "biometric", "checkpermission"
        )
        return keywords.any { combined.contains(it) }
    }

    private fun getSecurityCategory(name: String, className: String): String? {
        val combined = "$className.$name".lowercase()
        return when {
            combined.contains("cipher") || combined.contains("encrypt") || combined.contains("decrypt") || combined.contains("aes") || combined.contains("rsa") -> "Kriptografi & Enkripsi"
            combined.contains("ssl") || combined.contains("http") || combined.contains("retrofit") || combined.contains("socket") -> "Jaringan & API"
            combined.contains("permission") || combined.contains("checkpermission") -> "Akses Izin System"
            combined.contains("root") || combined.contains("su") || combined.contains("dexclassloader") -> "Integritas & Anti-Root"
            combined.contains("keystore") || combined.contains("biometric") -> "Kredensial & Autentikasi"
            combined.contains("camera") || combined.contains("location") || combined.contains("sms") -> "Akses Sensor & Data Sensitif"
            else -> null
        }
    }
}
