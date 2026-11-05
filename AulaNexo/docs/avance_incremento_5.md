# Incremento 5: recompensas, álbum y progreso

Fecha: 5 de noviembre de 2026, America/Lima.

## Fase completada

Implementación y validación automatizada del sistema de recompensas y seguimiento. La validación manual de accesibilidad, modo avión y revisión pedagógica sigue pendiente; no se declara terminada la estabilización del MVP.

## Resumen de implementación

- Seis stickers, uno por lección, agrupados en Cosmic Cat, Sunny Sun y Bouncing Bear.
- Álbum accesible desde la pantalla del grado, con premios obtenidos y pendientes, nombre y objetivo de cada uno. Usa símbolos locales del sistema.
- Resumen con lecciones completadas, ejercicios resueltos y progreso por área, con acceso a cada lección.
- El denominador incluye solo las seis lecciones y 18 ejercicios disponibles; excluye los 208 títulos de referencia.
- Cada premio es único por perfil y objetivo de lección, independiente de repeticiones o versiones del contenido. El registro conserva su fecha original.
- La respuesta final, el progreso, la finalización y el sticker se guardan en la misma transacción Room. La pantalla de resultado confirma el sticker obtenido sin presentarlo como nuevo al repetir.
- Cambiar de perfil carga su propio álbum y progreso. Practicar nuevamente no reduce el mejor avance ni elimina recompensas.

## Archivos creados

```text
app/src/main/java/com/example/triada/domain/model/Rewards.kt
app/src/main/java/com/example/triada/data/local/RewardsData.kt
app/src/main/java/com/example/triada/feature/learning/RewardsScreens.kt
app/schemas/com.example.triada.data.local.LearningDatabase/4.json
app/src/test/java/com/example/triada/domain/ProgressOverviewTest.kt
app/src/androidTest/java/com/example/triada/RewardsRepositoryTest.kt
app/src/androidTest/java/com/example/triada/RewardsMigrationTest.kt
app/src/androidTest/java/com/example/triada/RewardsUiTest.kt
AulaNexo/docs/avance_incremento_5.md
```

## Archivos modificados en este incremento

README y análisis del plan; `LearningDatabase`, `CatalogMigration`, `LearningModule`, `RoomLearningRepository`, `LearningRepository`, `Learning.kt`, `LearningViewModel`, `AulaNexoNavHost`, `CatalogScreens`, `LearningScreens` y `CatalogMigrationTest`. No se modificaron los documentos fuente ni los catálogos o ejercicios publicados.

## Migraciones aplicadas

`FROM_3_TO_4` añade `earned_stickers`, con clave primaria `(profileId, lessonId)`, unicidad `(profileId, stickerId)` y clave foránea al perfil. No borra ni altera datos anteriores.

La migración recupera las finalizaciones anteriores, agrupadas por perfil/lección, y concede una sola recompensa con la fecha de finalización más antigua. Los intentos incompletos no conceden premios. Sus IDs se congelaron en el SQL de la migración para mantenerla independiente de cambios futuros del catálogo de stickers.

Se registran las rutas 1 → 2 → 3 → 4. Se conservan los esquemas exportados anteriores y se validan las migraciones con datos de prueba. No se usa fallback destructivo.

## Pruebas ejecutadas y resultados

- **29 pruebas unitarias aprobadas**: se añaden tres para el resumen vacío, cálculo de avance y objetivos únicos.
- **36 pruebas instrumentadas distintas aprobadas** en SM-G998U, Android 15: suite completa de 35 y una prueba adicional de UI del álbum/progreso ejecutada por separado tras incorporarla.
- **Lint: 0 errores, 13 advertencias y 1 sugerencia**.

Las pruebas nuevas comprueban: ausencia de premio antes de terminar; errores que no conceden premio; reenvíos simultáneos del mismo evento; repetición completa sin duplicado ni cambio de fecha; aislamiento entre estudiantes; reversión de la respuesta y finalización si falla el premio; recuperación de premios antiguos entre versiones; persistencia al reabrir la base; y navegación por álbum/progreso al cambiar de perfil.

Se utilizó un trigger de fallo solo en una base aislada de prueba para demostrar la atomicidad. Al quitarlo y reintentar, se confirmó una sola respuesta final y un solo premio.

## Comandos y compilación

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest :app:lintDebug '-Pandroid.testInstrumentationRunnerArguments.class=com.example.triada.RewardsUiTest' --console=plain
```

Ambas ejecuciones terminaron en **BUILD SUCCESSFUL**. La segunda valida únicamente la prueba nueva del álbum y progreso; no repite el resto de la suite.

APK: `app/build/outputs/apk/debug/app-debug.apk`, 14 066 841 bytes.

Evidencias locales: `app/build/reports/incremento5-full.xml` conserva los resultados de las 35 pruebas; `app/build/outputs/androidTest-results/connected/debug/` contiene la prueba adicional. Los reportes de unitarias y lint están en `app/build/reports/`.

## Limitaciones conocidas

No se incorporó intercambio `.aula`/`.aula-result`, autoría docente ni lector de cuentos. Los stickers usan símbolos del sistema; no se añadieron ilustraciones raster personalizadas. Siguen pendientes modo avión, cierre completo del proceso, TalkBack, texto ampliado, dispositivo API 24 y revisión pedagógica humana. No se cambiaron las conexiones del teléfono.

## Siguiente fase sugerida

Incremento 6: contrato de intercambio offline, importación y exportación seguras. Probar validación de paquetes y recuperación ante fallos antes de exponer esas acciones en la interfaz. Completar en paralelo las verificaciones manuales pendientes del flujo de aprendizaje.
