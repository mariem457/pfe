package com.example.demo.controller;

import com.example.demo.dto.routing.MandatoryBinInsightDto;
import com.example.demo.service.RoutingFeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routing/feedback")
public class RoutingFeedbackController {

    private final RoutingFeedbackService routingFeedbackService;

    public RoutingFeedbackController(RoutingFeedbackService routingFeedbackService) {
        this.routingFeedbackService = routingFeedbackService;
    }

    @GetMapping("/mandatory-insights")
    public List<MandatoryBinInsightDto> getMandatoryInsights() {
        return routingFeedbackService.getMandatoryInsights();
    }

    @GetMapping("/mandatory-by-feedback")
    public List<MandatoryBinInsightDto> getMandatoryByFeedback() {
        return routingFeedbackService.getFeedbackMandatoryBins();
    }

    @GetMapping("/bin-classification")
    public List<MandatoryBinInsightDto> getBinClassification() {
        return routingFeedbackService.getMandatoryInsights();
    }
}