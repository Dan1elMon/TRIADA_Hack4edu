# AulaNexo

Aplicación educativa Android offline con Kotlin, Jetpack Compose, Room, DataStore y Hilt. Identificador: `com.example.triada`.

Estilo visual de exploración: fondo arena `#F7F3E9`, acciones verdes `#2ECC71`, logros miel `#F1C40F` y acentos océano `#3498DB`. Los textos usan tonos oscuros de la misma paleta para mantener la legibilidad.

## Iniciar sesión

En una instalación nueva:

- Usuario docente: **10000001**
- Contraseña inicial: **admin**

En **Mi acceso** se pueden cambiar el usuario y la contraseña verificando la contraseña actual. El usuario siempre tiene ocho dígitos, incluidos posibles ceros iniciales. Es un identificador local, no se requiere DNI. Las contraseñas se almacenan mediante una derivación con sal; no se exportan.

El docente puede crear cuentas de alumnos y otros docentes. Cada cuenta nueva recibe un usuario único y contraseña inicial `admin`; la pantalla confirma el usuario asignado. Los perfiles existentes se conservan como cuentas de alumnos. Sus usuarios aparecen en la lista **Vincular alumnos**. Los usuarios docentes aparecen en **Cuentas docentes**. El login es local; no hay servidor ni sincronización entre dispositivos.

La demo incluye **tres docentes y treinta alumnos**, diez por aula. Los docentes usan inicialmente **10000001**, **11000002** y **11000003**; los alumnos, **20000001–20000030**. La contraseña inicial es **admin**. [Lista y comportamiento de las cuentas](AulaNexo/docs/alumnos_demo.md).

## Funciones disponibles

- Encabezado común con logo a la izquierda de AulaNexo en todas las pantallas.
- Roles de alumno/docente y edición de nombre/avatar.
- Aula del docente con alumnos vinculados y consulta de sus notas; apartado **Mis notas** para el alumno.
- 214 lecciones asociadas a los 214 temas, con **642 ejercicios**: 18 anteriores y 624 nuevos. Las 208 ampliaciones son actividades introductorias originales de selección; requieren revisión pedagógica antes de uso escolar.
- Cinco motores reutilizables: conteo, selección, secuencias, asociación y formación de palabras.
- Tutor **Juan**, con búho vectorial, cuatro expresiones y reglas offline según errores, tipo de ejercicio y pistas disponibles. Se puede ocultar.
- Mapa visual de seis áreas y objetivos diarios ajustables de una a tres lecciones distintas.
- Tres cuentos originales en español y tres lecturas iniciales de fonética inglesa, con 18 páginas, narración, palabras interactivas, velocidad lenta, comprensión y progreso por alumno.
- 93 WAV incluidos: siete anteriores y 86 del lector. No se necesita descargar voces.
- Álbum con los seis stickers de los hitos originales; no se han añadido 208 premios nuevos.
- Editor docente con borradores, formulario de selección, estructura avanzada para cinco tipos, validación, vista previa y publicación versionada.
- Intercambio `.aula` de catálogo, actividades y audios de lecciones; resultados `.aula-result` con recuperación en perfil separado. Los cuentos, sus progresos, las credenciales, vínculos y borradores no se incluyen en estos formatos actuales.
- Persistencia y migraciones Room hasta esquema 6, conservando intentos anteriores.

Las notas de práctica van de 0 a 20 por intento completado: aciertos en la primera respuesta / preguntas × 20. Las pistas no descuentan puntos. Las lecturas tienen su propia comprobación de comprensión; no se mezclan con las notas de las lecciones.

## Ejecutar y verificar

Abrir **la raíz Triada** en Android Studio, sincronizar Gradle, seleccionar `app` y un dispositivo, y pulsar Run. Se requieren JDK 21 y compileSdk 37 según la configuración local.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

Usar un dispositivo dedicado a pruebas para `connectedDebugAndroidTest`: Gradle administra instalación y limpieza. Para actualizar una instalación con datos, usar Run/installDebug o instalar el APK conservando datos y firma, sin desinstalar.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Estado y documentación

- [Checklist actual](AulaNexo/docs/checklist_desarrollo.md)
- [Login, tutor, contenido y módulos ampliados](AulaNexo/docs/ampliacion_integral.md)
- [Registro de revisión pedagógica de 214 temas](AulaNexo/docs/revision_pedagogica_214.csv)
- [Documento técnico original](AulaNexo/docs/doc_tecnica.md)
- [Temario original](AulaNexo/docs/temario.md)
- [Plan progresivo e historial](AulaNexo/docs/analisis_y_plan_desarrollo.md)
- [Entrega anterior del MVP](AulaNexo/docs/entrega_mvp.md)
- [Entrega anterior de cuentas y notas](AulaNexo/docs/cuentas_y_notas.md)

La validación humana con TalkBack y docentes/alumnado, la adecuación curricular y la firma de distribución siguen pendientes. La fonética es introductoria, con familias CVC y rimas; no constituye un curso completo de pronunciación ni reconocimiento de voz.
