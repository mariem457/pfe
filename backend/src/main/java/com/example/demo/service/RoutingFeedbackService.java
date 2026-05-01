package com.example.demo.service;

import com.example.demo.dto.routing.MandatoryBinInsightDto;

import java.util.List;

public interface RoutingFeedbackService {

    List<MandatoryBinInsightDto> getMandatoryInsights();

    List<MandatoryBinInsightDto> getFeedbackMandatoryBins();

    double getAverageFeedbackScore();

    long countHighFeedbackScoreBins(double threshold);
}