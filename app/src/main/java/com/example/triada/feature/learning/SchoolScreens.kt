package com.example.triada.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.triada.domain.model.*

@Composable
fun AccountsScreen(state: SchoolState, onLogin: (String,String)->Unit, onReload: ()->Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LearningPage {
        Heading("AulaNexo", "Inicia sesión para comenzar")
        OutlinedTextField(username,{if(it.length<=8&&it.all{c->c in '0'..'9'})username=it},label={Text("Usuario de 8 dígitos")},singleLine=true,keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),modifier=Modifier.fillMaxWidth().testTag("login-user"))
        OutlinedTextField(password,{password=it.take(128)},label={Text("Contraseña")},singleLine=true,visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth().testTag("login-password"))
        ActionButton("Ingresar",username.length==8&&password.isNotEmpty()&&!state.busy,"login"){onLogin(username,password);password=""}
        if(state.busy) CircularProgressIndicator()
        state.error?.let { Notice(it,true); TextButton(onClick=onReload,enabled=!state.busy){Text("Reintentar")} }
        Notice("En una instalación nueva, el docente inicial usa 10000001 y contraseña admin. Después de ingresar puedes cambiarlos en Mi acceso. Las cuentas funcionan en este dispositivo.")
    }
}

@Composable
private fun CreateAccountForm(state:SchoolState,onCreate:(String,SchoolRole)->Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var teacher by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Crear cuenta simulada",style=MaterialTheme.typography.titleLarge)
        OutlinedTextField(name,{if(it.length<=24)name=it},label={Text("Nombre o apodo")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("account-name"))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FilterChip(!teacher,{teacher=false},label={Text("Alumno")},modifier=Modifier.heightIn(min=48.dp).testTag("role-student"))
            FilterChip(teacher,{teacher=true},label={Text("Docente")},modifier=Modifier.heightIn(min=48.dp).testTag("role-teacher"))
        }
        ActionButton("Crear cuenta",name.isNotBlank()&&!state.busy,"create-account"){onCreate(name,if(teacher)SchoolRole.TEACHER else SchoolRole.STUDENT);name=""}
        state.message?.let{Notice(it)}
    }
}

@Composable
fun CredentialsScreen(state:SchoolState,onBack:()->Unit,onSave:(String,String,String)->Unit) {
    var username by rememberSaveable(state.active?.id){mutableStateOf(state.active?.username ?: "")}
    var current by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
    LearningPage {
        TextButton(onClick=onBack,enabled=!state.busy){Text("Volver a mi espacio")}
        Heading("Mi acceso","Cambia tu usuario y contraseña")
        OutlinedTextField(username,{if(it.length<=8&&it.all{c->c in '0'..'9'})username=it},label={Text("Usuario de 8 dígitos")},singleLine=true,modifier=Modifier.fillMaxWidth().testTag("new-username"))
        OutlinedTextField(current,{current=it.take(128)},label={Text("Contraseña actual")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth().testTag("current-password"))
        OutlinedTextField(password,{password=it.take(128)},label={Text("Nueva contraseña (mínimo 5 caracteres)")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth().testTag("new-password"))
        OutlinedTextField(confirm,{confirm=it.take(128)},label={Text("Repite la nueva contraseña")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth().testTag("confirm-password"))
        ActionButton("Guardar acceso",!state.busy&&username.length==8&&current.isNotEmpty()&&password.length>=5&&password==confirm,"save-credentials"){onSave(current,username,password);current="";password="";confirm=""}
        state.error?.let{Notice(it,true)};state.message?.let{Notice(it)}
    }
}

@Composable
fun SchoolHomeScreen(state: SchoolState, onLogout: ()->Unit, onLearn: (String)->Unit, onGrades: (String)->Unit, onLink: (String,Boolean)->Unit, onEdit: (String,String)->Unit, onCredentials:()->Unit, onCreate:(String,SchoolRole)->Unit) {
    val actor=state.active ?: return
    var manage by rememberSaveable(actor.id) { mutableStateOf(false) }
    var editing by rememberSaveable(actor.id) { mutableStateOf(false) }
    var name by rememberSaveable(actor.id,actor.name) { mutableStateOf(actor.name) }
    var avatar by rememberSaveable(actor.id,actor.avatar) { mutableStateOf(actor.avatar) }
    if(editing) AlertDialog(onDismissRequest={if(!state.busy)editing=false},title={Text("Editar mi cuenta")},text={
        Column {
            OutlinedTextField(name,{if(it.length<=24)name=it},label={Text("Nombre o apodo")},modifier=Modifier.testTag("school-edit-name"))
            if(actor.role==SchoolRole.STUDENT) FlowRow { listOf("star" to "Estrella","sun" to "Sol","flower" to "Flor").forEach { (key,label) -> FilterChip(avatar==key,{avatar=key},label={Text(label)},modifier=Modifier.heightIn(min=48.dp)) } }
        }
    },confirmButton={TextButton(onClick={onEdit(name,avatar);editing=false},enabled=name.isNotBlank()&&!state.busy){Text("Guardar")}},dismissButton={TextButton(onClick={editing=false},enabled=!state.busy){Text("Cancelar")}})
    LearningPage {
        TextButton(onClick=onLogout,enabled=!state.busy){Text("Cerrar sesión")}
        Heading("Hola, ${actor.name}",if(actor.role==SchoolRole.TEACHER) "Mi aula" else "Mi espacio de aprendizaje")
        TextButton(onClick={editing=true},enabled=!state.busy){Text("Editar mi cuenta")}
        TextButton(onClick=onCredentials,enabled=!state.busy){Text("Mi acceso")}
        if(state.busy) CircularProgressIndicator()
        state.error?.let { Notice(it,true) }
        if(actor.role==SchoolRole.STUDENT) {
            ActionButton("Comenzar mi aventura",!state.busy,"student-learn"){onLearn(requireNotNull(actor.profileId))}
            ActionButton("Mis notas",!state.busy,"student-grades"){onGrades(actor.id)}
            Notice("Las notas muestran cómo respondiste la primera vez. Puedes seguir practicando y consultar todos tus intentos.")
        } else {
            CreateAccountForm(state,onCreate)
            Text("Mis alumnos (${state.students.size})",style=MaterialTheme.typography.titleLarge)
            if(state.students.isEmpty()) Notice("Todavía no tienes alumnos vinculados. Agrega una cuenta de alumno a tu aula para consultar sus notas.")
            state.students.forEach { student -> ActionButton("Ver notas de ${student.name}",!state.busy,"grades-${student.id}"){onGrades(student.id)} }
            OutlinedButton(onClick={manage=!manage},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){Text(if(manage) "Cerrar lista" else "Vincular alumnos")}
            if(manage) state.accounts.filter { it.role==SchoolRole.STUDENT }.forEach { student ->
                val linked=state.students.any{it.id==student.id}
                OutlinedButton(onClick={onLink(student.id,!linked)},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("link-${student.id}")){
                    Text("${if(linked) "Quitar de mi aula" else "Agregar a mi aula"}: ${student.name} (${student.username})")
                }
            }
        }
    }
}

@Composable
fun GradesScreen(state: SchoolState, studentId: String, onBack: ()->Unit, onReload: ()->Unit) {
    LearningPage {
        TextButton(onClick=onBack,enabled=!state.busy){Text("Volver a mi espacio")}
        Heading(if(state.active?.role==SchoolRole.STUDENT)"Mis notas" else "Notas del alumno",state.gradeStudent?.takeIf{it.id==studentId}?.name ?: "Cargando…")
        Notice("Nota de práctica de 0 a 20: aciertos en la primera respuesta ÷ total de ejercicios × 20. Las pistas no descuentan puntos. Se calcula al completar cada intento; no es una calificación oficial del docente.")
        if(state.busy) CircularProgressIndicator()
        state.error?.let { Notice(it,true) }
        if(!state.busy && state.error==null && state.gradeStudent?.id==studentId) {
            if(state.grades.isEmpty()) Notice("Aún no hay intentos. Las notas aparecerán al realizar las actividades.")
            state.grades.forEachIndexed { i, grade ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(grade.lesson,style=MaterialTheme.typography.titleMedium)
                        Text("Intento ${grade.attemptId.takeLast(6)} · Contenido v${grade.version}")
                        Text(grade.score?.let { "Nota: ${String.format(java.util.Locale.getDefault(),"%.1f",it)} / 20" } ?: "En curso · Sin nota final",modifier=Modifier.testTag("grade-$i"))
                        Text("Aciertos a la primera: ${grade.firstCorrect}/${grade.total} · Pistas: ${grade.hints}")
                    }
                }
            }
        }
        ActionButton("Actualizar notas",!state.busy,"refresh-grades",onReload)
    }
}
