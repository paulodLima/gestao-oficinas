package br.com.gestao.oficinas_api.notificacoes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationSchedule {
    private final NotificationWorker worker;
    public NotificationSchedule(NotificationWorker worker) { this.worker = worker; }
    @Scheduled(fixedDelayString = "${app.notifications.poll-ms:10000}", initialDelayString = "${app.notifications.poll-ms:10000}")
    public void deliver() {
        for (int i = 0; i < 20 && worker.processOne(); i++) { /* bounded batch, independent transactions */ }
    }
}
