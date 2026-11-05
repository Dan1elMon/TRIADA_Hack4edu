# AulaNexo: análisis y plan de desarrollo progresivo

Fecha: 4 de noviembre de 2026. Estado: auditoría documental y del repositorio completada; implementación pendiente.

Actualización posterior: el desarrollo de los incrementos 1–2 y sus resultados se registran en [avance_incrementos_1_2.md](avance_incrementos_1_2.md). El diagnóstico de este documento corresponde al estado inicial auditado.

El catálogo y la navegación del incremento 3 se registran en [avance_incremento_3.md](avance_incremento_3.md), con migración 1 → 2 y validación automatizada completadas el 5 de noviembre de 2026.

Las seis lecciones y sus motores se registran en [avance_incremento_4.md](avance_incremento_4.md), con esquema Room 3, catálogo versión 2 y validación automatizada completada el 5 de noviembre de 2026.

El álbum, los premios únicos y el progreso consolidado se registran en [avance_incremento_5.md](avance_incremento_5.md), con esquema Room 4 y validación automatizada el 5 de noviembre de 2026. La validación manual offline y pedagógica sigue pendiente.

## 1. Conclusión

El proyecto permite iniciar AulaNexo sobre la plantilla Android existente, pero todavía no contiene las funcionalidades que ambos documentos describen. Conviene construir primero un recorrido pequeño y completo: crear perfil → abrir una lección de conteo → responder y pedir pistas → guardar → cerrar y reanudar. Solo después de verificar ese recorrido se amplían el catálogo, los motores y las pantallas.

Las pruebas de persistencia, aislamiento de perfiles y actualización de contenidos deben acompañar los primeros incrementos. Dejarlas para una fase final obligaría a revisar pantallas y modelos ya extendidos a seis áreas.

Este análisis no modifica código de la aplicación ni establece que la aplicación actual compile. Las decisiones siguientes son propuestas de implementación; las divergencias de alcance quedan explícitas.

## 2. Fuentes y estado real

Fuentes revisadas íntegramente: `doc_tecnica.md` y `temario.md`, ubicados junto a este archivo. También se revisaron configuración Gradle, manifiesto, actividad principal, tema, recursos de respaldo y pruebas existentes.

| Elemento | Evidencia del repositorio | Consecuencia |
|---|---|---|
| Proyecto | Un módulo `app`; nombre Gradle `Triada` | Reutilizarlo; no crear otro proyecto |
| Identidad | `app_name=Triada`, namespace y applicationId `com.example.triada` | Cambiar nombre visible a AulaNexo en el primer incremento; conservar inicialmente identificadores técnicos para no mezclar cambios |
| Interfaz | `MainActivity` muestra `Hello Android!` | No hay mapa, lector, panel o álbum que migrar |
| Tema | Paleta de plantilla y color dinámico activado | Centralizar los tokens de AulaNexo en el tema actual y evitar que el color dinámico sustituya la paleta |
| Persistencia | Sin Room, DAOs, repositorios ni DataStore | Diseñar esquema inicial; todavía no existe una base propia que requiera migración |
| Navegación y negocio | Sin NavHost ni evaluadores | Crear una sola navegación y contratos compartidos |
| Pruebas | Suma `2+2` y comprobación del paquete Android | No prueban requisitos de AulaNexo |
| Contenido | Solo documentación; sin catálogo ejecutable ni medios educativos | Crear datos locales originales y validarlos |
| Diseño | No se encontró `diseno_ui.md` | Usar tokens y reglas escritos; no afirmar fidelidad a una referencia ausente |
| Respaldo | `allowBackup=true`; reglas de ejemplo | Resolver política de respaldo antes de almacenar perfiles infantiles; no dar por garantizado que todo permanezca solo en el dispositivo |

Configuración declarada: AGP 9.4.1, Gradle 9.6.0, plugin Compose de Kotlin 2.2.10, BOM Compose 2026.02.01, minSdk 24 y compile/target SDK 37. El criterio de JVM del daemon solicita Java 25 y el código declara compatibilidad Java 11. El Java localizado en PATH es 21; también se detectó el JBR de Android Studio y SDKs instalados, incluido el directorio `android-37.0`.

Estos datos son una lectura local, no una certificación de compatibilidad. No cambiar versiones en bloque: el primer incremento debe comprobar resolución, JVM efectiva y compilación, consultar documentación oficial si surge una incompatibilidad y ajustar únicamente lo necesario.

## 3. Conflictos y decisiones propuestas

| Punto | Diferencia o vacío | Tratamiento para avanzar |
|---|---|---|
| Idioma y finalidad | Documento técnico: fonética inglesa. Temario: primer grado y seis áreas en español | Tomar `temario.md` como especificación del primer MVP; conservar arquitectura offline del documento técnico. El programa de inglés queda como ampliación independiente, sin mezclar sonidos o evaluaciones |
| Jerarquía | `Course → World → Level` frente a `Grade → Subject → LearningUnit → Topic` | Usar la jerarquía del temario como modelo inicial; no crear ambas estructuras para representar los mismos contenidos |
| Estados | `AVAILABLE/CATALOG_ONLY` se mezclan con progreso personal | Separar disponibilidad del contenido de `NOT_STARTED/IN_PROGRESS/COMPLETED` por perfil |
| Desbloqueos | Mapa técnico con niveles bloqueados; temario con seis unidades abiertas | Las seis unidades iniciales estarán disponibles desde el inicio; un error no bloquea otra área |
| Alcance de intercambio | Documento técnico exige `.aula` y `.aula-result`; temario permite un MVP empaquetado y pospone importación si no existe | Distinguir hito de aprendizaje de hito de intercambio. El primero no se presenta como cumplimiento total del documento técnico |
| Autoría docente | Documento técnico describe preparación de cursos, exportación y consulta de resultados; el temario excluye edición avanzada | No incorporar un editor en el hito inicial. Mantener autoría/importación de resultados como trabajo posterior explícito |
| Tipos de ejercicio | Tipos pedagógicos como `PHONICS` y tipos de interacción como `SINGLE_CHOICE` | Separar objetivo pedagógico de mecanismo de respuesta; un único conjunto de contratos de evaluación |
| Formación de palabras | Se pide `WORD_BUILDING`, pero ninguna de las seis lecciones de ejemplo lo usa | Añadir un ejercicio original sencillo dentro de la lección de Comunicación, sujeto a revisión pedagógica, para validar los cinco motores sin agregar una séptima lección |
| Sonidos e imágenes | No existen recursos originales disponibles | Usar recursos locales propios y alternativas explícitas. Un ejercicio auditivo necesita audio local para comprobar su objetivo; mostrar texto no valida discriminación auditiva |
| Finalización | No se define con precisión cómo se completa o puntúa una lección | Propuesta: completar al responder correctamente todas sus preguntas, con reintentos ilimitados; conservar cada respuesta incorrecta. Mostrar finalización separada del resultado del primer intento |
| Versiones | Se exige conservar resultados, pero los ejemplos de tablas no siempre registran versión | Versionar lecciones y asociar intentos a la versión evaluada desde el esquema inicial |

No se validó alineamiento con MINEDU ni se consultaron fichas externas. Los títulos son referencias temáticas. La secuencia, los ejercicios, las pistas y los recursos requieren revisión pedagógica antes de un despliegue escolar.

## 4. Contratos que deben fijarse antes de extender la UI

1. **Contenido e identidad.** IDs estables independientes del título. `schemaVersion`, versión de catálogo y `contentVersion` cumplen funciones diferentes. Conservar código de área y orden original como metadatos; no usarlos como prerrequisitos. Mantener un archivo de correspondencia de IDs para que una reordenación no cambie identidades.
2. **Modelo de datos.** Grado, área, unidad, tema, lección, actividad, pregunta, opciones, pistas, perfil, intento, respuesta, progreso y recompensa. `Topic.unitId` puede ser nulo para referencias no organizadas en unidades. Claves foráneas e índices en relaciones; unicidad del progreso por perfil/lección/versión y de la recompensa única por perfil/objetivo.
3. **Evaluación.** Dominio Kotlin independiente de Android. Entradas tipadas para selección, conteo, secuencia, pares y letras. El modelo enviado a UI no incluye respuestas correctas. Validar conjuntos completos de pares, sin omisiones ni duplicados; normalización textual explícita por ejercicio.
4. **Intentos y respuestas.** Un intento contiene varias entregas de respuesta, incluso reintentos a una misma pregunta. No sobrescribir errores anteriores. Cada entrega debe tener identidad estable para que una repetición técnica del guardado no duplique el evento.
5. **Confirmación y reanudación.** Guardar respuesta, avance y recompensa aplicable en una transacción. Mostrar avance confirmado después de persistir; ante un fallo, informar y permitir reintentar. Mantener pregunta actual, preguntas resueltas y pistas utilizadas; no depender solo de un porcentaje o de un ViewModel.
6. **Perfil.** Pasar `profileId` explícitamente al iniciar y guardar una sesión. Room guarda resultados; DataStore conserva únicamente preferencias como el perfil seleccionado. Al cambiar de perfil, cerrar la sesión visual anterior y evitar que un guardado tardío se atribuya al nuevo perfil.
7. **Pistas.** Orden determinista, avance por pregunta e intento, agotamiento explícito y sin marcar un error como acierto. Persistir las pistas mostradas para no reiniciarlas al reabrir; una nueva ejecución de la lección crea otro intento.
8. **Actualización.** Aplicar semilla en una transacción, con IDs estables y comprobación de versión. Evitar borrar/reinsertar filas con dependencias. Cambios de texto pueden conservar identidad; cambios de preguntas o criterio de finalización generan nueva versión. Preservar histórico; no convertir progreso antiguo automáticamente en dominio del contenido nuevo.
9. **Intercambio futuro.** Definir después un adaptador entre DTOs `.aula` y el modelo interno. Los ejemplos actuales carecen de contrato completo para la jerarquía de seis áreas; documentar esa correspondencia y la compatibilidad antes de implementar importación.

Un solo módulo Android es suficiente para comenzar. Usar paquetes `domain`, `data`, `feature` y el tema compartido dentro del namespace existente. Introducir Room, DataStore, Navigation e inyección Hilt con el incremento que las necesita, compilando después de cada integración; incorporar audio e imágenes cuando haya recursos que consumir.

## 5. Incrementos y condiciones para avanzar

La siguiente secuencia sustituye el enfoque de construir todas las capas y probar al final. Cada incremento termina con una demostración ejecutable y pruebas de su riesgo principal.

| Incremento | Implementación y entrega | Prueba temprana / condición de salida |
|---|---|---|
| 0. Auditoría | Este documento, conflictos y archivos previstos | Recuento de títulos y revisión del código actual completados |
| 1. Base ejecutable | Nombre visible AulaNexo, tema, estructura mínima y un NavHost | Compilar APK y ejecutar prueba unitaria existente como comprobación del entorno; abrir en dispositivo. La prueba de plantilla no cuenta como cobertura funcional |
| 2. Recorrido completo mínimo | Perfiles Room, DataStore, una lección MAT de 3 ejercicios, conteo, feedback, dos pistas por pregunta, guardado y reanudación | Evaluación y pistas unitarias; Room con perfiles A/B; reinicio del proceso a mitad de lección y reapertura sin pérdida del avance confirmado |
| 3. Catálogo y navegación | 214 títulos versionados, seis áreas/unidades y pantallas grado → área → unidad → lección; habilitar solo las lecciones realmente completas | Recuento por área, IDs únicos, referencias válidas; semilla repetida sin duplicados; actualizar metadatos conservando intentos y progreso; ruta a `CATALOG_ONLY` controlada |
| 4. Seis lecciones | Completar COM, RMA, RVE, CYA y PSO; motores de selección, secuencia, pares y palabras; recursos locales necesarios | Probar cada evaluador al incorporarlo, más UI de interacción de cada motor. Al terminar: seis lecciones de 3–5 ejercicios y los cinco tipos demostrables |
| 5. Recompensas y consolidación | Álbum, progreso por área, recompensa transaccional única, accesibilidad y estados de error | Completar dos veces sin premio duplicado, dobles pulsaciones, cambio de perfil durante operaciones; UI sin respuestas filtradas; revisión de texto grande y TalkBack |
| 6. Intercambio offline | Contrato `.aula` reconciliado, importación y exportación de contenidos/resultados, SAF y Sharesheet | Paquete válido, duplicado, actualización, versión no compatible y corrupción. Pruebas adversas del ZIP antes de habilitar importación en UI; round-trip de exportación/importación de contenido |
| 7. Entrega | APK, instrucciones, evidencias y pendientes | Recorrido en modo avión desde primer inicio, reinicio, recursos faltantes y actualización; prueba real con docente/estudiante antes de despliegue |

**Hito A: MVP de aprendizaje** = incrementos 1–5 más validación offline del incremento 7. Incluye seis lecciones completas y catálogo; no incluye aún intercambio ni toda la experiencia de cuentos del documento técnico.

**Hito B: intercambio** = incremento 6 y su validación final. No equivale por sí mismo a una herramienta de autoría docente o a restauración completa de perfiles.

**Backlog posterior explícito:** lector de cuentos paginados con narración y palabras interactivas (RF-012–021), programa de fonética inglesa, autoría docente, consulta/importación de resultados y política de restauración de avances. La exportación de resultados no demuestra que exista restauración. Mantener trazabilidad de estos pendientes para no declarar cumplido todo `doc_tecnica.md` al alcanzar el Hito A.

El progreso y las pistas se adelantan al incremento 2; el álbum visual puede esperar. La primera recompensa se integra con la misma operación de finalización ya probada, sin duplicar lógica en la pantalla del álbum.

## 6. Archivos previstos

Rutas relativas a la raíz del repositorio. Son grupos previstos, no archivos ya implementados; crear solo los necesarios para cada incremento.

| Incremento | Modificar | Crear |
|---|---|---|
| 1 | `app/src/main/res/values/strings.xml`, `app/src/main/java/com/example/triada/MainActivity.kt`, `ui/theme/*` bajo ese mismo paquete; Gradle solo si hace falta | `core/navigation/AulaNexoNavHost.kt` dentro del paquete base |
| 2 | `gradle/libs.versions.toml`, `app/build.gradle.kts`, manifiesto y reglas de respaldo según política adoptada | `domain/model`, `domain/repository`, `domain/usecase`, `data/local`, `data/repository`, `core/di`, `feature/profile`, `feature/activity`, recurso MAT mínimo y pruebas correspondientes |
| 3 | Repositorios, semilla y navegación del incremento 2 | `app/src/main/assets/content/catalog-v1.json`, `feature/learning`, validador de catálogo y pruebas |
| 4 | Evaluadores, renderizador y datos locales ya creados | `assets/content/lessons/`, recursos originales y componentes por tipo en `feature/activity` |
| 5 | Finalización, progreso y tema compartidos | `feature/rewards`, `feature/progress`, pruebas de recompensas y UI |
| 6 | Repositorios y entrada a gestión de contenido | `data/packageio`, `feature/contentmanagement`, fixtures de paquetes y pruebas |
| 7 | Documentación y correcciones justificadas por fallos | Guía de ejecución y registro de validación |

No crear un segundo tema, segunda base de datos o segunda navegación. Exportar los esquemas Room desde la primera versión y conservarlos en el repositorio. Las migraciones se agregan cuando realmente cambie un esquema ya distribuido; no inventar una migración desde una base que hoy no existe.

## 7. Pruebas proporcionales al riesgo

| Riesgo | Validación mínima útil | Momento |
|---|---|---|
| Evaluación equivocada | Casos correctos, incorrectos e incompletos por motor; opción ajena a la pregunta y normalización explícita | Antes de conectar cada motor a UI |
| Pistas que se repiten | Orden, cero pistas, agotamiento, reanudación y aislamiento por pregunta/intento | Incremento 2 |
| Mezcla de estudiantes | Mismo contenido con dos perfiles y escrituras en curso durante cambio de perfil | Incremento 2 y regresión al ampliar progreso |
| Pérdida o doble registro | Transacción, cierre tras guardar, repetición del mismo evento y recuperación tras error | Incremento 2 |
| Semilla destructiva | Ejecutar dos veces; actualizar catálogo y mantener respuestas, recompensas y referencias | Incremento 3 |
| Contenido incompleto | 45 MAT + 58 COM + 36 CYA + 29 PSO + 26 RMA + 20 RVE; total 214. Exactamente seis lecciones disponibles al cerrar incremento 4 | Incrementos 3–4 |
| Datos inválidos | Payload incoherente, orden duplicado, referencia ausente, tipo desconocido y ausencia de medios | Al validar contrato y cada nuevo motor |
| Premio duplicado | Múltiples finalizaciones y eventos concurrentes con restricción de unicidad | Incremento 5 |
| Actualización de BD | Migración real desde cada esquema distribuido pertinente, sin pérdida de resultados | Cuando cambie el esquema |
| Importación parcial | Fallo tras preparar archivos o durante escritura; recuperación al reiniciar | Incremento 6 |
| ZIP peligroso | Rutas relativas ascendentes/absolutas, duplicados normalizados, exceso de entradas/tamaño descomprimido, hash inválido y JSON excesivo | Antes de exponer importación |
| Barreras de uso | Etiquetas, objetivos táctiles, foco, escalado de texto y alternativas a color/arrastre | Desde la primera pantalla y al añadir cada motor |

La importación requiere un directorio temporal y publicación recuperable: una transacción Room no revierte por sí sola archivos movidos. Validar todo primero, publicar recursos versionados sin sobrescribir los vigentes y activar referencias en una transacción. Si hay interrupción, recuperar o limpiar el staging sin afectar contenido activo. Los hashes verifican integridad; por sí solos no autentican al autor.

No añadir pruebas que solo comprueben colores o repliquen getters. Priorizar reglas educativas, datos durables y recorridos críticos. Usar repositorios de prueba para dominio cuando baste; reservar emulador/dispositivo para Room, UI, archivos y ciclo de vida real.

Comandos previstos, desde la raíz, una vez operativo el entorno:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:lintDebug
```

El primer comando será la comprobación inicial de compilación. El segundo requiere dispositivo o emulador conectado. Ejecutar las pruebas relevantes tras cada cambio; la regresión completa corresponde a hitos o cambios transversales. No repetir compilaciones sin cambios salvo para investigar un fallo.

## 8. Resultado de esta auditoría y siguiente paso

- Documentos revisados y contrastados con el repositorio.
- Recuento por script de los seis listados: 45, 58, 36, 29, 26 y 20; suma 214.
- No se encontró `AGENTS.md` ni `diseno_ui.md` dentro del repositorio mediante búsqueda de archivos, incluidos ocultos.
- Se localizaron Java, JBR, SDK y `adb.exe`. No se comprobó un dispositivo conectado ni la JVM efectiva de Gradle.
- No se ejecutaron Gradle, tests, lint, migraciones ni instalación de APK durante este análisis. No hay resultados funcionales que declarar aprobados.
- Archivo creado: `AulaNexo/docs/analisis_y_plan_desarrollo.md`. No se modificaron los documentos fuente ni el código de la app.

Siguiente paso: incremento 1, comprobando primero compilación y entorno antes de añadir persistencia o dependencias nuevas. A continuación, validar el recorrido mínimo del incremento 2 antes de extenderlo a las seis áreas.

En cada incremento registrar: alcance completado; archivos creados/modificados; esquema y migraciones; comandos y resultados reales; demostración; limitaciones y siguiente incremento. Un incremento con pruebas pendientes debe reportarse como pendiente de validación.

## Actualización de entrega — 5 de noviembre de 2026

El estado actual y la aceptación del MVP se documentan en [entrega_mvp.md](entrega_mvp.md). Este registro de análisis inicial se conserva como referencia histórica. El cierre añade recuperación de resultados al alcance inicial del intercambio.
