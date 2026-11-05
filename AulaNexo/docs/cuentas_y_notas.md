# Cuentas simuladas y notas

Fecha: 5 de noviembre de 2026.

## Comportamiento

- Entrada por selector de cuentas locales: alumno y docente, sin contraseña. Se identifica expresamente como simulación; no sustituye autenticación ni autorización de un sistema multiusuario real.
- Se conserva la base `aulanexo.db`. La migración Room 4 → 5 añade cuentas y relaciones docente–alumno, sin recrear tablas de aprendizaje.
- Cada perfil anterior o recuperado obtiene una cuenta de alumno al cargar la lista; conserva su identificador de perfil e historial. La operación es idempotente.
- Existe una cuenta «Docente de ejemplo». Se pueden crear más docentes y alumnos desde la pantalla inicial. No se inventan respuestas ni calificaciones de demostración.
- Alumno: editar su nombre/avatar, comenzar sus actividades y consultar «Mis notas».
- Docente: editar su nombre, vincular/quitar alumnos de su aula y consultar las notas de cada alumno vinculado. Un alumno puede estar vinculado a más de un docente; cada docente ve su propia lista.
- El repositorio valida los roles y la relación antes de consultar las notas. Un alumno solo consulta las suyas. Quitar un vínculo impide nuevas consultas por ese docente y conserva el historial del alumno.
- No se restaura automáticamente una sesión de cuenta después de terminar el proceso; se vuelve al selector. Los avances educativos siguen persistidos.

## Criterio de notas

Escala inicial de práctica: 0 a 20, con un decimal.

`nota = aciertos en la primera respuesta de cada pregunta / total de preguntas × 20`

Ejemplo: 2 aciertos a la primera en 3 preguntas producen 13,3/20. Corregir un error permite completar la lección, pero no cambia retroactivamente la primera respuesta. Las pistas se muestran como indicador de apoyo y no descuentan puntos.

Cada intento se presenta por separado y con la versión de su contenido. Un intento incompleto figura «En curso · Sin nota final». No se calcula un promedio entre versiones diferentes ni se oculta un intento anterior al volver a practicar. No es una nota oficial introducida por el docente; no se incorpora en esta entrega un editor de calificaciones.

Las notas se derivan de los eventos ya almacenados, por lo que no se duplica la información en otra tabla. La recuperación de resultados mantiene la política existente: contenido correspondiente instalado y perfil recuperado separado. Las cuentas y vínculos de aula no se transfieren con el paquete de resultados; el docente debe vincular la cuenta recuperada localmente.

## Verificación

- Pruebas unitarias de escala, redondeo y rechazo de cantidades inválidas.
- Pruebas Room de consulta por alumno/docente, vínculos, rechazo de acceso ajeno, conservación de notas al editar, pistas sin descuento y desvinculación.
- Migración real 4 → 5 con perfil e intento anteriores, adopción sin duplicados y sin vinculación automática a un docente.
- Prueba de UI del recorrido docente → vincular alumno → consultar nota → cambiar a alumno → consultar la misma nota.
- Arranque de producción actualizado al selector de cuentas y recreación de Activity.

Resultado final: BUILD SUCCESSFUL con assembleDebug, testDebugUnitTest, lintDebug y connectedDebugAndroidTest. Se aprobaron 37 pruebas unitarias y 50 pruebas en Android 15, sin fallos. Lint: 0 errores, 5 advertencias de versiones de dependencias. Se corrigieron etiqueta redundante del manifiesto, recursos de colores de plantilla sin uso, una sugerencia KTX y boxing de estado entero. Se mantienen las versiones ya validadas de dependencias para no mezclar la ampliación de cuentas con una migración del stack.

SHA-256 del APK: `4E0F26E13AFB7556896DC01EA12334121CDDF60B17CF12AC8C7B55454198F94B`.

## Recorrido manual

1. Ejecutar desde Android Studio con Run `app`.
2. Crear una cuenta de alumno o seleccionar una ya migrada.
3. Abrir «Comenzar mi aventura», realizar una lección y volver a «Mi espacio de aprendizaje».
4. Abrir «Mis notas» y revisar intentos, aciertos a la primera y pistas.
5. Cambiar de cuenta, seleccionar «Docente de ejemplo» y abrir «Vincular alumnos».
6. Agregar el alumno y abrir «Ver notas de …».
7. Crear otro docente para comprobar que empieza con su aula vacía.

## Alcance todavía pendiente

Esta ampliación no termina los 208 temas sin actividades, cuentos narrados, fonética inglesa, mapa visual, objetivos diarios, editor docente avanzado, accesibilidad integral, piloto pedagógico ni distribución release. Sus estados permanecen en el checklist; no se marcan completados por añadir cuentas y notas. La propuesta del tutor está en `tutor_con_avatar.md` y aún no es un avatar integrado.
