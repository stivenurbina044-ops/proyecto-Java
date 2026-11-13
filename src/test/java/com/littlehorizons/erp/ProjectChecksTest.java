package com.littlehorizons.erp;

import junit.framework.TestCase;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/** Runs each JDBC double in its own process so environment and drivers stay isolated. */
public final class ProjectChecksTest extends TestCase {
    public void testInvoices() throws Exception { runCheck("check_invoices.sh"); }
    public void testBilling() throws Exception { runCheck("check_student_billing.sh"); }
    public void testStatistics() throws Exception { runCheck("check_academic_statistics.sh"); }
    public void testRegistration() throws Exception { runCheck("check_registration.sh"); }

    private void runCheck(String script) throws Exception {
        var log = Files.createTempFile("little-horizons-check-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder("bash", "scripts/" + script)
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            assertTrue("Check timed out: " + script, process.waitFor(60, TimeUnit.SECONDS));
            assertEquals(Files.readString(log), 0, process.exitValue());
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            Files.deleteIfExists(log);
        }
    }
}
