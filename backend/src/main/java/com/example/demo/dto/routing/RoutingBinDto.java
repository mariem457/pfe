package com.example.demo.dto.routing;

public class RoutingBinDto {
    private Long id;
    private Double lat;
    private Double lng;
    private Double fillLevel;
    private Double predictedPriority;
    private Double estimatedLoadKg;
    private Double predictedHoursToFull;
    private Boolean mandatory;

    // new fields for Phase A
    private String decisionCategory;
    private String decisionReason;
    private Double feedbackScore;
    private Long postponementCount;
    private Boolean opportunistic;
    private Boolean reportable;

    public RoutingBinDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getFillLevel() {
        return fillLevel;
    }

    public void setFillLevel(Double fillLevel) {
        this.fillLevel = fillLevel;
    }

    public Double getPredictedPriority() {
        return predictedPriority;
    }

    public void setPredictedPriority(Double predictedPriority) {
        this.predictedPriority = predictedPriority;
    }

    public Double getEstimatedLoadKg() {
        return estimatedLoadKg;
    }

    public void setEstimatedLoadKg(Double estimatedLoadKg) {
        this.estimatedLoadKg = estimatedLoadKg;
    }

    public Double getPredictedHoursToFull() {
        return predictedHoursToFull;
    }

    public void setPredictedHoursToFull(Double predictedHoursToFull) {
        this.predictedHoursToFull = predictedHoursToFull;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getDecisionCategory() {
        return decisionCategory;
    }

    public void setDecisionCategory(String decisionCategory) {
        this.decisionCategory = decisionCategory;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Double getFeedbackScore() {
        return feedbackScore;
    }

    public void setFeedbackScore(Double feedbackScore) {
        this.feedbackScore = feedbackScore;
    }

    public Long getPostponementCount() {
        return postponementCount;
    }

    public void setPostponementCount(Long postponementCount) {
        this.postponementCount = postponementCount;
    }

    public Boolean getOpportunistic() {
        return opportunistic;
    }

    public void setOpportunistic(Boolean opportunistic) {
        this.opportunistic = opportunistic;
    }

    public Boolean getReportable() {
        return reportable;
    }

    public void setReportable(Boolean reportable) {
        this.reportable = reportable;
    }
}