package com.example.triada.data.repository

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import kotlinx.coroutines.flow.first
import java.util.UUID

/** Local role simulation. This is not authentication or a network authorization boundary. */
class SchoolRepository(private val db: LearningDatabase) {
    private val dao get() = db.schoolDao()
    suspend fun accounts(): List<SchoolAccount> = db.withTransaction {
        // Adopt existing/recovered profiles without rewriting their learning history.
        db.learningDao().profiles().first().forEach { p -> dao.addAccount(SchoolAccountEntity("student-${p.id}",p.displayName,"STUDENT",p.id)) }
        dao.addAccount(SchoolAccountEntity("demo-teacher","Docente de ejemplo","TEACHER",null))
        dao.accounts().map { row ->
            val profile=row.profileId?.let{db.learningDao().profile(it)}
            SchoolAccount(row.id,profile?.displayName ?: row.name,SchoolRole.valueOf(row.role),row.profileId,profile?.avatar ?: "star",db.extensionDao().credential(row.id)?.username ?: "")
        }
    }
    suspend fun create(name: String, role: SchoolRole) = db.withTransaction {
        val clean = name.trim(); require(clean.length in 1..24)
        if (role == SchoolRole.STUDENT) RoomLearningRepository(db).createProfile(clean,"star")
        else dao.addAccount(SchoolAccountEntity(UUID.randomUUID().toString(),clean,"TEACHER",null))
        accounts()
    }
    suspend fun edit(actor: String, name: String, avatar: String) = db.withTransaction {
        val who=requireNotNull(dao.account(actor)); val clean=name.trim(); require(clean.length in 1..24)
        who.profileId?.let { RoomLearningRepository(db).updateProfile(it,clean,avatar) }
        dao.rename(actor,clean)
    }
    suspend fun linkedStudents(actor: String): List<SchoolAccount> {
        require(dao.account(actor)?.role == "TEACHER")
        val linked = dao.students(actor).toSet()
        return accounts().filter { it.id in linked && it.role == SchoolRole.STUDENT }
    }
    suspend fun link(actor: String, student: String, linked: Boolean) = db.withTransaction {
        require(dao.account(actor)?.role == "TEACHER" && dao.account(student)?.role == "STUDENT")
        if (linked) dao.link(TeacherStudentEntity(actor,student)) else dao.unlink(actor,student)
    }
    suspend fun grades(actor: String, student: String): List<PracticeGrade> = db.withTransaction {
        val who = requireNotNull(dao.account(actor)); val target = requireNotNull(dao.account(student))
        require(target.role == "STUDENT" && (who.id == target.id || (who.role == "TEACHER" && student in dao.students(actor)))) { "Student is not assigned to this teacher" }
        val profile = requireNotNull(target.profileId)
        db.learningDao().profileAttempts(profile).map { attempt ->
            val questions = db.learningDao().questions(attempt.lessonId,attempt.version)
            val first = db.learningDao().answers(attempt.id).groupBy { it.questionId }.values.map { it.first() }
            PracticeGrade(attempt.id, requireNotNull(db.learningDao().lesson(attempt.lessonId,attempt.version)).title,attempt.version,attempt.completedAt,
                first.count { it.correct },questions.size,db.learningDao().states(attempt.id).sumOf { it.hintsUsed })
        }.sortedByDescending { it.completedAt ?: Long.MAX_VALUE }
    }
}

