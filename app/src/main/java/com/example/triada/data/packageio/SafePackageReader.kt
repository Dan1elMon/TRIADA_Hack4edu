package com.example.triada.data.packageio

import java.io.InputStream
import java.util.zip.ZipInputStream

data class PackageLimits(val maxFiles: Int = 512, val maxFileBytes: Int = 8 * 1024 * 1024, val maxTotalBytes: Int = 32 * 1024 * 1024, val maxInputBytes: Long = 40L * 1024 * 1024) {
    init { require(maxFiles > 0 && maxFileBytes > 0 && maxTotalBytes > 0 && maxInputBytes > 0) }
}

/** Reads into a bounded staging representation; never extracts to disk or modifies active data. */
class SafePackageReader(private val limits: PackageLimits = PackageLimits()) {
    fun read(input: InputStream): Map<String, ByteArray> {
        val files = linkedMapOf<String, ByteArray>()
        val names = mutableSetOf<String>()
        var entries = 0
        var total = 0L
        val bounded = object : java.io.FilterInputStream(input) {
            var readBytes = 0L
            override fun read(): Int = super.read().also { if (it >= 0) { readBytes++; require(readBytes <= limits.maxInputBytes) } }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = `in`.read(buffer, offset, length).also {
                if (it > 0) { readBytes += it; require(readBytes <= limits.maxInputBytes) { "Compressed input limit exceeded" } }
            }
        }
        ZipInputStream(bounded).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entries <= limits.maxFiles) { "Too many package entries" }
                val name = entry.name
                require(name.length in 1..240 && !name.startsWith('/') && !name.contains('\\') && !name.contains(':') && name.none { it.code < 32 }) { "Unsafe entry name" }
                val path = name.removeSuffix("/")
                require(path.split('/').all { it.isNotEmpty() && it != "." && it != ".." && it == it.trim() && !it.endsWith('.') }) { "Unsafe entry path" }
                require(names.add(path.lowercase(java.util.Locale.ROOT))) { "Duplicate entry" }
                require(names.none { other -> other != path.lowercase(java.util.Locale.ROOT) &&
                    (other.startsWith(path.lowercase(java.util.Locale.ROOT) + "/") && !entry.isDirectory) }) { "Conflicting entry paths" }
                if (entry.isDirectory) require(zip.read() == -1) { "Directory entry contains data" }
                if (!entry.isDirectory) {
                    require(files.keys.none { path.lowercase(java.util.Locale.ROOT).startsWith(it.lowercase(java.util.Locale.ROOT) + "/") }) { "Conflicting entry paths" }
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= limits.maxTotalBytes && output.size().toLong() + count <= limits.maxFileBytes) { "Package size limit exceeded" }
                        output.write(buffer, 0, count)
                    }
                    files[path] = output.toByteArray()
                }
                zip.closeEntry()
            }
        }
        require(files.containsKey("manifest.json")) { "Missing manifest" }
        return files
    }
}
