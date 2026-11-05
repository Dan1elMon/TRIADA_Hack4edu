# AulaNexo

Aplicación Android educativa con Kotlin, Jetpack Compose, Room y DataStore, desarrollada por incrementos sobre el proyecto Triada. Identificador técnico: `com.example.triada`.

## Funcionalidades disponibles

- Perfiles locales independientes, con apodo y avatar.
- Catálogo local versionado de **214 temas**, organizado en primer grado y seis áreas.
- Navegación perfil → grado → área → unidad → lección.
- Consulta de todos los títulos de referencia; los temas sin actividades aparecen **en preparación**.
- **Seis lecciones funcionales**, una por área, con 18 ejercicios originales, feedback y pistas progresivas.
- Actividades de conteo, selección, secuencias, asociación por tarjetas y formación de palabras con letras.
- Siete audios de palabras en español incluidos en la app e ilustraciones vectoriales originales, sin descargas.
- Guardado de errores, pistas, posición y progreso; reanudación y repetición conservando intentos anteriores.
- Migraciones Room desde las versiones 1 y 2 al esquema 3 sin borrar resultados. El último perfil se recuerda mediante DataStore.

Las recompensas y el intercambio de paquetes corresponden a siguientes incrementos. No se solicita cuenta ni permiso de Internet. El catálogo mantiene 208 temas adicionales como referencia, sin actividades implementadas.

| Área | Lección inicial | Actividades |
|---|---|---|
| Matemática | Cuento del 1 al 9 | Conteo |
| Comunicación | Descubro las vocales | Selección y formación de palabras |
| Ciencia y Ambiente | Conozco mi cuerpo | Asociación de palabras e ilustraciones |
| Personal Social | Convivimos mejor | Selección de acciones de respeto y cuidado |
| Razonamiento Matemático | Descubro patrones | Secuencias de formas y colores con etiquetas |
| Razonamiento Verbal | Escucho sonidos | Comparación del sonido inicial con audio local |

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
3. En **Comunicación**, resolver dos preguntas sobre vocales y formar SOL con las letras. Escuchar la referencia y comprobar que una letra no puede usarse dos veces.
4. Volver a las áreas y abrir **Matemática → Cuento del 1 al 9 → Cuento del 1 al 9**.
5. Comenzar la aventura, responder incorrectamente y solicitar dos pistas; el error y la ayuda no aumentan el progreso.
6. Responder correctamente, pasar al segundo ejercicio, salir y reabrir con el mismo perfil.
7. Cambiar de perfil: sus resultados deben permanecer separados.
8. Completar y volver a practicar: se conserva el mejor avance y el nuevo intento empieza desde cero.
9. En **Ciencia y Ambiente**, tocar una palabra y una imagen para unirlas. Comprobar que se pueden corregir las uniones y que no se puede enviar una asociación incompleta.
10. Explorar los patrones y las normas de convivencia. En **Razonamiento Verbal**, escuchar la referencia y las opciones antes de elegir la palabra que empieza con el mismo sonido.
11. Consultar el temario de Comunicación hasta el último título: los temas adicionales siguen en preparación y no abren actividades vacías.

Quedan pendientes la validación manual en modo avión, muerte completa del proceso, TalkBack, texto ampliado y revisión pedagógica. Reapertura de Room y recreación de Activity se prueban automáticamente, pero no sustituyen esas comprobaciones.

## Documentación

- [Documento técnico](AulaNexo/docs/doc_tecnica.md)
- [Temario](AulaNexo/docs/temario.md)
- [Plan progresivo](AulaNexo/docs/analisis_y_plan_desarrollo.md)
- [Primera entrega](AulaNexo/docs/avance_incrementos_1_2.md)
- [Catálogo y navegación](AulaNexo/docs/avance_incremento_3.md)
- [Seis lecciones y motores de actividades](AulaNexo/docs/avance_incremento_4.md)
