# Little Horizons ERP

## Descripción

Aplicación Java de consola para gestionar estudiantes, acudientes, matrículas,
profesores, materias, notas, asistencia, actividades extracurriculares, pagos y
recursos del Colegio Little Horizons. Centraliza registros en MySQL, conserva
historial y permite consultar promedios y saldos sin tratar datos faltantes como
ceros ni confundir pagos recibidos con deuda saldada.

## Autores

**Stiven**

Correo: **stivenurbina044@gmail.com**.

## Tecnologías utilizadas

| Herramienta | Uso |
| --- | --- |
| Java 17 | Records, colecciones, Streams, JDBC y aplicación de consola |
| Maven | Compilación y gestión de dependencias |
| IntelliJ IDEA | Entorno utilizado para ejecución y pruebas manuales |
| MySQL 8.0 o superior | Persistencia relacional con InnoDB |
| MySQL Connector/J 26.10.0 | Driver JDBC declarado en pom.xml |
| Biblioteca QR de Nayuki | Generación QR; código incluido bajo MIT |
| DBeaver | Ejecución de scripts y verificación de la base |

El PDF se escribe con la utilidad Java `PdfDocument`; no requiere un servicio
externo. Las atribuciones del generador QR están en [third_party.md](docs/third_party.md).

## Requerimientos previos

- JDK 17 y Maven; comprobar con `java -version` y `mvn -version`.
- **MySQL 8.0 o superior** y un usuario con permisos para la base.
- Git para clonar; IntelliJ IDEA para la configuración descrita.
- Acceso a Maven Central la primera vez que se resuelven las dependencias.
- Python 3 y `pdftotext` son opcionales para las comprobaciones adicionales del PDF.

## Configuración de la base de datos

### Creación

Para crear la base por primera vez, ejecuta [docs/database.sql](docs/database.sql)
desde DBeaver y detén la ejecución si aparece un error. Incluye dos secciones: DDL completo y DML de catálogos
más 30 registros ficticios en cada una de las ocho tablas principales. El esquema
usa `DECIMAL(2,1)` para notas y tablas separadas para préstamos de profesores y
estudiantes. El instalador está pensado para una base nueva; las sentencias DDL de MySQL
no se revierten con ROLLBACK.

Las actualizaciones de bases existentes están en `docs/migrations/`. Antes de
ejecutar una migración, comprueba si sus tablas ya están creadas.

Los archivos `docs/seed_checks.sql` y `docs/database_checks.sql` permiten revisar
los conteos y las relaciones después de la carga.
El lote LHSEED contiene datos ficticios. `docs/seed_data.sql` permite cargarlo
por separado solo en una base con esquema y catálogos que todavía no lo tenga.
Usa una sola de las dos cargas: `docs/database.sql` ya incluye el lote completo.

### Credenciales

Configura estas variables en **Run → Edit Configurations → Main → Environment variables**
de IntelliJ, con los datos de tu conexión MySQL:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/erp_little_horizons
DB_USER=<usuario local>
DB_PASSWORD=<contraseña local>
```

`DatabaseConnection.open()` lee estas variables. No hay credenciales en el código
ni carga automática de `.env`. Las credenciales se configuran en el entorno local.

### Driver JDBC

Recarga el proyecto Maven para resolver `com.mysql:mysql-connector-j`, ya declarado
en `pom.xml`. Ejecuta Main desde el módulo Maven para incluir el driver.
No hace falta copiar manualmente el JAR dentro de `src/`.

## Instalación y ejecución

### Clonar

Requiere acceso al repositorio:

```sh
git clone https://github.com/stivenurbina044-ops/proyecto-Java.git
cd proyecto-Java
```

### Abrir en el IDE

Abre la carpeta que contiene `pom.xml` como proyecto Maven y selecciona JDK 17.
Añade las variables de conexión a la configuración de ejecución de
`com.littlehorizons.erp.Main`.

### Compilar

```sh
mvn package
```

Maven descarga las dependencias necesarias y genera los archivos de compilación
en `target/`. La primera compilación requiere conexión a Maven Central.

### Ejecutar

Ejecuta `Main.main()` desde IntelliJ con la raíz del proyecto como directorio
de trabajo. Allí se crearán `error_log.txt` y la carpeta `facturas/` cuando
corresponda. El JAR generado por Maven no incluye las dependencias; la ejecución
desde el IDE utiliza el classpath del módulo.

Pruebas locales:

```sh
bash scripts/check_student_billing.sh
bash scripts/check_invoices.sh
bash scripts/check_academic_statistics.sh
```

Los casos JDBC usan dobles y no acceden a MySQL. Las pruebas manuales realizadas
en MySQL cubrieron abonos, liquidación, rechazos de aplicaciones incompatibles,
recuperación de factura y límites del reporte de vencidos.

## Estructura del proyecto

```text
src/main/java/
├── com/littlehorizons/erp/
│   ├── model/       Entidades, records y estados
│   ├── view/        Menús, formularios y presentación de consola
│   ├── controller/  Coordinación de acciones y navegación
│   ├── dao/         Consultas y transacciones JDBC
│   ├── service/     Cálculos y coordinación de operaciones
│   ├── util/        Conexión, logs y escritura PDF
│   └── Main.java    Construcción e inyección de colaboradores
└── io/nayuki/qrcodegen/  Biblioteca QR de terceros
src/main/resources/      Configuración ficticia del colegio
src/test/java/           Comprobaciones aisladas
scripts/                 Lanzadores de pruebas
docs/                    DDL, DML, migraciones y documentación
```

Las vistas no ejecutan SQL; los DAO devuelven datos y no leen consola.
Los nombres Java y sus comentarios se escriben en inglés. La interfaz de usuario
está en español y los nombres SQL conservan los del esquema del proyecto.

## Principios y patrones de diseño aplicados

### SOLID

| Principio | Aplicación concreta y alcance |
| --- | --- |
| Responsabilidad única | FinanceView presenta; PaymentSummaryService calcula totales; PaymentDao persiste |
| Abierto/cerrado | InvoiceService recibe InvoiceDocumentWriter; puede sustituirse el renderizador sin editar la coordinación de generación |
| Sustitución de Liskov | InvoicePdfWriter implementa el contrato render: recibe datos y devuelve bytes sin persistir archivos ni registros |
| Segregación de interfaces | InvoiceDocumentWriter tiene una sola operación, sin exigir al renderizador acceso JDBC o lectura de consola |
| Inversión de dependencias | InvoiceService depende del contrato de renderizado y Main proporciona InvoicePdfWriter; los DAO siguen siendo colaboradores concretos |

La abstracción del renderizador permite cambiar la generación del documento
sin modificar la coordinación de la factura. Los DAO se inyectan como clases
concretas.

### Patrones

- **MVC:** vistas de consola, modelos y controladores separados.
- **DAO:** StudentDao, PaymentDao y otros encapsulan JDBC y SQL parametrizado.
- **Estrategia de renderizado:** InvoiceDocumentWriter permite inyectar la forma de
  producir los bytes de la factura; InvoicePdfWriter es la implementación actual.
- **Inyección por constructor:** Main conecta controladores, vistas, DAO y servicios.

`DatabaseConnection.open()` crea una conexión JDBC para cada operación. Los DAO
cierran la conexión al salir del bloque try-with-resources.

## Funcionalidades principales

| Módulo | Operaciones disponibles |
| --- | --- |
| Estudiantes | Listar, registrar estudiante y acudiente, consultar grados, matricular e historial |
| Académico | Inscripciones, notas, promedios, asistencia e informe semanal de ausencias |
| Profesores | Registro, directorio, asignaciones y horarios con control de cruces |
| Extracurriculares | Clubes, cupos, inscripciones, retiros, eventos, participantes y resultados |
| Finanzas | Conceptos, pagos, facturas, tarifas, cobros, aplicación de abonos y saldos vencidos |
| Recursos | Inventario, registro, préstamos a estudiantes/profesores, devoluciones y vencidos |
| Reportes | Promedios por estudiante y por grado, materia, año y período |

Cada módulo se abre desde el menú principal. Los formularios indican cómo
cancelar y los selectores permiten volver con 0. Registrar un pago no reduce
el saldo de un cobro: debe aplicarse desde Gestión Financiera, opción 11.
La opción 6 permite consultar o recuperar la factura de un pago existente.

## Manejo de errores y logs

Los controladores capturan SQLException, muestran un mensaje y llaman a
`ErrorLogger.log`. El archivo `error_log.txt` se crea en el directorio de ejecución,
con fecha, contexto y traza; agrega entradas sin borrar las previas. Está excluido
de Git. Si no se puede escribir, se avisa por stderr y se permite continuar.
Las entradas inválidas se reintentan y las reglas de dominio usan excepciones
controladas. Los formularios permiten cancelar según los mensajes de cada menú.

Las consultas enlazan valores con PreparedStatement y cierran recursos mediante
try-with-resources. Las operaciones relacionadas usan transacciones y rollback;
los préstamos, aplicaciones y otros flujos emplean bloqueos de filas.

## Problemas conocidos y limitaciones

- Facturación y CUFE son simulaciones académicas; no hay validación DIAN.
- El lote se carga por SQL: sus pagos no generan PDF hasta solicitarlos en consola.
- La aplicación permite registrar y consultar las entidades principales, pero
  no ofrece edición y eliminación general para todas ellas.
- No hay autenticación, deshacer aplicaciones ni mantenimiento/baja de recursos
  desde consola. Se conserva historial y se restringen cambios relacionados.
- Las pruebas aisladas no sustituyen una prueba de integración con MySQL ni
  cubren concurrencia real.

## Futuras mejoras

Ampliar las opciones de edición, incorporar pruebas de integración automatizadas
y unificar la lectura y validación de formularios de consola.

## Licencia

El código propio no cuenta actualmente con una licencia de distribución. La biblioteca QR conserva su licencia MIT en los
archivos originales; ver [atribuciones](docs/third_party.md).

## Contacto

Correo: **stivenurbina044@gmail.com**.

Repositorio: [proyecto-Java](https://github.com/stivenurbina044-ops/proyecto-Java).
