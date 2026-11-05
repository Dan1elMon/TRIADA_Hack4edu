# Incremento 3: catálogo local y navegación por áreas

Fecha de cierre: 5 de noviembre de 2026 (America/Lima).

## Fase completada

Incremento 3 del plan progresivo: catálogo consultable, migración y navegación grado → área → unidad → lección. Se mantiene una lección ejecutable de Matemática; completar las otras cinco lecciones corresponde al incremento 4.

## Resumen de implementación

- Catálogo empaquetado en `assets/content/catalog-v1.json`: primer grado, seis áreas, seis unidades introductorias y **214 títulos originales**. Se contrastaron los 214 títulos con el documento fuente, respetando texto y orden.
- Distribución: MAT 45, COM 58, CYA 36, PSO 29, RMA 26 y RVE 20.
- Disponibilidad separada del progreso personal: un tema `AVAILABLE` enlaza la lección real de conteo y 213 temas permanecen `CATALOG_ONLY`.
- Las seis áreas y unidades pueden explorarse. Los temas sin actividades aparecen en preparación y sus tarjetas no tienen acción de abrir una actividad.
- Listas Compose reutilizables y con carga perezosa para consultar todos los títulos. Se reutilizan el tema, las pantallas de perfiles y ejercicios y el único NavHost existente.
- Recorrido nuevo: perfil → grado → áreas → unidades → lecciones → resumen de la lección → actividad. La pantalla de referencia permite consultar el temario de cada área.
- El catálogo muestra progreso por perfil, conserva el mejor avance y distingue lección sin empezar, en progreso y completada. Explorar el catálogo no crea un intento; se prepara al abrir una lección real.
- Se conservan las rutas anteriores necesarias para restaurar navegación guardada por la primera entrega.
- No se añadieron servicios remotos, permisos de red ni recursos descargados.

## Contrato y actualización de datos

`schemaVersion=1` describe el JSON; `catalogVersion=1` identifica esta edición del catálogo; `contentVersion=1` identifica la lección existente. Son independientes de la versión 2 del esquema Room.

El catálogo incluye IDs explícitos, independientes de títulos. Los IDs publicados en el archivo son la correspondencia estable: no regenerarlos al cambiar títulos u orden pedagógico. `sourceOrder` conserva el orden de referencia y no representa un prerrequisito educativo.

El importador local valida versión de formato, IDs únicos, conteos, orden, referencias entre grado/área/unidad/tema y disponibilidad de lecciones antes de escribir. Una lección solo se activa si existen sus preguntas, opciones y pistas locales completas. Esta comprobación se limita al motor de conteo implementado.

La instalación es transaccional e idempotente. Se usan upserts de metadatos, sin borrar tablas ni avances. Un catálogo de versión superior puede actualizar títulos manteniendo los IDs; no puede retirar temas ni enlaces publicados. No se aplica una versión anterior sobre una posterior ya instalada. Un checksum local impide reemplazar silenciosamente el contenido de una versión publicada; no es una firma de autoría.

Esta instalación lee el archivo empaquetado. No implementa importación de paquetes externos `.aula` ni actualización de ejercicios a otra versión de contenido.

## Archivos creados

```text
app/src/main/assets/content/catalog-v1.json
app/src/main/java/com/example/triada/domain/model/Catalog.kt
app/src/main/java/com/example/triada/domain/repository/CatalogRepository.kt
app/src/main/java/com/example/triada/data/local/CatalogData.kt
app/src/main/java/com/example/triada/data/local/CatalogSource.kt
app/src/main/java/com/example/triada/data/local/CatalogMigration.kt
app/src/main/java/com/example/triada/data/repository/RoomCatalogRepository.kt
app/src/main/java/com/example/triada/feature/learning/CatalogScreens.kt
app/schemas/com.example.triada.data.local.LearningDatabase/2.json
app/src/test/java/com/example/triada/domain/CatalogValidatorTest.kt
app/src/androidTest/java/com/example/triada/CatalogRepositoryTest.kt
app/src/androidTest/java/com/example/triada/CatalogMigrationTest.kt
AulaNexo/docs/avance_incremento_3.md
```

## Archivos modificados

```text
README.md
app/build.gradle.kts
gradle/libs.versions.toml
app/src/main/java/com/example/triada/core/di/LearningModule.kt
app/src/main/java/com/example/triada/core/navigation/AulaNexoNavHost.kt
app/src/main/java/com/example/triada/data/local/LearningDatabase.kt
app/src/main/java/com/example/triada/data/repository/RoomLearningRepository.kt
app/src/main/java/com/example/triada/domain/repository/LearningRepository.kt
app/src/main/java/com/example/triada/feature/learning/LearningViewModel.kt
app/src/main/java/com/example/triada/feature/learning/LearningScreens.kt
app/src/androidTest/java/com/example/triada/LearningFlowTest.kt
AulaNexo/docs/analisis_y_plan_desarrollo.md
```

Los documentos fuente `doc_tecnica.md` y `temario.md` no se modificaron.

## Migraciones aplicadas

`CatalogMigration.FROM_1_TO_2`: añade las tablas de metadatos, grados, áreas, unidades, temas y enlaces a lecciones, con índices y claves foráneas. No altera ni borra las ocho tablas anteriores. El proveedor real de la base registra esta migración; no existe fallback destructivo.

La prueba usa el esquema exportado de la versión 1, inserta perfil, contenido, intento, errores, acierto y pistas; ejecuta la migración y valida el esquema 2 con `MigrationTestHelper`. Después instala el catálogo y comprueba los datos conservados mediante los repositorios. El esquema 1 se mantiene intacto. Este flujo sigue la [guía oficial de pruebas de migración de Room](https://developer.android.com/training/data-storage/room/migrating-db-versions#test-migrations).

## Pruebas ejecutadas y resultados

| Validación | Resultado final |
|---|---|
| Unitarias de evaluación y pistas, existentes | 7 aprobadas |
| Unitarias del validador de catálogo, nuevas | 9 aprobadas |
| Room: perfiles, intentos y reanudación | 7 aprobadas |
| Catálogo: conteos, idempotencia, actualización y rechazo de datos inválidos | 6 aprobadas |
| Migración 1 → 2 y conservación de datos | 1 aprobada |
| UI: recorrido, perfiles y áreas sin actividades | 3 aprobadas |
| Preferencias y arranque/recreación reales | 2 aprobadas |
| Lint | 0 errores, 13 advertencias |

Total: **16 pruebas unitarias y 19 instrumentadas aprobadas**, sin fallos ni pruebas omitidas. Las instrumentadas se ejecutaron en SM-G998U con Android 15.

Además del recuento, se comparó por script cada título y orden del JSON contra `temario.md`: **214 coincidencias exactas**.

Se verificó que una actualización de metadatos conserva errores, pistas y progreso; que un catálogo inválido no modifica el instalado; y que la falta de contenido ejecutable revierte toda la instalación. La prueba de UI recorre las seis áreas, consulta hasta el último tema de Comunicación y comprueba que los temas de referencia no tienen acción de juego ni generan intentos.

Durante la ejecución conjunta apareció un `AbstractMethodError` en el decodificador de esquemas de la herramienta de pruebas de Room: el runtime de la app resolvía serialization-core 1.7.3 y el decodificador JSON de las pruebas requería 1.8.1. Se alineó el core a **1.8.1** mediante una dependencia explícita; después se repitió toda la validación con éxito. La versión se contrastó con su [publicación oficial](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.8.1). No se cambió de versión Room, Kotlin, AGP o Compose para resolverlo.

Las advertencias de lint corresponden a versiones más recientes disponibles y recursos/etiquetas de plantilla sin uso o redundantes. También permanece una advertencia de deprecación de `assets.srcDir` en la configuración de las pruebas; la configuración funciona y la migración se ejecutó.

## Comando de compilación y resultado

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug --console=plain
```

Resultado final: **BUILD SUCCESSFUL**. APK actualizado: `app/build/outputs/apk/debug/app-debug.apk` (13 353 707 bytes). Las pruebas conectadas instalaron la variante debug conservando la instalación existente.

Reportes locales:

- `app/build/reports/tests/testDebugUnitTest/index.html`.
- `app/build/reports/androidTests/connected/debug/index.html`.
- `app/build/reports/lint-results-debug.html`.

## Limitaciones conocidas

- Los 214 títulos son referencias; solo una lección tiene actividades. Las otras cinco unidades introductorias muestran su estado en preparación.
- No hay todavía selección de palabras/vocales, secuencias, asociación, formación de palabras, audio, recompensas ni intercambio de archivos.
- No se validó el temario como secuencia curricular oficial.
- Siguen pendientes las comprobaciones manuales en modo avión, muerte completa del proceso, TalkBack, texto grande y revisión pedagógica, y la ejecución en el SDK mínimo 24.
- Se probó una actualización de metadatos del catálogo; no se implementó una actualización de preguntas a otra `contentVersion`.

## Siguiente fase sugerida

Incremento 4: añadir los evaluadores y renderizadores de selección, secuencia, pares y formación de palabras, con pruebas al incorporar cada motor. Completar las cinco lecciones originales restantes y activar sus temas solo cuando tengan ejercicios y recursos utilizables; conservar el progreso ya probado del motor de conteo.
