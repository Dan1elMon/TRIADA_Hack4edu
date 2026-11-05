package com.example.triada.domain.model

enum class TutorMood { ATTENTIVE, THINKING, ENCOURAGING, CELEBRATING }
data class TutorAdvice(val mood:TutorMood,val message:String,val offerHint:Boolean=false)
object AdaptiveTutor {
    const val NAME="Juan"
    fun advise(session:LessonSession):TutorAdvice {
        if(session.completed)return TutorAdvice(TutorMood.CELEBRATING,"¡Completaste tu aventura! Puedes volver a practicar cuando quieras.")
        if(session.currentSolved)return TutorAdvice(TutorMood.CELEBRATING,"¡Encontraste la respuesta! Sigamos con el siguiente ejercicio.")
        val strategy=when(session.question.type){
            ActivityType.COUNT_OBJECTS -> "Señala cada objeto una sola vez y cuenta despacio."
            ActivityType.MATCH_PAIRS -> "Compara una pareja a la vez. Puedes cambiar una unión antes de responder."
            ActivityType.WORD_BUILDING -> "Di la palabra despacio y busca las letras en el orden en que las escuchas."
            ActivityType.SEQUENCE -> "Busca la parte que se repite y observa qué viene después."
            else -> if(session.question.cueAudioKey!=null)"Escucha de nuevo y compara las opciones." else "Lee la pregunta y compara cada opción antes de elegir."
        }
        if(session.hintsExhausted)return TutorAdvice(TutorMood.ENCOURAGING,"Ya vimos las pistas. Volvamos a una de ellas: ${session.visibleHints.lastOrNull() ?: strategy}")
        if(session.currentErrors>=2)return TutorAdvice(TutorMood.THINKING,"Probemos de otra manera. $strategy Puedes pedirme la siguiente pista.",true)
        if(session.visibleHints.isNotEmpty())return TutorAdvice(TutorMood.THINKING,"Usemos esta idea: ${session.visibleHints.last()}",session.currentErrors>0)
        if(session.currentErrors>0)return TutorAdvice(TutorMood.ENCOURAGING,"Puedes volver a intentarlo. $strategy",true)
        return TutorAdvice(TutorMood.ATTENTIVE,"Hola, soy $NAME. Estoy aquí para ayudarte. $strategy")
    }
}
