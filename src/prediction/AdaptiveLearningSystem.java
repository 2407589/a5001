 
package prediction;

import core.models.*;
import core.datastructures.EmergencyNetwork;
import java.time.LocalDateTime;
import java.util.*;

public class AdaptiveLearningSystem {
    
    private final EmergencyNetwork network;
    private final PredictiveAnalyzer predictor;
    private final Map<String, UnitPerformanceProfile> unitProfiles;
    private final Map<String, LocationPerformanceData> locationData;
    private double[] dispatchWeights;
    private double learningRate = 0.1;
    private final List<DispatchOutcome> outcomeHistory;
    private int metricsWindowDays = 30;
    
    public AdaptiveLearningSystem(EmergencyNetwork network, PredictiveAnalyzer predictor) {
        this.network = network;
        this.predictor = predictor;
        this.unitProfiles = new HashMap<>();
        this.locationData = new HashMap<>();
        this.outcomeHistory = new ArrayList<>();
        this.dispatchWeights = new double[]{0.30, 0.40, 0.20, 0.10};
    }
    
    public void learnFromDispatch(DispatchOutcome outcome) {
        outcomeHistory.add(outcome);
        updateUnitProfile(outcome);
        updateLocationData(outcome);
        adaptWeights(outcome);
        
        outcome.getUnit().updatePerformanceMetrics(
            outcome.isSuccessful(),
            outcome.getActualResponseTime()
        );
    }
    
    private void updateUnitProfile(DispatchOutcome outcome) {
        String unitId = outcome.getUnit().getId();
        
        UnitPerformanceProfile profile = unitProfiles.computeIfAbsent(
            unitId, k -> new UnitPerformanceProfile(outcome.getUnit())
        );
        
        profile.recordDispatch(
            outcome.getActualResponseTime(),
            outcome.getIncident().getSeverity(),
            outcome.isSuccessful()
        );
    }
    
    private void updateLocationData(DispatchOutcome outcome) {
        String locationId = outcome.getLocationId();
        
        LocationPerformanceData data = locationData.computeIfAbsent(
            locationId, k -> new LocationPerformanceData(locationId)
        );
        
        data.recordDispatch(
            outcome.getActualResponseTime(),
            outcome.isSuccessful()
        );
    }
    
    private void adaptWeights(DispatchOutcome outcome) {
        double reward = calculateReward(outcome);
        
        if (outcome.metTargetTime() && outcome.isSuccessful()) {
            if (outcome.wasResourceMatch()) {
                dispatchWeights[1] += learningRate * reward * 0.1;
            }
            if (outcome.wasNearby()) {
                dispatchWeights[0] += learningRate * reward * 0.1;
            }
        } else {
            if (!outcome.metTargetTime()) {
                dispatchWeights[0] += learningRate * Math.abs(reward) * 0.15;
                dispatchWeights[2] -= learningRate * Math.abs(reward) * 0.05;
            }
            if (!outcome.wasResourceMatch()) {
                dispatchWeights[1] += learningRate * Math.abs(reward) * 0.1;
            }
        }
        
        normalizeWeights();
    }
    
    private double calculateReward(DispatchOutcome outcome) {
        double reward = 0.0;
        
        if (outcome.isSuccessful()) {
            reward += 0.5;
        } else {
            reward -= 0.5;
        }
        
        if (outcome.metTargetTime()) {
            reward += 0.3;
        } else {
            double timeOverage = outcome.getTimeOverageRatio();
            reward -= Math.min(0.4, timeOverage * 0.2);
        }
        
        if (outcome.wasResourceMatch()) {
            reward += 0.2;
        }
        
        return Math.max(-1.0, Math.min(reward, 1.0));
    }
    
    private void normalizeWeights() {
        for (int i = 0; i < dispatchWeights.length; i++) {
            dispatchWeights[i] = Math.max(0.05, dispatchWeights[i]);
        }
        
        double sum = Arrays.stream(dispatchWeights).sum();
        for (int i = 0; i < dispatchWeights.length; i++) {
            dispatchWeights[i] /= sum;
        }
    }
    
    public double[] getOptimizedWeights() {
        return Arrays.copyOf(dispatchWeights, dispatchWeights.length);
    }
    
    public List<RepositioningRecommendation> recommendRepositioning() {
        List<RepositioningRecommendation> recommendations = new ArrayList<>();
        
        List<PredictiveAnalyzer.HotSpot> hotspots = 
            predictor.predictHighDemandAreas(LocalDateTime.now().plusHours(2));
        
        List<ResponseUnit> idleUnits = new ArrayList<>();
        for (Node node : network.getAllNodes()) {
            if (node.getType() == Node.NodeType.DISPATCH_CENTER) {
                idleUnits.addAll(node.getAllAvailableUnits());
            }
        }
        
        for (PredictiveAnalyzer.HotSpot hotspot : hotspots) {
            if (hotspot.getConfidence() < 0.6) continue;
            
            Node targetNode = network.getNode(hotspot.getNodeId());
            if (targetNode == null) continue;
            
            ResponseUnit bestUnit = findBestUnitForRepositioning(
                idleUnits, targetNode, hotspot.getMostCommonType()
            );
            
            if (bestUnit != null) {
                recommendations.add(new RepositioningRecommendation(
                    bestUnit,
                    bestUnit.getCurrentNode(),
                    targetNode,
                    hotspot.getPredictionScore(),
                    hotspot.getConfidence()
                ));
                
                idleUnits.remove(bestUnit);
            }
        }
        
        return recommendations;
    }
    
    private ResponseUnit findBestUnitForRepositioning(List<ResponseUnit> units, 
                                                     Node target,
                                                     Incident.IncidentType expectedType) {
        ResponseUnit best = null;
        double bestScore = -1.0;
        
        for (ResponseUnit unit : units) {
            if (!unit.isAvailable()) continue;
            
            double score = 0.0;
            
            if (unit.isSuitableFor(expectedType)) {
                score += 40.0;
            }
            
            if (unit.getTotalDispatches() > 0) {
                double successRate = (double) unit.getSuccessfulDispatches() / 
                                    unit.getTotalDispatches();
                score += successRate * 30.0;
            }
            
            double distance = unit.getCurrentNode().getLocation()
                .distanceTo(target.getLocation());
            score -= distance * 0.5;
            
            if (score > bestScore) {
                bestScore = score;
                best = unit;
            }
        }
        
        return best;
    }
    
    public SystemPerformanceMetrics getPerformanceMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(metricsWindowDays);
        
        List<DispatchOutcome> recentOutcomes = outcomeHistory.stream()
            .filter(o -> o.getTimestamp().isAfter(cutoff))
            .toList();
        
        if (recentOutcomes.isEmpty()) {
            return new SystemPerformanceMetrics(0, 0, 0, 0, 0, new double[]{0,0,0,0});
        }
        
        int total = recentOutcomes.size();
        long successful = recentOutcomes.stream().filter(DispatchOutcome::isSuccessful).count();
        long metTarget = recentOutcomes.stream().filter(DispatchOutcome::metTargetTime).count();
        
        double avgResponseTime = recentOutcomes.stream()
            .mapToDouble(DispatchOutcome::getActualResponseTime)
            .average()
            .orElse(0.0);
        
        double successRate = (double) successful / total;
        double targetMetRate = (double) metTarget / total;
        
        return new SystemPerformanceMetrics(
            total,
            avgResponseTime,
            successRate,
            targetMetRate,
            calculateSystemImprovement(),
            getOptimizedWeights()
        );
    }
    
    private double calculateSystemImprovement() {
        if (outcomeHistory.size() < 20) return 0.0;
        
        List<DispatchOutcome> early = outcomeHistory.subList(0, 
            Math.min(10, outcomeHistory.size()));
        List<DispatchOutcome> recent = outcomeHistory.subList(
            Math.max(0, outcomeHistory.size() - 10), outcomeHistory.size());
        
        double earlySuccess = early.stream()
            .filter(DispatchOutcome::isSuccessful).count() / (double) early.size();
        double recentSuccess = recent.stream()
            .filter(DispatchOutcome::isSuccessful).count() / (double) recent.size();
        
        return ((recentSuccess - earlySuccess) / Math.max(earlySuccess, 0.01)) * 100.0;
    }
    
    private static class UnitPerformanceProfile {
        private final ResponseUnit unit;
        private final List<Double> responseTimes;
        private final List<Boolean> outcomes;
        private double averagePerformanceScore;
        
        UnitPerformanceProfile(ResponseUnit unit) {
            this.unit = unit;
            this.responseTimes = new ArrayList<>();
            this.outcomes = new ArrayList<>();
            this.averagePerformanceScore = 0.5;
        }
        
        void recordDispatch(double responseTime, Incident.Severity severity, boolean success) {
            responseTimes.add(responseTime);
            outcomes.add(success);
            
            double targetTime = severity.getTargetResponseSeconds() / 60.0;
            double performance = success ? 1.0 : 0.0;
            if (responseTime <= targetTime) {
                performance += 0.5;
            }
            
            averagePerformanceScore = (averagePerformanceScore * 0.9) + (performance * 0.1);
        }
        
        double getAveragePerformanceScore() {
            return averagePerformanceScore;
        }
    }
    
    private static class LocationPerformanceData {
        private final String locationId;
        private final List<Double> responseTimes;
        private int successfulDispatches;
        private int totalDispatches;
        
        LocationPerformanceData(String locationId) {
            this.locationId = locationId;
            this.responseTimes = new ArrayList<>();
            this.successfulDispatches = 0;
            this.totalDispatches = 0;
        }
        
        void recordDispatch(double responseTime, boolean success) {
            responseTimes.add(responseTime);
            totalDispatches++;
            if (success) successfulDispatches++;
        }
        
        double getAverageResponseTime() {
            return responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }
    
    public static class DispatchOutcome {
        private final ResponseUnit unit;
        private final Incident incident;
        private final String locationId;
        private final double actualResponseTime;
        private final boolean successful;
        private final boolean resourceMatch;
        private final LocalDateTime timestamp;
        
        public DispatchOutcome(ResponseUnit unit, Incident incident, String locationId,
                              double actualResponseTime, boolean successful, boolean resourceMatch) {
            this.unit = unit;
            this.incident = incident;
            this.locationId = locationId;
            this.actualResponseTime = actualResponseTime;
            this.successful = successful;
            this.resourceMatch = resourceMatch;
            this.timestamp = LocalDateTime.now();
        }
        
        public ResponseUnit getUnit() { return unit; }
        public Incident getIncident() { return incident; }
        public String getLocationId() { return locationId; }
        public double getActualResponseTime() { return actualResponseTime; }
        public boolean isSuccessful() { return successful; }
        public boolean wasResourceMatch() { return resourceMatch; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean metTargetTime() {
            return actualResponseTime <= incident.getSeverity().getTargetResponseSeconds() / 60.0;
        }
        
        public boolean wasNearby() {
            return actualResponseTime < 5.0;
        }
        
        public double getTimeOverageRatio() {
            double target = incident.getSeverity().getTargetResponseSeconds() / 60.0;
            return (actualResponseTime - target) / target;
        }
    }
    
    public static class RepositioningRecommendation {
        private final ResponseUnit unit;
        private final Node currentLocation;
        private final Node targetLocation;
        private final double urgency;
        private final double confidence;
        
        public RepositioningRecommendation(ResponseUnit unit, Node current, Node target,
                                          double urgency, double confidence) {
            this.unit = unit;
            this.currentLocation = current;
            this.targetLocation = target;
            this.urgency = urgency;
            this.confidence = confidence;
        }
        
        public ResponseUnit getUnit() { return unit; }
        public Node getCurrentLocation() { return currentLocation; }
        public Node getTargetLocation() { return targetLocation; }
        public double getUrgency() { return urgency; }
        public double getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("Reposition[%s: %s -> %s, Urgency: %.2f, Confidence: %.2f]",
                unit.getId(), currentLocation.getName(), targetLocation.getName(), 
                urgency, confidence);
        }
    }
    
    public static class SystemPerformanceMetrics {
        public final int totalDispatches;
        public final double averageResponseTime;
        public final double successRate;
        public final double targetMetRate;
        public final double improvementPercentage;
        public final double[] optimizedWeights;
        
        public SystemPerformanceMetrics(int totalDispatches, double averageResponseTime,
                                       double successRate, double targetMetRate,
                                       double improvementPercentage, double[] optimizedWeights) {
            this.totalDispatches = totalDispatches;
            this.averageResponseTime = averageResponseTime;
            this.successRate = successRate;
            this.targetMetRate = targetMetRate;
            this.improvementPercentage = improvementPercentage;
            this.optimizedWeights = optimizedWeights;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Performance[%d dispatches, Avg Time: %.2f min, Success: %.1f%%, Target Met: %.1f%%, Improvement: %.1f%%]",
                totalDispatches, averageResponseTime, successRate * 100, 
                targetMetRate * 100, improvementPercentage
            );
        }
    }
}