package com.example.triada.feature.learning

import com.example.triada.ui.theme.OceanBlue
import com.example.triada.ui.theme.HoneyYellow
import com.example.triada.ui.theme.WarmSand
import com.example.triada.ui.theme.ForestText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.triada.core.audio.rememberLessonAudio

@Composable
fun ReadingLibraryScreen(state:ReadingState,onBack:()->Unit,onOpen:(String)->Unit){
    LearningPage {
        TextButton(onClick=onBack){Text("Volver a mi espacio")};Heading("Leer y escuchar","Cuentos originales y primeras familias de palabras en inglés")
        Text("Cuentos en español",style=MaterialTheme.typography.titleLarge)
        state.books.filter{it.language=="es"}.forEach{book->ActionButton(book.title,true,"book-${book.id}"){onOpen(book.id)}}
        Text("Fonética inglesa inicial",style=MaterialTheme.typography.titleLarge)
        Notice("Escucha palabras cortas y compara sus sonidos finales. Puedes repetirlas a velocidad lenta. No se evalúa tu voz.")
        state.books.filter{it.language=="en"}.forEach{book->ActionButton(book.title,true,"book-${book.id}"){onOpen(book.id)}}
    }
}
@Composable
private fun ReadingScene(scene:String){
    Canvas(Modifier.fillMaxWidth().height(120.dp).clearAndSetSemantics{}){
        val w=size.width;val h=size.height
        drawRect(WarmSand);drawCircle(HoneyYellow,h*.2f,Offset(w*.85f,h*.25f))
        if(scene=="plant") {drawLine(ForestText,Offset(w*.5f,h*.85f),Offset(w*.5f,h*.3f),12f);drawOval(ForestText,Offset(w*.35f,h*.35f),Size(w*.15f,h*.22f));drawOval(ForestText,Offset(w*.5f,h*.5f),Size(w*.15f,h*.22f))}
        else if(scene=="bridge"){drawRect(OceanBlue,Offset(0f,h*.7f),Size(w,h*.3f));drawLine(OceanBlue,Offset(w*.15f,h*.5f),Offset(w*.85f,h*.5f),16f);for(x in listOf(.2f,.8f))drawLine(OceanBlue,Offset(w*x,h*.5f),Offset(w*x,h),16f)}
        else if(scene=="cat"){drawCircle(OceanBlue,h*.32f,Offset(w*.5f,h*.55f));for(x in listOf(.45f,.55f))drawCircle(Color.White,h*.05f,Offset(w*x,h*.48f))}
        else {drawRect(OceanBlue,Offset(w*.2f,h*.2f),Size(w*.6f,h*.65f));drawRect(Color.White,Offset(w*.23f,h*.23f),Size(w*.54f,h*.57f));drawLine(OceanBlue,Offset(w*.5f,h*.23f),Offset(w*.5f,h*.8f),5f)}
    }
}
@Composable
fun ReaderScreen(state:ReadingState,profile:String,bookId:String,onBack:()->Unit,onMove:(Int)->Unit,onFinish:()->Unit,onAnswer:(Int)->Unit,onRetry:()->Unit){
    val book=state.book?.takeIf{state.profile==profile&&it.id==bookId};val progress=state.progress
    var slow by rememberSaveable{mutableStateOf(false)}
    val audio=rememberLessonAudio("$bookId-${progress?.pageIndex}")
    LearningPage {
        TextButton(onClick=onBack,enabled=!state.busy){Text("Volver a mis lecturas")}
        if(book!=null&&progress!=null){
            val page=book.pages[progress.pageIndex]
            Heading(book.title,"Página ${progress.pageIndex+1} de ${book.pages.size}")
            ReadingScene(book.scene)
            Text(page.text,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.testTag("reading-text"))
            FilterChip(slow,{slow=!slow},label={Text("Pronunciación lenta")},modifier=Modifier.heightIn(min=48.dp))
            ActionButton("Escuchar página",true,"listen-page"){audio.play(page.audio,if(slow).75f else 1f)}
            Text("Toca una palabra para escucharla")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){page.words.forEachIndexed{i,word->OutlinedButton(onClick={audio.play(word.audio,if(slow).75f else 1f)},modifier=Modifier.heightIn(min=48.dp).testTag("reading-word-$i")){Text(word.text)}}}
            audio.status?.let{Notice(it)}
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick={onMove(-1)},enabled=!state.busy&&progress.pageIndex>0,modifier=Modifier.weight(1f).heightIn(min=56.dp)){Text("Anterior")}
                OutlinedButton(onClick={onMove(1)},enabled=!state.busy&&progress.pageIndex<book.pages.lastIndex,modifier=Modifier.weight(1f).heightIn(min=56.dp).testTag("reading-next")){Text("Siguiente")}
            }
            if(progress.pageIndex==book.pages.lastIndex)ActionButton("Terminé de leer",!state.busy,"finish-reading",onFinish)
            if(progress.completed){
                Notice(if(progress.comprehensionCorrect)"Lectura completada y comprensión practicada." else "Lectura completada. Practiquemos una pregunta.")
                Text(book.question,style=MaterialTheme.typography.titleLarge)
                book.choices.forEachIndexed{i,choice->ActionButton(choice,!state.busy,"reading-answer-$i"){onAnswer(i)}}
            }
        }
        if(state.busy)CircularProgressIndicator()
        state.message?.let{Notice(it)}
        TextButton(onClick=onRetry,enabled=!state.busy){Text("Volver a cargar")}
    }
}
