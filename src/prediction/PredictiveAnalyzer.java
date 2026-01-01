package prediction;

import core.models.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class PredictiveAnalyzer {
    
    private final Map<String, LocationHistory> locationHistories;
    private final Map<DayOfWeek, Map<Integer, List<Incident>>> incidentsByDayAndHour;
    private final Map<String, Double> currentRiskScores;
    private double temporalDecayFactor = 0.95;
    private int minIncidentsForPrediction = 5;
    
    public PredictiveAnalyzer() {
        this.locationHistories = new HashMap<>();
        this.incidentsByDayAndHour = new HashMap<>();
        this.currentRiskScores = new HashMap<>();
        
        for (DayOfWeek day : DayOfWeek.values()) {
            Map<Integer, List<Incident>> hourMap = new HashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                hourMap.put(hour, new ArrayList<>());
            }
            incidentsByDayAndHour.put(day, hourMap);
        }
    }
    
    public void recordIncident(Incident incident, String nearestNodeId) {
        LocationHistory history = locationHistories.computeIfAbsent(
            nearestNodeId, k -> new LocationHistory(nearestNodeId)
        );
        history.addIncident(incident);
        
        DayOfWeek day = incident.getTimestamp().getDayOfWeek();
        int hour = incident.getTimestamp().getHour();
        incidentsByDayAndHour.get(day).get(hour).add(incident);
        
        updateRiskScores();
    }
    
    public List<HotSpot> predictHighDemandAreas(LocalDateTime targetTime) {
        DayOfWeek targetDay = targetTime.getDayOfWeek();
        int targetHour = targetTime.getHour();
        
        List<HotSpot> hotspots = new ArrayList<>();
        
        for (Map.Entry<String, LocationHistory> entry : locationHistories.entrySet()) {
            String nodeId = entry.getKey();
            LocationHistory history = entry.getValue();
            
            if (history.getTotalIncidents() < minIncidentsForPrediction) {
                continue;
            }
            
            double score = calculatePredictionScore(history, targetDay, targetHour);
            
            if (score > 0.1) {
                hotspots.add(new HotSpot(
                    nodeId,
                    score,
                    history.getAverageSeverity(),
                    history.getMostCommonIncidentType(),
                    calculateConfidence(history)
                ));
            }
        }
        
        hotspots.sort((a, b) -> Double.compare(b.getPredictionScore(), a.getPredictionScore()));
        
        return hotspots;
    }
    
    private double calculatePredictionScore(LocationHistory history, 
                                           DayOfWeek day, int hour) {
        double baseScore = (double) history.getTotalIncidents() / 
                          (locationHistories.size() + 1);
        
        double timeScore = history.getIncidentCountForTime(day, hour) / 
                          Math.max(1.0, history.getTotalIncidents());
        
        double recentScore = history.getRecentIncidentRate(7);
        
        double severityMultiplier = 1.0 + (history.getAverageSeverity() * 0.2);
        
        double score = (0.3 * baseScore) + 
                      (0.4 * timeScore) + 
                      (0.3 * recentScore);
        
        return score * severityMultiplier * 100.0;
    }
    
    private double calculateConfidence(LocationHistory history) {
        int incidents = history.getTotalIncidents();
        
        if (incidents < minIncidentsForPrediction) {
            return 0.0;
        }
        
        double confidence = 1.0 - Math.exp(-incidents / 20.0);
        
        double variance = history.getVariance();
        confidence *= (1.0 - Math.min(variance / 10.0, 0.3));
        
        return confidence;
    }
    
    private void updateRiskScores() {
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, LocationHistory> entry : locationHistories.entrySet()) {
            String nodeId = entry.getKey();
            LocationHistory history = entry.getValue();
            
            double recentRate = history.getRecentIncidentRate(3);
            double avgSeverity = history.getAverageSeverity();
            
            double risk = (recentRate * 50.0) + (avgSeverity * 25.0);
            currentRiskScores.put(nodeId, Math.min(risk, 100.0));
        }
    }
    
    public double getRiskScore(String nodeId) {
        return currentRiskScores.getOrDefault(nodeId, 0.0);
    }
    
    public List<ResourceRecommendation> getResourcePositioningRecommendations(int lookAheadHours) {
        LocalDateTime targetTime = LocalDateTime.now().plusHours(lookAheadHours);
        List<HotSpot> predictions = predictHighDemandAreas(targetTime);
        
        List<ResourceRecommendation> recommendations = new ArrayList<>();
        
        for (HotSpot hotspot : predictions) {
            if (hotspot.getPredictionScore() > 30.0 && hotspot.getConfidence() > 0.6) {
                int recommendedUnits = calculateRecommendedUnits(hotspot);
                
                recommendations.add(new ResourceRecommendation(
                    hotspot.getNodeId(),
                    recommendedUnits,
                    hotspot.getMostCommonType(),
                    hotspot.getPredictionScore(),
                    targetTime
                ));
            }
        }
        
        return recommendations;
    }
    
    private int calculateRecommendedUnits(HotSpot hotspot) {
        double score = hotspot.getPredictionScore();
        double severity = hotspot.getAverageSeverity();
        
        int units = 1;
        
        if (score > 50.0) units++;
        if (score > 75.0) units++;
        if (severity > 2.5) units++;
        
        return Math.min(units, 4);
    }
    
    public Map<DayOfWeek, Map<Integer, List<Incident>>> getIncidentPatterns() {
        return incidentsByDayAndHour;
    }
    
    public PredictionStatistics getStatistics() {
        int totalIncidents = locationHistories.values().stream()
            .mapToInt(LocationHistory::getTotalIncidents)
            .sum();
        
        double avgRisk = currentRiskScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        long highRiskLocations = currentRiskScores.values().stream()
            .filter(risk -> risk > 50.0)
            .count();
        
        return new PredictionStatistics(
            totalIncidents,
            locationHistories.size(),
            avgRisk,
            (int) highRiskLocations
        );
    }
    
    public void clearOldData(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        
        for (LocationHistory history : locationHistories.values()) {
            history.removeIncidentsBefore(cutoff);
        }
    }
    
    private static class LocationHistory {
        private final String nodeId;
        private final List<Incident> incidents;
        private final Map<DayOfWeek, Map<Integer, Integer>> timePatterns;
        
        LocationHistory(String nodeId) {
            this.nodeId = nodeId;
            this.incidents = new ArrayList<>();
            this.timePatterns = new HashMap<>();
            
            for (DayOfWeek day : DayOfWeek.values()) {
                Map<Integer, Integer> hourCounts = new HashMap<>();
                for (int hour = 0; hour < 24; hour++) {
                    hourCounts.put(hour, 0);
                }
                timePatterns.put(day, hourCounts);
            }
        }
        
        void addIncident(Incident incident) {
            incidents.add(incident);
            
            DayOfWeek day = incident.getTimestamp().getDayOfWeek();
            int hour = incident.getTimestamp().getHour();
            
            Map<Integer, Integer> hourCounts = timePatterns.get(day);
            hourCounts.put(hour, hourCounts.get(hour) + 1);
        }
        
        int getTotalIncidents() {
            return incidents.size();
        }
        
        int getIncidentCountForTime(DayOfWeek day, int hour) {
            return timePatterns.get(day).get(hour);
        }
        
        double getAverageSeverity() {
            if (incidents.isEmpty()) return 0.0;
            
            return incidents.stream()
                .mapToInt(i -> i.getSeverity().getLevel())
                .average()
                .orElse(0.0);
        }
        
        Incident.IncidentType getMostCommonIncidentType() {
            if (incidents.isEmpty()) return Incident.IncidentType.OTHER;
            
            Map<Incident.IncidentType, Long> typeCounts = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getType, Collectors.counting()));
            
            return typeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Incident.IncidentType.OTHER);
        }
        
        double getRecentIncidentRate(int days) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            
            long recentCount = incidents.stream()
                .filter(i -> i.getTimestamp().isAfter(cutoff))
                .count();
            
            return (double) recentCount / Math.max(days, 1);
        }
        
        double getVariance() {
            if (incidents.size() < 2) return 0.0;
            
            List<Integer> severities = incidents.stream()
                .map(i -> i.getSeverity().getLevel())
                .collect(Collectors.toList());
            
            double mean = severities.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double variance = severities.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);
            
            return variance;
        }
        
        void removeIncidentsBefore(LocalDateTime cutoff) {
            incidents.removeIf(i -> i.getTimestamp().isBefore(cutoff));
        }
    }
    
    public static class HotSpot {
        private final String nodeId;
        private final double predictionScore;
        private final double averageSeverity;
        private final Incident.IncidentType mostCommonType;
        private final double confidence;
        
        public HotSpot(String nodeId, double predictionScore, double averageSeverity,
                      Incident.IncidentType mostCommonType, double confidence) {
            this.nodeId = nodeId;
            this.predictionScore = predictionScore;
            this.averageSeverity = averageSeverity;
            this.mostCommonType = mostCommonType;
            this.confidence = confidence;
        }
        
        public String getNodeId() { return nodeId; }
        public double getPredictionScore() { return predictionScore; }
        public double getAverageSeverity() { return averageSeverity; }
        public Incident.IncidentType getMostCommonType() { return mostCommonType; }
        public double getConfidence() { return confidence; }
        
        @Override
        public String toString() {
            return String.format("HotSpot[%s: Score=%.2f, Confidence=%.2f, Type=%s]",
                nodeId, predictionScore, confidence, mostCommonType);
        }
    }
    
    public static class ResourceRecommendation {
        private final String nodeId;
        private final int recommendedUnits;
        private final Incident.IncidentType preferredType;
        private final double urgency;
        private final LocalDateTime targetTime;
        
        public ResourceRecommendation(String nodeId, int recommendedUnits,
                                    Incident.IncidentType preferredType,
                                    double urgency, LocalDateTime targetTime) {
            this.nodeId = nodeId;
            this.recommendedUnits = recommendedUnits;
            this.preferredType = preferredType;
            this.urgency = urgency;
            this.targetTime = targetTime;
        }
        
        public String getNodeId() { return nodeId; }
        public int getRecommendedUnits() { return recommendedUnits; }
        public Incident.IncidentType getPreferredType() { return preferredType; }
        public double getUrgency() { return urgency; }
        public LocalDateTime getTargetTime() { return targetTime; }
        
        @Override
        public String toString() {
            return String.format("Recommendation[%s: %d %s units, Urgency=%.2f]",
                nodeId, recommendedUnits, preferredType, urgency);
        }
    }
    
    public static class PredictionStatistics {
        public final int totalIncidents;
        public final int trackedLocations;
        public final double averageRisk;
        public final int highRiskLocations;
        
        public PredictionStatistics(int totalIncidents, int trackedLocations,
                                   double averageRisk, int highRiskLocations) {
            this.totalIncidents = totalIncidents;
            this.trackedLocations = trackedLocations;
            this.averageRisk = averageRisk;
            this.highRiskLocations = highRiskLocations;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Stats[%d incidents across %d locations, Avg Risk: %.2f, High Risk: %d]",
                totalIncidents, trackedLocations, averageRisk, highRiskLocations
            );
        }
    }
} 
