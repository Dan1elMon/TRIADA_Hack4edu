# Docentes y alumnos listos para la demo

Tres docentes ficticios, con diez alumnos distintos por aula. La contraseña inicial de las 33 cuentas es **admin**. Los códigos docentes son también sus usuarios de login, de ocho dígitos.

| Docente | Código docente / usuario | Contraseña inicial |
|---|---|---|
| María López | 10000001 | admin |
| Carlos Mendoza | 11000002 | admin |
| Elena Vargas | 11000003 | admin |

## Aula de María López — 10000001

| Alumno | Usuario | Contraseña inicial |
|---|---|---|
| Ana Torres | 20000001 | admin |
| Luis Ramos | 20000002 | admin |
| Sofía Vega | 20000003 | admin |
| Mateo Ríos | 20000004 | admin |
| Valentina Cruz | 20000005 | admin |
| Diego Flores | 20000006 | admin |
| Camila Soto | 20000007 | admin |
| Gabriel Luna | 20000008 | admin |
| Lucía Castro | 20000009 | admin |
| Nicolás Pérez | 20000010 | admin |

## Aula de Carlos Mendoza — 11000002

| Alumno | Usuario | Contraseña inicial |
|---|---|---|
| Martín Salazar | 20000011 | admin |
| Isabella Díaz | 20000012 | admin |
| Sebastián Mora | 20000013 | admin |
| Daniela Ruiz | 20000014 | admin |
| Andrés León | 20000015 | admin |
| Paula Medina | 20000016 | admin |
| Joaquín Silva | 20000017 | admin |
| Mariana Paredes | 20000018 | admin |
| Samuel Ortiz | 20000019 | admin |
| Victoria Rojas | 20000020 | admin |

## Aula de Elena Vargas — 11000003

| Alumno | Usuario | Contraseña inicial |
|---|---|---|
| Emilio Navarro | 20000021 | admin |
| Antonella Reyes | 20000022 | admin |
| Adrián Campos | 20000023 | admin |
| Renata Molina | 20000024 | admin |
| Tomás Herrera | 20000025 | admin |
| Mía Fuentes | 20000026 | admin |
| Alejandro Soto | 20000027 | admin |
| Emilia Ponce | 20000028 | admin |
| Bruno Aguilar | 20000029 | admin |
| Valeria Núñez | 20000030 | admin |

## Uso y conservación de datos

Las cuentas se preparan automáticamente al iniciar la versión actualizada. Los primeros diez alumnos conservan sus IDs y usuarios anteriores y pertenecen al aula inicial, ahora denominada María López cuando conservaba el nombre automático Docente de ejemplo. No se cambian nombres personalizados del docente ni contraseñas previamente modificadas. Tampoco se borran notas, avances o vínculos existentes. Las dos aulas nuevas reciben diez alumnos propios cada una.

Los códigos de la tabla se asignan si están disponibles. Si ya pertenecen a una cuenta anterior, se conserva esa cuenta y se asigna un código libre desde 19000001 a un docente o desde 29000001 a un alumno. El código efectivo se muestra en la pantalla del docente y en las listas de cuentas y alumnos.

Cada docente puede consultar las notas de sus alumnos vinculados; no puede consultar a un alumno de otra aula sin un vínculo. Las cuentas de demo no traen notas inventadas. El alumno ingresa con su usuario y admin y ve sus actividades y Mis notas.

La inicialización es idempotente: no duplica cuentas, no restablece credenciales cambiadas ni revierte desvinculaciones hechas por el docente. Las listas son locales al dispositivo.

## Juan y el encabezado

El búho tutor se llama **Juan**. Se identifica en su tarjeta como Juan, tu tutor y se presenta por su nombre en la orientación inicial.

El encabezado común de la navegación muestra el logo original a la izquierda de AulaNexo en todas las pantallas de la aplicación. Permanece visible al desplazarse por login, aula, notas, mapa, actividades, lector, álbum, editor e intercambio. Los selectores externos de Android conservan su propia interfaz.

## Validación

Pendiente de registrar el resultado de la ejecución final.
