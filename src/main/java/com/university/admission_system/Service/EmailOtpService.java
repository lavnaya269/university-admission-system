
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

    public void sendOtp(String email, String purpose, HttpSession session) {

        String otp = String.format("%06d", random.nextInt(1_000_000));

        // Store OTP and expiry in the current user's session.
        session.setAttribute(purpose + "_otp", otp);
        session.setAttribute(purpose + "_otp_email", email);
        session.setAttribute(
                purpose + "_otp_expiry",
                System.currentTimeMillis() + OTP_VALIDITY
        );
        session.setAttribute(purpose + "_otp_attempts", 0);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("University Admission System - Email Verification");
        message.setText(
                "Hello,\n\n"
                + "Your OTP is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n"
                + "Do not share this code with anyone.\n\n"
                + "If you did not request this code, you can ignore this email."
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            clearOtp(session, purpose);

            throw new IllegalStateException(
                    "Email could not be sent. Check your Gmail settings and try again."
            );
        }
    }

    public boolean verifyOtp(
            String submittedOtp,
            String email,
            String purpose,
            HttpSession session) {

        Object savedOtp = session.getAttribute(purpose + "_otp");
        Object savedEmail = session.getAttribute(purpose + "_otp_email");
        Object expiryValue = session.getAttribute(purpose + "_otp_expiry");
        Object attemptsValue = session.getAttribute(purpose + "_otp_attempts");

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

        if (System.currentTimeMillis() > expiry || attempts >= MAX_ATTEMPTS) {
            clearOtp(session, purpose);
            return false;
        }

        if (submittedOtp == null
                || !((String) savedOtp).equals(submittedOtp.trim())) {

            attempts++;
            session.setAttribute(purpose + "_otp_attempts", attempts);

            if (attempts >= MAX_ATTEMPTS) {
                clearOtp(session, purpose);
            }

            return false;
        }

        // OTP is correct: remove it so it cannot be reused.
        clearOtp(session, purpose);
        return true;
    }

    public void clearOtp(HttpSession session, String purpose) {
        session.removeAttribute(purpose + "_otp");
        session.removeAttribute(purpose + "_otp_email");
        session.removeAttribute(purpose + "_otp_expiry");
        session.removeAttribute(purpose + "_otp_attempts");
    }


public void sendSeatAllocationEmail(
        String email,
        String studentName,
        String allocatedCourse) {

    SimpleMailMessage message = new SimpleMailMessage();

    message.setTo(email);
    message.setSubject(
            "University Admission System - Seat Allocated Successfully!"
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
}
