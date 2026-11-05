package com.example.triada.domain

import com.example.triada.data.packageio.*
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class SafePackageReaderTest {
    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip -> entries.forEach { (name, text) -> zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray()); zip.closeEntry() } }
        return output.toByteArray()
    }
    private fun rejected(bytes: ByteArray, reader: SafePackageReader = SafePackageReader()) {
        try { reader.read(bytes.inputStream()); fail("Unsafe package accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun readsLocalFilesWithoutExtraction() {
        val files = SafePackageReader().read(zip("manifest.json" to "{}", "lessons/one.json" to "lesson").inputStream())
        assertEquals("lesson", files.getValue("lessons/one.json").toString(Charsets.UTF_8))
    }
    @Test fun rejectsTraversalAndAbsolutePaths() {
        listOf("../evil", "/evil", "C:/evil", "a\\evil", "a/./evil", "a//evil").forEach { rejected(zip("manifest.json" to "{}", it to "bad")) }
    }
    @Test fun rejectsCaseConflictsAndFileDirectoryConflicts() {
        rejected(zip("manifest.json" to "{}", "A.json" to "a", "a.json" to "b"))
        rejected(zip("manifest.json" to "{}", "folder" to "a", "folder/file" to "b"))
        rejected(zip("manifest.json" to "{}", "folder/file" to "b", "folder" to "a"))
    }
    @Test fun enforcesActualDecompressedLimits() {
        rejected(zip("manifest.json" to "{}", "data" to "x".repeat(1000)), SafePackageReader(PackageLimits(10, 64, 128)))
        rejected(zip("manifest.json" to "{}", "a" to "1234", "b" to "5678"), SafePackageReader(PackageLimits(10, 8, 9)))
        rejected(zip("manifest.json" to "{}", "a" to "a"), SafePackageReader(PackageLimits(1, 32, 64)))
    }
    @Test fun requiresManifest() { rejected(zip("data" to "hello")); rejected(byteArrayOf()) }
    @Test fun compressedInputAndDirectoryPayloadsAreBounded() {
        rejected(zip("manifest.json" to "{}"), SafePackageReader(PackageLimits(maxInputBytes = 8)))
        rejected(zip("manifest.json" to "{}", "folder/" to "hidden data"))
    }
}
