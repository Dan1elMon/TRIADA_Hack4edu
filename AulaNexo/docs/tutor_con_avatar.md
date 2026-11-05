# Propuesta del tutor con avatar sencillo

## Experiencia propuesta

Un búho pequeño, dibujado con círculos, ojos, alas y pico en Canvas de Compose. Mantiene el estilo crema/coral de AulaNexo, funciona sin descargar imágenes y tiene cuatro expresiones: atento, pensando, animando y celebrando. El mismo mensaje aparece como texto; la expresión nunca es la única forma de comunicar el estado.

El tutor acompaña los ejercicios, no resuelve automáticamente por el estudiante ni evalúa su personalidad. Se presenta como una ayuda de la aplicación. No necesita chat libre en la primera versión.

Ejemplo en conteo:

1. Al comenzar: «Vamos a contar. Toca cada objeto una sola vez».
2. Tras un error: «Probemos más despacio. Empieza por el primer objeto».
3. Tras errores repetidos: ofrece el botón «Muéstrame una pista» y utiliza la siguiente pista existente, sin saltarse su orden.
4. Si se agotaron las pistas: propone un ejemplo parecido con otros objetos, preparado y revisado como contenido educativo; después vuelve al ejercicio original.
5. Tras el acierto: «¡Lo lograste! ¿Seguimos con el siguiente?».

No insiste mientras el alumno permanece inactivo ni utiliza expresiones de culpa. Las notas no descuentan puntos por pedir ayuda.

## Inteligencia adaptativa sin Internet

La primera versión puede usar reglas deterministas sobre datos ya disponibles: tipo de ejercicio, errores en la pregunta actual, pistas mostradas y resultados anteriores de la misma habilidad. Por ejemplo, en asociación orienta a comparar una pareja; en palabras, a escuchar y localizar el sonido inicial; en secuencias, a identificar el bloque que se repite.

Para recomendaciones entre lecciones se debe añadir un identificador de habilidad al contenido. Un título de tema o una nota global no bastan para inferir dificultades concretas. Las reglas pueden priorizar practicar una habilidad con varios primeros intentos incorrectos y luego comprobar un ejercicio distinto. Se muestran como sugerencias, sin bloquear otras áreas.

La ayuda utiliza información de presentación y pistas autorizadas. El avatar no recibe la clave de respuestas como parte de su estado de UI. Los ejemplos adicionales se validan como contenido, separados de la respuesta de la pregunta evaluada.

## Interacciones

- Botón «Ayúdame»: abre el mensaje y la siguiente pista disponible.
- Botón «Ver un ejemplo»: aparece cuando existe un ejemplo preparado para esa habilidad.
- Botón «Escuchar»: reproduce una locución local disponible; si falta, conserva el texto y explica que no hay audio.
- Controles para ocultar el avatar, reducir movimiento y silenciarlo.
- Anuncio accesible del mensaje al cambiar, con foco estable y sin repetir continuamente el texto.

El avatar puede parpadear y mover ligeramente un ala, sin animaciones persistentes que distraigan de las opciones.

## Vista del docente

Para sus alumnos vinculados, el docente consulta aciertos a la primera, pistas utilizadas e intentos. Una ampliación puede agrupar estos indicadores por habilidad y mostrar recomendaciones como «practicar conteo uno a uno». Debe distinguir observación de recomendación; no diagnosticar trastornos ni etiquetar al alumno.

## Fases y pruebas

1. **Avatar y reglas locales:** cuatro expresiones, ayudas por tipo y uso de pistas existentes. Pruebas de orden de pistas, ausencia de respuestas filtradas y aislamiento por alumno/pregunta.
2. **Ejemplos y adaptación por habilidad:** etiquetas de habilidad, ejemplos revisados y recomendaciones. Probar que una mejora reciente puede cambiar la recomendación y que los intentos incompletos no cuentan como fracaso.
3. **Audio y accesibilidad:** locuciones locales, reducción de movimiento, TalkBack y texto ampliado en todas las pantallas del tutor.
4. **Modelo de lenguaje opcional:** solo tras definir la necesidad y los dispositivos objetivo. Requeriría evaluar tamaño, latencia, funcionamiento offline, calidad de las explicaciones y protección de datos. No se necesita para la primera versión y no se afirma que esté integrado.

## Estado

Actualización: la primera fase del búho vectorial y las reglas por tipo, errores y pistas ya está implementada y probada; ver `ampliacion_integral.md`. Las fases de adaptación longitudinal, ejemplos adicionales y modelo de lenguaje siguen como propuesta. No hay conversación abierta ni generación remota.
