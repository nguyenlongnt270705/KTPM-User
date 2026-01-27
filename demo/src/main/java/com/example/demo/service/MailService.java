package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendRegistrationEmail(String to, String username, String fullName) {
        String subject = "Chào mừng bạn đến với LovePhim";
        String text = String.format(
            "Xin chào %s,\n\n" +
            "Cảm ơn bạn đã đăng ký tài khoản tại LovePhim!\n\n" +
            "Thông tin tài khoản của bạn:\n" +
            "- Tên đăng nhập: %s\n" +
            "- Họ và tên: %s\n\n" +
            "Bạn có thể sử dụng tài khoản này để đăng nhập vào hệ thống.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ LovePhim",
            fullName, username, fullName
        );
        sendEmail(to, subject, text);
    }
}

