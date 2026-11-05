# AulaNexo

Aplicación Android educativa con Kotlin, Jetpack Compose, Room y DataStore, desarrollada por incrementos sobre el proyecto Triada. Identificador técnico: `com.example.triada`.

## Funcionalidades disponibles

- Cuentas locales simuladas de alumnos y docentes, sin contraseña ni servidor.
- Espacio del alumno con actividades y **Mis notas**; aula del docente con alumnos vinculados y consulta de sus notas.
- Notas de práctica de 0 a 20 por intento completado, basadas en aciertos a la primera; pistas registradas sin descuento. Los intentos incompletos no tienen nota final.
- Perfiles locales independientes, con creación y edición de apodo y avatar.
- Catálogo local versionado de **214 temas**, organizado en primer grado y seis áreas.
- Navegación perfil → grado → área → unidad → lección.
- Consulta de todos los títulos de referencia; los temas sin actividades aparecen **en preparación**.
- **Seis lecciones funcionales**, una por área, con 18 ejercicios originales, feedback y pistas progresivas.
- Actividades de conteo, selección, secuencias, asociación por tarjetas y formación de palabras con letras.
- Siete audios de palabras en español incluidos en la app e ilustraciones vectoriales originales, sin descargas.
- Guardado de errores, pistas, posición y progreso; reanudación y repetición conservando intentos anteriores.
- Migraciones Room desde las versiones anteriores al esquema 5 sin borrar resultados. El último perfil se recuerda mediante DataStore; al terminar el proceso se requiere seleccionar de nuevo la cuenta simulada.

Desde la pantalla del grado se puede abrir **Mi álbum de stickers** y **Mi progreso**. Cada lección entrega un sticker único por perfil; repetirla conserva el premio y el mejor avance. Se recuperan también los premios de las lecciones completadas antes de actualizar. El resumen considera las seis lecciones disponibles y sus 18 ejercicios, sin incluir los 208 temas de referencia.

Desde el grado, **Intercambiar catálogo** permite importar paquetes `.aula`, exportar los metadatos originales o exportar todo el contenido instalado con ejercicios y audios WAV. Se validan rutas, tamaños, hashes, contratos y versiones antes de activar los cambios. El contenido publicado conserva su versión; una modificación requiere otra versión.

**Exportar mis resultados** guarda un `.aula-result` con intentos, respuestas, pistas y progreso, sin nombre ni avatar y con identificador seudónimo estable. **Recuperar resultados** crea un perfil separado y requiere las lecciones y versiones correspondientes. Reimportar el mismo archivo no duplica intentos ni premios. Tras guardar se puede abrir el selector nativo para compartir el archivo. No se solicita cuenta ni permiso de Internet.

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

Las pruebas conectadas requieren un dispositivo o emulador Android autorizado dedicado a pruebas. Usan bases aisladas y perfiles sintéticos, pero Gradle administra la instalación y limpieza. Para actualizar una instalación con resultados que deban conservarse, usar installDebug o instalar el APK conservando datos.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

```powershell
.\gradlew.bat :app:installDebug
```

No desinstalar ni borrar los datos para actualizar desde la primera entrega: la migración conserva el progreso existente.

## Recorrido de validación

La pantalla inicial permite crear o seleccionar cuentas simuladas. Los perfiles anteriores se conservan como alumnos. Para consultar notas como docente: seleccionar **Docente de ejemplo → Vincular alumnos → Agregar a mi aula → Ver notas de…**. Para consultar las propias: seleccionar alumno → **Mis notas**. No se incluyen notas ficticias: primero hay que realizar actividades. El recorrido educativo siguiente se abre desde **Comenzar mi aventura**.

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

La aceptación automatizada incluye las seis lecciones con Wi-Fi y datos móviles desactivados, recuperación de progreso entre procesos, edición de perfil con texto al 200 % y guardado mediante el selector real de Android. Consultar el informe de entrega para resultados y límites. La revisión pedagógica y la evaluación manual con TalkBack siguen requiriendo participación humana.

## Documentación

- [Documento técnico](AulaNexo/docs/doc_tecnica.md)
- [Temario](AulaNexo/docs/temario.md)
- [Plan progresivo](AulaNexo/docs/analisis_y_plan_desarrollo.md)
- [Primera entrega](AulaNexo/docs/avance_incrementos_1_2.md)
- [Catálogo y navegación](AulaNexo/docs/avance_incremento_3.md)
- [Seis lecciones y motores de actividades](AulaNexo/docs/avance_incremento_4.md)
- [Recompensas, álbum y progreso](AulaNexo/docs/avance_incremento_5.md)
- [Selector y validación de catálogos](AulaNexo/docs/avance_incremento_6_selector.md)
- [Exportación de resultados por estudiante](AulaNexo/docs/avance_incremento_6_resultados.md)

- [Entrega y aceptación del MVP](AulaNexo/docs/entrega_mvp.md)
- [Cuentas simuladas y criterio de notas](AulaNexo/docs/cuentas_y_notas.md)
- [Checklist del desarrollo](AulaNexo/docs/checklist_desarrollo.md)
- [Propuesta de tutor con avatar](AulaNexo/docs/tutor_con_avatar.md)
