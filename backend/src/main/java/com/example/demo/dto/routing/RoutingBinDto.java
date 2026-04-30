package com.example.demo.dto.routing;

public class RoutingBinDto {

    private Long id;
    private Double lat;
    private Double lng;

    private Long zoneId;
    private Integer clusterId;

    private Double fillLevel;
    private Double predictedPriority;
    private Double estimatedLoadKg;
    private Double predictedHoursToFull;

    private Boolean mandatory;

    private String wasteType;

    private String decisionCategory;
    private String decisionReason;
    private Double feedbackScore;
    private Long postponementCount;
    private Boolean opportunistic;
    private Boolean reportable;
    private Double opportunisticScore;

    private Boolean collectionAllowedNow;
    private String collectionWindowExplanation;

    private Integer windowStartMinutes;
    private Integer windowEndMinutes;

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

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Integer getClusterId() {
        return clusterId;
    }

    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
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

    public String getWasteType() {
        return wasteType;
    }

    public void setWasteType(String wasteType) {
        this.wasteType = wasteType;
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

    public Double getOpportunisticScore() {
        return opportunisticScore;
    }

    public void setOpportunisticScore(Double opportunisticScore) {
        this.opportunisticScore = opportunisticScore;
    }

    public Boolean getCollectionAllowedNow() {
        return collectionAllowedNow;
    }

    public void setCollectionAllowedNow(Boolean collectionAllowedNow) {
        this.collectionAllowedNow = collectionAllowedNow;
    }

    public String getCollectionWindowExplanation() {
        return collectionWindowExplanation;
    }

    public void setCollectionWindowExplanation(String collectionWindowExplanation) {
        this.collectionWindowExplanation = collectionWindowExplanation;
    }

    public Integer getWindowStartMinutes() {
        return windowStartMinutes;
    }

    public void setWindowStartMinutes(Integer windowStartMinutes) {
        this.windowStartMinutes = windowStartMinutes;
    }

    public Integer getWindowEndMinutes() {
        return windowEndMinutes;
    }

    public void setWindowEndMinutes(Integer windowEndMinutes) {
        this.windowEndMinutes = windowEndMinutes;
    }
}