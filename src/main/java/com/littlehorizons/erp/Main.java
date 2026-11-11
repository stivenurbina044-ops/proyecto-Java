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
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.ClassScheduleDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.view.TeacherView;
import java.util.Scanner;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.ResourceLoanDao;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.view.EventView;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.view.ClubView;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.view.AttendanceView;

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
            var attendance = new AttendanceController(view, new AttendanceView(input, System.out), new AttendanceDao());
            var academic = new AcademicController(view, new SubjectRegistrationDao(),
                    new AcademicRecordDao(), new AcademicAverageService(), attendance);
            var teachers = new TeacherController(view, new TeacherView(input, System.out), new TeacherDao(), new TeacherAssignmentDao(), new ClassScheduleDao());
            var events = new EventController(view, new EventView(input, System.out),
                    new TeacherView(input, System.out), new TeacherDao(), new EventDao());
            var clubs = new ClubController(view, new ClubView(input, System.out),
                    new TeacherView(input, System.out), new ClubDao(), events);
            var resources = new ResourceController(view, new ResourceView(input, System.out),
                    new ResourceDao(), new ResourceLoanDao());
            new MainMenuController(view, studentDao, guardianDao, gradeDao,
                    registrationView, registration, enrollment, academic, teachers, clubs, resources).run();
        }
    }
}
