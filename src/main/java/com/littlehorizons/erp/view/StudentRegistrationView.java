package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.GuardianRegistration;
import com.littlehorizons.erp.model.StudentRegistration;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

public final class StudentRegistrationView {
    private static final Pattern DOCUMENT_NUMBER = Pattern.compile("[0-9]+");
    private static final Pattern GUARDIAN_DOCUMENT_NUMBER = Pattern.compile("[0-9]{2,}");
    private static final Pattern PASSPORT_NUMBER = Pattern.compile("[A-Za-z0-9]{2,}");
    private static final Pattern PHONE_NUMBER = Pattern.compile("[+]?[0-9]{7,15}");
    private static final Pattern PERSON_NAME =
            Pattern.compile("\\p{L}[\\p{L}\\p{M}]*(?:[ '\u2019-]\\p{L}[\\p{L}\\p{M}]*)*");

    private final Scanner input;
    private final PrintStream output;

    public StudentRegistrationView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public Optional<StudentRegistration> readRegistration() {
        output.println("Registro con acudiente existente. Escribe /volver para cancelar.");
        try {
            String documentType = readChoice("Tipo de documento (TI, CC, CE, Pasaporte)",
                    List.of("TI", "CC", "CE", "Pasaporte"));
            String documentNumber = readMatching("Número de documento del estudiante", 20,
                    documentType.equals("Pasaporte") ? PASSPORT_NUMBER : DOCUMENT_NUMBER,
                    "Usa dígitos para el documento; el pasaporte también admite letras.");
            String firstName = readName("Primer nombre");
            String middleName = readOptionalName("Segundo nombre (opcional)");
            String lastName = readName("Primer apellido");
            String secondLastName = readOptionalName("Segundo apellido (opcional)");
            String address = readRequired("Dirección", 255);
            LocalDate birthDate = readDate("Fecha de nacimiento", null);
            String gender = readChoice("Género (M, F, Otro)", List.of("M", "F", "Otro"));
            LocalDate admissionDate = readDate("Fecha de ingreso", birthDate);
            String guardianDocument = readRequired("Número de documento del acudiente", 20);
            return Optional.of(new StudentRegistration(documentType, documentNumber, firstName,
                    lastName, birthDate, gender, admissionDate, guardianDocument, middleName, secondLastName, address));
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    public Optional<GuardianRegistration> readGuardianRegistration() {
        output.println("Registro de acudiente. Escribe /volver para cancelar.");
        try {
            String documentType = readChoice("Tipo de documento (CC, CE, Pasaporte)",
                    List.of("CC", "CE", "Pasaporte"));
            boolean passport = documentType.equals("Pasaporte");
            String documentNumber = readMatching("Número de documento del acudiente", 20,
                    passport ? PASSPORT_NUMBER : GUARDIAN_DOCUMENT_NUMBER,
                    passport ? "El pasaporte debe tener de 2 a 20 letras o dígitos."
                            : "El documento del acudiente debe contener de 2 a 20 dígitos.");
            String firstName = readName("Primer nombre");
            String middleName = readOptionalName("Segundo nombre (opcional)");
            String lastName = readName("Primer apellido");
            String secondLastName = readOptionalName("Segundo apellido (opcional)");
            String address = readRequired("Dirección", 255);
            String relationship = readMatching("Parentesco", 50, PERSON_NAME,
                    "Ingresa un parentesco como Madre, Padre o Tutor legal.");
            String phone = readMatching("Teléfono principal", 20, PHONE_NUMBER,
                    "Ingresa de 7 a 15 dígitos. Puedes usar + al inicio.");
            String responsible = readChoice("¿Es responsable financiero? (S/N)", List.of("S", "N"));
            return Optional.of(new GuardianRegistration(documentType, documentNumber, firstName,
                    lastName, relationship, phone, responsible.equals("S"), middleName, secondLastName, address));
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    private String readRequired(String prompt, int maxLength) {
        while (true) {
            output.print(prompt + ": ");
            if (!input.hasNextLine()) {
                throw new RegistrationCancelled();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                throw new RegistrationCancelled();
            }
            if (!value.isEmpty() && value.length() <= maxLength) {
                return value;
            }
            output.printf("Ingresa entre 1 y %d caracteres.%n", maxLength);
        }
    }

    private String readOptionalName(String prompt) {
        while (true) {
            output.print(prompt + ": ");
            if (!input.hasNextLine()) {
                throw new RegistrationCancelled();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                throw new RegistrationCancelled();
            }
            if (value.isEmpty() || (value.length() <= 100 && PERSON_NAME.matcher(value).matches())) {
                return value.isEmpty() ? null : value;
            }
            output.println("Usa letras y máximo 100 caracteres, o Enter para omitir.");
        }
    }

    private String readName(String prompt) {
        return readMatching(prompt, 100, PERSON_NAME,
                "Usa letras. Puedes separar las partes con un espacio, guion o apóstrofo.");
    }

    private String readMatching(String prompt, int maxLength, Pattern pattern, String errorMessage) {
        while (true) {
            String value = readRequired(prompt, maxLength);
            if (pattern.matcher(value).matches()) {
                return value;
            }
            output.println(errorMessage);
        }
    }

    private String readChoice(String prompt, List<String> choices) {
        while (true) {
            String value = readRequired(prompt, 20);
            for (String choice : choices) {
                if (choice.equalsIgnoreCase(value)) {
                    return choice;
                }
            }
            output.println("Ingresa una de las opciones indicadas.");
        }
    }

    private LocalDate readDate(String prompt, LocalDate minimum) {
        while (true) {
            String value = readRequired(prompt + " (AAAA-MM-DD)", 10);
            try {
                LocalDate date = LocalDate.parse(value);
                LocalDate today = LocalDate.now();
                if (date.getYear() < 1000) {
                    output.println("Ingresa un año de cuatro cifras a partir de 1000.");
                } else if (minimum == null && date.isAfter(today)) {
                    output.println("La fecha de nacimiento no puede estar en el futuro.");
                } else if (minimum != null && date.getYear() > today.getYear()) {
                    output.println("La fecha de ingreso no puede ser de un año posterior al actual.");
                } else if (minimum != null && date.isBefore(minimum)) {
                    output.println("La fecha de ingreso no puede ser anterior al nacimiento.");
                } else {
                    return date;
                }
            } catch (DateTimeParseException exception) {
                output.println("Ingresa una fecha válida con el formato AAAA-MM-DD.");
            }
        }
    }

    private static final class RegistrationCancelled extends RuntimeException {
    }
}
