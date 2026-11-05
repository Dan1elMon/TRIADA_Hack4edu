package com.example.triada.data.packageio

import com.example.triada.data.local.*
import com.example.triada.data.repository.RoomCatalogRepository
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** First supported interchange contract: catalog metadata referencing bundled, executable lessons. */
class CatalogPackageService(private val database: LearningDatabase, private val bundled: CatalogSource) {
    suspend fun importCatalog(input: InputStream) = withContext(Dispatchers.IO) {
        importFiles(SafePackageReader().read(input))
    }
    suspend fun importFiles(files: Map<String, ByteArray>) = withContext(Dispatchers.IO) {
        require(files.keys == setOf("manifest.json", "catalog.json", "checksums.json")) { "Unsupported package entries" }
        val manifest = json(files.getValue("manifest.json"), 8192)
        require(manifest.getString("format") == "aula" && manifest.getInt("formatVersion") == 1)
        require(manifest.getString("kind") == "catalog-metadata") { "Unsupported package kind" }
        require(manifest.get("formatVersion") == 1)
        val hashes = json(files.getValue("checksums.json"), 8192)
        require(hashes.keys().asSequence().toSet() == setOf("manifest.json", "catalog.json"))
        listOf("manifest.json", "catalog.json").forEach { name -> require(hashes.getString(name) == hash(files.getValue(name))) { "Checksum mismatch" } }
        val raw = files.getValue("catalog.json").toString(Charsets.UTF_8)
        json(files.getValue("catalog.json"), 2 * 1024 * 1024)
        val catalog = CatalogParser.parse(raw)
        require(manifest.getInt("catalogVersion") == catalog.catalogVersion)
        val allowed = CatalogParser.parse(bundled.read()).lessons.toSet()
        require(catalog.lessons.all { it in allowed }) { "Only bundled lesson versions are supported" }
        val installed = database.catalogDao().metadata()
        require(installed == null || catalog.catalogVersion >= installed.catalogVersion) { "Catalog downgrade rejected" }
        RoomCatalogRepository(database, object : CatalogSource {
            override fun read() = raw
            override fun hasAudio(key: String) = bundled.hasAudio(key)
        }).install()
    }

    /** Exports the bundled catalog, not installed updates, profiles, attempts or answer keys. */
    suspend fun exportBundledCatalog(output: OutputStream) = withContext(Dispatchers.IO) {
        val raw = bundled.read()
        val version = CatalogParser.parse(raw).catalogVersion
        val manifest = JSONObject().put("format", "aula").put("formatVersion", 1)
            .put("kind", "catalog-metadata").put("catalogVersion", version).toString().toByteArray(Charsets.UTF_8)
        val catalog = raw.toByteArray(Charsets.UTF_8)
        val checksums = JSONObject().put("manifest.json", hash(manifest)).put("catalog.json", hash(catalog)).toString().toByteArray(Charsets.UTF_8)
        ZipOutputStream(output).use { zip ->
            listOf("manifest.json" to manifest, "catalog.json" to catalog, "checksums.json" to checksums).forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
        }
    }
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun json(bytes: ByteArray, limit: Int): JSONObject {
        require(bytes.size <= limit) { "JSON size limit exceeded" }
        val text = bytes.toString(Charsets.UTF_8)
        var depth = 0
        var quoted = false
        var escaped = false
        text.forEach { char ->
            if (quoted) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 32) { "JSON nesting limit exceeded" } }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(depth == 0 && !quoted)
        return JSONObject(text)
    }
}
