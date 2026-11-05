# Primera entrega de desarrollo: base y recorrido mínimo

Fecha: 4 de noviembre de 2026 (America/Lima).

## Fase completada

Incremento 1: base ejecutable validada. Incremento 2: recorrido mínimo implementado y validado automáticamente; quedan pendientes la prueba manual de muerte completa del proceso, modo avión, accesibilidad y revisión pedagógica. No se declara completado el MVP de seis áreas.

## Resumen de implementación

- Nombre visible **AulaNexo**, paleta centralizada, superficies redondeadas y una navegación Compose: perfiles → aventura de Matemática → ejercicio/resultado.
- Perfiles locales con apodo y tres avatares. Se conserva el último perfil elegido en DataStore.
- Primera lección original de conteo, con tres ejercicios de 4, 6 y 9 círculos. Evaluación Kotlin independiente de Android y modelos de presentación sin claves de respuesta.
- Dos pistas progresivas por pregunta. Pedir ayuda no aumenta el avance; las pistas se conservan y dejan de ofrecerse al agotarse.
- Room conserva perfiles, contenido versionado, preguntas, opciones, pistas, intentos, respuestas y estado de cada pregunta. El esquema inicial se exporta.
- Los errores se guardan como respuestas independientes; un acierto no los sobrescribe. Un mismo evento de respuesta puede reenviarse sin duplicar el registro.
- Confirmación de respuesta y avance en una transacción. No se permite avanzar una pregunta sin resolver. La UI bloquea acciones de escritura simultáneas y permite recuperar el avance guardado ante errores.
- Reapertura del intento en la posición confirmada; repetir crea otro intento sin eliminar el anterior. El mapa calcula el mejor avance y conserva la finalización, mientras la práctica muestra el avance del intento actual.
- Aislamiento por `profileId`, con comprobación de propiedad del intento en cada lectura/escritura.
- Sin permiso `INTERNET`. Respaldo automático y transferencia de datos de la aplicación excluidos para esta entrega; la exportación explícita llegará en otro incremento.

Se conserva `com.example.triada` como identificador técnico. No se crearon otro módulo Android ni otro aplicativo.

## Archivos creados

Rutas relativas a la raíz del repositorio. Para los archivos Kotlin de producción, el prefijo es `app/src/main/java/com/example/triada/`:

```text
AulaNexoApplication.kt
core/di/LearningModule.kt
core/navigation/AulaNexoNavHost.kt
data/local/LearningDatabase.kt
data/local/StarterContent.kt
data/repository/DataStoreProfilePreferences.kt
data/repository/RoomLearningRepository.kt
domain/model/Learning.kt
domain/repository/LearningRepository.kt
feature/learning/LearningScreens.kt
feature/learning/LearningViewModel.kt
```

Otros archivos creados:

```text
app/schemas/com.example.triada.data.local.LearningDatabase/1.json
app/src/test/java/com/example/triada/domain/CountLearningTest.kt
app/src/androidTest/java/com/example/triada/LearningFlowTest.kt
app/src/androidTest/java/com/example/triada/RoomLearningRepositoryTest.kt
app/src/androidTest/java/com/example/triada/ProfilePreferencesTest.kt
app/src/androidTest/java/com/example/triada/ProductionStartupTest.kt
AulaNexo/docs/avance_incrementos_1_2.md
```

## Archivos modificados

```text
README.md
build.gradle.kts
app/build.gradle.kts
gradle/libs.versions.toml
gradle/gradle-daemon-jvm.properties
gradle.properties
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/triada/MainActivity.kt
app/src/main/java/com/example/triada/ui/theme/Color.kt
app/src/main/java/com/example/triada/ui/theme/Theme.kt
app/src/main/res/values/strings.xml
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
AulaNexo/docs/analisis_y_plan_desarrollo.md
```

Se retiraron `ExampleUnitTest.kt` y `ExampleInstrumentedTest.kt`, sustituidos por pruebas del comportamiento real de AulaNexo. No se modificaron `doc_tecnica.md` ni `temario.md`.

## Migraciones aplicadas

Ninguna: esquema propio inicial Room versión 1. No se usa recuperación destructiva de la base de datos. El progreso del incremento se deriva de los intentos y estados durables; no se duplica en una segunda tabla de porcentajes. Al incorporar catálogo o cambiar el esquema se deberá añadir y probar la migración correspondiente.

La semilla de la versión publicada inserta contenido faltante sin reemplazar registros ni progreso. Se verificó su repetición idempotente. Todavía no se implementó actualización a otra versión de catálogo o lección.

## Dependencias y ajuste del entorno

Se conservaron AGP, Gradle, Kotlin y BOM Compose del proyecto. Las dependencias añadidas se contrastaron con sus fuentes oficiales:

| Dependencia | Versión usada | Referencia |
|---|---|---|
| Navigation Compose | 2.9.8 | [Notas de Navigation](https://developer.android.com/jetpack/androidx/releases/navigation#2.9.8) |
| Room | 2.8.5 | [Notas de Room](https://developer.android.com/jetpack/androidx/releases/room#2.8.5) |
| DataStore | 1.3.0 | [Notas de DataStore](https://developer.android.com/jetpack/androidx/releases/datastore#1.3.0) |
| KSP | 2.3.13 | [Versión publicada de KSP](https://github.com/google/ksp/releases/tag/2.3.13) |
| Dagger/Hilt | 2.60.1 | [Versión publicada de Dagger](https://github.com/google/dagger/releases/tag/dagger-2.60.1) |

El primer build con código Java generado falló porque el criterio de daemon Java 25 seleccionaba el JRE de VS Code, sin `jlink`. Se alineó `gradle-daemon-jvm.properties` con el JDK 21 completo ya configurado en `gradle.properties`. También se escaparon los separadores de unidad de sus dos rutas Windows para corregir errores `PropertyEscape` de lint.

## Pruebas ejecutadas y resultados

| Validación | Resultado |
|---|---|
| Plantilla antes de modificar | Compilación y test de plantilla correctos |
| Base de nombre, tema y navegación | Compilación y test inicial correctos antes de integrar persistencia |
| Pruebas unitarias funcionales | **7 aprobadas**, 0 fallos |
| Pruebas instrumentadas de Room | **7 aprobadas**, 0 fallos |
| Pruebas Compose del recorrido y cambio de perfil | **2 aprobadas**, 0 fallos |
| Recreación de DataStore | **1 aprobada**, 0 fallos |
| Arranque real con Hilt y recreación de Activity | **1 aprobada**, 0 fallos |
| Lint final | **0 errores, 12 advertencias** |

Las 11 pruebas instrumentadas se ejecutaron en el teléfono conectado SM-G998U, Android 15. Las pruebas de datos y navegación usan bases aisladas; la prueba de arranque usa el grafo real sin crear/modificar perfiles del usuario.

Las pruebas cubren evaluación correcta/incorrecta/inválida, orden y agotamiento de pistas, registros de errores, propiedad e independencia de perfiles, rechazo de avance prematuro, reenvío idempotente de respuesta, semilla repetida, finalización, repetición sin pérdida del histórico y reapertura de base en disco conservando posición/pistas. La recreación de Activity y la reapertura de Room no equivalen a matar y relanzar el proceso completo.

Las advertencias restantes son recomendaciones de nuevas versiones y recursos/etiquetas de la plantilla sin uso o redundantes. No se ocultaron con un baseline ni se actualizaron dependencias existentes solo para silenciarlas.

## Comando de compilación y resultado

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --console=plain
```

Resultado tras corregir entorno y lint: **BUILD SUCCESSFUL**. Tras añadir la prueba de arranque se ejecutó de nuevo `:app:connectedDebugAndroidTest :app:lintDebug`, también **BUILD SUCCESSFUL** (11 pruebas instrumentadas).

APK: `app/build/outputs/apk/debug/app-debug.apk` (13 322 567 bytes). Los tests conectados instalaron la variante debug en el teléfono para validarla.

Evidencias generadas localmente por Gradle:

- Unitarias: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Instrumentadas: `app/build/reports/androidTests/connected/debug/index.html` y `app/build/outputs/androidTest-results/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.

## Limitaciones conocidas

- Solo una lección de Matemática; todavía no se carga el catálogo de 214 títulos ni las otras cinco áreas.
- Solo motor de conteo. No hay audio, cuentos, formación de palabras, secuencias, asociaciones, álbum o intercambio de paquetes.
- Sin edición/eliminación de perfiles en esta entrega.
- Pruebas manuales de modo avión, muerte completa del proceso, texto ampliado, TalkBack y revisión pedagógica pendientes. No se cambiaron las conexiones del teléfono.
- No se validó el SDK mínimo 24 en otro dispositivo ni la actualización entre esquemas/versions de contenido distintos.
- Se usan tipografías locales del sistema. Falta el documento visual `diseno_ui.md` y no se añadieron fuentes ni medios de terceros.
- Las rutas de JDK existentes en `gradle.properties` siguen siendo propias de este equipo; deben configurarse al trasladar el proyecto.

## Siguiente fase sugerida

Completar la validación manual del recorrido mínimo y avanzar al incremento 3: catálogo local versionado de 214 títulos y jerarquía grado → área → unidad → lección. Añadir la migración necesaria con pruebas de conservación del avance antes de extender las pantallas. Solo habilitar una lección cuando tenga ejercicios completos.
