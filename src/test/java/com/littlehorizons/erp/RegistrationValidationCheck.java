package com.littlehorizons.erp;

import com.littlehorizons.erp.view.StudentRegistrationView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

/** Form regression checks without database access. */
public final class RegistrationValidationCheck {
    public static void main(String[] args) {
        var output = new PrintStream(new ByteArrayOutputStream());
        String studentInput = String.join("\n", "Pasaporte", "AB123456", "Ana", "María", "Rojas", "Pérez",
                "Carrera 18 No. 24-32", "2016-05-10", "F", "2026-01-15", "5226001") + "\n";
        var student = new StudentRegistrationView(new Scanner(studentInput), output).readRegistration().orElseThrow();
        require(student.documentNumber().equals("AB123456"), "Alphanumeric passport rejected");
        require(student.middleName().equals("María") && student.secondLastName().equals("Pérez"), "Incomplete name");
        require(student.address().equals("Carrera 18 No. 24-32"), "Student address missing");

        String guardianInput = String.join("\n", "CC", "ABC", "5226001", "Mariana", "", "Rojas", "",
                "", "Calle 42 No. 15-20", "Madre", "123", "3004567890", "S") + "\n";
        var guardian = new StudentRegistrationView(new Scanner(guardianInput), output).readGuardianRegistration().orElseThrow();
        require(guardian.documentNumber().equals("5226001"), "Invalid document was accepted");
        require(guardian.primaryPhone().equals("3004567890"), "Invalid phone was accepted");
        require(guardian.address().equals("Calle 42 No. 15-20"), "Required address retry failed");
        require(guardian.middleName() == null && guardian.secondLastName() == null, "Optional names failed");
        require(new StudentRegistrationView(new Scanner("/volver\n"), output).readRegistration().isEmpty(), "Cancellation failed");
        require(new StudentRegistrationView(new Scanner(""), output).readRegistration().isEmpty(), "EOF failed");
        System.out.println("Registration checks passed: passport, full names, address, retries, cancellation and EOF.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
