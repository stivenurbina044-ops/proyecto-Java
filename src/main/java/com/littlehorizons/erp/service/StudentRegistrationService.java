package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.StudentRegistration;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.time.LocalDate;

public final class StudentRegistrationService {
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final StudentGuardianDao studentGuardianDao;

    public StudentRegistrationService(StudentDao studentDao, GuardianDao guardianDao,
                                      StudentGuardianDao studentGuardianDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.studentGuardianDao = studentGuardianDao;
    }

    public int registerWithExistingGuardian(String documentType, String documentNumber,
                                            String firstName, String lastName, LocalDate birthDate,
                                            String gender, LocalDate admissionDate,
                                            String guardianDocumentNumber) throws SQLException {
        return registerWithExistingGuardian(new StudentRegistration(documentType, documentNumber,
                firstName, lastName, birthDate, gender, admissionDate, guardianDocumentNumber,
                null, null, null));
    }

    public void update(String document, StudentRegistration student) throws SQLException {
        if (!document.equals(student.documentNumber())) {
            throw new IllegalArgumentException("El documento del estudiante no se puede cambiar en esta operación.");
        }
        studentDao.update(student);
    }

    public int registerWithExistingGuardian(StudentRegistration student) throws SQLException {
        String guardianDocumentNumber = student.guardianDocumentNumber();
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int guardianId = guardianDao.findGuardianIdByDocument(connection, guardianDocumentNumber)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No existe un acudiente con ese documento."));
                int studentId = studentDao.insertStudent(connection, student);
                studentGuardianDao.linkPrimaryGuardian(connection, studentId, guardianId);
                connection.commit();
                return studentId;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }
}
