# AulaNexo

Aplicación Android educativa con Kotlin, Jetpack Compose, Room y DataStore, desarrollada por incrementos sobre el proyecto Triada. Identificador técnico: `com.example.triada`.

## Funcionalidades disponibles

- Perfiles locales independientes, con apodo y avatar.
- Catálogo local versionado de **214 temas**, organizado en primer grado y seis áreas.
- Navegación perfil → grado → área → unidad → lección.
- Consulta de todos los títulos de referencia; los temas sin actividades aparecen **en preparación**.
- Una lección funcional de Matemática: **Cuento del 1 al 9**, con tres ejercicios originales, feedback y pistas progresivas.
- Guardado de errores, pistas, posición y progreso; reanudación y repetición conservando intentos anteriores.
- Migración Room de versión 1 a 2 sin borrar resultados. El último perfil se recuerda mediante DataStore.

Las otras cinco lecciones, los demás motores, las recompensas y el intercambio de paquetes corresponden a siguientes incrementos. No se solicita cuenta ni permiso de Internet.

## Compilar y probar

Abrir la raíz en Android Studio. Se necesitan un JDK 21 completo y el SDK de `compileSdk 37`. Ajustar las rutas locales de SDK (`local.properties`) y JDK (`gradle.properties`) si se traslada el proyecto a otro equipo.

Desde PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas conectadas requieren dispositivo o emulador Android autorizado. Usan bases aisladas y perfiles sintéticos; la prueba de arranque abre la aplicación real sin modificar perfiles.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

```powershell
.\gradlew.bat :app:installDebug
```

No desinstalar ni borrar los datos para actualizar desde la primera entrega: la migración conserva el progreso existente.

## Recorrido de validación

1. Crear un perfil y seleccionar su tarjeta.
2. Abrir **Primer grado** y explorar las seis áreas.
3. En **Comunicación**, abrir su unidad: se muestran actividades en preparación, sin botón para jugar. Consultar sus 58 temas de referencia.
4. Volver a las áreas y abrir **Matemática → Cuento del 1 al 9 → Cuento del 1 al 9**.
5. Comenzar la aventura, responder incorrectamente y solicitar dos pistas; el error y la ayuda no aumentan el progreso.
6. Responder correctamente, pasar al segundo ejercicio, salir y reabrir con el mismo perfil.
7. Cambiar de perfil: sus resultados deben permanecer separados.
8. Completar y volver a practicar: se conserva el mejor avance y el nuevo intento empieza desde cero.

Quedan pendientes la validación manual en modo avión, muerte completa del proceso, TalkBack, texto ampliado y revisión pedagógica. Reapertura de Room y recreación de Activity se prueban automáticamente, pero no sustituyen esas comprobaciones.

## Documentación

- [Documento técnico](AulaNexo/docs/doc_tecnica.md)
- [Temario](AulaNexo/docs/temario.md)
- [Plan progresivo](AulaNexo/docs/analisis_y_plan_desarrollo.md)
- [Primera entrega](AulaNexo/docs/avance_incrementos_1_2.md)
- [Catálogo y navegación](AulaNexo/docs/avance_incremento_3.md)
