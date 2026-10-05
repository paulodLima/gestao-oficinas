package br.com.gestao.oficinas_api.notificacoes;

import br.com.gestao.oficinas_api.identidade.ApiException;
import java.net.URL;
import org.apache.commons.mail2.jakarta.ImageHtmlEmail;
import org.apache.commons.mail2.jakarta.resolver.DataSourceUrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransactionalEmail {
    private static final Logger log = LoggerFactory.getLogger(TransactionalEmail.class);

    private final String from;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean authenticationRequired;
    private final boolean tlsEnabled;
    private final String publicUrl;

    public TransactionalEmail(
            @Value("${app.mail.from:}") String from,
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${MAIL_AUTH:false}") boolean authenticationRequired,
            @Value("${MAIL_TLS:false}") boolean tlsEnabled,
            @Value("${app.auth.public-url:http://localhost:4200}") String publicUrl) {
        this.from = from;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authenticationRequired = authenticationRequired;
        this.tlsEnabled = tlsEnabled;
        this.publicUrl = publicUrl;
    }
    public void send(String recipient, String subject, String text) {
        if (from.isBlank()) {
            log.warn("Envio de e-mail ignorado: MAIL_FROM não foi configurado.");
            throw new ApiException(503, "EMAIL_INDISPONIVEL", "Envio de e-mail indisponível. Tente mais tarde.");
        }
        try {
            ImageHtmlEmail email = new ImageHtmlEmail();
            email.setDataSourceResolver(new DataSourceUrlResolver(new URL(normalizeBaseUrl(publicUrl))));
            email.setCharset("utf-8");
            email.setHostName(host);
            email.setSmtpPort(port);
            email.setStartTLSRequired(tlsEnabled);
            if (authenticationRequired && !username.isBlank()) {
                email.setAuthentication(username, password);
            }
            email.setFrom(from);
            email.addTo(recipient);
            email.setSubject(subject);
            email.setTextMsg(text);
            email.setHtmlMsg(toHtml(text));
            email.send();
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail via SMTP (host={}, porta={}, autenticação={}, TLS={}).",
                    host, port, authenticationRequired, tlsEnabled, e);
            throw new ApiException(503, "EMAIL_INDISPONIVEL", "Envio de e-mail indisponível. Tente mais tarde.");
        }
    }

    private String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private String toHtml(String text) {
        String escapedText = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style=\"font-family:Arial,sans-serif;color:#18233a;line-height:1.5\">"
                + escapedText + "</body></html>";
    }
}
