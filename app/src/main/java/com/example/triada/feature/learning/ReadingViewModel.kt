package com.example.triada.feature.learning

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triada.data.local.*
import com.example.triada.data.repository.ReadingRepository
import com.example.triada.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReadingState(val books:List<ReadingBook> = emptyList(),val profile:String?=null,val book:ReadingBook?=null,val progress:ReadingProgressEntity?=null,val busy:Boolean=false,val message:String?=null)
@HiltViewModel
class ReadingViewModel @Inject constructor(db:LearningDatabase,@ApplicationContext context:Context):ViewModel(){
    private val repository=ReadingRepository(db,context)
    private val mutable=MutableStateFlow(ReadingState(books=repository.books));val state=mutable.asStateFlow()
    private fun action(block:suspend()->Unit){if(state.value.busy)return;mutable.value=state.value.copy(busy=true,message=null);viewModelScope.launch{
        try{block()}catch(e:Exception){if(e is CancellationException)throw e;mutable.value=state.value.copy(message="No pudimos guardar la lectura. Vuelve a intentarlo.")}finally{mutable.value=state.value.copy(busy=false)}
    }}
    fun open(profile:String,book:String)=action{val progress=repository.open(profile,book);mutable.value=state.value.copy(profile=profile,book=repository.books.first{it.id==book},progress=progress)}
    fun move(delta:Int)=action{val s=state.value;mutable.value=s.copy(progress=repository.move(requireNotNull(s.profile),requireNotNull(s.book).id,delta))}
    fun finish()=action{val s=state.value;mutable.value=s.copy(progress=repository.finish(requireNotNull(s.profile),requireNotNull(s.book).id))}
    fun answer(index:Int)=action{val s=state.value;val correct=repository.answer(requireNotNull(s.profile),requireNotNull(s.book).id,index);mutable.value=s.copy(progress=repository.open(s.profile,s.book.id),message=if(correct)"¡Bien! Relacionaste la respuesta con la lectura." else "Volvamos a leer y comparar las opciones.")}
}
