package com.example.triada.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "school_accounts", foreignKeys = [ForeignKey(entity = ProfileEntity::class, parentColumns = ["id"], childColumns = ["profileId"])], indices = [Index(value = ["profileId"], unique = true)])
data class SchoolAccountEntity(@PrimaryKey val id: String, val name: String, val role: String, val profileId: String?)

@Entity(tableName = "teacher_students", primaryKeys = ["teacherId", "studentId"], foreignKeys = [
    ForeignKey(entity = SchoolAccountEntity::class, parentColumns = ["id"], childColumns = ["teacherId"]),
    ForeignKey(entity = SchoolAccountEntity::class, parentColumns = ["id"], childColumns = ["studentId"])], indices = [Index("studentId")])
data class TeacherStudentEntity(val teacherId: String, val studentId: String)

@Dao
interface SchoolDao {
    @Query("SELECT * FROM school_accounts ORDER BY role, name, id") suspend fun accounts(): List<SchoolAccountEntity>
    @Query("SELECT * FROM school_accounts WHERE id = :id") suspend fun account(id: String): SchoolAccountEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addAccount(row: SchoolAccountEntity)
    @Query("UPDATE school_accounts SET name = :name WHERE id = :id") suspend fun rename(id: String, name: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(row: TeacherStudentEntity)
    @Query("SELECT studentId FROM teacher_students WHERE teacherId = :teacherId ORDER BY studentId") suspend fun students(teacherId: String): List<String>
    @Query("DELETE FROM teacher_students WHERE teacherId = :teacherId AND studentId = :studentId") suspend fun unlink(teacherId: String, studentId: String)
}

object SchoolMigration {
    val FROM_4_TO_5 = object : Migration(4,5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS school_accounts (id TEXT NOT NULL, name TEXT NOT NULL, role TEXT NOT NULL, profileId TEXT, PRIMARY KEY(id), FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_school_accounts_profileId ON school_accounts(profileId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS teacher_students (teacherId TEXT NOT NULL, studentId TEXT NOT NULL, PRIMARY KEY(teacherId,studentId), FOREIGN KEY(teacherId) REFERENCES school_accounts(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(studentId) REFERENCES school_accounts(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_students_studentId ON teacher_students(studentId)")
        }
    }
}
