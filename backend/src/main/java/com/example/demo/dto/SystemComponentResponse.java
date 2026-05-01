package com.example.demo.dto;

public class SystemComponentResponse {

    private String name;
    private String description;
    private String status;

    private String metric1Label;
    private String metric1Value;

    private String metric2Label;
    private String metric2Value;

    private String metric3Label;
    private String metric3Value;

    public SystemComponentResponse() {
    }

    public SystemComponentResponse(
            String name,
            String description,
            String status,
            String metric1Label,
            String metric1Value,
            String metric2Label,
            String metric2Value,
            String metric3Label,
            String metric3Value
    ) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.metric1Label = metric1Label;
        this.metric1Value = metric1Value;
        this.metric2Label = metric2Label;
        this.metric2Value = metric2Value;
        this.metric3Label = metric3Label;
        this.metric3Value = metric3Value;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getMetric1Label() { return metric1Label; }
    public String getMetric1Value() { return metric1Value; }
    public String getMetric2Label() { return metric2Label; }
    public String getMetric2Value() { return metric2Value; }
    public String getMetric3Label() { return metric3Label; }
    public String getMetric3Value() { return metric3Value; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(String status) { this.status = status; }
    public void setMetric1Label(String metric1Label) { this.metric1Label = metric1Label; }
    public void setMetric1Value(String metric1Value) { this.metric1Value = metric1Value; }
    public void setMetric2Label(String metric2Label) { this.metric2Label = metric2Label; }
    public void setMetric2Value(String metric2Value) { this.metric2Value = metric2Value; }
    public void setMetric3Label(String metric3Label) { this.metric3Label = metric3Label; }
    public void setMetric3Value(String metric3Value) { this.metric3Value = metric3Value; }
}