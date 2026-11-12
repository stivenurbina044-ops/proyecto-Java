package com.littlehorizons.erp;

import com.littlehorizons.erp.dao.StudentBillingDao;
import com.littlehorizons.erp.model.ChargeStatus;
import com.littlehorizons.erp.model.StudentCharge;
import com.littlehorizons.erp.model.StudentRateRegistration;
import com.littlehorizons.erp.view.StudentBillingView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

/** Isolated state-transition checks; no connection to the user's MySQL. */
public final class StudentBillingCheck {
    private static Map<Integer, Integer> applications = new HashMap<>();
    private static boolean failInsert;
    private static int rollbacks;
    private static final Map<Integer, BigDecimal> AMOUNTS = Map.of(
            1, new BigDecimal("100.10"), 2, new BigDecimal("199.90"),
            3, new BigDecimal("200.00"), 4, BigDecimal.ONE);

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) { return false; }
        if (type == int.class) { return 0; }
        return null;
    }

    private static ResultSet result(List<Map<String, Object>> rows) {
        int[] index = {-1};
        return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[] {ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next": return ++index[0] < rows.size();
                        case "getInt": return ((Number) rows.get(index[0]).get(args[0])).intValue();
                        case "getString": return rows.get(index[0]).get(args[0]);
                        case "getBigDecimal": return rows.get(index[0]).get(args[0]);
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static PreparedStatement statement(String sql) {
        Map<Integer, Object> parameters = new HashMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("set")) {
                        parameters.put((Integer) args[0], args[1]);
                        return null;
                    }
                    if (method.getName().equals("executeQuery")) {
                        if (sql.contains("SELECT p.id_estudiante")) {
                            int payment = (Integer) parameters.get(1);
                            if (!AMOUNTS.containsKey(payment) || !"123".equals(parameters.get(2))) {
                                return result(List.of());
                            }
                            return result(List.of(Map.of("id_estudiante", 7, "id_concepto", payment == 4 ? 2 : 1,
                                    "año_academico", 2026, "periodo_academico", "III", "monto_pagado", AMOUNTS.get(payment))));
                        }
                        if (sql.contains("SELECT monto_cobrado")) {
                            boolean matches = Integer.valueOf(10).equals(parameters.get(1))
                                    && Integer.valueOf(7).equals(parameters.get(2)) && Integer.valueOf(1).equals(parameters.get(3))
                                    && Integer.valueOf(2026).equals(parameters.get(4)) && "III".equals(parameters.get(5));
                            return result(matches ? List.of(Map.of("monto_cobrado", new BigDecimal("300.00"))) : List.of());
                        }
                        if (sql.startsWith("SELECT id_pago")) {
                            int payment = (Integer) parameters.get(1);
                            return result(applications.containsKey(payment) ? List.of(Map.of("id_pago", payment)) : List.of());
                        }
                        if (sql.contains("SELECT p.monto_pagado")) {
                            List<Map<String, Object>> rows = new ArrayList<>();
                            for (var entry : applications.entrySet()) {
                                if (entry.getValue().equals(parameters.get(1))) {
                                    rows.add(Map.of("monto_pagado", AMOUNTS.get(entry.getKey())));
                                }
                            }
                            return result(rows);
                        }
                        throw new AssertionError("Unexpected query: " + sql);
                    }
                    if (method.getName().equals("executeUpdate")) {
                        if (failInsert) {
                            throw new SQLException("Injected application failure");
                        }
                        applications.put((Integer) parameters.get(1), (Integer) parameters.get(2));
                        return 1;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    public static void main(String[] args) throws Exception {
        DriverManager.registerDriver(new Driver() {
            @Override public boolean acceptsURL(String url) { return "jdbc:billing-check".equals(url); }
            @Override public Connection connect(String url, Properties properties) {
                if (!acceptsURL(url)) { return null; }
                Map<Integer, Integer> before = new HashMap<>(applications);
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                        (proxy, method, values) -> {
                            if (method.getName().equals("prepareStatement")) { return statement((String) values[0]); }
                            if (method.getName().equals("rollback")) {
                                applications = new HashMap<>(before);
                                rollbacks++;
                            }
                            return defaultValue(method.getReturnType());
                        });
            }
            @Override public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) { return new DriverPropertyInfo[0]; }
            @Override public int getMajorVersion() { return 1; }
            @Override public int getMinorVersion() { return 0; }
            @Override public boolean jdbcCompliant() { return false; }
            @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        });
        StudentBillingDao dao = new StudentBillingDao();
        dao.applyPayment("123", 1, 10);
        require(applications.size() == 1, "Partial payment not applied");
        for (int payment : new int[] {1, 3, 4}) {
            try {
                dao.applyPayment("123", payment, 10);
                throw new AssertionError("Accepted duplicate, excess or incompatible payment: " + payment);
            } catch (IllegalArgumentException expected) {
                require(applications.size() == 1, "Rejected application changed the balance");
            }
        }
        failInsert = true;
        try {
            dao.applyPayment("123", 2, 10);
            throw new AssertionError("Missing insert failure");
        } catch (SQLException expected) {
            require(applications.size() == 1, "Failed application was not rolled back");
        }
        failInsert = false;
        dao.applyPayment("123", 2, 10);
        require(applications.size() == 2 && rollbacks >= 4, "Settlement/recovery failed");
        LocalDate today = LocalDate.of(2026, 11, 11);
        require(ChargeStatus.calculate(BigDecimal.ONE, today, today) == ChargeStatus.PENDING, "Due today is not overdue");
        require(ChargeStatus.calculate(BigDecimal.ONE, today.minusDays(1), today) == ChargeStatus.OVERDUE, "Overdue not detected");
        require(ChargeStatus.calculate(BigDecimal.ZERO, today.minusDays(1), today) == ChargeStatus.PAID, "Zero amount not settled");
        StudentCharge charge = new StudentCharge(10, "123", "Ana", "Pensión", 2026, "III", "NOV",
                new BigDecimal("300.00"), new BigDecimal("100.10"), today, today);
        require(charge.balance().equals(new BigDecimal("199.90")), "Inexact balance");
        var out = new PrintStream(new ByteArrayOutputStream());
        var rate = new StudentBillingView(new Scanner("2026\n-1\n0\nBeca completa\n"), out).readRate().orElseThrow();
        require(rate.amount().signum() == 0, "Full scholarship rejected");
        var form = new StudentBillingView(new Scanner("2026\n3\nNOV\n2026-02-30\n2026-10-10\n"), out)
                .readCharge().orElseThrow();
        require(form.dueDate().equals(LocalDate.of(2026, 10, 10)), "Invalid date not retried");
        require(new StudentBillingView(new Scanner("/volver\n"), out).readRate().isEmpty(), "Rate cancellation failed");
        require(new StudentBillingView(new Scanner(""), out).readCharge().isEmpty(), "Charge EOF failed");
        try {
            new StudentRateRegistration(2026, new BigDecimal("-1"), "Invalid");
            throw new AssertionError("Negative scholarship accepted");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Billing checks passed: partial payment, settlement, duplicate/excess/mismatch rejection, rollback, statuses and forms.");
        System.out.println("JDBC double used. MySQL migration and integration must be checked separately.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
