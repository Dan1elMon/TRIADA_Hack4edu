# Incremento 6: exportación de resultados por estudiante

5 de noviembre de 2026. Se añade exportación `.aula-result`; la importación/restauración de resultados no está implementada.

## Implementación

La pantalla de archivos incorpora «Exportar mis resultados» mediante CreateDocument. El ID del perfil forma parte de la ruta de navegación y se conserva para el callback del selector: no se consulta un perfil activo mutable al generar el archivo. La ruta antigua sin perfil vuelve a selección de perfiles para evitar atribuciones ambiguas.

`StudentResultsExporter` lee una instantánea en una transacción Room y después escribe el ZIP fuera de la transacción. No modifica registros. La interfaz prepara la salida en memoria antes de abrir el stream de destino. Un fallo del proveedor puede dejar un archivo incompleto; se informa al usuario.

La exportación omite nombre, avatar, enunciados y claves correctas. Incluye un seudónimo estable derivado del ID local, por lo que permite relacionar exportaciones del mismo estudiante; no se presenta como anonimización. Las respuestas se conservan con su tipo, incluidos pares y secuencias de fichas. Las respuestas numéricas anteriores se convierten al mismo contrato al exportar.

## Contrato inicial

ZIP con estos archivos UTF-8:

- `manifest.json`: `format=aula-result`, `formatVersion=1`, `kind=learning-results` y fecha en milisegundos UTC.
- `student.json`: identificador seudónimo.
- `attempts.json`: intentos, lección, versión, posición, fechas y estados de preguntas/pistas.
- `answers.json`: respuestas, corrección, pistas utilizadas, fecha y versión de contenido.
- `progress.json`: mejor avance por lección y versión con intentos registrados.
- `rewards.json`: stickers obtenidos, objetivo y fecha.
- `checksums.json`: SHA-256 de los otros seis archivos. Verifica integridad, no autoría.

Se limita la exportación a 10 000 intentos y 50 000 respuestas para acotar el trabajo en memoria. Superar el límite produce un error, sin truncar silenciosamente el historial. Un perfil sin intentos exporta colecciones vacías. Un perfil inexistente se rechaza antes de escribir.

Este contrato adapta la especificación al modelo de seis áreas y respuestas tipadas. No es un backup restaurable ni un formato de importación de nuevos ejercicios.

## Archivos

Nuevos: `data/packageio/StudentResultsExporter.kt`, bajo `app/src/main/java/com/example/triada/`, `app/src/androidTest/java/com/example/triada/StudentResultsExporterTest.kt` y este registro.

Modificados: `LearningDatabase.kt` (consultas por perfil, sin cambiar esquema), `PackageViewModel.kt`, `PackageScreen.kt`, `AulaNexoNavHost.kt` y README. No se requieren migraciones nuevas; continúa Room 4.

## Validación

Se ejecutó:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

Resultado: **BUILD SUCCESSFUL**, 35 unitarias y 40 instrumentadas aprobadas, sin fallos. Las dos pruebas nuevas verifican aislamiento entre perfiles, omisión de nombres/IDs locales, respuestas erróneas, pistas, letras ordenadas, versiones, recompensa, hashes, perfil vacío y rechazo de un perfil inexistente sin salida parcial. El teléfono utilizado fue SM-G998U, Android 15.

Después se añadió la ruta antigua de compatibilidad y se volvió a ejecutar compilación y lint. La restauración de esa ruta y el selector real requieren comprobación manual; no se declaran probados por la prueba de ZIP en memoria.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Reportes en `app/build/reports/`.

## Pendientes

Validación manual de abrir/guardar/cancelar con proveedores de documentos y revisión del archivo exportado por un docente; pruebas offline, TalkBack, escalado de texto y muerte del proceso. Los resultados no se importan ni restauran aún. La importación de nuevos ejercicios y audios sigue requiriendo staging recuperable y ampliación del contrato.
