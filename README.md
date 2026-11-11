# Little Horizons ERP

Aplicación Java de consola para gestionar la operación académica, extracurricular, financiera y de recursos del Colegio Little Horizons.

## Ejecutar la base del proyecto

Requiere JDK 17 y Maven. Los módulos que persistan datos requerirán MySQL 8.0 o superior.

```sh
mvn package
```

El menú valida entradas y permite salir. En **Gestión de Estudiantes** puedes listar estudiantes, registrar un acudiente y vincularlo a un estudiante nuevo usando su documento. Durante el registro puedes escribir `/volver` para cancelar.

Para probarla en IntelliJ, configura `DB_URL`, `DB_USER` y `DB_PASSWORD` en la configuración de ejecución de `Main`. Usa los datos de tu conexión de DBeaver. Por ejemplo, `DB_URL=jdbc:mysql://127.0.0.1:3306/erp_little_horizons`. Ejecuta `Main` desde el módulo Maven para incluir el driver JDBC. Si eliges listar estudiantes y la tabla está vacía, verás `Estudiantes registrados: 0` y `No hay estudiantes registrados.`

## Matrículas

En **Gestión de Estudiantes**, la opción **5** permite matricular por documento, ID del grado y año académico. La fecha se toma del día de registro y el estado inicial es `Activa`. La opción **6** muestra el historial. La restricción de MySQL impide más de una matrícula por estudiante y año, independientemente de su estado. Se aceptan años de cuatro cifras, incluidos años futuros para permitir planificación académica.

## Inscripciones en materias

En **Gestión Académica**, inscribe un estudiante por documento, año y período (I–IV). El catálogo muestra materias activas del grado de su matrícula activa. La inscripción vuelve a comprobar esas condiciones al guardar. La sección es opcional. También puedes consultar el historial por documento. MySQL impide duplicar estudiante, materia, año y período incluso si la inscripción anterior está retirada.

## Calificaciones

En **Gestión Académica → Registrar calificación**, indica documento, año y período. Se muestran las materias con inscripción activa o completada y profesor activo asignado al mismo año y sección, incluidos los casos sin sección. La nota admite coma o punto y máximo un decimal entre 0 y 5. Una nota duplicada se rechaza.

Para **Final** se admite una inscripción activa o completada en cualquier período de ese año. Es una nota ingresada manualmente. El historial muestra los promedios por año y período con el mismo peso para cada materia y redondeo a un decimal. Las notas Final se promedian por separado.

## Organización MVC

- `Main` conecta las piezas y arranca la aplicación.
- `view` muestra la interfaz de consola y lee entradas.
- `controller` coordina las acciones de la aplicación.
- `dao` ejecuta las consultas JDBC y `util` abre la conexión con MySQL.
- `model` representa los datos que usa la aplicación. `service` coordina el registro de estudiantes y la transacción con el acudiente.

## Orden de implementación

1. Revisar el DDL oficial y preparar `docs/database.sql` para `erp_little_horizons`.
2. Completar el primer flujo de estudiantes y acudientes con JDBC. Después añadir matrícula e inscripciones.
3. Añadir profesores, asignaciones, calificaciones y asistencia. Usar Streams para promedios y reportes.
4. Implementar clubes, eventos y préstamos con sus reglas de cupo y disponibilidad.
5. Modelar cobros pendientes, registrar pagos y generar el PDF de factura con elementos DIAN simulados según el enunciado.
6. Completar datos de prueba, documentación, registro de errores y verificación de la entrega.

## Manejo de errores y logs

Los controladores capturan los errores JDBC de los flujos de estudiantes, acudientes,
matrículas, inscripciones y calificaciones. Muestran un mensaje en consola y guardan
la fecha con zona horaria, el contexto y la traza de la excepción en `error_log.txt`,
en el directorio desde el que se ejecuta la aplicación. El archivo se crea al primer
error y conserva los registros anteriores. Las entradas inválidas se muestran al
usuario para que pueda corregirlas sin generar una traza de error.

Si no se puede escribir el archivo, se muestra un aviso en la salida de errores y
la aplicación permite continuar. El archivo de logs está excluido de Git; las trazas
pueden contener información de la base de datos y deben tratarse como datos locales.

## Directorio de profesores

En **Gestión de Profesores → Listar profesores** puedes consultar el ID, documento,
nombre completo y estado de los profesores registrados, ordenados por apellido y
nombre. El listado incluye profesores activos e inactivos. La opción **0** vuelve
al menú principal. La opción **2** registra profesores con documento, nombres y apellidos, correo,
teléfono, especialización y fecha de contratación. Segundo nombre, segundo apellido,
teléfono y especialización son opcionales; se guardan como NULL al dejarlos vacíos.
Si la contratación comienza después de la fecha actual de MySQL, el estado inicial
es `Inactivo`; en caso contrario, es `Activo`. Se admiten fechas futuras para
contratos programados. El cambio a `Activo` al iniciar el contrato es manual.
El registro de calificaciones exige un profesor activo cuya fecha de contratación
ya haya llegado, tanto al consultar las materias como al guardar la nota. Documento y correo deben ser únicos. Puedes cancelar con
`/volver` desde cualquier campo. En **Gestión de Profesores → Asignar materia** se seleccionan profesor activo,
materia activa, año y sección opcional. El guardado verifica que la contratación
haya iniciado y que no sea posterior al año elegido. Evita duplicados incluso
cuando la sección es NULL, mediante una transacción y bloqueo del profesor.
La sección vacía se guarda como NULL. Los horarios requieren la migración descrita a continuación.

La opción **4. Consultar asignaciones** muestra profesor, código y nombre de materia,
año y sección de todas las asignaciones. Las secciones NULL se presentan como
`Sin sección`. El listado incluye asignaciones históricas y se ordena por año
descendente, profesor y materia.

## Horarios de clase

Antes de usar horarios en una base ya creada, ejecuta una sola vez en DBeaver
`docs/migrations/001_class_schedules.sql`. Es una ampliación del esquema existente;
ya está incluida en `docs/database.sql` para instalaciones nuevas. No ejecutes
la migración si la tabla ya existe.

En **Gestión de Profesores → 5. Registrar horario**, selecciona una asignación,
un día (1=Lunes a 7=Domingo) y horas `HH:mm` en formato de 24 horas. La hora final
debe ser posterior al inicio, dentro del mismo día. Puedes cancelar con `/volver`.
La opción **6. Consultar horarios** muestra el listado guardado.

El guardado comprueba que el profesor esté activo, su contratación haya comenzado,
el año de asignación no sea anterior a la contratación y la materia esté activa.
Una transacción con bloqueo del profesor evita cruces entre sus clases del mismo
día y año, incluso entre materias o secciones diferentes. Se permiten clases
consecutivas (08:00–09:00 y 09:00–10:00). Los horarios son semanales por año;
no incluyen fechas de vigencia, aulas ni validación de cruces de estudiantes.
Las inserciones manuales desde DBeaver deben respetar la regla de cruces, que
se aplica desde la aplicación; el SQL comprueba día y orden de horas.

## Scripts de base de datos

- `docs/database.sql`: instalación inicial en una base nueva. Contiene el DDL del
  anexo, la ampliación de horarios y el DML de catálogos básicos. No ejecutar sobre
  la base actual ni reutilizar como archivo de pruebas. Detener el script al primer
  error; el DDL de MySQL no se revierte con ROLLBACK. No contiene DROP DATABASE.
- `docs/database_checks.sql`: consultas de solo lectura para revisar registros,
  conteos de las ocho tablas principales, duplicados y cruces de horarios.
- `docs/migrations/001_class_schedules.sql`: ampliación para bases existentes que
  aún no tengan `horarios_clase`. Ejecutar una sola vez.

El script inicial conserva tipos, claves, índices y reglas del anexo. Se corrigió
el nombre de la base en la documentación a `erp_little_horizons` y se separaron las
consultas de diagnóstico de las sentencias de instalación. Se eliminaron las
pruebas manuales con documentos de ejemplo y sus inserciones temporales. La tabla
`horarios_clase` permite registrar varios bloques semanales por asignación y añade
restricciones para día y orden de horas. Los cruces y la inactivación inicial de
profesores contratados en el futuro se validan desde la aplicación; no hay triggers
que apliquen esas reglas a inserciones manuales.

Los catálogos iniciales incluyen 12 grados, 5 categorías y 7 conceptos de pago.
Todavía queda pendiente el DML con al menos 30 registros en cada una de las ocho
tablas principales exigidas para la entrega.

## Asistencia

En **Gestión Académica → 5. Registrar asistencia** se pide documento, fecha,
período, materia/profesor, estado y observaciones opcionales (máximo 1000 caracteres).
Estados: Presente, Ausente, Tardanza y Excusa Médica. Se permite cancelar en cada
paso. No se admiten fechas futuras ni anteriores a la contratación del profesor,
matrícula o inscripción. El año se toma de la fecha. Se exige estudiante activo,
matrícula e inscripción activas, materia activa y profesor activo asignado al
mismo año y sección. Las condiciones se revisan también al guardar.

El período se usa para validar la inscripción: la tabla no almacena el período ni
sus fechas de inicio y fin, por lo que se selecciona manualmente. La asistencia es
única por estudiante, materia y fecha, aunque se elija otro profesor o período.
No se exige un horario semanal para permitir jornadas excepcionales. El registro
no autentica al profesor: la consola selecciona la asignación habilitada.
La opción **6. Consultar historial de asistencia** muestra todos los registros
para el documento, incluyendo notas y profesor, con las fechas más recientes
primero. No modifica la tabla existente. Los errores JDBC se guardan en
`error_log.txt`. En **7. Reporte semanal de inasistencias**, ingresa una fecha no futura de la
semana que deseas revisar. El rango es de lunes a domingo, aunque cruce mes o año.
Se muestran los estudiantes con **más de 3 días distintos** con estado `Ausente`,
ordenados por cantidad descendente y luego por nombre. Varias ausencias en materias
del mismo día cuentan como un solo día; Presente, Tardanza y Excusa Médica no suman.
El reporte consulta todos los estudiantes con registros en ese rango, incluyendo
historial de quienes hoy están inactivos. Una ausencia en alguna materia basta
para contar ese día: no significa necesariamente ausencia durante toda la jornada.
Si no hay coincidencias se indica que no hay días de ausencia registrados que
superen el umbral; no se infieren asistencias faltantes. No requiere cambios SQL.

## Clubes extracurriculares

En **4. Actividades Extracurriculares** están disponibles:

- **1. Listar clubes**: muestra activos e inactivos, descripción, profesor encargado,
  horario, cupo máximo, miembros activos y plazas disponibles.
- **2. Registrar club**: selecciona un profesor activo cuya contratación ya comenzó.
  Pide nombre único (máximo 100 caracteres), descripción opcional (máximo 1000),
  cupo entero positivo y horario de reunión opcional (máximo 100). El club inicia activo.
- **3. Inscribir estudiante en club**: pide documento y club activo. Verifica que
  el estudiante esté activo y haya ingresado al colegio, que no tenga una membresía
  activa en el club y que haya cupo. Se guarda la fecha actual de MySQL y estado
  `Activa`. Una membresía retirada permite una nueva inscripción si hay cupo.
- **4. Consultar clubes de estudiante**: muestra las membresías activas y retiradas,
  con fechas de ingreso y retiro.

Los formularios permiten `/volver`; los selectores de ID permiten 0. Se usan
PreparedStatement y try-with-resources; los errores JDBC se guardan en error_log.txt.
Las inscripciones bloquean la fila del club y comprueban cupos y duplicados dentro
de una transacción para serializar solicitudes concurrentes desde la aplicación.
Los miembros inactivos como estudiantes siguen ocupando cupo mientras su membresía
sea `Activa`. Un cupo NULL o no positivo impide nuevas inscripciones.
No requiere cambios de esquema. El control de cupo y membresías duplicadas se
aplica desde Java; las escrituras manuales deben respetar estas reglas.
El horario de reunión es texto descriptivo, sin validación de cruces con clases.
En **5. Retirar estudiante de club**, ingresa documento, selecciona una membresía
activa e indica el motivo obligatorio (máximo 1000 caracteres). Puedes cancelar
con 0 en el selector o `/volver` en el motivo. Se guarda `Retirada`, la fecha actual
de MySQL y el motivo; no se borra el registro. No se permite retirar dos veces la
misma membresía ni registrar un retiro anterior al ingreso. Se puede retirar una
membresía aunque el club o estudiante estén inactivos. El retiro usa el mismo
bloqueo de club que la inscripción y libera cupo al confirmar la transacción.
El estudiante puede inscribirse otra vez si cumple las condiciones y hay cupo;
la reinscripción crea una membresía nueva y conserva la anterior.
La gestión de eventos se abre desde la opción 6 del mismo menú.

## Eventos

En **4. Actividades Extracurriculares → 6. Gestión de Eventos**:

- **1. Consultar calendario de eventos**: listado por fecha de eventos activos e
  inactivos, con descripción, tipo, lugar, organizador y si tienen competencia.
- **2. Registrar evento**: selecciona un profesor activo y completa nombre (150),
  descripción opcional (1000), tipo, fecha, lugar opcional (150) y competencia.
  Se admiten eventos pasados para registrar historial y futuros para planificar.
  El organizador debe haber iniciado su contratación tanto a día de hoy como en
  la fecha del evento. El evento se crea activo. Los nombres pueden repetirse
  para ediciones distintas: se identifican por ID y fecha.
- **3. Registrar participante**: selecciona evento activo, documento de estudiante,
  rol y observaciones opcionales (1000). Se exige estudiante activo con ingreso
  no posterior al evento. Se permite inscribir anticipadamente en eventos futuros;
  el registro por sí solo no acredita asistencia. Una participación por estudiante
  y evento, protegida por la clave única de MySQL. `Competidor` solo está disponible
  en eventos con competencia. Las posiciones se registran por separado.
- **4. Consultar participantes de evento**: incluye documento, nombre, rol,
  resultado y observaciones, también para eventos inactivos.
- **5. Registrar o actualizar resultado de competencia**: solo eventos activos
  con competencia cuya fecha ya llegó y participantes con rol `Competidor`.
  Posiciones: 1er Lugar, 2do Lugar, 3er Lugar o Participante. Permite corregir el
  resultado; no se limita cada posición a un solo estudiante, para admitir empates.

Se permite `/volver` en formularios y 0 en selectores. Las reglas se vuelven a
comprobar al guardar mediante SQL parametrizado; los errores JDBC se registran en
`error_log.txt`. Las tablas actuales no requieren migraciones. No se validan cruces
con horarios de clase ni hay control de asistencia real a eventos. Los registros
históricos se conservan en las consultas aunque hoy el estudiante esté inactivo.

## Recursos y préstamos

En **6. Gestión de Recursos**:

- **1. Listar recursos**: código, nombre, categoría, estado, ubicación, descripción
  y fecha de adquisición.
- **2. Registrar recurso**: categoría existente, código único (50), nombre (150),
  descripción opcional (1000), adquisición opcional no futura y ubicación obligatoria
  (100). Inicia como `Disponible`.
- **3. Prestar a estudiante / 4. Prestar a profesor**: recurso disponible, documento,
  devolución esperada de hoy o posterior y observaciones opcionales (1000).
  El solicitante debe estar activo y haber iniciado su vinculación al colegio.
  La fecha del préstamo es la fecha actual de MySQL, sin préstamos retroactivos.
- **5. Consultar préstamos**: historial de estudiantes y profesores, con tipo e ID
  separados para evitar confundir IDs iguales de las dos tablas.
- **6. Registrar devolución**: selecciona tipo de solicitante y préstamo pendiente.
  Registra fecha actual y estado `Devuelto`, dejando el recurso `Disponible`.
  Acepta préstamos vencidos y solicitantes que hoy están inactivos.
- **7. Consultar préstamos vencidos**: no devueltos cuya devolución esperada sea
  anterior a la fecha actual de MySQL. El vencimiento se calcula al consultar y se
  muestra como `Vencido` incluso si el estado almacenado aún es `Activo`; la consulta
  no escribe datos. El día de vencimiento todavía no se considera atrasado.

Préstamo y devolución usan transacciones, bloqueo del recurso y revisión de ambas
tablas de préstamos antes de cambiar el estado. Ante fallos se revierte toda la
operación. Los nombres de tablas y columnas variables proceden exclusivamente del
enum BorrowerType; los valores se enlazan con PreparedStatement. Los errores JDBC
se registran en error_log.txt. `/volver` cancela formularios y 0 cancela selectores.
No requiere migraciones. El control de exclusividad se aplica desde la aplicación;
las escrituras manuales deben respetarlo. Si un recurso tiene préstamos sin cerrar
incompatibles con su estado, la aplicación rechaza la operación para revisar sus datos.
El cambio a mantenimiento o baja desde consola queda pendiente.
