package dev.basementlab.dealtracker.notify;

import dev.basementlab.dealtracker.domain.Observation;
import dev.basementlab.dealtracker.domain.TriggerRule;
import dev.basementlab.dealtracker.domain.WatchTarget;
import dev.basementlab.dealtracker.rule.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
public class SesNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SesNotificationSender.class);

    private final SesV2Client sesClient;
    private final String fromAddress;
    private final String toAddress;

    public SesNotificationSender(
            SesV2Client sesClient,
            @Value("${dealtracker.email.from}") String fromAddress,
            @Value("${dealtracker.email.to}") String toAddress
    ) {
        this.sesClient = sesClient;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
    }

    @Override
    public void send(WatchTarget watchTarget, TriggerRule rule, RuleResult result) {
        Observation observation = result.observation();
        String subject = "DealTracker: %s — $%s".formatted(watchTarget.name(), observation.effectivePrice());
        String body = """
                %s just hit $%s (threshold: $%s).

                Title seen: %s
                Availability: %s
                Observed at: %s
                """.formatted(
                watchTarget.name(),
                observation.effectivePrice(),
                rule.thresholdValue(),
                observation.title(),
                observation.availability(),
                observation.observedAt()
        );

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(toAddress).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).build())
                                .body(Body.builder().text(Content.builder().data(body).build()).build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
        log.info("Sent notification email for watch_target '{}' (rule {})", watchTarget.name(), rule.id());
    }
}
