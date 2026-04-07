package com.example.demo.dto.routing;

public class MandatoryBinInsightDto {
    private Long binId;
    private Double fillLevel;
    private Double predictedPriority;
    private Double predictedHoursToFull;
    private Boolean mandatory;
    private Boolean mandatoryByUrgency;
    private Boolean mandatoryByFeedback;
    private Long postponementCount;
    private Double feedbackScore;
    private String reason;

    private String decisionCategory;
    private String decisionReason;
    private Boolean opportunistic;
    private Boolean reportable;

    public MandatoryBinInsightDto() {
    }

    public Long getBinId() {
        return binId;
    }

    public void setBinId(Long binId) {
        this.binId = binId;
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

    public Boolean getMandatoryByUrgency() {
        return mandatoryByUrgency;
    }

    public void setMandatoryByUrgency(Boolean mandatoryByUrgency) {
        this.mandatoryByUrgency = mandatoryByUrgency;
    }

    public Boolean getMandatoryByFeedback() {
        return mandatoryByFeedback;
    }

    public void setMandatoryByFeedback(Boolean mandatoryByFeedback) {
        this.mandatoryByFeedback = mandatoryByFeedback;
    }

    public Long getPostponementCount() {
        return postponementCount;
    }

    public void setPostponementCount(Long postponementCount) {
        this.postponementCount = postponementCount;
    }

    public Double getFeedbackScore() {
        return feedbackScore;
    }

    public void setFeedbackScore(Double feedbackScore) {
        this.feedbackScore = feedbackScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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