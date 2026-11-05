package com.example.triada.feature.learning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun EditorScreen(state:EditorState,onBack:()->Unit,onSelect:(String)->Unit,onNew:()->Unit,onSave:(String)->Unit,onPreview:(String)->Unit,onPublish:(String)->Unit) {
    var raw by rememberSaveable(state.id,state.raw){mutableStateOf(state.raw)}
    var advanced by rememberSaveable{mutableStateOf(false)};var topicsOpen by remember{mutableStateOf(false)}
    val parsed=runCatching{JSONObject(raw)}.getOrNull()
    fun update(block:(JSONObject)->Unit){parsed?.let{val copy=JSONObject(it.toString());block(copy);raw=copy.toString(2)}}
    LearningPage {
        TextButton(onClick=onBack,enabled=!state.busy){Text("Volver a mi aula")};Heading("Editor de lecciones","Guarda borradores, revisa y publica nuevas versiones")
        Notice("La publicación conserva las versiones anteriores. Revisa la pertinencia de cada pregunta con un docente antes de usarla con el alumnado.")
        ActionButton("Nuevo borrador",!state.busy&&state.topics.isNotEmpty(),"new-draft",onNew)
        state.drafts.forEach{draft->OutlinedButton(onClick={onSelect(draft.id)},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(runCatching{JSONObject(draft.json).getJSONObject("lesson").getString("title")}.getOrDefault("Borrador"))}}
        if(parsed!=null) {
            Text("Tema: "+(state.topics.firstOrNull{it.id==parsed.optString("topicId")}?.title ?: "Seleccionar"))
            Box {OutlinedButton(onClick={topicsOpen=true},enabled=!state.busy){Text("Elegir tema")};DropdownMenu(topicsOpen,{topicsOpen=false},modifier=Modifier.heightIn(max=320.dp)){state.topics.forEach{topic->DropdownMenuItem(text={Text(topic.title)},onClick={update{it.put("topicId",topic.id)};topicsOpen=false})}}}
            val lesson=parsed.optJSONObject("lesson")
            if(lesson!=null) {
                OutlinedTextField(lesson.optString("title"),{text->update{it.getJSONObject("lesson").put("title",text.take(160))}},label={Text("Título")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy)
                OutlinedTextField(lesson.optString("objective"),{text->update{it.getJSONObject("lesson").put("objective",text.take(1000))}},label={Text("Objetivo")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy)
                val questions=lesson.optJSONArray("questions")
                if(!advanced&&questions!=null) repeat(questions.length()){n ->
                    val q=questions.getJSONObject(n);Text("Ejercicio ${n+1}",style=MaterialTheme.typography.titleLarge)
                    OutlinedTextField(q.optString("prompt"),{text->update{it.getJSONObject("lesson").getJSONArray("questions").getJSONObject(n).put("prompt",text.take(2000))}},label={Text("Pregunta")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy)
                    if(q.optString("type")=="SINGLE_CHOICE") {
                        val choices=q.getJSONObject("payload").getJSONArray("choices")
                        repeat(choices.length()){c->val choice=choices.getJSONObject(c);OutlinedTextField(choice.getString("text"),{text->update{it.getJSONObject("lesson").getJSONArray("questions").getJSONObject(n).getJSONObject("payload").getJSONArray("choices").getJSONObject(c).put("text",text.take(1000))}},label={Text(if(choice.getString("id")==q.getJSONObject("answerKey").getString("choice"))"Opción correcta" else "Otra opción")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy)}
                    }
                    val hints=q.optJSONArray("hints");if(hints!=null)repeat(hints.length()){h->OutlinedTextField(hints.getString(h),{text->update{it.getJSONObject("lesson").getJSONArray("questions").getJSONObject(n).getJSONArray("hints").put(h,text.take(2000))}},label={Text("Pista ${h+1}")},modifier=Modifier.fillMaxWidth(),enabled=!state.busy)}
                }
            }
        }
        FilterChip(advanced,{advanced=!advanced},label={Text("Edición avanzada de estructura")},modifier=Modifier.heightIn(min=48.dp))
        if(advanced||parsed==null)OutlinedTextField(raw,{if(it.length<=64_000)raw=it},label={Text("Contenido JSON: cinco tipos de actividad")},modifier=Modifier.fillMaxWidth().heightIn(min=240.dp).testTag("draft-json"),enabled=!state.busy)
        ActionButton("Guardar borrador",!state.busy&&raw.isNotEmpty(),"save-draft"){onSave(raw)}
        ActionButton("Validar y previsualizar",!state.busy&&raw.isNotEmpty(),"preview-draft"){onPreview(raw)}
        state.preview.takeIf{raw==state.raw}?.forEachIndexed{n,q->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Vista previa ${n+1}: ${q.prompt}");Text(q.instructions);(q.choices+q.left+q.right+q.tiles).forEach{Text(it.text)};if(q.objectCount>0)Text("Objetos: ${q.objectCount}")}}}
        ActionButton("Publicar lección",!state.busy&&raw==state.raw&&state.preview.isNotEmpty(),"publish-draft"){onPublish(raw)}
        if(state.busy)CircularProgressIndicator();state.message?.let{Notice(it)}
    }
}

