# Incremento 6: selector e intercambio limitado de catálogos

5 de noviembre de 2026. Esta entrega habilita el contrato de metadatos; no completa el intercambio de cursos, medios y resultados del documento técnico.

## Implementación

Desde el grado se abre «Intercambiar catálogo». Se utilizan OpenDocument y CreateDocument de Android, sin permisos generales de almacenamiento. Cancelar el selector no ejecuta operaciones. Las operaciones se ejecutan fuera del hilo principal y bloquean nuevas acciones mientras están en curso.

Se importan únicamente paquetes `catalog-metadata` con los tres archivos del contrato y referencias a lecciones ya incluidas. La exportación guarda el catálogo original del APK, sin perfiles, respuestas o actualizaciones importadas. La pantalla explica ese alcance. Un fallo de exportación puede dejar un archivo incompleto; el mensaje lo indica.

La validación limita los bytes comprimidos consumidos a 40 MiB, cada archivo a 8 MiB, el total descomprimido a 32 MiB y las entradas a 512. Manifiesto y checksums tienen límite de 8 KiB; catálogo, 2 MiB; anidamiento JSON, 32 niveles. Rechaza rutas inseguras, duplicados, directorios con datos, archivos adicionales, hashes alterados y versiones incompatibles. Los hashes no autentican al autor del paquete.

La instalación reutiliza la transacción existente del catálogo. No extrae recursos ni cambia ejercicios o resultados. No requiere una nueva migración Room.

## Archivos

Nuevos: `feature/learning/PackageViewModel.kt` y `PackageScreen.kt`, bajo `app/src/main/java/com/example/triada/`, y este registro.

Modificados: `MainActivity.kt`, `core/navigation/AulaNexoNavHost.kt`, `feature/learning/CatalogScreens.kt`, `data/packageio/SafePackageReader.kt`, `data/packageio/CatalogPackageService.kt`, `SafePackageReaderTest.kt`, `CatalogPackageTest.kt` y README.

## Validación ejecutada

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

Resultado: **BUILD SUCCESSFUL**, **35 pruebas unitarias y 38 instrumentadas aprobadas**, sin fallos. Lint: 0 errores, 13 advertencias y 1 sugerencia. Dispositivo: SM-G998U, Android 15.

Se cerró la prueba de ida/vuelta que había quedado pendiente por ausencia de dispositivo. La importación repetida conserva respuestas y progreso. Los paquetes con formato incompatible, catálogo alterado sin actualizar su hash o archivos inesperados se rechazan sin instalar datos. Las pruebas unitarias incluyen límites de entrada comprimida y datos ocultos en directorios.

El APK actualizado está en `app/build/outputs/apk/debug/app-debug.apk`. Los reportes se encuentran en `app/build/reports/`.

## Pendientes y límites

El selector nativo está implementado y compila, pero falta recorrer manualmente abrir/guardar/cancelar con un proveedor de documentos real, incluidos fallos del proveedor. Las pruebas instrumentadas de paquetes trabajan con streams en memoria; no se presenta el flujo del selector como validado manualmente.

Todavía no se exportan resultados `.aula-result` ni se admiten nuevos ejercicios o medios. Para incorporarlos se necesita ampliar el contrato, staging recuperable y pruebas adversas adicionales. Siguen pendientes las verificaciones manuales offline, accesibilidad, muerte completa del proceso y revisión pedagógica.

Siguiente paso: exportación de resultados por estudiante y validación manual del selector antes de ampliar la importación de contenidos.
