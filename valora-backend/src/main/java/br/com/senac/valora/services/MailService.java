package br.com.senac.valora.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public MailService(JavaMailSender mailSender, @Value("${valora.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async("mailExecutor")
    public void sendSubmissionConfirmation(String toEmail, String studentName, String courseName,
                                           String categoryName, int requestedHours) {
        if (from == null || from.isBlank() || toEmail == null || toEmail.isBlank()) {
            log.debug("E-mail nao configurado ou destinatario ausente — envio pulado");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("VALORA — Atividade complementar recebida");
            message.setText("Ola, " + studentName + ".\n\n"
                    + "Recebemos sua submissao de atividade complementar:\n"
                    + "Curso: " + courseName + "\n"
                    + "Categoria: " + categoryName + "\n"
                    + "Horas solicitadas: " + requestedHours + "h\n\n"
                    + "Status: PENDENTE — voce sera avisado quando o coordenador avaliar.\n\n"
                    + "VALORA — Senac PE");
            mailSender.send(message);
            log.info("E-mail de confirmacao enviado para {}", toEmail);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail de confirmacao para {}: {}", toEmail, ex.getMessage());
        }
    }
}
