package com.example.triada.domain

import com.example.triada.domain.model.*
import org.junit.Test
import org.junit.Assert.*

class AdaptiveTutorTest {
    private val session=LessonSession("a","p","l",1,"Título","Objetivo",0,3,0,QuestionPresentation("q","Pregunta",4,listOf(3,4,5)),false,emptyList(),false,false,0,false)
    @Test fun repeatedErrorsOfferAnOrderedHintAndCompletionCelebrates(){
        assertEquals(TutorMood.ATTENTIVE,AdaptiveTutor.advise(session).mood)
        assertTrue(AdaptiveTutor.advise(session.copy(currentErrors=2)).offerHint)
        assertFalse(AdaptiveTutor.advise(session.copy(currentErrors=2,hintsExhausted=true,visibleHints=listOf("Cuenta despacio"))).offerHint)
        assertEquals(TutorMood.CELEBRATING,AdaptiveTutor.advise(session.copy(currentSolved=true)).mood)
        assertEquals(TutorMood.CELEBRATING,AdaptiveTutor.advise(session.copy(completed=true)).mood)
    }
    @Test fun strategyDependsOnInteractionAndDoesNotRevealCount(){
        assertFalse(AdaptiveTutor.advise(session).message.contains("4"))
        val pairs=session.copy(question=session.question.copy(type=ActivityType.MATCH_PAIRS))
        assertTrue(AdaptiveTutor.advise(pairs).message.contains("pareja"))
    }
}
