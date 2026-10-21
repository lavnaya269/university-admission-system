
package com.university.admission_system.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class EmailOtpService {

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    private static final long OTP_VALIDITY = 5 * 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;

    public EmailOtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ---------------- SEND OTP ----------------

    public void sendOtp(
            String email,
            String purpose,
            HttpSession session) {

        String otp = String.format(
                "%06d",
                random.nextInt(1_000_000)
        );

        session.setAttribute(purpose + "_otp", otp);
        session.setAttribute(purpose + "_otp_email", email);

        session.setAttribute(
                purpose + "_otp_expiry",
                System.currentTimeMillis() + OTP_VALIDITY
        );

        session.setAttribute(purpose + "_otp_attempts", 0);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject(
                "University Admission System - Email Verification"
        );

        message.setText(
                "Hello,\n\n"
                + "Your OTP is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n"
                + "Do not share this code with anyone.\n\n"
                + "If you did not request this code, "
                + "you can ignore this email."
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {

            clearOtp(session, purpose);

            throw new IllegalStateException(
                    "Email could not be sent. "
                    + "Check your Gmail settings and try again."
            );
        }
    }

    // ---------------- VERIFY OTP ----------------

    public boolean verifyOtp(
            String submittedOtp,
            String email,
            String purpose,
            HttpSession session) {

        Object savedOtp =
                session.getAttribute(purpose + "_otp");

        Object savedEmail =
                session.getAttribute(purpose + "_otp_email");

        Object expiryValue =
                session.getAttribute(purpose + "_otp_expiry");

        Object attemptsValue =
                session.getAttribute(purpose + "_otp_attempts");

        if (!(savedOtp instanceof String)
                || !(savedEmail instanceof String)
                || !(expiryValue instanceof Long)
                || !(attemptsValue instanceof Integer)) {
            return false;
        }

        if (!email.equalsIgnoreCase((String) savedEmail)) {
            return false;
        }

        long expiry = (Long) expiryValue;
        int attempts = (Integer) attemptsValue;

        if (System.currentTimeMillis() > expiry
                || attempts >= MAX_ATTEMPTS) {

            clearOtp(session, purpose);
            return false;
        }

        if (submittedOtp == null
                || !((String) savedOtp).equals(
                        submittedOtp.trim())) {

            attempts++;

            session.setAttribute(
                    purpose + "_otp_attempts",
                    attempts
            );

            if (attempts >= MAX_ATTEMPTS) {
                clearOtp(session, purpose);
            }

            return false;
        }

        // Remove OTP after successful verification.
        clearOtp(session, purpose);

        return true;
    }

    // ---------------- CLEAR OTP ----------------

    public void clearOtp(
            HttpSession session,
            String purpose) {

        session.removeAttribute(purpose + "_otp");
        session.removeAttribute(purpose + "_otp_email");
        session.removeAttribute(purpose + "_otp_expiry");
        session.removeAttribute(purpose + "_otp_attempts");
    }

    // ---------------- SEAT ALLOCATION EMAIL ----------------

    public void sendSeatAllocationEmail(
            String email,
            String studentName,
            String allocatedCourse) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "University Admission System - "
                + "Seat Allocated Successfully!"
        );

        message.setText(
                "Dear " + studentName + ",\n\n"
                + "Congratulations!\n\n"
                + "Your seat has been successfully allocated.\n\n"
                + "Student Name: " + studentName + "\n"
                + "Allocated Course: " + allocatedCourse + "\n"
                + "Allocation Status: ALLOCATED\n\n"
                + "Please contact the university administration "
                + "for further admission instructions.\n\n"
                + "Regards,\n"
                + "University Admission Team"
        );

        mailSender.send(message);
    }

    // ---------------- SEAT CONFIRMATION EMAIL ----------------

    public void sendSeatConfirmationEmail(
            String email,
            String studentName,
            String confirmedCourse) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "University Admission System - "
                + "Seat Confirmation"
        );

        String course = confirmedCourse;

        if (course == null || course.isBlank()) {
            course = "As per your application";
        }

        message.setText(
                "Dear " + studentName + ",\n\n"
                + "Congratulations!\n\n"
                + "Your seat has been confirmed successfully "
                + "by the university administration.\n\n"
                + "Student Name: " + studentName + "\n"
                + "Confirmed Course: " + course + "\n"
                + "Admission Status: CONFIRMED\n\n"
                + "Please keep this email for your records.\n"
                + "Contact the university administration for "
                + "further admission instructions.\n\n"
                + "Regards,\n"
                + "University Admission Team"
        );

        mailSender.send(message);
    }
}
