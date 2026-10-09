
package com.university.admission_system.service;

import com.university.admission_system.model.Student;
import com.university.admission_system.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SeatAllocationService {

    private final StudentRepository studentRepository;

    // Sample seat limits for testing
    private final Map<String, Integer> seatLimits = Map.ofEntries(
        Map.entry("BSc Information Technology", 2),
        Map.entry("BSc Computer Science", 2),
        Map.entry("BCA", 2),
        Map.entry("BTech Computer Science", 2),
        Map.entry("BTech Information Technology", 2),
        Map.entry("BCom", 2),
        Map.entry("BMS", 2),
        Map.entry("BBA", 2),
        Map.entry("BA", 2),
        Map.entry("BSc Mathematics", 2),
        Map.entry("BSc Data Science", 2),
        Map.entry("BSc Artificial Intelligence", 2)
    );

    public SeatAllocationService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public void allocateSeats() {

        List<Student> students = studentRepository.findAll();

        // Rank students from highest marks to lowest
        students.sort(
            Comparator.comparingDouble(Student::getMarks).reversed()
        );

        // Reset previous allocation results
        for (Student student : students) {
            student.setAllocationStatus("NOT_ALLOCATED");
            student.setAllocatedCourse(null);
        }

        // Allocate seats separately for each course
        for (Map.Entry<String, Integer> entry : seatLimits.entrySet()) {

            String courseName = entry.getKey();
            int seatLimit = entry.getValue();
            int count = 0;

            for (Student student : students) {

                if (courseName.equals(student.getCourse())
                        && count < seatLimit) {

                    student.setAllocationStatus("ALLOCATED");
                    student.setAllocatedCourse(courseName);
                    count++;
                }
            }
        }

        // Save results in the database
        studentRepository.saveAll(students);
    }
}