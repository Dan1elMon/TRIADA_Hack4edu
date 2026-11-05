package com.example.triada.data.local

import com.example.triada.domain.model.*

data class PreparedQuestion(val exercise: ExerciseDefinition, val hints: List<String>)
data class PreparedLesson(val id: String, val title: String, val objective: String, val questions: List<PreparedQuestion>, val version: Int = 1)

/** Original exercises. Published IDs and content versions remain immutable. */
object AdditionalContent {
    private fun choice(id: String, prompt: String, options: List<String>, correct: Int, hints: List<String>, cue: String? = null) = PreparedQuestion(
        ExerciseDefinition(QuestionPresentation(id, prompt, 0, emptyList(), ActivityType.SINGLE_CHOICE,
            if (cue == null) "Lee las opciones y elige una respuesta." else "Escucha la palabra de referencia y cada opción. Compara cómo empiezan.",
            choices = options.mapIndexed { index, text -> ExerciseItem("o${index + 1}", text, audioKey = if (cue == null) null else text) }, cueAudioKey = cue,
            feedbackCorrect = if (cue == null) "¡Bien! Encontraste la opción que corresponde." else "¡Bien! Encontraste el mismo sonido inicial.",
            feedbackIncorrect = if (cue == null) "Observa las opciones otra vez. Puedes pedir una pista." else "Escucha de nuevo el comienzo de cada palabra. Puedes pedir una pista."),
            correctChoiceId = "o${correct + 1}"), hints,
    )

    private val redCircle = ExerciseItem("circle", "Círculo rojo", "●", "red")
    private val blueSquare = ExerciseItem("square", "Cuadrado azul", "■", "blue")
    private val yellowTriangle = ExerciseItem("triangle", "Triángulo amarillo", "▲", "yellow")
    private fun sequence(id: String, items: List<ExerciseItem>, correct: ExerciseItem) = PreparedQuestion(
        ExerciseDefinition(QuestionPresentation(id, "¿Qué figura sigue en la secuencia?", 0, emptyList(), ActivityType.SEQUENCE,
            "Observa el orden de las figuras y elige la que continúa.", choices = listOf(blueSquare, yellowTriangle, redCircle), sequence = items,
            feedbackCorrect = "¡Bien! Descubriste cómo se repite el patrón.", feedbackIncorrect = "Mira el patrón desde el principio y vuelve a intentarlo."), correctChoiceId = correct.id),
        listOf("Recorre las figuras de izquierda a derecha.", "Busca el grupo de figuras que se repite y piensa cuál sigue."),
    )

    private fun pairs(id: String, parts: List<Pair<String, String>>) = PreparedQuestion(
        ExerciseDefinition(QuestionPresentation(id, "Une cada palabra con su imagen.", 0, emptyList(), ActivityType.MATCH_PAIRS,
            "Toca una palabra y después su imagen. Puedes cambiar una unión antes de comprobar.",
            left = parts.map { (key, text) -> ExerciseItem("word-$key", text) },
            right = parts.reversed().mapIndexed { index, (key, _) -> ExerciseItem("image-$key", "Imagen ${'A' + index}", illustrationKey = key) },
            feedbackCorrect = "¡Bien! Reconociste estas partes del cuerpo.", feedbackIncorrect = "Observa las imágenes y revisa tus uniones."),
            correctPairs = parts.associate { (key, _) -> "word-$key" to "image-$key" }),
        listOf("Mira cada imagen con calma antes de unirla.", "Señala esa parte de tu cuerpo y compárala con el dibujo."),
    )

    val lessons = listOf(
        PreparedLesson("g1-com-vowels", "Descubro las vocales", "Reconocer vocales iniciales y ordenar letras para formar una palabra.", listOf(
            choice("g1-com-vowels-q01", "¿Con qué vocal empieza oso?", listOf("A", "O", "E"), 1,
                listOf("Di oso despacio y escucha el comienzo.", "Alarga el primer sonido y compáralo con cada vocal.")),
            choice("g1-com-vowels-q02", "¿Con qué vocal empieza uva?", listOf("U", "I", "A"), 0,
                listOf("Pronuncia uva con calma.", "Escucha solo el sonido que dices antes de la v.")),
            PreparedQuestion(ExerciseDefinition(QuestionPresentation("g1-com-vowels-q03", "Escucha y forma el nombre de lo que ves.", 0, emptyList(), ActivityType.WORD_BUILDING,
                "Toca las letras en orden. Puedes borrar la última letra y volver a intentarlo.",
                tiles = listOf(ExerciseItem("t1", "L"), ExerciseItem("t2", "S"), ExerciseItem("t3", "O")), cueAudioKey = "sol", illustrationKey = "sun",
                feedbackCorrect = "¡Bien! Ordenaste las letras para formar la palabra.", feedbackIncorrect = "Escucha otra vez y revisa el orden de las letras."), correctWord = "SOL", normalization = WordNormalization.UPPERCASE),
                listOf("Escucha el nombre y busca la letra de su primer sonido.", "Coloca una letra cada vez y lee lo que vas formando.")),
        )),
        PreparedLesson("g1-rma-patterns", "Descubro patrones", "Continuar patrones usando figuras y colores como señales complementarias.", listOf(
            sequence("g1-rma-patterns-q01", listOf(redCircle, blueSquare, redCircle, blueSquare), redCircle),
            sequence("g1-rma-patterns-q02", listOf(yellowTriangle, yellowTriangle, redCircle, yellowTriangle, yellowTriangle), redCircle),
            sequence("g1-rma-patterns-q03", listOf(redCircle, blueSquare, yellowTriangle, redCircle, blueSquare), yellowTriangle),
        )),
        PreparedLesson("g1-cya-body", "Conozco mi cuerpo", "Asociar seis partes del cuerpo con ilustraciones originales.", listOf(
            pairs("g1-cya-body-q01", listOf("hand" to "mano", "foot" to "pie")),
            pairs("g1-cya-body-q02", listOf("eye" to "ojo", "ear" to "oreja")),
            pairs("g1-cya-body-q03", listOf("mouth" to "boca", "nose" to "nariz")),
        )),
        PreparedLesson("g1-pso-coexistence", "Convivimos mejor", "Reconocer acciones de respeto y cuidado en el aula.", listOf(
            choice("g1-pso-coexistence-q01", "Una compañera está hablando. ¿Qué puedes hacer?", listOf("Interrumpirla", "Escuchar y esperar mi turno", "Gritar más fuerte"), 1,
                listOf("Piensa en cómo te gusta que te escuchen.", "Busca la acción que permite que todos participen con calma.")),
            choice("g1-pso-coexistence-q02", "Encuentras colores que otra persona está usando. ¿Qué haces?", listOf("Pedir permiso para compartir", "Quitárselos sin preguntar", "Esconderlos"), 0,
                listOf("Los materiales pueden compartirse con respeto.", "Piensa qué puedes decir antes de tomar algo que usa otra persona.")),
            choice("g1-pso-coexistence-q03", "Se cae agua al suelo del aula. ¿Qué ayuda a cuidarnos?", listOf("Correr por el charco", "Dejarlo y no avisar", "Avisar a una persona adulta y evitar que alguien resbale"), 2,
                listOf("El suelo mojado puede hacer que alguien se caiga.", "Busca una acción que pida ayuda y cuide a los demás.")),
        )),
        PreparedLesson("g1-rve-initial-sounds", "Escucho sonidos", "Identificar palabras que comparten su sonido inicial mediante audios locales.", listOf(
            choice("g1-rve-sounds-q01", "¿Qué palabra empieza como mano?", listOf("sol", "mesa", "luna"), 1,
                listOf("Escucha primero mano y fíjate en su comienzo.", "Repite el inicio de cada opción, sin decir toda la palabra."), "mano"),
            choice("g1-rve-sounds-q02", "¿Qué palabra empieza como sol?", listOf("mesa", "pato", "sapo"), 2,
                listOf("Vuelve a escuchar el comienzo de sol.", "Compara solo el primer sonido de las opciones."), "sol"),
            choice("g1-rve-sounds-q03", "¿Qué palabra empieza como luna?", listOf("lobo", "pato", "mesa"), 0,
                listOf("Escucha el inicio de luna con atención.", "Repite despacio cada opción y busca un comienzo parecido."), "luna"),
        )),
    ).also { lessons ->
        lessons.forEach { lesson ->
            require(lesson.questions.size in 3..5)
            lesson.questions.forEach { ExerciseValidator.validate(it.exercise); require(it.hints.size >= 2) }
        }
    }
}
