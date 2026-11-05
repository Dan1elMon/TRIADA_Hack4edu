package com.example.triada.data.local

import com.example.triada.domain.model.CountLesson
import com.example.triada.domain.model.CountQuestion

/** Original exercises, packaged locally. IDs and published versions must stay stable. */
object StarterContent {
    val lesson = CountLesson(
        id = "g1-mat-count-1-9", version = 1,
        title = "Cuento del 1 al 9",
        objective = "Contar grupos de hasta nueve objetos, uno por uno.",
        questions = listOf(
            CountQuestion(
                "g1-mat-count-q01", "¿Cuántos círculos hay?", 4, listOf(3, 4, 5),
                listOf("Señala cada círculo mientras cuentas.", "Empieza por el de arriba a la izquierda y cuenta cada uno una sola vez."),
            ),
            CountQuestion(
                "g1-mat-count-q02", "Cuenta este nuevo grupo de círculos.", 6, listOf(7, 5, 6),
                listOf("Cuenta despacio, sin saltarte ningún círculo.", "Al terminar una fila, sigue contando en la siguiente."),
            ),
            CountQuestion(
                "g1-mat-count-q03", "¿Cuántos círculos ves ahora?", 9, listOf(8, 9, 7),
                listOf("Recorre los círculos de izquierda a derecha.", "El último número que dices indica cuántos círculos hay en total."),
            ),
        ),
    )
}
