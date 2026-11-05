# Incremento 4: seis lecciones y motores de actividades

Fecha de cierre: 5 de noviembre de 2026 (America/Lima).

## Fase completada

Incremento 4 implementado y validado automáticamente: cinco nuevas lecciones originales, integradas con el conteo anterior. Hay **seis lecciones disponibles y 18 ejercicios**. Se conserva el desarrollo progresivo; álbum, recompensas y consolidación corresponden al incremento 5.

## Resumen de implementación

| Área | Lección | Ejercicios | Motor |
|---|---|---:|---|
| MAT | Cuento del 1 al 9 | 3 | Conteo, conservado sin cambiar su versión |
| COM | Descubro las vocales | 3 | Selección y formación de SOL con letras |
| CYA | Conozco mi cuerpo | 3 | Asociación de palabras con seis ilustraciones del cuerpo |
| PSO | Convivimos mejor | 3 | Selección de acciones de respeto y cuidado |
| RMA | Descubro patrones | 3 | Secuencias con formas, colores y etiquetas |
| RVE | Escucho sonidos | 3 | Comparación del sonido inicial mediante palabras con audio local |

Cada ejercicio incluye instrucciones, dos pistas y retroalimentación preparada. Las lecciones se completan resolviendo sus tres ejercicios; los reintentos conservan los errores anteriores y usar pistas no otorga aciertos.

La evaluación de los cinco tipos es Kotlin independiente de Android. La presentación recibe opciones, figuras o letras, sin las claves correctas. Las asociaciones deben cubrir todos los elementos sin duplicarlos; las letras se identifican como fichas individuales para permitir palabras con letras repetidas sin reutilizar la misma ficha. La normalización textual es explícita: coincidencia exacta o conversión a mayúsculas, sin eliminar tildes.

La interfaz permite corregir uniones y borrar letras antes de comprobar. Los borradores se mantienen al recrear la pantalla mediante estado guardable; el almacenamiento durable registra respuestas enviadas, pistas y avance confirmado. No se promete recuperar un borrador no enviado tras forzar el cierre.

Los tipos desconocidos muestran un mensaje recuperable y permiten regresar a las lecciones; no producen aciertos ni registros falsos. La ausencia de un audio requerido impide activar un catálogo incompleto. Si falla la reproducción durante el uso, se informa sin cerrar la actividad.

## Recursos locales

Se incluyen siete WAV: mano, mesa, sol, luna, sapo, pato y lobo. Se sintetizaron una vez con la voz local Microsoft Sabina en español de México, a velocidad reducida. La app reproduce los archivos empaquetados; no usa TTS del teléfono, descargas ni servicios externos.

`tools/generate_audio.ps1` permite regenerarlos en un equipo Windows con esa voz instalada. El script no se ejecuta durante la compilación. Los audios son sintéticos y requieren revisión pedagógica de pronunciación antes de despliegue escolar.

Las ilustraciones de mano, pie, ojo, oreja, boca, nariz y sol son vectores originales dibujados con Compose Canvas. Las secuencias incluyen formas y etiquetas para no depender únicamente del color. Las asociaciones se realizan tocando tarjetas, sin exigir arrastrar.

El reproductor nativo prepara el audio de forma asíncrona, utiliza un solo clip por pregunta y libera recursos al cambiar de pantalla o pasar a segundo plano. La implementación sigue el manejo de recursos y preparación indicado en la [documentación de MediaPlayer](https://developer.android.com/media/platform/mediaplayer/basics).

## Archivos creados

Rutas relativas a la raíz del repositorio:

```text
app/src/main/java/com/example/triada/domain/model/Activities.kt
app/src/main/java/com/example/triada/data/local/ActivityCodec.kt
app/src/main/java/com/example/triada/data/local/AdditionalContent.kt
app/src/main/java/com/example/triada/core/audio/LocalLessonAudio.kt
app/src/main/java/com/example/triada/feature/learning/ActivityRenderers.kt
app/src/main/assets/content/catalog-v2.json
app/src/main/assets/audio/mano.wav
app/src/main/assets/audio/mesa.wav
app/src/main/assets/audio/sol.wav
app/src/main/assets/audio/luna.wav
app/src/main/assets/audio/sapo.wav
app/src/main/assets/audio/pato.wav
app/src/main/assets/audio/lobo.wav
app/schemas/com.example.triada.data.local.LearningDatabase/3.json
app/src/test/java/com/example/triada/domain/ExerciseEvaluatorTest.kt
app/src/androidTest/java/com/example/triada/ExpandedActivitiesRepositoryTest.kt
app/src/androidTest/java/com/example/triada/ExpandedActivitiesUiTest.kt
app/src/androidTest/java/com/example/triada/LocalAudioTest.kt
tools/generate_audio.ps1
AulaNexo/docs/avance_incremento_4.md
```

## Archivos modificados

```text
README.md
AulaNexo/docs/analisis_y_plan_desarrollo.md
app/src/main/java/com/example/triada/domain/model/Learning.kt
app/src/main/java/com/example/triada/domain/repository/LearningRepository.kt
app/src/main/java/com/example/triada/data/local/LearningDatabase.kt
app/src/main/java/com/example/triada/data/local/CatalogMigration.kt
app/src/main/java/com/example/triada/data/local/CatalogSource.kt
app/src/main/java/com/example/triada/data/repository/RoomLearningRepository.kt
app/src/main/java/com/example/triada/data/repository/RoomCatalogRepository.kt
app/src/main/java/com/example/triada/core/di/LearningModule.kt
app/src/main/java/com/example/triada/core/navigation/AulaNexoNavHost.kt
app/src/main/java/com/example/triada/feature/learning/LearningScreens.kt
app/src/main/java/com/example/triada/feature/learning/LearningViewModel.kt
app/src/androidTest/java/com/example/triada/CatalogMigrationTest.kt
app/src/androidTest/java/com/example/triada/CatalogRepositoryTest.kt
app/src/androidTest/java/com/example/triada/LearningFlowTest.kt
```

Los documentos fuente, el catálogo versión 1 y los esquemas Room 1 y 2 se conservaron.

## Migraciones aplicadas

Se añade `CatalogMigration.FROM_2_TO_3`, registrada junto a `FROM_1_TO_2` en el proveedor real:

- `questions.activityType`: tipo de interacción; por defecto, conteo para las preguntas anteriores.
- `questions.payloadJson`: datos de presentación y retroalimentación.
- `questions.answerKeyJson`: clave interna de evaluación, separada de la presentación.
- `answers.answerPayloadJson`: respuesta tipada serializada.

No se eliminan ni reemplazan tablas existentes. El campo numérico `choice` se conserva para compatibilidad; las respuestas nuevas tienen su representación completa en `answerPayloadJson`. El reenvío de eventos antiguos con payload vacío sigue interpretando su elección numérica original. La exportación futura deberá usar el payload tipado para pares y letras.

Se probaron actualizaciones desde bases con esquemas 1 y 2 hasta el esquema 3, con datos reales de prueba de perfiles, errores, aciertos, posición y pistas. Se validó el esquema final con `MigrationTestHelper`.

El catálogo nuevo usa `catalogVersion=2` y mantiene `schemaVersion=1`. Activa cinco temas adicionales mediante los IDs ya existentes, conservando los 214 títulos. Las seis lecciones mantienen `contentVersion=1`; no se alteró el contenido publicado de conteo. Quedan 208 temas de referencia sin actividades.

## Pruebas ejecutadas y resultados

| Grupo | Resultado final |
|---|---:|
| Unitarias de conteo y pistas | 7 aprobadas |
| Unitarias de validación del catálogo | 9 aprobadas |
| Unitarias de motores y contenido nuevo | 10 aprobadas |
| Migraciones desde esquemas anteriores | 2 aprobadas |
| Catálogo y actualizaciones de metadatos | 6 aprobadas |
| Persistencia y reglas de las nuevas actividades | 5 aprobadas |
| UI de letras, pares, secuencias, audio y tipo desconocido | 5 aprobadas |
| UI de recorrido y perfiles anterior | 3 aprobadas |
| Archivos de audio y reproducción nativa | 2 aprobadas |
| Arranque/recreación y preferencias | 2 aprobadas |
| Persistencia y aislamiento anteriores | 7 aprobadas |

Total: **26 unitarias y 32 instrumentadas aprobadas**, sin fallos ni omisiones. Las instrumentadas se ejecutaron en SM-G998U, Android 15.

La prueba de integración completa las seis lecciones con respuestas explícitas y comprueba que otro perfil permanece sin avance. Se prueban asociaciones incompletas, duplicadas e incorrectas, corrección de letras, normalización sin pérdida de tildes, eventos repetidos, conservación de respuestas tipadas tras reabrir Room y ausencia de aciertos para tipos desconocidos.

Se verificó que los siete audios son archivos reproducibles con duración positiva y que el reproductor nativo reproduce un clip y maneja un recurso ausente. La UI también ejercita la referencia y una opción de audio de la lección de sonidos. Esto no sustituye una revisión auditiva pedagógica.

## Comando de compilación y resultado

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --console=plain
```

Resultado: **BUILD SUCCESSFUL**. Lint: **0 errores, 13 advertencias y 1 sugerencia**. APK: `app/build/outputs/apk/debug/app-debug.apk` (13 930 204 bytes). Las pruebas conectadas instalaron la actualización debug para validarla.

Reportes generados: `app/build/reports/tests/testDebugUnitTest/index.html`, `app/build/reports/androidTests/connected/debug/index.html` y `app/build/reports/lint-results-debug.html`.

## Limitaciones conocidas

- No se incorporaron álbum, recompensas, lector de cuentos ni intercambio `.aula`/`.aula-result`.
- Los 208 temas restantes son referencias, no fichas interactivas.
- Siguen pendientes las pruebas manuales en modo avión, muerte completa del proceso, TalkBack, texto ampliado y revisión pedagógica con docentes/estudiantes. No se cambiaron las conexiones del teléfono.
- No se ejecutó la app en un dispositivo con API 24 ni se validó la secuencia como currículo oficial.
- La reproducción y duración de los WAV se verificaron automáticamente; la calidad fonética y claridad de las ilustraciones requieren revisión humana.

## Siguiente fase sugerida

Incremento 5: recompensas transaccionales y únicas, álbum y vista consolidada de progreso. Probar desde el primer incremento de recompensas que repetir una lección o reenviar un evento no duplica premios, y cerrar la revisión de accesibilidad y validación manual offline.
