package com.example.triada.feature.learning

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.triada.R

@Composable
fun BrandLogo(modifier:Modifier=Modifier) {
    Image(painter=painterResource(R.drawable.aulanexo_logo),contentDescription="Logo de AulaNexo",
        contentScale=ContentScale.Fit,modifier=modifier.testTag("aulanexo-logo"))
}

@Composable
fun BrandHeader() {
    Surface(color=MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=24.dp,vertical=8.dp).heightIn(min=56.dp).testTag("brand-header"),
            horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
            BrandLogo(Modifier.size(48.dp))
            Text("AulaNexo",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
    }
}
