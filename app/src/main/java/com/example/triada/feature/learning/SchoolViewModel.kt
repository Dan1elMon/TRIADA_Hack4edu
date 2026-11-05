package com.example.triada.feature.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.triada.data.local.LearningDatabase
import com.example.triada.data.repository.SchoolRepository
import com.example.triada.data.repository.LocalAuthRepository
import com.example.triada.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SchoolState(val accounts: List<SchoolAccount> = emptyList(), val active: SchoolAccount? = null,
    val students: List<SchoolAccount> = emptyList(), val grades: List<PracticeGrade> = emptyList(),
    val gradeStudent: SchoolAccount? = null, val busy: Boolean = false, val error: String? = null, val message:String?=null)

@HiltViewModel
class SchoolViewModel @Inject constructor(db: LearningDatabase) : ViewModel() {
    private val repository = SchoolRepository(db)
    private val auth=LocalAuthRepository(db)
    private var failed=0
    private var retryAt=0L
    private val mutable = MutableStateFlow(SchoolState())
    val state = mutable.asStateFlow()
    init { refresh() }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy=true,error=null)
        viewModelScope.launch {
            try { block() } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.value = mutable.value.copy(error=if(e is IllegalArgumentException) e.message ?: "Comprueba los datos." else "No pudimos cargar o guardar los datos. Vuelve a intentarlo.")
            } finally { mutable.value = mutable.value.copy(busy=false) }
        }
    }
    fun refresh() = action { auth.initialize();mutable.value = mutable.value.copy(accounts=repository.accounts()) }
    fun create(name: String, role: SchoolRole) = action {
        val teacher=requireNotNull(state.value.active);require(teacher.role==SchoolRole.TEACHER)
        val before=repository.accounts().map{it.id}.toSet()
        repository.create(name,role); auth.initialize()
        val all=repository.accounts(); val added=all.first{it.id !in before}
        if(role==SchoolRole.STUDENT)repository.link(teacher.id,added.id,true)
        mutable.value=mutable.value.copy(accounts=all,students=repository.linkedStudents(teacher.id),message="Cuenta creada. Usuario: ${added.username}. Contraseña inicial: admin.")
    }
    fun login(username:String,password:String) = action {
        require(android.os.SystemClock.elapsedRealtime()>=retryAt){"Espera 30 segundos antes de volver a intentar."}
        val active=auth.login(username,password)
        if(active==null){failed++;if(failed>=5){retryAt=android.os.SystemClock.elapsedRealtime()+30_000;failed=0};throw IllegalArgumentException("Usuario o contraseña incorrectos.")}
        failed=0;retryAt=0
        val all=repository.accounts()
        mutable.value=mutable.value.copy(accounts=all,active=active,grades=emptyList(),gradeStudent=null,
            message=null,students=if(active.role==SchoolRole.TEACHER)repository.linkedStudents(active.id) else emptyList())
    }
    fun changeCredentials(current:String,username:String,password:String)=action {
        val id=requireNotNull(state.value.active).id
        auth.change(id,current,username,password)
        val all=repository.accounts();mutable.value=mutable.value.copy(accounts=all,active=all.first{it.id==id},message="Usuario y contraseña actualizados.")
    }
    fun logout() { if(!state.value.busy) mutable.value=SchoolState(accounts=state.value.accounts) }
    fun edit(name: String, avatar: String) = action {
        val id=requireNotNull(state.value.active).id
        repository.edit(id,name,avatar)
        val accounts=repository.accounts()
        mutable.value=mutable.value.copy(accounts=accounts,active=accounts.first{it.id==id})
    }
    fun link(student: String, linked: Boolean) = action {
        val active=requireNotNull(state.value.active)
        repository.link(active.id,student,linked)
        mutable.value=mutable.value.copy(students=repository.linkedStudents(active.id),grades=emptyList(),gradeStudent=null)
    }
    fun loadGrades(student: String) = action {
        val actor=requireNotNull(state.value.active)
        val grades=repository.grades(actor.id,student)
        mutable.value=mutable.value.copy(grades=grades,gradeStudent=repository.accounts().first { it.id==student })
    }
}
