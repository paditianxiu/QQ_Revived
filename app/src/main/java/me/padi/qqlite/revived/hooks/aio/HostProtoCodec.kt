package me.padi.qqlite.revived.hooks.aio

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

internal class HostProtoNode {
    private val values = LinkedHashMap<Int, MutableList<Any>>()

    fun put(field: Int, value: Any) {
        values.getOrPut(field) { ArrayList(1) }.add(value)
    }

    fun encode(): ByteArray {
        val out = ByteArrayOutputStream()
        values.forEach { (field, items) ->
            items.forEach { value -> writeField(out, field, value) }
        }
        return out.toByteArray()
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        values.forEach { (field, items) ->
            val value = if (items.size == 1) {
                items.first().toJsonValue()
            } else {
                JSONArray().apply { items.forEach { put(it.toJsonValue()) } }
            }
            json.put(field.toString(), value)
        }
        return json
    }

    private fun writeField(out: ByteArrayOutputStream, field: Int, value: Any) {
        when (value) {
            is Int -> {
                writeVarint(out, (field shl 3).toLong())
                writeVarint(out, value.toLong())
            }

            is Long -> {
                writeVarint(out, (field shl 3).toLong())
                writeVarint(out, value)
            }

            is String -> {
                val bytes = value.toByteArray(Charsets.UTF_8)
                writeVarint(out, ((field shl 3) or 2).toLong())
                writeVarint(out, bytes.size.toLong())
                out.write(bytes)
            }

            is ByteArray -> {
                writeVarint(out, ((field shl 3) or 2).toLong())
                writeVarint(out, value.size.toLong())
                out.write(value)
            }

            is HostProtoNode -> {
                val bytes = value.encode()
                writeVarint(out, ((field shl 3) or 2).toLong())
                writeVarint(out, bytes.size.toLong())
                out.write(bytes)
            }
        }
    }

    private fun Any.toJsonValue(): Any {
        return when (this) {
            is HostProtoNode -> toJson()
            else -> this
        }
    }

    companion object {
        fun decodeToJson(raw: ByteArray?): JSONObject? {
            val payload = normalizePacket(raw) ?: return null
            return runCatching { decodeNode(payload, 0, payload.size).toJson() }.getOrNull()
        }

        private fun decodeNode(bytes: ByteArray, start: Int, end: Int): HostProtoNode {
            val node = HostProtoNode()
            var index = start
            while (index < end) {
                val (tagValue, tagNext) = readVarint(bytes, index) ?: break
                index = tagNext
                if (tagValue == 0L) break
                val field = (tagValue ushr 3).toInt()
                val wireType = (tagValue and 0x7L).toInt()
                when (wireType) {
                    0 -> {
                        val (value, next) = readVarint(bytes, index) ?: break
                        node.put(field, value)
                        index = next
                    }

                    1 -> {
                        if (index + 8 > end) break
                        node.put(field, readFixed64(bytes, index))
                        index += 8
                    }

                    2 -> {
                        val (sizeValue, next) = readVarint(bytes, index) ?: break
                        val size = sizeValue.toInt()
                        val contentStart = next
                        val contentEnd = (contentStart + size).coerceAtMost(end)
                        val slice = bytes.copyOfRange(contentStart, contentEnd)
                        node.put(field, decodeLengthDelimited(slice))
                        index = contentEnd
                    }

                    5 -> {
                        if (index + 4 > end) break
                        node.put(field, readFixed32(bytes, index))
                        index += 4
                    }

                    else -> break
                }
            }
            return node
        }

        private fun decodeLengthDelimited(bytes: ByteArray): Any {
            if (bytes.isEmpty()) return ""
            if (looksLikeProto(bytes)) {
                runCatching { return decodeNode(bytes, 0, bytes.size) }
            }
            decodeGzip(bytes)?.let { unzipped ->
                if (looksLikeProto(unzipped)) {
                    runCatching { return decodeNode(unzipped, 0, unzipped.size) }
                }
                if (isStrictUtf8(unzipped)) {
                    return String(unzipped, Charsets.UTF_8)
                }
            }
            if (isStrictUtf8(bytes)) {
                return String(bytes, Charsets.UTF_8)
            }
            return "hex->${bytes.joinToString("") { "%02X".format(it) }}"
        }

        private fun normalizePacket(raw: ByteArray?): ByteArray? {
            if (raw == null || raw.isEmpty()) return null
            if (raw.size >= 4 &&
                raw[0].toInt() and 0xFF == 0 &&
                raw[1].toInt() and 0xFF == 0
            ) {
                return raw.copyOfRange(4, raw.size)
            }
            if (raw.size > 4) {
                val declared = ((raw[0].toInt() and 0xFF) shl 24) or
                    ((raw[1].toInt() and 0xFF) shl 16) or
                    ((raw[2].toInt() and 0xFF) shl 8) or
                    (raw[3].toInt() and 0xFF)
                if (declared == raw.size) {
                    return raw.copyOfRange(4, raw.size)
                }
            }
            return raw
        }

        fun wrapPacket(body: ByteArray): ByteArray {
            return ByteArrayOutputStream(body.size + 4).apply {
                write(byteArrayOf(
                    ((body.size + 4) ushr 24).toByte(),
                    ((body.size + 4) ushr 16).toByte(),
                    ((body.size + 4) ushr 8).toByte(),
                    (body.size + 4).toByte()
                ))
                write(body)
            }.toByteArray()
        }

        private fun looksLikeProto(bytes: ByteArray): Boolean {
            if (bytes.isEmpty()) return false
            val tag = bytes[0].toInt() and 0xFF
            val wire = tag and 0x07
            val field = tag ushr 3
            return field != 0 && wire <= 5 && wire != 3 && wire != 4
        }

        private fun decodeGzip(bytes: ByteArray): ByteArray? {
            if (bytes.size < 2 || bytes[0].toInt() != 0x1F || bytes[1].toInt() != 0x8B) return null
            return runCatching {
                GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
                    ByteArrayOutputStream().use { output ->
                        val buffer = ByteArray(1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count <= 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                }
            }.getOrNull()
        }

        private fun isStrictUtf8(bytes: ByteArray): Boolean {
            var index = 0
            while (index < bytes.size) {
                val byte = bytes[index].toInt() and 0xFF
                when {
                    byte < 0x80 -> index += 1
                    byte in 0xC2..0xDF -> {
                        if (index + 1 >= bytes.size || bytes[index + 1].toInt() and 0xC0 != 0x80) return false
                        index += 2
                    }

                    byte in 0xE0..0xEF -> {
                        if (index + 2 >= bytes.size) return false
                        if (bytes[index + 1].toInt() and 0xC0 != 0x80 ||
                            bytes[index + 2].toInt() and 0xC0 != 0x80
                        ) return false
                        index += 3
                    }

                    byte in 0xF0..0xF4 -> {
                        if (index + 3 >= bytes.size) return false
                        if (bytes[index + 1].toInt() and 0xC0 != 0x80 ||
                            bytes[index + 2].toInt() and 0xC0 != 0x80 ||
                            bytes[index + 3].toInt() and 0xC0 != 0x80
                        ) return false
                        index += 4
                    }

                    else -> return false
                }
            }
            return true
        }

        private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int>? {
            var index = start
            var shift = 0
            var result = 0L
            while (index < bytes.size && shift < 64) {
                val byte = bytes[index].toInt() and 0xFF
                result = result or (((byte and 0x7F).toLong()) shl shift)
                index += 1
                if (byte and 0x80 == 0) {
                    return result to index
                }
                shift += 7
            }
            return null
        }

        private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
            var current = value
            while (true) {
                if (current and -128L == 0L) {
                    out.write(current.toInt())
                    return
                }
                out.write(((current and 0x7F) or 0x80).toInt())
                current = current ushr 7
            }
        }

        private fun readFixed32(bytes: ByteArray, start: Int): Int {
            return (bytes[start].toInt() and 0xFF) or
                ((bytes[start + 1].toInt() and 0xFF) shl 8) or
                ((bytes[start + 2].toInt() and 0xFF) shl 16) or
                ((bytes[start + 3].toInt() and 0xFF) shl 24)
        }

        private fun readFixed64(bytes: ByteArray, start: Int): Long {
            var result = 0L
            repeat(8) { offset ->
                result = result or ((bytes[start + offset].toLong() and 0xFFL) shl (offset * 8))
            }
            return result
        }
    }
}
