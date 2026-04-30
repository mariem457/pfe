package com.example.demo.service;

public class RoutingDecision {

    public enum Strategy {
        FULL_OPTIMIZATION,
        MANDATORY_ONLY,
        REFUEL_ONLY,
        SKIP
    }

    private boolean shouldOptimize;
    private boolean refuelOnly;
    private boolean includeOpportunistic;
    private Strategy strategy;
    private String reason;

    public RoutingDecision() {
    }

    public static RoutingDecision skip(String reason) {
        RoutingDecision decision = new RoutingDecision();
        decision.setShouldOptimize(false);
        decision.setRefuelOnly(false);
        decision.setIncludeOpportunistic(false);
        decision.setStrategy(Strategy.SKIP);
        decision.setReason(reason);
        return decision;
    }

    public static RoutingDecision refuelOnly(String reason) {
        RoutingDecision decision = new RoutingDecision();
        decision.setShouldOptimize(false);
        decision.setRefuelOnly(true);
        decision.setIncludeOpportunistic(false);
        decision.setStrategy(Strategy.REFUEL_ONLY);
        decision.setReason(reason);
        return decision;
    }

    public static RoutingDecision mandatoryOnly(String reason) {
        RoutingDecision decision = new RoutingDecision();
        decision.setShouldOptimize(true);
        decision.setRefuelOnly(false);
        decision.setIncludeOpportunistic(false);
        decision.setStrategy(Strategy.MANDATORY_ONLY);
        decision.setReason(reason);
        return decision;
    }

    public static RoutingDecision fullOptimization(String reason) {
        RoutingDecision decision = new RoutingDecision();
        decision.setShouldOptimize(true);
        decision.setRefuelOnly(false);
        decision.setIncludeOpportunistic(true);
        decision.setStrategy(Strategy.FULL_OPTIMIZATION);
        decision.setReason(reason);
        return decision;
    }

    public boolean isShouldOptimize() {
        return shouldOptimize;
    }

    public void setShouldOptimize(boolean shouldOptimize) {
        this.shouldOptimize = shouldOptimize;
    }

    public boolean isRefuelOnly() {
        return refuelOnly;
    }

    public void setRefuelOnly(boolean refuelOnly) {
        this.refuelOnly = refuelOnly;
    }

    public boolean isIncludeOpportunistic() {
        return includeOpportunistic;
    }

    public void setIncludeOpportunistic(boolean includeOpportunistic) {
        this.includeOpportunistic = includeOpportunistic;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}