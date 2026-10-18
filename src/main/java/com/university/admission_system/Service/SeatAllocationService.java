
package com.university.admission_system.service;

import com.university.admission_system.model.Student;
import com.university.admission_system.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SeatAllocationService {

    private final StudentRepository studentRepository;
    private final EmailOtpService emailOtpService;

    private final Map<String, Integer> seatLimits = Map.ofEntries(
        Map.entry("BSc Information Technology", 2),
        Map.entry("BSc Computer Science", 2),
        Map.entry("BSc Mathematics", 2),
        Map.entry("BSc Physics", 2),
        Map.entry("BSc Chemistry", 2),
        Map.entry("BSc Statistics", 2),
        Map.entry("BSc Data Science", 2),
        Map.entry("BSc Artificial Intelligence", 2),
        Map.entry("BSc Cyber Security", 2),
        Map.entry("BSc Biotechnology", 2),
        Map.entry("BSc Microbiology", 2),
        Map.entry("BSc Botany", 2),
        Map.entry("BSc Zoology", 2),
        Map.entry("BSc Environmental Science", 2),

        Map.entry("BCA", 2),
        Map.entry("MCA", 2),
        Map.entry("BTech Computer Science", 2),
        Map.entry("BTech Information Technology", 2),
        Map.entry("BTech Artificial Intelligence", 2),
        Map.entry("BTech Data Science", 2),
        Map.entry("BTech Electronics", 2),
        Map.entry("BTech Mechanical Engineering", 2),
        Map.entry("BTech Civil Engineering", 2),
        Map.entry("BTech Electrical Engineering", 2),

        Map.entry("BCom", 2),
        Map.entry("BCom Accounting and Finance", 2),
        Map.entry("BCom Banking and Insurance", 2),
        Map.entry("BCom Financial Markets", 2),
        Map.entry("BMS", 2),
        Map.entry("BBA", 2),
        Map.entry("MBA", 2),
        Map.entry("BAF", 2),
        Map.entry("BBI", 2),
        Map.entry("BFM", 2),

        Map.entry("BA", 2),
        Map.entry("BA English", 2),
        Map.entry("BA Psychology", 2),
        Map.entry("BA Economics", 2),
        Map.entry("BA Sociology", 2),
        Map.entry("BA Political Science", 2),
        Map.entry("BA History", 2),
        Map.entry("BA Geography", 2),
        Map.entry("BA Philosophy", 2),

        Map.entry("Bachelor of Design", 2),
        Map.entry("Bachelor of Journalism", 2),
        Map.entry("Bachelor of Social Work", 2),
        Map.entry("Bachelor of Hotel Management", 2),
        Map.entry("Bachelor of Education", 2),
        Map.entry("Bachelor of Pharmacy", 2),
        Map.entry("Bachelor of Architecture", 2),
        Map.entry("Bachelor of Law", 2)
    );

    public SeatAllocationService(
            StudentRepository studentRepository,
            EmailOtpService emailOtpService) {
        this.studentRepository = studentRepository;
        this.emailOtpService = emailOtpService;
    }

    @Transactional
    public void allocateSeats() {

        List<Student> students = studentRepository.findAll();

        students.sort(
            Comparator.comparingDouble(Student::getMarks)
                .reversed()
                .thenComparing(Student::getId)
        );

        // Keep the previous allocation algorithm.
        for (Student student : students) {
            student.setAllocationStatus("NOT_ALLOCATED");
            student.setAllocatedCourse(null);
        }

        for (Map.Entry<String, Integer> entry : seatLimits.entrySet()) {

            String courseName = entry.getKey();
            int seatLimit = entry.getValue();
            int allocatedCount = 0;

            for (Student student : students) {

                if (courseName.equals(student.getCourse())
                        && allocatedCount < seatLimit) {

                    student.setAllocationStatus("ALLOCATED");
                    student.setAllocatedCourse(courseName);
                    allocatedCount++;
                }
            }
        }

        // Save the allocation results first.
        List<Student> savedStudents =
                studentRepository.saveAll(students);

        // Send notifications only after the transaction commits.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (Student student : savedStudents) {
                            if ("ALLOCATED".equals(
                                    student.getAllocationStatus())
                                    && student.getEmail() != null
                                    && !student.getEmail().isBlank()) {
                                try {
                                    emailOtpService.sendSeatAllocationEmail(
                                        student.getEmail(),
                                        student.getFullName(),
                                        student.getAllocatedCourse()
                                    );
                                } catch (RuntimeException ex) {
                                    // Email failure must not undo a successful allocation.
                                    System.err.println(
                                        "Could not send allocation email to "
                                        + student.getEmail()
                                    );
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            );
        }
    }

    public Map<String, Integer> getSeatLimits() {
        return seatLimits;
    }
}
