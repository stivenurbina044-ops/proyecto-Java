package com.littlehorizons.erp.service;

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
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int guardianId = guardianDao.findGuardianIdByDocument(connection, guardianDocumentNumber)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "No existe un acudiente con ese documento."));
                int studentId = studentDao.insertStudent(connection, documentType, documentNumber,
                        firstName, lastName, birthDate, gender, admissionDate);
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
