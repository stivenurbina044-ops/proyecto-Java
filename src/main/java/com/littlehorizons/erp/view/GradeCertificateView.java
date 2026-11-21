package com.littlehorizons.erp.view;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.Scanner;

public final class GradeCertificateView {
    private static final String RULE = "=".repeat(66);
    private final Scanner input;
    private final PrintStream output;
    private final ConsoleView console;

    public GradeCertificateView(Scanner input, PrintStream output, ConsoleView console) {
        this.input = input;
        this.output = output;
        this.console = console;
    }

    public OptionalInt readStudentId() {
        while (true) {
            output.print("Ingrese el ID del estudiante para generar su certificado: ");
            if (!input.hasNextLine()) {
                return OptionalInt.empty();
            }
            String value = input.nextLine().strip();
            try {
                if (value.matches("[0-9]+")) {
                    int id = Integer.parseInt(value);
                    if (id > 0) {
                        return OptionalInt.of(id);
                    }
                }
            } catch (NumberFormatException exception) {
                // The value exceeded the range of a database ID.
            }
            showError("Debe ingresar un número válido.");
        }
    }

    public OptionalInt readAcademicYear() {
        return console.readAcademicYear();
    }

    public void showError(String message) {
        output.println("Error: " + message);
    }

    public void showProgress(int year) {
        output.printf("Generando certificado para el año %d...%nRecopilando información académica...%n%n", year);
    }

    public void showSuccess(String name, Path file, int year) {
        output.printf("""
                %s
                            CERTIFICADO GENERADO EXITOSAMENTE
                %s

                Estudiante: %s
                Archivo generado: %s
                Ubicación: %s

                El certificado del año %d está listo para imprimir.

                """, RULE, RULE, name, file.getFileName(), file, year);
    }

    public void showFileError() {
        output.printf("""
                %s
                ERROR: No se pudo crear el archivo del certificado.
                Verifique los permisos de escritura en la carpeta del proyecto.
                %s
                """, RULE, RULE);
    }

    public void waitForEnter() {
        output.println(RULE);
        output.println("Presione Enter para volver al menú principal...");
        output.println(RULE);
        while (input.hasNextLine() && !input.nextLine().isBlank()) {
            // Wait for Enter or end of input.
        }
    }
}
