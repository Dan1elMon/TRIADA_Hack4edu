# Incremento 6: contrato inicial de catálogo

Se añadió `CatalogPackageService`: exporta el catálogo empaquetado como ZIP `.aula` e importa metadatos que referencian exclusivamente versiones de lecciones ya incluidas en la aplicación.

Contrato: tres archivos obligatorios (`manifest.json`, `catalog.json`, `checksums.json`), `format=aula`, `formatVersion=1`, `kind=catalog-metadata` y versión del catálogo coincidente. Se verifican hashes SHA-256 de manifiesto y catálogo, relaciones pedagógicas y compatibilidad de enlaces. Los hashes detectan corrupción, no autentican al autor.

La instalación reutiliza la transacción del repositorio de catálogo. No escribe recursos externos ni modifica perfiles o intentos. El exportador exporta el catálogo incluido en el APK, no actualizaciones importadas ni resultados de estudiantes.

El contrato es una adaptación acotada al modelo actual; no implementa todavía el formato completo de cursos, medios y resultados propuesto en el documento técnico. No hay selector de archivos en UI ni importación de nuevos ejercicios/audio. Tampoco se declara completada la seguridad para archivos externos: siguen pendientes límites de entrada comprimida, más pruebas adversas, staging recuperable de medios y validación exhaustiva antes de habilitar la función al usuario.

Se añadió una prueba instrumentada de exportación/importación repetida conservando respuestas y progreso. El próximo incremento debe ampliar las pruebas de corrupción y versiones y conectar el selector nativo solo para el alcance soportado.

Validación ejecutada: `:app:assembleDebug` y `:app:testDebugUnitTest` finalizaron correctamente. La ejecución de `:app:connectedDebugAndroidTest` no pudo comenzar porque Gradle no encontró dispositivos conectados (`No connected devices`). La nueva prueba de intercambio queda pendiente; no se declara aprobada.
