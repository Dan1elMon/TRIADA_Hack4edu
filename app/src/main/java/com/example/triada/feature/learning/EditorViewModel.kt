package com.example.triada.feature.learning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triada.data.local.*
import com.example.triada.data.repository.LessonEditorRepository
import com.example.triada.domain.model.QuestionPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class EditorState(val teacher:String?=null,val drafts:List<LessonDraftEntity> = emptyList(),val topics:List<TopicEntity> = emptyList(),val id:String="",val raw:String="",val preview:List<QuestionPresentation> = emptyList(),val busy:Boolean=false,val message:String?=null)
@HiltViewModel
class EditorViewModel @Inject constructor(db:LearningDatabase,@ApplicationContext context:Context):ViewModel(){
    private val repo=LessonEditorRepository(db,context);private val mutable=MutableStateFlow(EditorState());val state=mutable.asStateFlow()
    private fun action(block:suspend()->Unit){if(state.value.busy)return;mutable.value=state.value.copy(busy=true,message=null);viewModelScope.launch{
        try{withContext(Dispatchers.IO){block()}}catch(e:Exception){if(e is CancellationException)throw e;mutable.value=state.value.copy(message="No se completó la operación. Revisa el contenido, las pistas y los recursos antes de reintentar.")}finally{mutable.value=state.value.copy(busy=false)}
    }}
    fun open(teacher:String)=action{val topics=repo.topics(teacher);mutable.value=EditorState(teacher,repo.drafts(teacher),topics,"teacher-${UUID.randomUUID()}",LessonEditorRepository.template(topics.first().id),busy=true)}
    fun select(id:String){if(state.value.busy)return;val draft=state.value.drafts.first{it.id==id};mutable.value=state.value.copy(id=id,raw=draft.json,preview=emptyList(),message=null)}
    fun newDraft(){if(!state.value.busy)mutable.value=state.value.copy(id="teacher-${UUID.randomUUID()}",raw=LessonEditorRepository.template(state.value.topics.first().id),preview=emptyList(),message=null)}
    fun save(raw:String)=action{val s=state.value;repo.save(requireNotNull(s.teacher),s.id,raw);mutable.value=s.copy(raw=raw,drafts=repo.drafts(s.teacher),message="Borrador guardado.")}
    fun preview(raw:String)=action{mutable.value=state.value.copy(raw=raw,preview=repo.validate(raw).map{it.content},message="Estructura válida. Revisa la claridad y las respuestas antes de publicar.")}
    fun publish(raw:String)=action{val s=state.value;val version=repo.publish(requireNotNull(s.teacher),s.id,raw);mutable.value=s.copy(raw=raw,drafts=repo.drafts(s.teacher),message="Lección publicada como versión $version. Los intentos anteriores se conservan.")}
}
