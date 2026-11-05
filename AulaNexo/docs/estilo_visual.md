# Estilo visual de exploración

Actualización solicitada por el usuario: 5 de noviembre de 2026. Esta paleta sustituye los colores de marca anteriores en la implementación; los documentos originales se conservan como referencia histórica.

| Uso | Color |
|---|---|
| Fondo y arranque | Arena cálida `#F7F3E9` |
| Acciones principales | Verde `#2ECC71` |
| Logros y distintivos | Miel `#F1C40F` |
| Información, mapa e ilustraciones | Océano `#3498DB` |
| Texto principal | Verde tinta `#20352B` |
| Enlaces y controles sobre fondo claro | Verde oscuro `#17633A` |
| Texto de información | Azul oscuro `#145078` |

Los tokens están centralizados en `ui/theme/Color.kt` y se aplican en `Theme.kt`. Los botones principales comparten verde y texto oscuro; los controles de texto/contorno usan un verde más profundo para conservar contraste. Tarjetas de stickers obtenidos usan un fondo miel suave y los avisos informativos un fondo azul suave. Mapa, tutor y escenas de lectura utilizan la nueva paleta. Los colores que identifican objetos educativos y los estados de error mantienen su significado.

Contrastes calculados por luminancia sRGB: tinta sobre verde 6,22:1; tinta sobre miel 7,87:1; verde oscuro sobre arena 6,58:1; azul oscuro sobre fondo informativo 7,32:1. Estas comprobaciones de pares de colores no certifican toda la accesibilidad del aplicativo.

Validación del cambio: compilación debug y lint. El cambio es visual y no modifica datos, actividades ni reglas de evaluación.

Compilación y lint completados con `.\gradlew.bat :app:assembleDebug :app:lintDebug --offline`. La instalación automática no se completó: el dispositivo USB aparece como no autorizado. Autorizarlo en el teléfono y ejecutar Run para ver la actualización.
