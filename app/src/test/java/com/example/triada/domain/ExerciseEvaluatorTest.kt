package com.example.triada.domain

import com.example.triada.data.local.AdditionalContent
import com.example.triada.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ExerciseEvaluatorTest {
    private val choice get() = AdditionalContent.lessons.first().questions.first().exercise
    private val pattern get() = AdditionalContent.lessons.single { it.id == "g1-rma-patterns" }.questions.first().exercise
    private val matching get() = AdditionalContent.lessons.single { it.id == "g1-cya-body" }.questions.first().exercise
    private val word get() = AdditionalContent.lessons.first().questions.last().exercise

    @Test fun selectionAcceptsOnlyTheCorrectExistingOption() {
        assertEquals(Evaluation.Correct, ExerciseEvaluator.evaluate(choice, LearningAnswer.Choice("o2")))
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(choice, LearningAnswer.Choice("o1")))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(choice, LearningAnswer.Choice("foreign")))
    }
    @Test fun sequenceEvaluatesTheNextItem() {
        assertEquals(Evaluation.Correct, ExerciseEvaluator.evaluate(pattern, LearningAnswer.Choice("circle")))
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(pattern, LearningAnswer.Choice("square")))
    }
    @Test fun pairsRequireEveryCorrectAssociation() {
        val answer = matching.correctPairs.map { PairSelection(it.key, it.value) }
        assertEquals(Evaluation.Correct, ExerciseEvaluator.evaluate(matching, LearningAnswer.Pairs(answer.reversed())))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(matching, LearningAnswer.Pairs(answer.take(1))))
        val swapped = answer.mapIndexed { index, pair -> pair.copy(rightId = answer[1 - index].rightId) }
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(matching, LearningAnswer.Pairs(swapped)))
    }
    @Test fun duplicateOrForeignPairsCannotCountAsCorrect() {
        val pair = matching.correctPairs.entries.first().let { PairSelection(it.key, it.value) }
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(matching, LearningAnswer.Pairs(listOf(pair, pair))))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(matching, LearningAnswer.Pairs(listOf(pair, PairSelection("foreign", "foreign")))))
    }
    @Test fun lettersAreEvaluatedInOrder() {
        assertEquals(Evaluation.Correct, ExerciseEvaluator.evaluate(word, LearningAnswer.Letters(listOf("t2", "t3", "t1"))))
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(word, LearningAnswer.Letters(listOf("t1", "t2", "t3"))))
    }
    @Test fun aTileCannotBeReusedAndForeignTilesAreInvalid() {
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(word, LearningAnswer.Letters(listOf("t1", "t1"))))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(word, LearningAnswer.Letters(listOf("missing"))))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(word, LearningAnswer.Letters(emptyList())))
    }
    @Test fun repeatedLettersUseDifferentTilesAndExplicitCaseNormalization() {
        val q = QuestionPresentation("repeat", "Forma la palabra", 0, emptyList(), ActivityType.WORD_BUILDING,
            tiles = listOf(ExerciseItem("m1", "m"), ExerciseItem("a1", "a"), ExerciseItem("m2", "m"), ExerciseItem("a2", "a")))
        val definition = ExerciseDefinition(q, correctWord = "MAMA", normalization = WordNormalization.UPPERCASE)
        val answer = LearningAnswer.Letters(listOf("m1", "a1", "m2", "a2"))
        assertEquals(Evaluation.Correct, ExerciseEvaluator.evaluate(definition, answer))
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(definition.copy(normalization = WordNormalization.EXACT), answer))
        assertEquals(Evaluation.Incorrect, ExerciseEvaluator.evaluate(definition.copy(correctWord = "MAMÁ"), answer))
    }
    @Test fun wrongAnswerTypeAndUnknownRendererAreRecoverable() {
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(choice, LearningAnswer.Letters(listOf("t1"))))
        assertEquals(Evaluation.Invalid, ExerciseEvaluator.evaluate(choice.copy(content = choice.content.copy(type = ActivityType.UNKNOWN)), LearningAnswer.Choice("o2")))
    }
    @Test fun allFiveNewLessonsHaveValidOriginalExercisesAndHints() {
        assertEquals(5, AdditionalContent.lessons.size)
        val questions = AdditionalContent.lessons.flatMap { it.questions }
        assertEquals(15, questions.size)
        assertEquals(15, questions.map { it.exercise.content.id }.toSet().size)
        questions.forEach { ExerciseValidator.validate(it.exercise); assertTrue(it.hints.size >= 2) }
    }
    @Test(expected = IllegalArgumentException::class) fun incompleteAnswerKeyCannotBePublished() {
        ExerciseValidator.validate(matching.copy(correctPairs = emptyMap()))
    }
}
