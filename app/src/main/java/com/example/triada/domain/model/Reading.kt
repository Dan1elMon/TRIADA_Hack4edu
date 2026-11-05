package com.example.triada.domain.model

data class ReadingWord(val text:String,val audio:String)
data class ReadingPage(val text:String,val audio:String,val words:List<ReadingWord>)
data class ReadingBook(val id:String,val title:String,val language:String,val scene:String,val pages:List<ReadingPage>,val question:String,val choices:List<String>)
