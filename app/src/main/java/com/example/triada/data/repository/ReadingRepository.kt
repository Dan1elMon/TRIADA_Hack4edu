package com.example.triada.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.*
import org.json.JSONObject

class ReadingRepository(private val db:LearningDatabase,context:Context) {
    private val definitions=JSONObject(context.assets.open("content/readers-v1.json").bufferedReader().use{it.readText()}).getJSONArray("books")
    val books=(0 until definitions.length()).map { i ->
        val b=definitions.getJSONObject(i);val pages=b.getJSONArray("pages")
        ReadingBook(b.getString("id"),b.getString("title"),b.getString("language"),b.getString("scene"),(0 until pages.length()).map{p ->
            val page=pages.getJSONObject(p);val words=page.getJSONArray("words")
            ReadingPage(page.getString("text"),page.getString("audio"),(0 until words.length()).map{w->words.getJSONObject(w).let{ReadingWord(it.getString("text"),it.getString("audio"))}})
        },b.getString("question"),b.getJSONArray("choices").let{a->(0 until a.length()).map{a.getString(it)}})
    }
    suspend fun open(profile:String,book:String):ReadingProgressEntity {
        requireNotNull(db.learningDao().profile(profile));require(books.any{it.id==book})
        return db.extensionDao().reading(profile,book) ?: ReadingProgressEntity(profile,book,0,false,false)
    }
    suspend fun move(profile:String,book:String,delta:Int)=db.withTransaction {
        require(delta in -1..1);val current=open(profile,book);val definition=books.first{it.id==book}
        current.copy(pageIndex=(current.pageIndex+delta).coerceIn(definition.pages.indices)).also{db.extensionDao().saveReading(it)}
    }
    suspend fun finish(profile:String,book:String)=db.withTransaction {
        val current=open(profile,book);require(current.pageIndex==books.first{it.id==book}.pages.lastIndex)
        current.copy(completed=true).also{db.extensionDao().saveReading(it)}
    }
    suspend fun answer(profile:String,book:String,index:Int):Boolean=db.withTransaction {
        val current=open(profile,book);val definition=books.first{it.id==book}
        require(current.completed&&index in definition.choices.indices)
        val row=(0 until definitions.length()).map{definitions.getJSONObject(it)}.first{it.getString("id")==book}
        val correct=index==row.getInt("correct")
        db.extensionDao().saveReading(current.copy(comprehensionCorrect=current.comprehensionCorrect||correct));correct
    }
}
