package rs.ac.ftn.isa.isabackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${site.base.url}")
    private String baseUrl;

    @Async
    public void sendActivationEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Aktivacija naloga - ISA Projekat");

            String activationLink = baseUrl + "/activate?email=" + toEmail;

            message.setText("Pozdrav,\n\nMolimo vas kliknite na link ispod da aktivirate nalog:\n" + activationLink);

            javaMailSender.send(message);
            System.out.println("Email uspesno poslat na: " + toEmail);

        } catch (Exception e) {
            System.out.println("GRESKA prilikom slanja emaila: " + e.getMessage());
            e.printStackTrace();
        }
    }
}