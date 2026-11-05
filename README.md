# AulaNexo

Aplicación Android educativa con Kotlin, Jetpack Compose y almacenamiento local. Se desarrolla por incrementos sobre el proyecto original Triada; el identificador técnico continúa siendo `com.example.triada`.

## Primera entrega funcional

- Crear perfiles locales con apodo y avatar; seleccionar y cambiar de estudiante.
- Abrir la primera aventura de Matemática: **Cuento del 1 al 9**.
- Resolver tres ejercicios originales de conteo, con retroalimentación y dos pistas por ejercicio.
- Conservar respuestas incorrectas, pistas, posición y avance confirmado en Room.
- Reanudar el intento y volver a practicar conservando las finalizaciones anteriores.
- Recordar el último perfil mediante DataStore. No solicita cuenta ni permiso de Internet.

El catálogo de 214 títulos, las otras cinco lecciones, los demás motores, las recompensas y los paquetes de intercambio corresponden a siguientes incrementos. No se muestran como funcionalidades disponibles.

## Entorno y ejecución

Abrir la raíz del repositorio en Android Studio. Se necesitan un JDK 21 completo y el SDK correspondiente a `compileSdk 37`. `local.properties` indica el SDK local; las rutas existentes de JDK en `gradle.properties` deben ajustarse si se usa otro equipo. El criterio del daemon está fijado a Java 21 para evitar seleccionar un JRE sin `jlink`.

Desde PowerShell en la raíz:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

El último comando requiere un dispositivo o emulador Android autorizado. Las pruebas de repositorio utilizan bases aisladas; las pruebas de navegación usan perfiles sintéticos en memoria. La prueba de arranque abre la aplicación real sin crear ni modificar perfiles.

APK generado: `app/build/outputs/apk/debug/app-debug.apk`. Para instalarlo en el dispositivo conectado:

```powershell
.\gradlew.bat :app:installDebug
```

## Recorrido de prueba

1. Abrir **AulaNexo**, escribir un apodo, elegir un avatar y crear el perfil.
2. Seleccionar su tarjeta y pulsar **Comenzar aventura**.
3. Elegir una respuesta incorrecta y solicitar las dos pistas; comprobar que el avance no aumenta.
4. Contestar correctamente, pasar al segundo ejercicio y volver al mapa.
5. Cerrar y reabrir la aplicación, elegir el mismo perfil y continuar.
6. Crear otro perfil y comprobar que empieza sin el avance del primero.
7. Completar los tres ejercicios y volver a practicar: el mapa conserva la finalización y el nuevo intento empieza desde cero.

Repetir en modo avión y verificar accesibilidad con texto grande y TalkBack antes de dar por terminada la validación manual. Las pruebas automatizadas de recreación de actividad y reapertura de Room no sustituyen una prueba de muerte completa del proceso ni una prueba pedagógica.

## Documentación

- [Especificación técnica](AulaNexo/docs/doc_tecnica.md)
- [Temario y alcance del MVP](AulaNexo/docs/temario.md)
- [Análisis y plan progresivo](AulaNexo/docs/analisis_y_plan_desarrollo.md)
- [Registro de la primera entrega](AulaNexo/docs/avance_incrementos_1_2.md)
