package dev.basementlab.dealtracker.scheduler;

import dev.basementlab.dealtracker.collector.CollectorRegistry;
import dev.basementlab.dealtracker.collector.RawObservation;
import dev.basementlab.dealtracker.domain.Listing;
import dev.basementlab.dealtracker.domain.Observation;
import dev.basementlab.dealtracker.domain.Source;
import dev.basementlab.dealtracker.domain.TriggerRule;
import dev.basementlab.dealtracker.domain.WatchTarget;
import dev.basementlab.dealtracker.notify.NotificationDedupeService;
import dev.basementlab.dealtracker.notify.NotificationSender;
import dev.basementlab.dealtracker.repository.ListingRepository;
import dev.basementlab.dealtracker.repository.ObservationRepository;
import dev.basementlab.dealtracker.repository.SourceRepository;
import dev.basementlab.dealtracker.repository.TriggerRuleRepository;
import dev.basementlab.dealtracker.repository.WatchTargetRepository;
import dev.basementlab.dealtracker.rule.RuleEvaluator;
import dev.basementlab.dealtracker.rule.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// @Scheduled(fixedDelayString=...) - like a .NET BackgroundService with a Task.Delay loop, but
// declarative: no manual while(!stoppingToken.IsCancellationRequested) loop to write. Spring's
// TaskScheduler handles the timing; this method just needs to be idempotent per call.
@Component
public class PollingJob {

    private static final Logger log = LoggerFactory.getLogger(PollingJob.class);

    private final WatchTargetRepository watchTargetRepository;
    private final ListingRepository listingRepository;
    private final SourceRepository sourceRepository;
    private final ObservationRepository observationRepository;
    private final TriggerRuleRepository triggerRuleRepository;
    private final CollectorRegistry collectorRegistry;
    private final RuleEvaluator ruleEvaluator;
    private final NotificationDedupeService dedupeService;
    private final NotificationSender notificationSender;

    public PollingJob(
            WatchTargetRepository watchTargetRepository,
            ListingRepository listingRepository,
            SourceRepository sourceRepository,
            ObservationRepository observationRepository,
            TriggerRuleRepository triggerRuleRepository,
            CollectorRegistry collectorRegistry,
            RuleEvaluator ruleEvaluator,
            NotificationDedupeService dedupeService,
            NotificationSender notificationSender
    ) {
        this.watchTargetRepository = watchTargetRepository;
        this.listingRepository = listingRepository;
        this.sourceRepository = sourceRepository;
        this.observationRepository = observationRepository;
        this.triggerRuleRepository = triggerRuleRepository;
        this.collectorRegistry = collectorRegistry;
        this.ruleEvaluator = ruleEvaluator;
        this.dedupeService = dedupeService;
        this.notificationSender = notificationSender;
    }

    @Scheduled(fixedDelayString = "${dealtracker.poll.fixed-delay-ms}")
    public void pollAll() {
        List<WatchTarget> targets = watchTargetRepository.findAllActive();
        log.info("Starting poll cycle for {} active watch targets", targets.size());

        for (WatchTarget target : targets) {
            try {
                pollOne(target);
            } catch (Exception e) {
                // One bad source (site down, schema drift, whatever) must not kill the whole
                // cycle - every other watch_target still needs to run this round.
                log.error("Poll failed for watch_target '{}' (id={})", target.name(), target.id(), e);
            }
        }
    }

    private void pollOne(WatchTarget target) {
        List<Listing> listings = listingRepository.findActiveByWatchTarget(target.id());
        List<Observation> batch = new ArrayList<>();

        for (Listing listing : listings) {
            Source source = sourceRepository.findById(listing.sourceId());
            List<RawObservation> raw = collectorRegistry.forKind(source.kind()).collect(target, listing);
            Instant observedAt = Instant.now();
            for (RawObservation r : raw) {
                Observation toSave = ObservationRepository.forListing(
                        listing.id(), observedAt, r.title(), r.availability(),
                        r.price(), r.salePrice(), r.promoText(), r.metaJson()
                );
                batch.add(observationRepository.insert(toSave));
            }
        }

        if (batch.isEmpty()) {
            log.debug("No observations collected for watch_target '{}' this cycle", target.name());
            return;
        }

        for (TriggerRule rule : triggerRuleRepository.findActiveByWatchTarget(target.id())) {
            RuleResult result = ruleEvaluator.evaluate(rule, batch);
            if (!result.fires()) {
                continue;
            }
            if (!dedupeService.shouldSend(target.id(), rule.id(), rule.cooldownHours())) {
                log.debug("Rule {} fired for '{}' but is within cooldown - skipping notification",
                        rule.id(), target.name());
                continue;
            }
            notificationSender.send(target, rule, result);
            dedupeService.recordSent(target.id(), result.observation().listingId(), rule.id(), result.observation().id());
        }
    }
}
