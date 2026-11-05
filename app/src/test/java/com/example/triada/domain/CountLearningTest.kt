package com.example.triada.domain

import com.example.triada.data.local.StarterContent
import com.example.triada.domain.model.CountEvaluator
import com.example.triada.domain.model.Evaluation
import com.example.triada.domain.model.ProgressiveHints
import org.junit.Assert.*
import org.junit.Test

class CountLearningTest {
    @Test fun everyOriginalQuestionAcceptsItsCount() {
        StarterContent.lesson.questions.forEach { assertEquals(Evaluation.Correct, CountEvaluator.evaluate(it, it.objectCount)) }
    }
    @Test fun wrongChoiceIsNotAccepted() {
        val question = StarterContent.lesson.questions.first()
        assertEquals(Evaluation.Incorrect, CountEvaluator.evaluate(question, 3))
    }
    @Test fun choiceOutsideQuestionIsInvalid() {
        assertEquals(Evaluation.Invalid, CountEvaluator.evaluate(StarterContent.lesson.questions.first(), 99))
    }
    @Test fun hintsAdvanceAndStopAtTheEnd() {
        assertEquals(1, ProgressiveHints.nextCount(2, 0))
        assertEquals(2, ProgressiveHints.nextCount(2, 1))
        assertEquals(2, ProgressiveHints.nextCount(2, 2))
        assertEquals(0, ProgressiveHints.nextCount(0, 0))
    }
    @Test fun hintsAreShownInOrderWithoutInventingContent() {
        assertEquals(listOf("first"), ProgressiveHints.visible(listOf("first", "second"), 1))
        assertTrue(ProgressiveHints.visible(emptyList(), 4).isEmpty())
    }
    @Test(expected = IllegalArgumentException::class) fun invalidHintStateIsRejected() {
        ProgressiveHints.nextCount(2, 3)
    }
    @Test fun starterContentHasStableUniqueQuestionsAndValidOptions() {
        val questions = StarterContent.lesson.questions
        assertEquals(3, questions.size)
        assertEquals(questions.size, questions.map { it.id }.toSet().size)
        questions.forEach {
            assertTrue(it.objectCount in 1..9)
            assertEquals(1, it.options.count { option -> option == it.objectCount })
            assertEquals(it.options.size, it.options.toSet().size)
            assertTrue(it.hints.size >= 2)
        }
    }
}
