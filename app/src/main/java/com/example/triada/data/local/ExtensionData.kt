package com.example.triada.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName="credentials", foreignKeys=[ForeignKey(entity=SchoolAccountEntity::class,parentColumns=["id"],childColumns=["accountId"])],indices=[Index(value=["username"],unique=true)])
data class CredentialEntity(@PrimaryKey val accountId:String,val username:String,val salt:String,val passwordHash:String,val iterations:Int)
@Entity(tableName="reading_progress",primaryKeys=["profileId","bookId"],foreignKeys=[ForeignKey(entity=ProfileEntity::class,parentColumns=["id"],childColumns=["profileId"])])
data class ReadingProgressEntity(val profileId:String,val bookId:String,val pageIndex:Int,val completed:Boolean,val comprehensionCorrect:Boolean)
@Entity(tableName="daily_goals",foreignKeys=[ForeignKey(entity=ProfileEntity::class,parentColumns=["id"],childColumns=["profileId"])])
data class DailyGoalEntity(@PrimaryKey val profileId:String,val target:Int)
@Entity(tableName="lesson_drafts",foreignKeys=[ForeignKey(entity=SchoolAccountEntity::class,parentColumns=["id"],childColumns=["teacherId"])],indices=[Index("teacherId")])
data class LessonDraftEntity(@PrimaryKey val id:String,val teacherId:String,val json:String,val updatedAt:Long)
@Dao interface ExtensionDao {
    @Query("SELECT * FROM credentials WHERE username=:username") suspend fun byUsername(username:String):CredentialEntity?
    @Query("SELECT * FROM credentials WHERE accountId=:account") suspend fun credential(account:String):CredentialEntity?
    @Query("SELECT username FROM credentials") suspend fun usernames():List<String>
    @Insert suspend fun insertCredential(row:CredentialEntity)
    @Update suspend fun updateCredential(row:CredentialEntity)
    @Query("SELECT * FROM reading_progress WHERE profileId=:profile AND bookId=:book") suspend fun reading(profile:String,book:String):ReadingProgressEntity?
    @Upsert suspend fun saveReading(row:ReadingProgressEntity)
    @Query("SELECT * FROM daily_goals WHERE profileId=:profile") suspend fun goal(profile:String):DailyGoalEntity?
    @Upsert suspend fun saveGoal(row:DailyGoalEntity)
    @Query("SELECT * FROM lesson_drafts WHERE teacherId=:teacher ORDER BY updatedAt DESC") suspend fun drafts(teacher:String):List<LessonDraftEntity>
    @Upsert suspend fun saveDraft(row:LessonDraftEntity)
}
object ExtensionMigration {
    val FROM_5_TO_6=object:Migration(5,6){override fun migrate(db:SupportSQLiteDatabase){
        db.execSQL("CREATE TABLE IF NOT EXISTS credentials (accountId TEXT NOT NULL, username TEXT NOT NULL, salt TEXT NOT NULL, passwordHash TEXT NOT NULL, iterations INTEGER NOT NULL, PRIMARY KEY(accountId), FOREIGN KEY(accountId) REFERENCES school_accounts(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credentials_username ON credentials(username)")
        db.execSQL("CREATE TABLE IF NOT EXISTS reading_progress (profileId TEXT NOT NULL, bookId TEXT NOT NULL, pageIndex INTEGER NOT NULL, completed INTEGER NOT NULL, comprehensionCorrect INTEGER NOT NULL, PRIMARY KEY(profileId,bookId), FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE TABLE IF NOT EXISTS daily_goals (profileId TEXT NOT NULL, target INTEGER NOT NULL, PRIMARY KEY(profileId), FOREIGN KEY(profileId) REFERENCES profiles(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE TABLE IF NOT EXISTS lesson_drafts (id TEXT NOT NULL, teacherId TEXT NOT NULL, json TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(teacherId) REFERENCES school_accounts(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lesson_drafts_teacherId ON lesson_drafts(teacherId)")
    }}
}
