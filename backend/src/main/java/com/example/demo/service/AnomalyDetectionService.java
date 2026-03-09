package com.example.demo.service;

import com.example.demo.entity.Anomaly;
import com.example.demo.entity.Bin;
import com.example.demo.entity.BinTelemetry;
import com.example.demo.repository.AnomalyRepository;
import com.example.demo.repository.BinTelemetryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AnomalyDetectionService {

    private final BinTelemetryRepository telemetryRepository;
    private final AnomalyRepository anomalyRepository;

    public AnomalyDetectionService(BinTelemetryRepository telemetryRepository,
                                   AnomalyRepository anomalyRepository) {
        this.telemetryRepository = telemetryRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @Transactional
    public void evaluateAndPersist(Bin bin, BinTelemetry current) {

        Long binId = bin.getId();

        List<BinTelemetry> last = telemetryRepository
                .findByBinIdOrderByTimestampDesc(binId, PageRequest.of(0, 6));

        // OUTLIER
        if (isOutlier(current)) {
            upsertActive(bin, "OUTLIER",
                    BigDecimal.valueOf(0.90),
                    Map.of("reason", "value out of expected range"),
                    current.getTimestamp(),
                    "rules_v1");
        } else {
            closeIfActive(binId, "OUTLIER", current.getTimestamp());
        }

        // PACKET LOSS
        if (last.size() >= 2) {
            BinTelemetry prev = last.get(1);

            Duration gap = Duration.between(prev.getTimestamp(), current.getTimestamp());
            Duration threshold = Duration.ofMinutes(5);

            if (!gap.isNegative() && gap.compareTo(threshold) > 0) {
                upsertActive(bin, "PACKET_LOSS",
                        BigDecimal.valueOf(0.75),
                        Map.of("gapSeconds", gap.toSeconds()),
                        prev.getTimestamp(),
                        "rules_v1");
            } else {
                closeIfActive(binId, "PACKET_LOSS", current.getTimestamp());
            }
        }

        // STUCK
        if (last.size() >= 5 && isStuck(last)) {
            Instant oldestTs = last.get(last.size() - 1).getTimestamp();

            upsertActive(bin, "STUCK",
                    BigDecimal.valueOf(0.80),
                    Map.of("reason", "fill level not changing"),
                    oldestTs,
                    "rules_v1");
        } else {
            closeIfActive(binId, "STUCK", current.getTimestamp());
        }

        // DRIFT
        if (last.size() >= 2 && isDrift(last.get(1), current)) {
            upsertActive(bin, "DRIFT",
                    BigDecimal.valueOf(0.70),
                    Map.of("reason", "rapid fill change"),
                    last.get(1).getTimestamp(),
                    "rules_v1");
        } else {
            closeIfActive(binId, "DRIFT", current.getTimestamp());
        }
    }

    private boolean isOutlier(BinTelemetry t) {
        short fill = t.getFillLevel();
        Short battery = t.getBatteryLevel();
        BigDecimal weight = t.getWeightKg();

        if (fill < 0 || fill > 100) return true;
        if (battery != null && (battery < 0 || battery > 100)) return true;
        if (weight != null && weight.compareTo(BigDecimal.ZERO) < 0) return true;

        return false;
    }

    private boolean isStuck(List<BinTelemetry> lastDesc) {
        List<BinTelemetry> five = lastDesc.subList(0, 5);

        short first = five.get(0).getFillLevel();
        int tolerance = 1;

        for (BinTelemetry t : five) {
            if (Math.abs(t.getFillLevel() - first) > tolerance) return false;
        }
        return true;
    }

    private boolean isDrift(BinTelemetry prev, BinTelemetry curr) {
        Duration dt = Duration.between(prev.getTimestamp(), curr.getTimestamp());
        if (dt.isNegative() || dt.isZero()) return false;

        int delta = Math.abs(curr.getFillLevel() - prev.getFillLevel());

        return delta >= 40 && dt.compareTo(Duration.ofMinutes(5)) <= 0;
    }

    private void upsertActive(Bin bin,
                              String type,
                              BigDecimal score,
                              Map<String, Object> details,
                              Instant startTime,
                              String modelName) {

        Long binId = bin.getId();

        Anomaly a = anomalyRepository
                .findFirstByBinIdAndAnomalyTypeAndActiveTrue(binId, type)
                .orElseGet(() -> {
                    Anomaly n = new Anomaly();
                    n.setBin(bin);
                    n.setAnomalyType(type);
                    n.setStartTime(startTime != null ? startTime : Instant.now());
                    n.setActive(true);
                    return n;
                });

        a.setScore(score);
        a.setDetails(details);
        a.setModelName(modelName);
        a.setDetectedAt(Instant.now());
        a.setActive(true);

        anomalyRepository.save(a);
    }

    private void closeIfActive(Long binId, String type, Instant endTime) {
        anomalyRepository
                .findFirstByBinIdAndAnomalyTypeAndActiveTrue(binId, type)
                .ifPresent(a -> {
                    a.setActive(false);
                    a.setEndTime(endTime != null ? endTime : Instant.now());
                    anomalyRepository.save(a);
                });
    }
}