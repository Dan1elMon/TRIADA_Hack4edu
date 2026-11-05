# Ampliación integral de AulaNexo

Fecha: 5 de noviembre de 2026. Se añaden login, tutor, contenido para los temas restantes, lector, mapa, objetivos y editor. No se modifican los documentos fuente `doc_tecnica.md` ni `temario.md`.

## Login y cuentas

En una instalación nueva, el docente inicial utiliza **10000001 / admin**. Cada nueva cuenta recibe un usuario local único de ocho dígitos y contraseña inicial `admin`. Los ceros iniciales son válidos. No se necesita DNI, email, red ni servicio remoto.

El cambio de acceso exige la contraseña actual, ocho dígitos únicos para el usuario y una nueva contraseña de 5 a 128 caracteres, confirmada en UI. No se fuerza el cambio al primer ingreso. La contraseña no se guarda en preferencias ni en estado guardado de Compose, ni se incluye en paquetes exportados. Se almacena una derivación PBKDF2-HMAC-SHA1 de 256 bits, 120000 iteraciones y sal aleatoria de 24 bytes por credencial, usando un algoritmo disponible en el mínimo Android declarado. La comparación utiliza MessageDigest.isEqual. Cinco fallos introducen una espera de 30 segundos en la sesión; no es un servicio de autenticación de servidor y esa limitación no persiste al terminar el proceso.

Los perfiles anteriores se adoptan sin cambiar sus IDs. El docente inicial permite consultar los usuarios de alumnos en Vincular alumnos y los de otros docentes en Cuentas docentes. El docente puede crear cuentas; no existe selección pública de un rol para omitir el login. El estudiante accede a su perfil; las notas se consultan con validación de rol y vínculo. Las rutas restauradas de alumno no componen ni cargan contenido antes de tener una sesión correspondiente. Al terminar el proceso se exige nuevo login; una recreación de Activity conserva el ViewModel de la sesión.

## Catálogo y tutor

El catálogo distribuido pasa a versión 3 y conserva los 214 IDs de tema y las seis áreas. Las seis lecciones anteriores permanecen con sus 18 ejercicios. Se añaden **208 lecciones introductorias de tres preguntas específicas**, total **624 preguntas nuevas y 642 ejercicios**. Las actividades nuevas son de selección entre dos opciones; los cinco motores previos siguen funcionando. Los seis stickers corresponden a los hitos originales, no se inventan 208 premios nuevos.

Las fuentes originales de las preguntas están en `tools/curriculum_math.tsv`, `curriculum_language.tsv` y `curriculum_world.tsv`. `generate_expanded_content.py` genera `catalog-v3.json` y `expanded-lessons-v1.json`. No son fichas copiadas ni una certificación curricular. La primera pista evita dar directamente la respuesta y la segunda orienta a contrastar opciones. Esta base no sustituye ejemplos guiados ni prácticas físicas cuando el título las requiere.

El tutor es un búho vectorial de Compose. Sus reglas locales consideran el tipo de ejercicio, los errores de la pregunta, las pistas utilizadas y si hubo acierto/finalización. Ofrece la siguiente pista disponible, anima a reintentar, cambia de estrategia por motor y celebra aciertos. Puede ocultarse. Las expresiones también se acompañan de texto; no se usa IA remota ni se simula generación de respuestas. La adaptación longitudinal por habilidades y los ejemplos adicionales siguen siendo ampliaciones.

## Cuentos y fonética

Se incluyen tres cuentos originales: La semilla de Luna, El puente de papel y Un libro para dos. Se añaden tres lecturas iniciales en inglés para familias con vocales cortas y rimas CVC (-at, -en, -ig). Son **seis lecturas de tres páginas**, no un curso completo de fonética inglesa ni reconocimiento de pronunciación.

Cada página ofrece escena vectorial, texto, narración repetible, reproducción de palabras y velocidad normal o reducida. Tras terminar, aparece una pregunta de comprensión o rima. Se guarda página, finalización y acierto de comprensión por perfil y libro. Se conserva el mejor estado de comprensión sin impedir reintentos. La lectura y su comprensión no se mezclan con las notas de los intentos de lección.

Se generaron **86 WAV locales** con Microsoft Sabina y David; sumados a los siete anteriores hay 93. El APK contiene los WAV: no depende de las voces instaladas en el teléfono. Las fuentes de generación están en `generate_readers.py`, `reader_audio.json` y `generate_reader_audio.ps1`. La voz sintética requiere revisión auditiva/pedagógica, especialmente para fonética.

## Mapa, objetivos y editor

El mapa dibuja un recorrido de las seis áreas con acceso, disponibilidad y lecciones completadas. Todas siguen abiertas; no se inventan prerrequisitos a partir del orden del temario.

El objetivo diario se puede ajustar de una a tres lecciones. Se cuentan lecciones distintas completadas en el día local, según completedAt. Repetir la misma lección no duplica el objetivo. El día se calcula al cargar o actualizar el perfil; no se ha añadido un servicio de calendario o notificaciones.

El editor docente permite nuevos borradores persistentes por propietario, selección de tema, edición visual de título/objetivo/preguntas/opciones/pistas de selección, edición avanzada JSON para los cinco motores y vista previa del contenido. Se valida antes de habilitar publicación. Los textos de plantilla sin completar se rechazan. No es un editor de dibujos, grabación de voz o cuentos.

Publicar genera una versión nueva del contenido docente y del catálogo, reutilizando el contrato de paquetes y su validación transaccional. Las versiones anteriores permanecen para conservar intentos. Cada docente solo puede guardar sus propios borradores; un alumno no puede utilizar el repositorio del editor. Dos publicaciones del mismo borrador producen versiones 1 y 2. La importación admite hasta 512 versiones de lecciones por paquete; permanecen los límites ZIP de 512 entradas, 8 MiB por archivo y 32 MiB descomprimidos. Los catálogos equivalentes no se rechazan por espacios, orden de propiedades o serialización distinta; un cambio de metadatos del catálogo sigue exigiendo otra versión.

## Persistencia e intercambio

Room pasa de esquema 5 a **6**, con migración que agrega credenciales, progreso lector, objetivos y borradores; no borra tablas ni resultados anteriores. Los esquemas previos permanecen disponibles para pruebas de migración.

Los formatos actuales `.aula` y `.aula-result` conservan el alcance de lecciones y resultados de lecciones. No transfieren cuentos, progreso lector, credenciales, vínculos del aula, objetivos ni borradores. No se presentan como un respaldo íntegro de toda la aplicación. Las credenciales tampoco deben viajar con los resultados infantiles.

## Verificación ejecutada

- Compilación debug: BUILD SUCCESSFUL.
- **39 pruebas unitarias**, sin fallos.
- **57 pruebas conectadas en Samsung SM-G998U / Android 15**, sin fallos ni omisiones.
- Lint: **0 errores y 5 advertencias de versiones de dependencias**. Se mantienen las versiones ya validadas, sin actualización en bloque.
- Login inicial, rechazo de credenciales incorrectas, usuario con ceros iniciales, cambio de acceso, colisiones de usuario y sal renovada.
- Migración real 5 → 6 conservando un perfil anterior, además de las migraciones y pruebas previas.
- 214 lecciones y 642 ejercicios; evaluación de la clave correcta y una opción incorrecta de cada una de las 624 preguntas nuevas.
- Intercambio del catálogo completo con progreso y carga del paquete en una base nueva, seguida de apertura con el catálogo distribuido.
- Decodificación de todos los recursos de audio del lector; progreso/comprensión separados entre alumnos.
- Publicación docente de dos versiones, conservación del intento anterior y rechazo de modificación de un borrador ajeno.
- Login, cambio de acceso y lectura con fuente al 200 %, además de la prueba previa de edición de perfil.
- Regresión de los cinco motores, notas, perfiles, recompensas, recuperación, selector nativo, audio y persistencia. Parte de esas pruebas conserva explícitamente el catálogo v2 como fixture de contenido previamente distribuido; ExtendedFeaturesTest y arranque de producción verifican el catálogo v3 completo.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
python tools/audit_curriculum.py
```

## Accesibilidad y revisión pedagógica

Se completó la revisión técnica de etiquetas, tamaño mínimo de controles, alternativas textuales, escenas/avatar decorativos, ausencia de animación persistente y los recorridos automatizados de fuente ampliada indicados. Esto no es una certificación de accesibilidad ni una evaluación manual completa con TalkBack.

La auditoría de contenido verifica IDs, tres preguntas por ampliación, ausencia de opciones idénticas dentro de una pregunta, una clave válida, pistas y 25 operaciones aritméticas explícitas. Se preparó `revision_pedagogica_214.csv` con filas por tema, estado Pendiente y espacio para revisor/fecha/observaciones. Los temas de centenas, multiplicación/división y otros contenidos avanzados se señalan para revisar edad y secuencia; los proyectos requieren complemento práctico supervisado. Las lecciones de identidad se orientan al respeto y la igualdad y requieren que el docente confirme su enfoque.

**Pendiente humano:** revisión y aprobación de los 214 temas, pronunciación de los audios, evaluación de comprensión con alumnado y recorrido manual con TalkBack. No se afirma que una persona docente haya revisado el material ni que exista alineamiento curricular MINEDU certificado.

Android 24 sigue siendo el mínimo declarado, pero la ejecución registrada utiliza Android 15. Quedan pendientes ese dispositivo mínimo, pruebas en equipos de pocos recursos y una release firmada con la clave del responsable.

## Entrega

APK de pruebas: `app/build/outputs/apk/debug/app-debug.apk`. Actualizar desde Run/installDebug sin desinstalar ni borrar datos. La firma es debug; no se ha publicado en tienda.

SHA-256 del APK validado e instalado: `D518D6E06FB8E1CA8477338576FC7BD697420DDD1176518C9D9A9FE4BED4D555`.
