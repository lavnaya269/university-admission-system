
package com.university.admission_system.controller;

import com.university.admission_system.model.Student;
import com.university.admission_system.repository.StudentRepository;
import com.university.admission_system.service.EmailOtpService;
import com.university.admission_system.service.SeatAllocationService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdmissionController {

    private final StudentRepository studentRepository;
    private final SeatAllocationService seatAllocationService;
    private final EmailOtpService emailOtpService;

    public AdmissionController(
            StudentRepository studentRepository,
            SeatAllocationService seatAllocationService,
            EmailOtpService emailOtpService) {

        this.studentRepository = studentRepository;
        this.seatAllocationService = seatAllocationService;
        this.emailOtpService = emailOtpService;
    }

    // ---------------- HOME ----------------

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // ---------------- ADMISSION ----------------

    @GetMapping("/admission")
    public String admission(Model model) {
        model.addAttribute("student", new Student());
        return "admission";
    }

    @PostMapping("/admission/send-otp")
    public String sendAdmissionOtp(
            @ModelAttribute("student") Student student,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (student.getEmail() == null
                || student.getEmail().isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter your email address."
            );

            return "redirect:/admission";
        }

        String email = student.getEmail().trim();
        student.setEmail(email);

        session.setAttribute("pendingAdmission", student);

        try {
            emailOtpService.sendOtp(
                    email,
                    "ADMISSION",
                    session
            );

            return "redirect:/verify-otp?purpose=ADMISSION";

        } catch (IllegalStateException ex) {

            session.removeAttribute("pendingAdmission");

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/admission";
        }
    }

    // ---------------- OTP VERIFICATION ----------------

    @GetMapping("/verify-otp")
    public String showVerifyOtp(
            @RequestParam("purpose") String purpose,
            HttpSession session,
            Model model) {

        if (!"ADMISSION".equals(purpose)
                && !"LOGIN".equals(purpose)) {
            return "redirect:/";
        }

        if ("ADMISSION".equals(purpose)
                && session.getAttribute("pendingAdmission") == null) {
            return "redirect:/admission";
        }

        if ("LOGIN".equals(purpose)
                && session.getAttribute("loginOtpEmail") == null) {
            return "redirect:/student/login";
        }

        Object email = session.getAttribute(
                purpose + "_otp_email"
        );

        if (!(email instanceof String)) {
            return "ADMISSION".equals(purpose)
                    ? "redirect:/admission"
                    : "redirect:/student/login";
        }

        model.addAttribute("email", email);
        model.addAttribute("purpose", purpose);

        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam("purpose") String purpose,
            @RequestParam("otp") String otp,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!"ADMISSION".equals(purpose)
                && !"LOGIN".equals(purpose)) {
            return "redirect:/";
        }

        Object savedEmail = session.getAttribute(
                purpose + "_otp_email"
        );

        if (!(savedEmail instanceof String)) {
            return "ADMISSION".equals(purpose)
                    ? "redirect:/admission"
                    : "redirect:/student/login";
        }

        String email = (String) savedEmail;

        if (!emailOtpService.verifyOtp(
                otp, email, purpose, session)) {

            model.addAttribute("email", email);
            model.addAttribute("purpose", purpose);

            model.addAttribute(
                    "errorMessage",
                    "Invalid or expired OTP. "
                    + "Please request a new code if needed."
            );

            return "verify-otp";
        }

        if ("ADMISSION".equals(purpose)) {

            Object pending =
                    session.getAttribute("pendingAdmission");

            if (!(pending instanceof Student)) {
                return "redirect:/admission";
            }

            Student student = (Student) pending;

            student.setAllocationStatus("PENDING");
            student.setAllocatedCourse(null);

            studentRepository.save(student);

            session.removeAttribute("pendingAdmission");

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Email verified! Your application "
                    + "was submitted successfully."
            );

            return "redirect:/";
        }

        session.setAttribute("studentLoggedInEmail", email);
        session.removeAttribute("loginOtpEmail");

        return "redirect:/student/home";
    }

    // ---------------- STUDENT LOGIN ----------------

    @GetMapping("/student/login")
    public String studentLogin() {
        return "student-login";
    }

    @PostMapping("/student/login/send-otp")
    public String sendStudentLoginOtp(
            @RequestParam("email") String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (email == null || email.isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter your email."
            );

            return "redirect:/student/login";
        }

        String cleanEmail = email.trim();

        boolean registered = studentRepository.findAll()
                .stream()
                .anyMatch(student ->
                        student.getEmail() != null
                        && student.getEmail()
                                .equalsIgnoreCase(cleanEmail));

        if (!registered) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "No registered student found with this email."
            );

            return "redirect:/student/login";
        }

        session.setAttribute("loginOtpEmail", cleanEmail);

        try {
            emailOtpService.sendOtp(
                    cleanEmail,
                    "LOGIN",
                    session
            );

            return "redirect:/verify-otp?purpose=LOGIN";

        } catch (IllegalStateException ex) {

            session.removeAttribute("loginOtpEmail");

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/student/login";
        }
    }

    @GetMapping("/student/home")
    public String studentHome(
            HttpSession session,
            Model model) {

        Object loginEmail =
                session.getAttribute("studentLoggedInEmail");

        if (!(loginEmail instanceof String)) {
            return "redirect:/student/login";
        }

        Student student = studentRepository.findAll()
                .stream()
                .filter(s -> s.getEmail() != null
                        && s.getEmail().equalsIgnoreCase(
                                (String) loginEmail))
                .findFirst()
                .orElse(null);

        if (student == null) {
            session.removeAttribute("studentLoggedInEmail");
            return "redirect:/student/login";
        }

        model.addAttribute("student", student);

        return "student-home";
    }

    @GetMapping("/student/logout")
    public String studentLogout(HttpSession session) {
        session.removeAttribute("studentLoggedInEmail");
        return "redirect:/student/login";
    }

    // ---------------- ADMIN LOGIN ----------------

    @GetMapping("/admin")
    public String adminLogin() {
        return "admin";
    }

    @PostMapping("/admin/login")
    public String adminLoginProcess(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if ("admin".equals(username)
                && "Admin@123".equals(password)) {

            session.setAttribute("adminLoggedIn", true);

            return "redirect:/admin/dashboard";
        }

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                "Invalid username or password."
        );

        return "redirect:/admin";
    }

    // ---------------- ADMIN DASHBOARD ----------------

    @GetMapping("/admin/dashboard")
    public String dashboard(
            HttpSession session,
            Model model) {

        if (!Boolean.TRUE.equals(
                session.getAttribute("adminLoggedIn"))) {
            return "redirect:/admin";
        }

        model.addAttribute(
                "students",
                studentRepository.findAll()
        );

        return "dashboard";
    }

    // ---------------- ALLOCATE SEATS ----------------

    @PostMapping("/admin/allocate")
    public String allocateSeats(
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!Boolean.TRUE.equals(
                session.getAttribute("adminLoggedIn"))) {
            return "redirect:/admin";
        }

        seatAllocationService.allocateSeats();

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Seat allocation process completed."
        );

        return "redirect:/admin/dashboard";
    }

    // ---------------- CONFIRM SEAT + SEND EMAIL ----------------

    @PostMapping("/admin/confirm-seat")
    public String confirmSeat(
            @RequestParam("studentId") Long studentId,
            HttpSession session) {

        if (!Boolean.TRUE.equals(
                session.getAttribute("adminLoggedIn"))) {
            return "redirect:/admin";
        }

        Student student = studentRepository
                .findById(studentId)
                .orElse(null);

        if (student == null) {
            return "redirect:/admin/dashboard?seatError=notfound";
        }

        if (!"PENDING".equals(student.getAllocationStatus())) {
            return "redirect:/admin/dashboard?seatError=invalid";
        }

        // Save the confirmed status first.
        student.setAllocationStatus("CONFIRMED");
        studentRepository.save(student);

        // Send the confirmation email.
        try {
            String course = student.getAllocatedCourse();

            if (course == null || course.isBlank()) {
                course = student.getCourse();
            }

            if (student.getEmail() == null
                    || student.getEmail().isBlank()) {

                return "redirect:/admin/dashboard"
                        + "?seatConfirmed=true&emailFailed=true";
            }

            emailOtpService.sendSeatConfirmationEmail(
                    student.getEmail(),
                    student.getFullName(),
                    course
            );

            return "redirect:/admin/dashboard"
                    + "?seatConfirmed=true&emailSent=true";

        } catch (RuntimeException ex) {

            System.err.println(
                    "Seat confirmation email could not be sent."
            );

            ex.printStackTrace();

            return "redirect:/admin/dashboard"
                    + "?seatConfirmed=true&emailFailed=true";
        }
    }

    // ---------------- ALLOCATION CHECK ----------------

    @GetMapping("/allocation")
    public String allocationPage() {
        return "allocation";
    }

    @PostMapping("/check-allocation")
    public String checkAllocation(
            @RequestParam("email") String email,
            Model model) {

        Student student = studentRepository.findAll()
                .stream()
                .filter(s -> s.getEmail() != null
                        && s.getEmail().equalsIgnoreCase(
                                email.trim()))
                .findFirst()
                .orElse(null);

        if (student == null) {

            model.addAttribute(
                    "errorMessage",
                    "No application found for this email."
            );

            return "allocation";
        }

        model.addAttribute("student", student);

        return "result";
    }

    // ---------------- ADMIN LOGOUT ----------------

    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin";
    }
}
