/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import util.ConfigUtil;

/**
 *
 * @author PC
 */
public class MailService {

    private final Properties config = ConfigUtil.load("mail.properties");

    public void send(String to, String subject, String body) throws MessagingException {
        final String username = config.getProperty("mail.username");
        final String appPassword = config.getProperty("mail.app.password");

        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", "smtp.gmail.com");
        smtp.put("mail.smtp.port", "587");
        smtp.put("mail.smtp.auth", "true");
        smtp.put("mail.smtp.starttls.enable", "true");
        smtp.put("mail.smtp.ssl.protocols", "TLSv1.2");
        smtp.put("mail.smtp.connectiontimeout", "10000");
        smtp.put("mail.smtp.timeout", "10000");
        smtp.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(smtp, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }
}
