package com.example.triada.feature.learning

import com.example.triada.ui.theme.Ink
import com.example.triada.ui.theme.OceanBlue
import com.example.triada.ui.theme.HoneyYellow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.triada.domain.model.*

@Composable
fun TutorAvatar(session:LessonSession,onHint:()->Unit,enabled:Boolean) {
    var visible by rememberSaveable{mutableStateOf(true)}
    val advice=AdaptiveTutor.advise(session)
    Column {
        TextButton(onClick={visible=!visible},modifier=Modifier.heightIn(min=48.dp).testTag("toggle-tutor")){Text(if(visible)"Ocultar tutor" else "Mostrar tutor")}
        if(visible) Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.size(88.dp).clearAndSetSemantics{}) {
                    val w=size.width;val h=size.height
                    drawOval(HoneyYellow,topLeft=Offset(w*.1f,h*.14f),size=androidx.compose.ui.geometry.Size(w*.8f,h*.8f))
                    for(x in listOf(.33f,.67f)) {
                        drawCircle(Color.White,w*.19f,Offset(w*x,h*.43f))
                        if(advice.mood==TutorMood.CELEBRATING) drawArc(Ink,180f,180f,false,Offset(w*(x-.09f),h*.4f),androidx.compose.ui.geometry.Size(w*.18f,h*.12f),style=Stroke(w*.03f))
                        else drawCircle(Ink,w*.065f,Offset(w*x,h*(if(advice.mood==TutorMood.THINKING).39f else .43f)))
                    }
                    drawPath(Path().apply{moveTo(w*.44f,h*.57f);lineTo(w*.56f,h*.57f);lineTo(w*.5f,h*.7f);close()},OceanBlue)
                }
                Text("${AdaptiveTutor.NAME}, tu tutor",style=MaterialTheme.typography.titleMedium)
                Text(advice.message,modifier=Modifier.testTag("tutor-message").semantics{liveRegion=LiveRegionMode.Polite})
                if(advice.offerHint) OutlinedButton(onClick=onHint,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("tutor-hint")){Text("Ver la siguiente pista")}
            }
        }
    }
}
