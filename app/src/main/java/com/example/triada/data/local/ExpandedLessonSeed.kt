package com.example.triada.data.local

import com.example.triada.domain.model.ExerciseValidator
import org.json.JSONObject

object ExpandedLessonSeed {
    suspend fun install(db:LearningDatabase,raw:String) {
        val rows=JSONObject(raw).getJSONArray("lessons");val dao=db.learningDao()
        repeat(rows.length()){i ->
            val row=rows.getJSONObject(i);val id=row.getString("id");val version=row.getInt("version")
            val lesson=LessonEntity(id,version,row.getString("title"),row.getString("objective"));val old=dao.lesson(id,version)
            if(old!=null){require(old==lesson);return@repeat}
            val questions=row.getJSONArray("questions");require(questions.length() in 3..5)
            dao.insertLesson(lesson)
            repeat(questions.length()){n ->
                val q=questions.getJSONObject(n)
                val question=QuestionEntity(q.getString("id"),version,id,n,q.getString("prompt"),q.getInt("objectCount"),q.getString("type"),q.getJSONObject("payload").toString(),q.getJSONObject("answerKey").toString())
                ExerciseValidator.validate(ActivityCodec.decode(question,emptyList()))
                dao.insertQuestions(listOf(question))
                val hints=q.getJSONArray("hints");require(hints.length()>=2)
                dao.insertHints((0 until hints.length()).map{HintEntity(question.id,version,it,hints.getString(it))})
            }
        }
    }
}
