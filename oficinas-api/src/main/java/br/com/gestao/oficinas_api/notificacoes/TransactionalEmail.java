package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.identidade.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmail {
    private final JavaMailSender sender;
    private final String from;
    public TransactionalEmail(JavaMailSender sender, @Value("${app.mail.from:}") String from) {
        this.sender=sender; this.from=from;
    }
    public void send(String recipient, String subject, String text) {
        if(from.isBlank()) throw new ApiException(503,"EMAIL_INDISPONIVEL","Envio de e-mail indisponível. Tente mais tarde.");
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject(subject); message.setText(text);
        try { sender.send(message); }
        catch(MailException e) { throw new ApiException(503,"EMAIL_INDISPONIVEL","Envio de e-mail indisponível. Tente mais tarde."); }
    }
}

