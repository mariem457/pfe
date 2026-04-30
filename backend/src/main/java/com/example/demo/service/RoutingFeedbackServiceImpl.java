package com.example.demo.service;

import com.example.demo.dto.routing.MandatoryBinInsightDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoutingFeedbackServiceImpl implements RoutingFeedbackService {

    private final RoutingPayloadBuilderService routingPayloadBuilderService;

    public RoutingFeedbackServiceImpl(RoutingPayloadBuilderService routingPayloadBuilderService) {
        this.routingPayloadBuilderService = routingPayloadBuilderService;
    }

    @Override
    public List<MandatoryBinInsightDto> getMandatoryInsights() {
        return routingPayloadBuilderService.getMandatoryBinInsights();
    }

    @Override
    public List<MandatoryBinInsightDto> getFeedbackMandatoryBins() {
        return routingPayloadBuilderService.getMandatoryBinInsights()
                .stream()
                .filter(dto -> Boolean.TRUE.equals(dto.getMandatoryByFeedback()))
                .toList();
    }

    @Override
    public double getAverageFeedbackScore() {
        List<MandatoryBinInsightDto> insights = routingPayloadBuilderService.getMandatoryBinInsights();

        if (insights.isEmpty()) {
            return 0.0;
        }

        return insights.stream()
                .map(MandatoryBinInsightDto::getFeedbackScore)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    @Override
    public long countHighFeedbackScoreBins(double threshold) {
        return routingPayloadBuilderService.getMandatoryBinInsights()
                .stream()
                .map(MandatoryBinInsightDto::getFeedbackScore)
                .filter(score -> score != null && score >= threshold)
                .count();
    }
}