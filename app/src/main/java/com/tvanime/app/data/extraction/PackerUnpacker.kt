package com.tvanime.app.data.extraction

import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackerUnpacker @Inject constructor() {

    fun unpack(packedJs: String): String? {
        val match = PACKER_PATTERN.find(packedJs) ?: return null
        
        val p = match.groupValues[1].toIntOrNull() ?: return null
        val a = match.groupValues[2].toIntOrNull() ?: return null
        val c = match.groupValues[3].toIntOrNull() ?: return null
        val k = match.groupValues[4].split("|")
        
        return unpackPayload(p, a, c, k)
    }

    fun tryUnpackAll(js: String): List<String> {
        val results = mutableListOf<String>()
        PACKER_PATTERN.findAll(js).forEach { match ->
            val unpacked = unpack(match.value) ?: return@forEach
            results.add(unpacked)
        }
        return results
    }

    private fun unpackPayload(p: Int, a: Int, c: Int, k: List<String>): String {
        val payload = generatePayload(p, a)
        
        var result = payload
        for (i in c - 1 downTo 0) {
            if (i < k.size && k[i].isNotEmpty()) {
                result = result.replace(generateKey(i, a), k[i])
            }
        }
        
        return result
    }

    private fun generatePayload(p: Int, a: Int): String {
        val chars = (0 until p).map { charCode ->
            if (charCode < a) {
                charCode.toChar().toString()
            } else {
                generateKey(charCode, a)
            }
        }
        return chars.joinToString("")
    }

    private fun generateKey(index: Int, radix: Int): String {
        if (radix <= 36 && index < radix) {
            return index.toString(radix)
        }
        return index.toString(radix)
    }

    companion object {
        private val PACKER_PATTERN = Regex(
            """eval\(function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*d\s*[^)]*\)\s*\{[\s\S]*?\}\s*\(\s*'([^']*)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'([^']*)'\s*""",
            RegexOption.IGNORE_CASE
        )
    }
}