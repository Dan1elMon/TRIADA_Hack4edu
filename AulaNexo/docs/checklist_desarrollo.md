# Checklist de desarrollo de AulaNexo

Estado revisado: 5 de noviembre de 2026. Basado en `doc_tecnica.md`, el plan progresivo y las evidencias de `entrega_mvp.md` y la ampliación `cuentas_y_notas.md`.

`[x]` implementado o comprobado según las evidencias registradas. `[ ]` pendiente. Cuando la cobertura es parcial, se indica expresamente.

## 1. Base técnica

- [x] Proyecto Android con Kotlin y Jetpack Compose.
- [x] Tema visual compartido y navegación centralizada.
- [x] Separación de interfaz, dominio y datos; inyección con Hilt.
- [x] Persistencia Room y preferencias DataStore.
- [x] Versionado de contenido y migraciones hasta el esquema Room 5.
- [x] Generar el APK debug e instalarlo en dispositivo.
- [x] Compilación desde Android Studio: BUILD SUCCESSFUL aportado por el usuario.
- [x] Ejecución desde Android Studio confirmada por el usuario.

## 2. Perfiles y navegación

- [x] Crear, seleccionar y cambiar de perfil.
- [x] Editar nombre visible y avatar.
- [x] Separar intentos, avances y recompensas entre estudiantes.
- [x] Recordar el último perfil seleccionado.
- [x] Navegar por grado, área, unidad y lección.
- [x] Mostrar disponibilidad de contenido y progreso personal.
- [ ] Implementar el mapa visual de mundos/niveles del documento original; el MVP utiliza la jerarquía del temario.
- [ ] Implementar objetivos diarios.

## 3. Temario y actividades

- [x] Incorporar los 214 títulos de referencia del temario.
- [x] Organizar las seis áreas: Matemática, Comunicación, Ciencia y Ambiente, Personal Social, Razonamiento Matemático y Razonamiento Verbal.
- [x] Entregar seis lecciones funcionales, una por área, con 18 ejercicios en total.
- [x] Motor de conteo de objetos.
- [x] Motor de selección de respuestas.
- [x] Motor de secuencias.
- [x] Motor de asociación de pares.
- [x] Motor de formación de palabras con letras.
- [x] Ilustraciones originales y siete audios locales en español.
- [x] Distinguir los temas en preparación para evitar actividades vacías.
- [ ] Crear actividades para los otros 208 temas; sus títulos catalogados no equivalen a lecciones terminadas.
- [ ] Desarrollar el programa de fonética inglesa y audio por letra/fonema descritos en el documento original.

## 4. Apoyo, progreso y recompensas

- [x] Evaluar respuestas y mostrar retroalimentación.
- [x] Ofrecer pistas progresivas y registrar su uso.
- [x] Conservar respuestas incorrectas e intentos anteriores.
- [x] Guardar la posición y reanudar las actividades.
- [x] Repetir lecciones conservando el mejor progreso.
- [x] Entregar seis stickers únicos por perfil sin duplicarlos al repetir.
- [x] Mostrar álbum por categorías, premios obtenidos/pendientes y resumen de progreso.
- [ ] Revisar con docentes la calidad, dificultad, orden de pistas y pertinencia curricular de todo el contenido.

## 5. Lector de cuentos — pendiente

- [ ] Cuentos ilustrados divididos en páginas y niveles.
- [ ] Navegación entre páginas y registro de avance de lectura.
- [ ] Narración por página y repetición.
- [ ] Palabras interactivas con pronunciación individual y modalidad lenta.
- [ ] Preguntas de comprensión vinculadas al cuento.

Los audios de las actividades actuales no constituyen un lector de cuentos.

## 6. Intercambio y recuperación

- [x] Importar paquetes `.aula` de metadatos o contenido con ejercicios y WAV.
- [x] Exportar metadatos originales y contenido instalado.
- [x] Validar estructura, rutas, tamaños, hashes, contratos y versiones.
- [x] Conservar contenido publicado y progreso existente al importar.
- [x] Exportar resultados `.aula-result` sin nombre ni avatar, con identificador seudónimo.
- [x] Recuperar resultados en un perfil separado con las mismas versiones de lecciones instaladas.
- [x] Evitar duplicados en la reimportación y reevaluar aciertos antes de recuperar.
- [x] Abrir y guardar mediante el selector de Android.
- [x] Abrir el selector nativo de aplicaciones para compartir archivos.
- [x] Incorporar cuentas simuladas de alumnos y docentes, vínculos de aula y apartados de notas (validación aprobada en el informe de cuentas).
- [ ] Implementar editor docente avanzado y análisis agregado de aula.

## 7. Pruebas y calidad

- [x] 37 pruebas unitarias aprobadas en la regresión registrada.
- [x] 50 pruebas conectadas aprobadas en Samsung SM-G998U con Android 15.
- [x] Probar aislamiento de perfiles, migraciones, evaluadores, pistas y premios.
- [x] Probar intercambio completo, importaciones inválidas e idempotencia de recuperación.
- [x] Completar las seis lecciones con Wi-Fi y datos móviles desactivados.
- [x] Recuperar posición, avance y pistas entre procesos con PID distinto.
- [x] Probar guardado real y cancelación mediante el selector nativo.
- [x] Probar edición de perfil con fuente al 200 %, conservando progreso.
- [x] Ejecutar lint: 0 errores y 5 advertencias de versiones de dependencias.
- [x] Resolver las advertencias de recursos/código y documentar por qué se mantienen las versiones validadas de dependencias.
- [ ] Validar manualmente todas las pantallas con texto ampliado y TalkBack; la prueba de perfil no certifica toda la interfaz.
- [ ] Probar en ejecución Android 24 y ampliar cobertura de dispositivos/tamaños de pantalla.
- [ ] Validar arranque inicial y recorrido en modo avión; la prueba registrada desactiva Wi-Fi/datos, no activa modo avión.
- [ ] Medir rendimiento y consumo de memoria en un dispositivo de recursos limitados.
- [ ] Realizar piloto con docentes y estudiantes.

## 8. Distribución

- [x] APK debug disponible con huella SHA-256 registrada.
- [x] README, informes incrementales e informe de entrega.
- [ ] Preparar y verificar una compilación release firmada.
- [ ] Definir custodia de firma, distribución y procedimiento de actualización.
- [ ] Publicar por el canal de distribución elegido, si se requiere.

## Orden sugerido para continuar

1. Validar el recorrido de cuentas y notas con el usuario; la ampliación se documenta en `cuentas_y_notas.md`.
2. Cerrar revisión pedagógica y accesibilidad del recorrido existente antes de multiplicar el contenido.
3. Ampliar pruebas de compatibilidad y resolver advertencias relevantes.
4. Producir las lecciones restantes por lotes pequeños con validación de contenido y pruebas de sus riesgos nuevos.
5. Implementar cuentos, fonética inglesa y herramientas docentes como ampliaciones diferenciadas.
6. Preparar release firmada y realizar el piloto antes de una distribución escolar.

El MVP técnico de seis lecciones está entregado. El alcance completo de `doc_tecnica.md` y la conversión de todo `temario.md` en actividades siguen incompletos. No se asigna un porcentaje global porque los títulos, los motores y la validación tienen esfuerzos distintos.
