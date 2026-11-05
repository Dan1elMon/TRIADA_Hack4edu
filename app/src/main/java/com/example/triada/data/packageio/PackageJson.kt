package com.example.triada.data.packageio

import org.json.JSONObject
import java.security.MessageDigest

internal object PackageJson {
    fun hash(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    fun parse(bytes: ByteArray, limit: Int = 2 * 1024 * 1024): JSONObject {
        require(bytes.size <= limit)
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        var depth = 0; var quoted = false; var escaped = false
        text.forEach { c ->
            if (quoted) { if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false }
            else when (c) { '"' -> quoted = true; '{','[' -> { depth++; require(depth <= 32) }; '}',']' -> { depth--; require(depth >= 0) } }
        }
        require(depth == 0 && !quoted)
        return JSONObject(text)
    }
    fun verify(files: Map<String, ByteArray>, format: String, kind: String): JSONObject {
        val manifest = parse(files.getValue("manifest.json"), 8192)
        require(manifest.getString("format") == format && manifest.get("formatVersion") == 1 && manifest.getString("kind") == kind)
        val hashes = parse(files.getValue("checksums.json"), 128 * 1024)
        require(hashes.keys().asSequence().toSet() == files.keys - "checksums.json")
        files.filterKeys { it != "checksums.json" }.forEach { (name, data) -> require(hashes.getString(name) == hash(data)) { "Checksum mismatch: $name" } }
        return manifest
    }
    fun zip(files: Map<String, ByteArray>, output: java.io.OutputStream) {
        val sums = JSONObject(); files.forEach { (name, data) -> sums.put(name, hash(data)) }
        java.util.zip.ZipOutputStream(output).use { zip ->
            (files + ("checksums.json" to sums.toString().toByteArray(Charsets.UTF_8))).forEach { (name, data) ->
                zip.putNextEntry(java.util.zip.ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
    }
}
