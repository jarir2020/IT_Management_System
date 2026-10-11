package com.itmanagement.servicedesk.modules.tickets.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class TicketEmailNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TicketEmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String supportEmail;

    public TicketEmailNotificationService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.support-email:}") String supportEmail) {
        this.mailSender = mailSender;
        this.from = from;
        this.supportEmail = supportEmail;
    }

    @Async("mailTaskExecutor")
    public void ticketCreated(Long id, String title, String requesterEmail) {
        send(recipients(requesterEmail, supportEmail), "Ticket #" + id + " received",
                "Ticket #" + id + " (" + title + ") was created and is waiting for support.");
    }

    @Async("mailTaskExecutor")
    public void ticketAssigned(Long id, String title, String assigneeEmail) {
        send(recipients(assigneeEmail), "Ticket #" + id + " assigned to you",
                "Ticket #" + id + " (" + title + ") was assigned to your support queue.");
    }

    @Async("mailTaskExecutor")
    public void ticketStatusChanged(Long id, String title, String status, String recipient) {
        send(recipients(recipient), "Ticket #" + id + " updated",
                "Ticket #" + id + " (" + title + ") moved to " + status + ".");
    }

    @Async("mailTaskExecutor")
    public void ticketCommented(Long id, String title, String recipient) {
        send(recipients(recipient), "New update on ticket #" + id,
                "There is a new public update on ticket #" + id + " (" + title + "). Sign in to view it.");
    }

    private void send(Set<String> recipients, String subject, String body) {
        if (recipients.isEmpty()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Could not send ticket email notification ({})", exception.getClass().getSimpleName());
        }
    }

    private static Set<String> recipients(String... values) {
        Set<String> recipients = new LinkedHashSet<>();
        Arrays.stream(values).filter(value -> value != null && !value.isBlank())
                .map(String::trim).forEach(recipients::add);
        return recipients;
    }
}
