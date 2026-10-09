
package com.university.admission_system.controller;

import com.university.admission_system.model.Student;
import com.university.admission_system.repository.StudentRepository;
import com.university.admission_system.service.SeatAllocationService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdmissionController {

    private final StudentRepository studentRepository;
    private final SeatAllocationService seatAllocationService;

    // Constructor
    public AdmissionController(
            StudentRepository studentRepository,
            SeatAllocationService seatAllocationService) {

        this.studentRepository = studentRepository;
        this.seatAllocationService = seatAllocationService;
    }

    // Home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Display student admission form
    @GetMapping("/admission")
    public String admissionForm(Model model) {

        model.addAttribute("student", new Student());

        return "admission";
    }

    // Save student admission details
    @PostMapping("/admission")
    public String submitAdmission(
            @ModelAttribute Student student,
            Model model) {

        student.setAllocationStatus("PENDING");
        student.setAllocatedCourse(null);

        studentRepository.save(student);

        model.addAttribute("student", student);

        return "success";
    }

    // Display admin login page
    @GetMapping("/admin")
    public String adminLogin() {
        return "admin";
    }

    // Verify admin credentials
    @PostMapping("/admin/login")
    public String processAdminLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if ("admin".equals(username)
                && "Admin@123".equals(password)) {

            session.setAttribute("adminLoggedIn", true);

            return "redirect:/admin/dashboard";
        }

        session.removeAttribute("adminLoggedIn");

        model.addAttribute(
                "error",
                "Invalid admin username or password"
        );

        return "admin";
    }

    // Display admin dashboard
    @GetMapping("/admin/dashboard")
    public String adminDashboard(
            HttpSession session,
            Model model) {

        Boolean loggedIn =
                (Boolean) session.getAttribute("adminLoggedIn");

        if (!Boolean.TRUE.equals(loggedIn)) {
            return "redirect:/admin";
        }

        model.addAttribute(
                "students",
                studentRepository.findAll()
        );

        return "dashboard";
    }

    // Allocate seats (admin only)
    @PostMapping("/admin/allocate")
    public String allocateSeats(HttpSession session) {

        Boolean loggedIn =
                (Boolean) session.getAttribute("adminLoggedIn");

        if (!Boolean.TRUE.equals(loggedIn)) {
            return "redirect:/admin";
        }

        seatAllocationService.allocateSeats();

        return "redirect:/admin/dashboard";
    }

    // Open Check Seat Allocation page
    @GetMapping("/check-allocation")
    public String checkAllocationPage() {
        return "check-allocation";
    }

    // Search for a student's allocation result
    @PostMapping("/check-allocation")
    public String checkAllocationResult(
            @RequestParam String email,
            Model model) {

        Student student = studentRepository.findAll()
                .stream()
                .filter(s -> s.getEmail() != null
                        && s.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst()
                .orElse(null);

        if (student == null) {
            model.addAttribute(
                    "error",
                    "No application found with this email address."
            );

            return "check-allocation";
        }

        model.addAttribute("student", student);

        return "result";
    }

    // Admin logout
    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {

        session.invalidate();

        return "redirect:/admin";
    }
}