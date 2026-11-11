package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.MainMenuController;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.service.AcademicAverageService;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.service.StudentRegistrationService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.StudentRegistrationView;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            ConsoleView view = new ConsoleView(input, System.out);
            StudentDao studentDao = new StudentDao();
            GuardianDao guardianDao = new GuardianDao();
            var registration = new StudentRegistrationService(
                    studentDao, guardianDao, new StudentGuardianDao());
            var registrationView = new StudentRegistrationView(input, System.out);
            GradeDao gradeDao = new GradeDao();
            var enrollment = new EnrollmentController(view, new EnrollmentDao(), gradeDao);
            var academic = new AcademicController(view, new SubjectRegistrationDao(),
                    new AcademicRecordDao(), new AcademicAverageService());
            new MainMenuController(view, studentDao, guardianDao, gradeDao,
                    registrationView, registration, enrollment, academic).run();
        }
    }
}
