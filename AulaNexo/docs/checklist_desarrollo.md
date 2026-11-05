# Checklist actual de AulaNexo

Actualización: 5 de noviembre de 2026. Evidencia detallada en `ampliacion_integral.md`. Las marcas distinguen implementación y validación humana.

## Acceso y cuentas

- [x] Compilar y ejecutar desde Android Studio, confirmado por el usuario.
- [x] Login local con usuario único de ocho dígitos y contraseña inicial `admin`.
- [x] Cambiar usuario y contraseña comprobando la contraseña actual.
- [x] Derivación de contraseñas con sal y limitación temporal de intentos en la sesión.
- [x] Crear cuentas de alumnos/docentes desde una cuenta docente autenticada.
- [x] Demo preparada con tres docentes y treinta alumnos, diez por aula; lista de accesos en `alumnos_demo.md`.
- [x] Conservar los perfiles anteriores y asignarles credenciales sin borrar sus notas.
- [x] Editar nombre/avatar, cerrar sesión y exigir nuevo login después de terminar el proceso.
- [x] Vincular/desvincular alumnos por docente y limitar la consulta de notas a su propia lista.
- [x] Apartado Mis notas para cada alumno, con intentos completos e incompletos diferenciados.

- [x] Encabezado común con el logo a la izquierda de AulaNexo en todas las pantallas.

## Contenido y aprendizaje

- [x] Seis áreas y 214 temas conservando los títulos de referencia.
- [x] Seis lecciones anteriores con 18 ejercicios.
- [x] 208 lecciones introductorias nuevas con 624 ejercicios específicos, originales y revisables.
- [x] Total de 214 lecciones y 642 ejercicios ejecutables en el catálogo inicial.
- [x] Motores de conteo, selección, secuencia, asociación y formación de palabras.
- [x] Guardar respuestas, errores, pistas, posición y mejor avance por alumno y versión.
- [x] Seis stickers de los hitos originales y álbum de recompensas.
- [x] Mapa visual de las seis áreas con estado y acceso a las unidades.
- [x] Objetivos diarios de una a tres lecciones distintas, sin duplicar un objetivo al repetir la misma lección.

## Tutor

- [x] Avatar de búho vectorial integrado, llamado Juan, sin descargas ni animación persistente.
- [x] Expresiones de atención, reflexión, ánimo y celebración.
- [x] Reglas según tipo de actividad, errores y pistas mostradas.
- [x] Ofrecer la siguiente pista sin saltarse el orden ni agotar repetidamente el mismo recurso.
- [x] Ocultar/mostrar tutor y comunicar la ayuda también mediante texto.
- [ ] Adaptación longitudinal por habilidades y ejemplos adicionales revisados; las reglas actuales se adaptan a la pregunta/intento.

## Lectura y fonética inicial

- [x] Tres cuentos originales en español y tres lecturas iniciales en inglés.
- [x] Páginas ilustradas, navegación, narración repetible y palabras interactivas.
- [x] Pronunciación a velocidad reducida.
- [x] Preguntas de comprensión/rima y progreso de lectura separado por alumno.
- [x] 86 grabaciones nuevas incluidas localmente, además de los siete WAV anteriores.
- [ ] Completar un programa de fonética inglesa más amplio; la base actual trabaja tres familias CVC.

## Herramientas docentes e intercambio

- [x] Borradores docentes persistidos y separados por propietario.
- [x] Formulario para preguntas/opciones/pistas de selección.
- [x] Edición avanzada de estructura compatible con los cinco motores.
- [x] Validación y vista previa antes de publicar.
- [x] Publicación versionada conservando los intentos y contenidos anteriores.
- [x] Importación/exportación de catálogo y lecciones con validaciones de rutas, límites, integridad y versiones.
- [x] Recuperación de resultados de lecciones sin duplicar respuestas ni premios.
- [ ] Intercambiar cuentos/progreso lector, credenciales, vínculos y borradores; los formatos actuales no los incluyen.

## Calidad, accesibilidad y revisión

- [x] Compilación aprobada, 39 pruebas unitarias y 57 conectadas sin fallos; lint con 0 errores y 5 avisos de versiones.

- [x] Migración Room 5 → 6, además de las anteriores, sin recreación destructiva.
- [x] Pruebas de login, cambio de acceso, roles, notas, recuperación y migraciones.
- [x] Pruebas del catálogo completo y de intercambio en una base nueva.
- [x] Pruebas de decodificación de los recursos del lector y de publicación de dos versiones.
- [x] Pruebas de texto al 200 % en login, cambio de acceso, lectura y edición de perfil.
- [x] Mensajes textuales, etiquetas de controles y avatar/escenas decorativas fuera del foco accesible.
- [x] Auditoría estructural de las 624 preguntas nuevas y comprobación automática de 25 operaciones aritméticas explícitas.
- [x] Registro CSV de los 214 temas preparado para revisión docente.
- [ ] Evaluación manual completa con TalkBack, foco, orientación y distintos tamaños de pantalla.
- [ ] Aprobación pedagógica con docente y piloto con alumnado; no la sustituyen las pruebas automáticas.
- [ ] Ejecutar en Android 24 y dispositivos de pocos recursos; las pruebas actuales usan Android 15.
- [ ] Firmar y validar una release de distribución con la clave del responsable del proyecto.

Los temas nuevos son una cobertura introductoria de actividades, no reproducciones de fichas originales ni una certificación de alineamiento curricular. Los temas avanzados, las actividades prácticas y los audios sintéticos requieren revisión humana antes de uso escolar.
