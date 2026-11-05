package com.example.triada.data.packageio

import android.content.Context
import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.data.repository.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*

/** Portable lessons plus immutable content-addressed WAVs. Only DB references activate resources. */
class LessonPackageService(private val db: LearningDatabase, private val context: Context) {
    private val assets = AssetCatalogSource(context)
    private fun bytes(text: String) = text.toByteArray(Charsets.UTF_8)
    private fun resource(key: String): ByteArray {
        require(key.matches(Regex("[a-z0-9-]{1,80}")))
        return try { context.assets.open("audio/$key.wav").use { it.readBytes() } }
        catch (_: IOException) { File(context.filesDir, "content/audio/$key.wav").readBytes() }
    }
    suspend fun export(output: OutputStream) = withContext(Dispatchers.IO) {
        RoomCatalogRepository(db, assets).install()
        val files = db.withTransaction {
            val catalog = InstalledCatalogJson.encode(db)
            val result = linkedMapOf("manifest.json" to bytes(JSONObject().put("format", "aula").put("formatVersion", 1)
                .put("kind", "learning-content").put("catalogVersion", CatalogParser.parse(catalog).catalogVersion).toString()), "catalog.json" to bytes(catalog))
            val lessons = JSONArray()
            db.catalogDao().lessons().forEach { link ->
                val dao = db.learningDao(); val lesson = requireNotNull(dao.lesson(link.lessonId, link.contentVersion))
                val questions = JSONArray()
                dao.questions(lesson.id, lesson.version).forEach { q ->
                    val options = dao.options(q.id, q.version).map { it.value }
                    val content = ActivityCodec.decode(q, options).content
                    (listOfNotNull(content.cueAudioKey) + content.choices.mapNotNull { it.audioKey }).distinct().forEach { key -> result["resources/audio/$key.wav"] = resource(key) }
                    questions.put(JSONObject().put("id", q.id).put("prompt", q.prompt).put("objectCount", q.objectCount)
                        .put("type", q.activityType).put("payload", JSONObject(q.payloadJson)).put("answerKey", JSONObject(q.answerKeyJson))
                        .put("options", JSONArray(options)).put("hints", JSONArray(dao.hints(q.id, q.version).map { it.text })))
                }
                lessons.put(JSONObject().put("id", lesson.id).put("version", lesson.version).put("title", lesson.title).put("objective", lesson.objective).put("questions", questions))
            }
            result["lessons.json"] = bytes(JSONObject().put("lessons", lessons).toString()); result
        }
        require(files.values.sumOf { it.size.toLong() } <= 32L * 1024 * 1024)
        PackageJson.zip(files, output)
    }

    suspend fun import(input: InputStream) = withContext(Dispatchers.IO) { importFiles(SafePackageReader().read(input)) }
    suspend fun importFiles(files: Map<String, ByteArray>) = withContext(Dispatchers.IO) {
        val manifest = PackageJson.verify(files, "aula", "learning-content")
        require(files.keys.all { it in setOf("manifest.json", "catalog.json", "lessons.json", "checksums.json") || it.matches(Regex("resources/audio/[a-z0-9-]{1,80}\\.wav")) })
        val raw = files.getValue("catalog.json").toString(Charsets.UTF_8)
        PackageJson.parse(files.getValue("catalog.json"))
        val bundle = CatalogParser.parse(raw)
        require(manifest.getInt("catalogVersion") == bundle.catalogVersion)
        val rows = PackageJson.parse(files.getValue("lessons.json"), 8 * 1024 * 1024).getJSONArray("lessons")
        require(rows.length() in 1..128)
        val aliases = files.filterKeys { it.startsWith("resources/audio/") }.map { (path, data) ->
            require(data.size in 44..(8 * 1024 * 1024) && data.copyOfRange(0,4).toString(Charsets.US_ASCII) == "RIFF" && data.copyOfRange(8,12).toString(Charsets.US_ASCII) == "WAVE")
            path.substringAfterLast('/').removeSuffix(".wav") to ("pkg-" + PackageJson.hash(data))
        }.toMap()
        fun normalized(def: ExerciseDefinition, incoming: Boolean): ExerciseDefinition {
            fun key(key: String?): String? = key?.let { if (incoming) requireNotNull(aliases[it]) else "pkg-" + PackageJson.hash(resource(it)) }
            return def.copy(content = def.content.copy(cueAudioKey = key(def.content.cueAudioKey), choices = def.content.choices.map { it.copy(audioKey = key(it.audioKey)) }))
        }
        data class Prepared(val lesson: LessonEntity, val questions: List<QuestionEntity>, val options: List<OptionEntity>, val hints: List<HintEntity>)
        val prepared = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index); val id = row.getString("id"); val version = row.getInt("version")
            require(id.matches(Regex("[a-z0-9-]{1,100}")) && version > 0)
            val lesson = LessonEntity(id, version, row.getString("title"), row.getString("objective"))
            require(lesson.title.isNotBlank() && lesson.objective.isNotBlank())
            val questions = row.getJSONArray("questions"); require(questions.length() in 3..5)
            val optionRows = mutableListOf<OptionEntity>(); val hintRows = mutableListOf<HintEntity>()
            val questionRows = (0 until questions.length()).map { order ->
                val q = questions.getJSONObject(order); val qid = q.getString("id")
                require(qid.matches(Regex("[a-z0-9-]{1,100}")))
                val optionArray = q.getJSONArray("options"); require(optionArray.length() <= 12)
                val options = (0 until optionArray.length()).map { optionArray.getInt(it) }
                val entity = QuestionEntity(qid, version, id, order, q.getString("prompt"), q.getInt("objectCount"), q.getString("type"), q.getJSONObject("payload").toString(), q.getJSONObject("answerKey").toString())
                val definition = normalized(ActivityCodec.decode(entity, options), true)
                ExerciseValidator.validate(definition)
                val hints = q.getJSONArray("hints"); require(hints.length() in 2..8)
                repeat(hints.length()) { n -> val text = hints.getString(n); require(text.isNotBlank() && text.length <= 2000); hintRows.add(HintEntity(qid, version, n, text)) }
                options.forEachIndexed { n, value -> optionRows.add(OptionEntity(qid, version, value, n)) }
                entity.copy(payloadJson = ActivityCodec.payload(definition.content))
            }
            require(questionRows.map { it.id }.distinct().size == questionRows.size)
            Prepared(lesson, questionRows, optionRows, hintRows)
        }
        require(prepared.map { it.lesson.id to it.lesson.version }.toSet().size == prepared.size)
        val questionKeys = prepared.flatMap { it.questions }.map { it.id to it.version }
        require(questionKeys.distinct().size == questionKeys.size)
        require(prepared.map { it.lesson.id to it.lesson.version }.toSet() == bundle.lessons.map { it.lessonId to it.contentVersion }.toSet())
        // Publish only immutable hash-named files. Interrupted installs leave inert, reusable files, never active partial lessons.
        val directory = File(context.filesDir, "content/audio").apply { mkdirs() }
        directory.listFiles().orEmpty().filter { it.name.startsWith("stage-") && it.name.endsWith(".tmp") }.forEach { it.delete() }
        require(directory.listFiles().orEmpty().sumOf { it.length() } + files.values.sumOf { it.size.toLong() } <= 128L * 1024 * 1024)
        aliases.forEach { (original, stored) ->
            val data = files.getValue("resources/audio/$original.wav"); val destination = File(directory, "$stored.wav")
            if (destination.exists()) require(PackageJson.hash(destination.readBytes()) == PackageJson.hash(data))
            else {
                val staging = File.createTempFile("stage-", ".tmp", directory)
                try { FileOutputStream(staging).use { it.write(data); it.fd.sync() }; check(staging.renameTo(destination)) }
                finally { staging.delete() }
            }
            val media = android.media.MediaMetadataRetriever()
            try {
                media.setDataSource(destination.absolutePath)
                require((media.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) in 1..120_000)
            } finally { media.release() }
        }
        db.withTransaction {
            val metadata = db.catalogDao().metadata()
            require(metadata == null || bundle.catalogVersion >= metadata.catalogVersion)
            val dao = db.learningDao()
            prepared.forEach { lesson ->
                val old = dao.lesson(lesson.lesson.id, lesson.lesson.version)
                if (old != null) {
                    require(old == lesson.lesson) { "Published lesson metadata is immutable" }
                    val existing = dao.questions(old.id, old.version)
                    require(existing.size == lesson.questions.size)
                    existing.zip(lesson.questions).forEach { (a,b) ->
                        val options = dao.options(a.id, a.version).map { it.value }
                        val incomingOptions = lesson.options.filter { it.questionId == b.id }.map { it.value }
                        require(a.id == b.id && a.sortOrder == b.sortOrder && normalized(ActivityCodec.decode(a, options), false) == ActivityCodec.decode(b, incomingOptions))
                        require(dao.hints(a.id,a.version) == lesson.hints.filter { it.questionId == a.id })
                    }
                } else {
                    lesson.questions.forEach { require(dao.question(it.id,it.version)==null) { "Question identity belongs to another lesson" } }
                    dao.insertLesson(lesson.lesson); dao.insertQuestions(lesson.questions); dao.insertOptions(lesson.options); dao.insertHints(lesson.hints)
                }
            }
            if (metadata != null && metadata.catalogVersion == bundle.catalogVersion) {
                require(InstalledCatalogJson.equivalent(InstalledCatalogJson.encode(db), raw)) { "Published catalog is immutable" }
            } else RoomCatalogRepository(db, object : CatalogSource {
                override fun read() = raw
                override fun hasAudio(key: String) = assets.hasAudio(key)
            }).install()
        }
    }
}
