# Little Horizons ERP

## 1. Descripción

Aplicación Java de consola para gestionar estudiantes, acudientes, matrículas,
profesores, materias, notas, asistencia, actividades extracurriculares, pagos y
recursos del Colegio Little Horizons. Centraliza registros en MySQL, conserva
historial y permite consultar promedios y saldos sin tratar datos faltantes como
ceros ni confundir pagos recibidos con deuda saldada.

## 2. Autores

Autor registrado en Git: **Stiven**, correo **stivenurbina044@gmail.com**.
Nombre completo para la entrega: pendiente de confirmación del autor.

## 3. Tecnologías utilizadas

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

## 4. Requerimientos previos

- JDK 17 y Maven; comprobar con `java -version` y `mvn -version`.
- **MySQL 8.0 o superior** y un usuario con permisos para la base.
- Git para clonar; IntelliJ IDEA para la configuración descrita.
- Acceso a Maven Central la primera vez que se resuelven las dependencias.
- Python 3 y `pdftotext` son opcionales para las comprobaciones adicionales del PDF.

## 5. Configuración de la base de datos

### 5.1. Creación

En una instalación nueva y vacía, ejecutar **docs/database.sql** desde DBeaver,
deteniendo al primer error. Incluye dos secciones: DDL completo y DML de catálogos
más 30 registros ficticios en cada una de las ocho tablas principales. El esquema
usa `DECIMAL(2,1)` para notas y tablas separadas para préstamos de profesores y
estudiantes. No ejecutar el instalador sobre la base actual: el DDL no se revierte
con ROLLBACK.

Para una base existente, las ampliaciones se encuentran en `docs/migrations/`;
revisar antes si ya están instaladas. La base utilizada durante las pruebas ya
contiene esas ampliaciones y el lote LHSEED: no volver a cargarlos.

Verificar con `docs/seed_checks.sql` y `docs/database_checks.sql`.
El lote LHSEED contiene datos ficticios. `docs/seed_data.sql` permite cargarlo
por separado solo en una base con esquema y catálogos que todavía no lo tenga.
No ejecutar esa carga después de instalar `docs/database.sql`, que ya lo incluye.

### 5.2. Credenciales

En IntelliJ: **Run → Edit Configurations → Main → Environment variables**:

```text
DB_URL=jdbc:mysql://127.0.0.1:3306/erp_little_horizons
DB_USER=<usuario local>
DB_PASSWORD=<contraseña local>
```

`DatabaseConnection.open()` lee estas variables. No hay credenciales en el código
ni carga automática de `.env`. No subir contraseñas al repositorio.

### 5.3. Driver JDBC

Recargar el proyecto Maven para resolver `com.mysql:mysql-connector-j`, ya declarado
en `pom.xml`. Ejecutar Main desde el módulo Maven para incluir el driver.
No hace falta copiar manualmente el JAR dentro de `src/`.

## 6. Instalación y ejecución

### 6.1. Clonar

Requiere acceso al repositorio:

```sh
git clone https://github.com/stivenurbina044-ops/proyecto-Java.git
cd proyecto-Java
```

### 6.2. Abrir en el IDE

Abrir la carpeta que contiene `pom.xml` como proyecto Maven y seleccionar JDK 17.
Configurar las variables anteriores en la ejecución de `com.littlehorizons.erp.Main`.

### 6.3. Compilar

```sh
mvn package
```

El empaquetado aún debe confirmarse con `BUILD SUCCESS` en el entorno de entrega.
En el entorno del agente no se pudo resolver Maven Central por restricciones de
red; los scripts locales sí compilan el código Java con `javac --release 17`.

### 6.4. Ejecutar

Desde IntelliJ, ejecutar `Main.main()`. El directorio de trabajo debe ser la raíz
del proyecto para encontrar logs y PDFs con las rutas documentadas. El JAR normal
no incluye sus dependencias; no se presenta como un ejecutable autónomo.

Pruebas locales:

```sh
bash scripts/check_student_billing.sh
bash scripts/check_invoices.sh
bash scripts/check_academic_statistics.sh
```

Los casos JDBC usan dobles y no acceden a MySQL. Las pruebas manuales realizadas
en MySQL cubrieron abonos, liquidación, rechazos de aplicaciones incompatibles,
recuperación de factura y límites del reporte de vencidos.

## 7. Estructura del proyecto

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

La estructura Maven añade `main/java` respecto al ejemplo del enunciado.
Las vistas no ejecutan SQL; los DAO devuelven datos y no leen consola.
Los nombres Java y sus comentarios se escriben en inglés. La interfaz de usuario
está en español y los nombres SQL conservan los del esquema del proyecto.

## 8. Principios y patrones de diseño aplicados

### 8.1. SOLID

| Principio | Aplicación concreta y alcance |
| --- | --- |
| Responsabilidad única | FinanceView presenta; PaymentSummaryService calcula totales; PaymentDao persiste |
| Abierto/cerrado | InvoiceService recibe InvoiceDocumentWriter; puede sustituirse el renderizador sin editar la coordinación de generación |
| Sustitución de Liskov | InvoicePdfWriter implementa el contrato render: recibe datos y devuelve bytes sin persistir archivos ni registros |
| Segregación de interfaces | InvoiceDocumentWriter tiene una sola operación, sin exigir al renderizador acceso JDBC o lectura de consola |
| Inversión de dependencias | InvoiceService depende del contrato de renderizado y Main proporciona InvoicePdfWriter; los DAO siguen siendo colaboradores concretos |

La aplicación de estos principios es parcial y concreta: no se afirma que todos
los colaboradores estén abstraídos ni que una interfaz por sí sola demuestre LSP.

### 8.2. Patrones

- **MVC:** vistas de consola, modelos y controladores separados.
- **DAO:** StudentDao, PaymentDao y otros encapsulan JDBC y SQL parametrizado.
- **Estrategia de renderizado:** InvoiceDocumentWriter permite inyectar la forma de
  producir los bytes de la factura; InvoicePdfWriter es la implementación actual.
- **Inyección por constructor:** Main conecta controladores, vistas, DAO y servicios.

`DatabaseConnection` es una utilidad con constructor privado y método estático;
**no es un Singleton**, y abre conexiones que cada operación debe cerrar.

## 9. Funcionalidades principales

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

## 10. Manejo de errores y logs

Los controladores capturan SQLException, muestran un mensaje y llaman a
`ErrorLogger.log`. El archivo `error_log.txt` se crea en el directorio de ejecución,
con fecha, contexto y traza; agrega entradas sin borrar las previas. Está excluido
de Git. Si no se puede escribir, se avisa por stderr y se permite continuar.
Las entradas inválidas se reintentan y las reglas de dominio usan excepciones
controladas. Los formularios permiten cancelar según los mensajes de cada menú.

Las consultas enlazan valores con PreparedStatement y cierran recursos mediante
try-with-resources. Las operaciones relacionadas usan transacciones y rollback;
los préstamos, aplicaciones y otros flujos emplean bloqueos de filas.

## 11. Capturas de pantalla

Pendientes de incorporar. Son opcionales según el enunciado. No incluir datos
personales ni credenciales en las capturas de entrega.

## 12. Problemas conocidos y limitaciones

- Facturación y CUFE son simulaciones académicas; no hay validación DIAN.
- El lote se carga por SQL: sus pagos no generan PDF hasta solicitarlos en consola.
- No hay edición o eliminación general de todas las entidades. No se afirma
  cumplimiento completo del checklist de CRUD; falta contrastar las operaciones
  exigidas en la descripción funcional completa.
- No hay autenticación, deshacer aplicaciones ni mantenimiento/baja de recursos
  desde consola. Se conserva historial y se restringen cambios relacionados.
- La integración manual no cubre concurrencia real ni todas las variantes de fallo.
- Falta validar desde cero la revisión del DML incorporada al instalador.

## 13. Futuras mejoras

Completar operaciones de actualización requeridas, ampliar pruebas de integración,
reducir duplicación de entrada y abstraer persistencia cuando resulte útil.

## 14. Licencia

No se ha elegido una licencia para el código propio: no se concede aquí una
licencia de reutilización. La biblioteca QR conserva su licencia MIT en los
archivos originales; ver [atribuciones](docs/third_party.md).

## 15. Contacto

Correo registrado del autor: **stivenurbina044@gmail.com**.
Repositorio: [proyecto-Java](https://github.com/stivenurbina044-ops/proyecto-Java).
La privacidad del repositorio y el acceso del trainer deben verificarse antes de
entregar. La rama local actual es `main`; no se ha creado el commit final.
Para obtener el hash completo después de confirmar y publicar la entrega:

```sh
git rev-parse HEAD
```

El hash de entrega debe corresponder al commit final publicado en la rama principal.
