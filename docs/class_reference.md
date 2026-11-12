# Referencia de las clases para Little Horizons ERP

Material revisado el 11 de noviembre de 2026: los 15 PDF indicados por el usuario,
en `/home/camper/Descargas`. Se revisaron visualmente las diapositivas porque los
archivos no contienen una capa de texto extraíble. No se copian los PDF al
repositorio. Esta guía resume su aplicación al ERP; no sustituye el enunciado ni
convierte cada tema de clase en una funcionalidad obligatoria.

## Material y aplicación

| Archivo | Temas revisados | Aplicación al ERP |
| --- | --- | --- |
| 1. Fundamentos_Java.pdf | JVM, JDK, paquetes, main, variables, tipos y salida | Java 17, paquetes descriptivos y arranque desde Main |
| 2. Estructuras_de_Decisión_en_Java.pdf | Operadores, cortocircuito, equals, if, switch y ternario | Validaciones, selección de opciones y tratamiento de valores ausentes |
| 3. Estructuras_Repetitivas_Ramificación_en_Java.pdf | while, do-while, for, break y continue | Menús con salida, reintentos y recorridos claros |
| 4. Cadenas_Conversiones.pdf | Strings, wrappers, parsing, null y StringBuilder | Limpieza de entradas, conversiones controladas y construcción de documentos |
| 5. Introduccion_POO.pdf | Clases, objetos, métodos, constructores, this y responsabilidad única | Entidades, colaboración entre objetos y clases con propósito definido |
| 6. Encapsulamiento_en_Java.pdf | Accesos, atributos privados, getters, setters y validación | Proteger datos y validar operaciones antes de cambiar su estado |
| 7. Arquitectura_de_Objetos_en_Java.pdf | UML, herencia, polimorfismo, abstracción e interfaces | Contratos para responsabilidades intercambiables y explicación de relaciones |
| 8. Estructuras_de_Datos_en_Java.pdf | Arrays, List, ArrayList, LinkedList y genéricos | Resultados JDBC tipados y colecciones para consultas y reportes |
| 10. Kit_del_Desarrollador_Java.pdf | Set, Map, enum y excepciones | Estados tipados, agrupaciones, ausencia de duplicados y errores controlados |
| 11. Concurrencia_en_Java.pdf | Thread, Runnable, start, interrupt y join | Comprender concurrencia; introducir tareas en paralelo solo ante una necesidad concreta |
| 12. Sincronización_y_Atomicidad_en_Java.pdf | Condiciones de carrera, synchronized y clases atómicas | Distinguir protección en memoria de transacciones y bloqueos de la base de datos |
| 13. Inmortalizando_Datos_en_Java.pdf | Archivos, NIO, serialización y try-with-resources | Logs, configuración, PDF y cierre de recursos; MySQL sigue siendo la persistencia del ERP |
| 14. Java_JDBC_and_MVC_Architecture.pdf | DriverManager, Connection, ResultSet, PreparedStatement, MVC y DAO | Consultas parametrizadas, separación entre consola y SQL, y DAO que devuelve objetos |
| 15. Expresiones_Lambda_en_Java.pdf | SAM, Consumer, Supplier, Function, Predicate e interfaces propias | Lambdas y referencias a métodos para filtros y transformaciones con sentido |
| 16. Java_Stream_API.pdf | Origen, operaciones intermedias, terminales, filter, map, flatMap y anyMatch | Consultas sobre colecciones, búsquedas y procesamiento de reportes |

## Coincidencias con el proyecto actual

- `StudentDao` y los demás DAO encapsulan las consultas JDBC y devuelven objetos.
  Se usan PreparedStatement y try-with-resources, alineados con las diapositivas
  5, 9 y 13–15 del PDF 14 y la diapositiva 13 del PDF 13.
- Las vistas muestran menús y leen entradas. Los controladores coordinan los
  flujos. `AcademicAverageService` agrupa por año y período y calcula promedios.
- `TeacherController` y otros controladores usan lambdas para filtrar y localizar
  registros. `AcademicAverageService` usa Streams y referencias a métodos;
  `ResourceLoanDao` ordena resultados con Comparator y Streams.
- `ErrorLogger` registra fecha, contexto y traza en un archivo de texto mediante
  Files.writeString. El PDF 13, diapositivas 5 y 7, también presenta la API NIO.
- `BorrowerType` ya demuestra un enum con atributos y métodos, como en el PDF 10,
  diapositivas 9–10.
- Los préstamos y las inscripciones a clubes protegen cambios relacionados mediante
  transacciones JDBC, rollback y bloqueo de filas. Esto protege las operaciones
  de varias instancias contra la misma base; no equivale a synchronized en Java.

## Ajustes recomendados antes de ampliar el módulo financiero

1. Sustituir gradualmente los estados repetidos como texto por enums con etiquetas
   para MySQL y consola. Empezar por estados de recursos, préstamos y asistencia,
   manteniendo los valores existentes de la base. No requiere alterar los ENUM SQL.
2. Mover el cálculo del total de pagos desde `FinanceView.showPayments` a un servicio
   o al modelo de reporte. La vista debería recibir el total ya calculado.
3. Incorporar interfaces pequeñas donde exista una responsabilidad intercambiable,
   por ejemplo un contrato para generar documentos de factura. Evitar crear una
   interfaz por cada clase sin justificar su uso.
4. Reducir duplicación de lectura y validación entre vistas mediante una utilidad
   de entrada de consola, manteniendo mensajes y navegación consistentes.
5. Documentar ejemplos concretos de responsabilidad única, encapsulamiento,
   composición, DAO, lambdas y Streams. Añadir un diagrama de clases de un flujo
   representativo para poder explicarlo en la sustentación.

Estas son propuestas de revisión, no cambios ya implementados. El análisis no
modifica código, tablas, registros ni commits existentes.

## Decisiones de alcance

Los ejemplos de clases y getters/setters de las clases no establecen una prohibición
sobre records. El proyecto usa records de Java 17 para transportar datos; conviene
explicar su constructor, sus campos privados finales y sus métodos de acceso. No
convertirlos todos en clases mutables solo por igualar la sintaxis de una diapositiva.

La herencia debe expresar una relación de dominio o un contrato útil. No introducir
jerarquías, serialización de entidades o hilos únicamente para demostrar que se
conoce la sintaxis. Mantener los requisitos explícitos del proyecto como prioridad.
Las diapositivas ilustran conceptos y algunas simplifican detalles técnicos;
aplicarlas con el alcance y las limitaciones de cada herramienta.

La siguiente funcionalidad pendiente sigue siendo valores personalizados por
estudiante y cobros con vencimiento para poder calcular deudas con datos reales.
Antes de implementarla, aplicar los ajustes de diseño que correspondan a ese flujo.

## Avance posterior a la revisión

La ampliación de cobros incorpora `ChargeStatus` con etiquetas y cálculo de estado.
Se extrajo el total de pagos de `FinanceView` a `PaymentSummaryService` para que la
vista solo muestre el resultado. La ampliación añade tarifas, cobros y aplicación
expresa de pagos mediante una migración independiente; queda pendiente su prueba
en el MySQL del usuario. Las recomendaciones restantes se aplicarán gradualmente.
