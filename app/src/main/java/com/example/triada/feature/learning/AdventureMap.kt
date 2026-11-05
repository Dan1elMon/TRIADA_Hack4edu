package com.example.triada.feature.learning

import com.example.triada.ui.theme.OceanBlue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.triada.domain.model.*

@Composable
fun AdventureMap(catalog:LearningCatalog,busy:Boolean,onSubject:(String)->Unit) {
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val n=catalog.subjects.size
            if(n>1) for(i in 0 until n-1)drawLine(OceanBlue,Offset(size.width*(if(i%2==0).25f else .75f),size.height*(i+.5f)/n),Offset(size.width*(if(i%2==0).75f else .25f),size.height*(i+1.5f)/n),8.dp.toPx(),StrokeCap.Round)
        }
        Column(verticalArrangement=Arrangement.spacedBy(24.dp)) {
            catalog.subjects.forEachIndexed { i,subject ->
                val topics=catalog.topics.filter{it.subjectId==subject.id}.map{it.id}.toSet()
                val lessons=catalog.lessons.filter{it.topicId in topics};val done=lessons.count{it.status==LearningStatus.COMPLETED}
                FilledTonalButton(onClick={onSubject(subject.id)},enabled=!busy,modifier=Modifier.fillMaxWidth().padding(start=if(i%2==0)0.dp else 40.dp,end=if(i%2==0)40.dp else 0.dp).heightIn(min=80.dp).testTag("map-${subject.code}")) {
                    Column {Text("${i+1}. ${subject.title}",style=MaterialTheme.typography.titleMedium);Text("$done de ${lessons.size} lecciones completadas · Disponible")}
                }
            }
        }
    }
}
