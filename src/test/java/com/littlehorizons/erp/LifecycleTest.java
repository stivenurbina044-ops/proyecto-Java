package com.littlehorizons.erp;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.ResourceDao;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import junit.framework.TestCase;

/** Checks transaction decisions without connecting to a database. */
public final class LifecycleTest extends TestCase {
    private final List<String> writes = new ArrayList<>();
    private boolean futureEnrollment;
    private boolean outstandingLoan;
    private String resourceStatus = "Disponible";
    private boolean committed;
    private boolean rolledBack;
    private Driver driver;
    private final EnrollmentDao enrollments = new EnrollmentDao();

    @Override
    protected void setUp() throws Exception {
        driver = new Driver() {
            public Connection connect(String url, Properties properties) {
                if (!acceptsURL(url)) { return null; }
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            return switch (method.getName()) {
                                case "prepareStatement" -> statement((String) args[0]);
                                case "commit" -> { committed = true; yield null; }
                                case "rollback" -> { rolledBack = true; yield null; }
                                default -> defaultValue(method.getReturnType());
                            };
                        });
            }
            public boolean acceptsURL(String url) { return "jdbc:lifecycle-check".equals(url); }
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(driver);
    }

    @Override
    protected void tearDown() throws Exception { DriverManager.deregisterDriver(driver); }

    private PreparedStatement statement(String sql) {
        Map<Integer, Object> parameters = new HashMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getName().startsWith("set")) {
                        parameters.put((Integer) args[0], args[1]);
                        return null;
                    }
                    if (method.getName().equals("executeUpdate")) {
                        writes.add(sql + parameters);
                        return 1;
                    }
                    if (method.getName().equals("executeQuery")) {
                        boolean hasRow = sql.contains("FROM estudiantes") || sql.contains("FROM recursos")
                                || (sql.contains("FROM matriculas") && futureEnrollment)
                                || (sql.contains("FROM prestamos_") && outstandingLoan);
                        boolean[] consumed = {false};
                        return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                                (resultProxy, resultMethod, resultArgs) -> {
                                    return switch (resultMethod.getName()) {
                                        case "next" -> { boolean next = hasRow && !consumed[0]; consumed[0] = true; yield next; }
                                        case "getInt" -> 42;
                                        case "getString" -> resourceStatus;
                                        default -> defaultValue(resultMethod.getReturnType());
                                    };
                                });
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) { return false; }
        if (type == int.class) { return 0; }
        return null;
    }

    public void testGraduationClosesOlderEnrollments() throws Exception {
        enrollments.closeEnrollment("1026001", 2026, true);
        assertTrue(committed);
        assertFalse(rolledBack);
        assertTrue(writes.stream().anyMatch(sql -> sql.contains("UPDATE matriculas") && sql.contains("`año_academico` < ?")));
        assertTrue(writes.stream().anyMatch(sql -> sql.contains("UPDATE inscripciones_materia") && sql.contains("`año_academico` < ?")));
        assertTrue(writes.stream().anyMatch(sql -> sql.contains("UPDATE estudiantes") && sql.contains("Graduado")));
    }

    public void testFutureEnrollmentRejectsHistoricalGraduation() throws Exception {
        futureEnrollment = true;
        try {
            enrollments.closeEnrollment("1026001", 2025, true);
            fail("Future active enrollment must block graduation");
        } catch (IllegalArgumentException expected) {
            assertTrue(rolledBack);
            assertFalse(committed);
            assertTrue(writes.isEmpty());
        }
    }

    public void testWithdrawalPreservesOtherAcademicYears() throws Exception {
        enrollments.closeEnrollment("1026001", 2026, false);
        assertTrue(committed);
        assertFalse(writes.stream().anyMatch(sql -> sql.contains("`año_academico` < ?")));
        assertTrue(writes.stream().anyMatch(sql -> sql.contains("NOT EXISTS") && sql.contains("Retirado")));
    }

    public void testReactivationDoesNotReopenHistoricalEnrollments() throws Exception {
        enrollments.reactivate("1026001");
        assertEquals(1, writes.size());
        assertTrue(writes.get(0).contains("estado IN ('Retirado', 'Suspendido')"));
    }

    public void testMaintenanceRejectsOutstandingLoan() throws Exception {
        outstandingLoan = true;
        try {
            new ResourceDao().changeStatus(1, "En Mantenimiento");
            fail("Outstanding loan must block maintenance");
        } catch (IllegalArgumentException expected) {
            assertTrue(rolledBack);
            assertTrue(writes.isEmpty());
        }
    }

    public void testRetiredResourceCannotBeReactivated() throws Exception {
        resourceStatus = "Dado de Baja";
        try {
            new ResourceDao().changeStatus(1, "Disponible");
            fail("Retirement must be final");
        } catch (IllegalArgumentException expected) {
            assertTrue(rolledBack);
            assertTrue(writes.isEmpty());
        }
    }

    public void testAvailableResourceCanEnterMaintenance() throws Exception {
        new ResourceDao().changeStatus(1, "En Mantenimiento");
        assertTrue(committed);
        assertTrue(writes.stream().anyMatch(sql -> sql.contains("En Mantenimiento")));
    }
}
