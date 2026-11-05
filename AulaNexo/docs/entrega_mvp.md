# Entrega técnica del MVP AulaNexo

Fecha: 5 de noviembre de 2026. Proyecto Android `com.example.triada`.

Ampliación posterior: las cuentas simuladas, las notas y la migración al esquema 5 se documentan en [cuentas_y_notas.md](cuentas_y_notas.md). Los resultados de este informe corresponden al cierre anterior de seis lecciones.

## Alcance entregado

Se completan los incrementos del MVP en español definidos por el plan progresivo: seis áreas, seis lecciones y 18 ejercicios originales. Los 214 títulos del temario están disponibles como referencia; 208 siguen en preparación. Esto no equivale a desarrollar 214 lecciones ni a implementar todos los requisitos del documento técnico original.

| Capacidad | Entrega |
|---|---|
| Perfiles | Crear, seleccionar y editar nombre/avatar; progreso separado y último perfil recordado |
| Navegación | Grado, área, unidad, lección y consulta del temario |
| Actividades | Conteo, selección, secuencia, asociación y formación de palabras |
| Apoyo | Feedback, pistas progresivas, ilustraciones originales y siete audios locales en español |
| Persistencia | Intentos, respuestas, posición, pistas, mejor progreso y migraciones Room hasta esquema 4 |
| Recompensas | Seis stickers únicos por perfil, álbum y resumen |
| Contenido | Importar metadatos o lecciones completas con WAV; exportar contenido instalado |
| Resultados | Exportación seudónima y recuperación en perfil separado, idempotente y transaccional |
| Archivos | Selector nativo de Android para abrir/guardar y selector de aplicaciones para compartir |

## Contratos y protección de datos

Un `.aula` de contenido es un ZIP con `manifest.json`, `catalog.json`, `lessons.json`, `checksums.json` y recursos `resources/audio/<clave>.wav`. El manifiesto declara `format=aula`, `formatVersion=1`, `kind=learning-content` y `catalogVersion`. Se siguen admitiendo paquetes de metadatos anteriores.

Las lecciones contienen identificador, versión, título, objetivo y preguntas con tipo, payload, respuesta, opciones y pistas. Los contratos se validan antes de instalar. No se permite cambiar una lección publicada conservando su versión. El catálogo instalado puede exportarse de nuevo con sus actividades y recursos.

La lectura limita el ZIP a 512 entradas, 8 MiB por archivo, 32 MiB descomprimidos y 40 MiB de entrada. Se rechazan rutas inseguras, duplicados, archivos inesperados, hashes incoherentes y JSON excesivo. Los WAV se validan y publican como archivos privados inmutables identificados por hash; las referencias activas cambian en una transacción Room. Un fallo previo al commit puede dejar recursos inactivos reutilizables, sin sustituir recursos vigentes. Se limita el almacén de audio a 128 MiB y se limpia staging temporal al importar. Los hashes comprueban integridad, no autoría.

Un `.aula-result` contiene manifiesto, estudiante seudónimo, intentos, respuestas, progreso, recompensas y hashes. No contiene nombre ni avatar. La recuperación requiere las versiones de las lecciones referenciadas. Crea `Recuperado <sufijo>` y no sustituye el perfil original. Se recalculan aciertos y recompensas con el contenido instalado; las afirmaciones del archivo no se aceptan sin evaluación. La misma exportación puede recuperarse repetidamente sin duplicar registros. Se rechaza retroceder el estado ya recuperado. La seudonimización no equivale a anonimato ni cifrado.

No se añadió servidor, cuenta ni permiso de Internet. Los archivos exportados quedan en la ubicación elegida por la persona. Compartir abre el selector del sistema y concede lectura del archivo seleccionado.

## Validación

Dispositivo: Samsung SM-G998U, Android 15. Las pruebas usan bases aisladas, nombres únicos para documentos temporales y limpieza restringida a esos datos de prueba. La suite conectada debe ejecutarse en un dispositivo de pruebas: Gradle administra instalación y limpieza y no se debe usar esa tarea para actualizar una instalación con resultados que deban conservarse. Para actualizar se utiliza installDebug o instalación del APK con conservación de datos.

- Evaluadores, contratos ZIP y cálculo de progreso: 35 pruebas unitarias.
- Suite conectada: 47 pruebas de repositorios, migraciones, UI, medios e intercambio.
- Intercambio completo: exportar/importar, añadir una lección versionada, reexportar y conservar el progreso existente.
- Recuperación: perfil separado, reimportación idempotente y rechazo transaccional de respuestas falseadas incluso con hashes recalculados.
- Sin conexión: completar las seis lecciones y obtener sus seis premios con Wi-Fi y datos móviles desactivados; se restaura el estado original de las radios al terminar.
- Texto ampliado: edición de nombre/avatar con fuente al 200 %, conservando el avance.
- Selector nativo: guardar un documento real, leer y verificar su ZIP, volver a la app y cancelar la apertura sin importar datos.
- Persistencia entre procesos: preparación y verificación mediante ADB en invocaciones independientes del runner, sin reinstalar entre ambas, con comprobación explícita de PID diferente, posición y pistas guardadas.

Resultado final: BUILD SUCCESSFUL. Compilación debug, 35 pruebas unitarias y 47 pruebas conectadas aprobadas (0 fallos, 0 omitidas). Lint: 0 errores, 13 advertencias y 1 sugerencia; no se presenta como una ejecución sin advertencias. Las dos invocaciones ADB de recuperación entre procesos aprobaron una prueba cada una.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain

# Tras instalar ambos APK con adb install -r, sin reinstalar entre estas dos llamadas:
adb shell am instrument -w -e class 'com.example.triada.ProcessCheckpointTest#a_prepareConfirmedCheckpoint' com.example.triada.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class 'com.example.triada.ProcessCheckpointTest#b_restoreConfirmedCheckpoint' -e requireFreshProcess true com.example.triada.test/androidx.test.runner.AndroidJUnitRunner
```

## Archivos principales de este cierre

- `data/packageio`: LessonPackageService, PackageJson, InstalledCatalogJson y StudentResultsImporter; ampliación del exportador de resultados.
- `data/local`, `data/repository`, `domain`: consultas y reglas para edición de perfiles, restauración y contratos de contenido.
- `core/audio` y fuente de catálogo: reproducción y validación de WAV privados instalados.
- `feature/learning`: pantalla de intercambio, recuperación, compartir y edición del perfil; integración en navegación y ViewModels.
- Tema: primer plano oscuro en botones de relleno coral y coral oscuro para controles sobre fondo claro, para mejorar contraste.
- Pruebas: CompleteInterchangeTest, DeviceAcceptanceTest, NativeDocumentTest, ProcessCheckpointTest y LargeTextProfileTest.
- No cambia el esquema Room 4 en este cierre. No se modifican `doc_tecnica.md` ni `temario.md`.

## Instalación y límites

APK de pruebas: `app/build/outputs/apk/debug/app-debug.apk`. Se instala con ` .\gradlew.bat :app:installDebug`. Para actualizar, conservar el mismo identificador y firma; no desinstalar ni borrar datos. Es una compilación debug; la firma de distribución y publicación en tienda requieren un proceso de entrega posterior.

Quedan para una fase posterior el lector de cuentos narrados, el programa de fonética inglesa, la autoría docente avanzada y la producción de actividades para los 208 temas de referencia. La recuperación local de resultados antes diferida sí se entrega en este cierre.

La revisión pedagógica con docentes y alumnado y la evaluación manual con TalkBack no se consideran realizadas. La prueba de fuente ampliada cubre el recorrido de edición de perfil; no certifica todas las pantallas. Android 24 es el mínimo declarado y revisado estáticamente, pero la ejecución aquí se realizó en Android 15. No se afirma prueba de modo avión: se comprobaron Wi-Fi y datos móviles desactivados.

SHA-256 del APK validado: `1EF30FB511968FD6D45321D5D9EF53A5F048525D3451702181EE50EB31D380BB`.
