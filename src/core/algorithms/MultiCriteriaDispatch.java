package core.algorithms;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import java.util.*;

public class MultiCriteriaDispatch {
    
    private final EmergencyNetwork network;
    private final AStarPathfinder pathfinder;
    
    private double distanceWeight = 0.30;
    private double resourceMatchWeight = 0.40;
    private double availabilityWeight = 0.20;
    private double performanceWeight = 0.10;
    
    public MultiCriteriaDispatch(EmergencyNetwork network) {
        this.network = network;
        this.pathfinder = new AStarPathfinder(network);
    }
    
    public void setWeights(double distance, double resourceMatch, 
                          double availability, double performance) {
        double total = distance + resourceMatch + availability + performance;
        this.distanceWeight = distance / total;
        this.resourceMatchWeight = resourceMatch / total;
        this.availabilityWeight = availability / total;
        this.performanceWeight = performance / total;
    }
    
    public DispatchDecision findOptimalUnit(Incident incident) {
        List<Node> dispatchCenters = network.getAvailableDispatchCenters();
        
        if (dispatchCenters.isEmpty()) {
            return null;
        }
        
        List<UnitCandidate> candidates = new ArrayList<>();
        
        Node incidentNode = findNearestNode(incident.getLocation());
        if (incidentNode == null) {
            return null;
        }
        
        for (Node center : dispatchCenters) {
            List<ResponseUnit> units = center.getAllAvailableUnits();
            
            for (ResponseUnit unit : units) {
                if (!unit.isAvailable()) continue;
                
                Path path = pathfinder.findOptimalPath(center.getId(), incidentNode.getId());
                
                if (!path.isValid()) continue;
                
                double score = calculateScore(unit, incident, path);
                candidates.add(new UnitCandidate(unit, center, path, score));
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        UnitCandidate best = candidates.get(0);
        
        return new DispatchDecision(
            best.unit,
            best.dispatchCenter,
            best.path,
            incident,
            best.score
        );
    }
    
    private double calculateScore(ResponseUnit unit, Incident incident, Path path) {
        double distanceScore = calculateDistanceScore(path);
        double resourceScore = calculateResourceMatchScore(unit, incident);
        double availScore = calculateAvailabilityScore(unit);
        double perfScore = calculatePerformanceScore(unit);
        
        return (distanceWeight * distanceScore) +
               (resourceMatchWeight * resourceScore) +
               (availabilityWeight * availScore) +
               (performanceWeight * perfScore);
    }
    
    private double calculateDistanceScore(Path path) {
        double time = path.getTotalTime();
        return 100.0 * Math.exp(-time / 15.0);
    }
    
    private double calculateResourceMatchScore(ResponseUnit unit, Incident incident) {
        if (unit.isSuitableFor(incident.getType())) {
            return 100.0;
        } else {
            return 40.0;
        }
    }
    
    private double calculateAvailabilityScore(ResponseUnit unit) {
        return unit.getReadinessScore() * 100.0;
    }
    
    private double calculatePerformanceScore(ResponseUnit unit) {
        if (unit.getTotalDispatches() == 0) {
            return 70.0;
        }
        
        double successRate = (double) unit.getSuccessfulDispatches() / 
                            unit.getTotalDispatches();
        return successRate * 100.0;
    }
    
    public List<DispatchDecision> findAllViableOptions(Incident incident) {
        List<Node> dispatchCenters = network.getAvailableDispatchCenters();
        List<DispatchDecision> options = new ArrayList<>();
        
        Node incidentNode = findNearestNode(incident.getLocation());
        if (incidentNode == null) return options;
        
        for (Node center : dispatchCenters) {
            List<ResponseUnit> units = center.getAllAvailableUnits();
            
            for (ResponseUnit unit : units) {
                if (!unit.isAvailable()) continue;
                
                Path path = pathfinder.findOptimalPath(center.getId(), incidentNode.getId());
                if (!path.isValid()) continue;
                
                double score = calculateScore(unit, incident, path);
                
                if (score > 30.0) {
                    options.add(new DispatchDecision(
                        unit, center, path, incident, score
                    ));
                }
            }
        }
        
        options.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return options;
    }
    
    private Node findNearestNode(Location location) {
        Node nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Node node : network.getAllNodes()) {
            double distance = node.getLocation().distanceTo(location);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = node;
            }
        }
        
        return nearest;
    }
    
    public static class DispatchDecision {
        private final ResponseUnit unit;
        private final Node dispatchCenter;
        private final Path path;
        private final Incident incident;
        private final double score;
        
        public DispatchDecision(ResponseUnit unit, Node dispatchCenter, 
                               Path path, Incident incident, double score) {
            this.unit = unit;
            this.dispatchCenter = dispatchCenter;
            this.path = path;
            this.incident = incident;
            this.score = score;
        }
        
        public ResponseUnit getUnit() { return unit; }
        public Node getDispatchCenter() { return dispatchCenter; }
        public Path getPath() { return path; }
        public Incident getIncident() { return incident; }
        public double getScore() { return score; }
        
        public double getEstimatedResponseTime() {
            return path.getTotalTime();
        }
        
        public boolean willMeetTarget() {
            long targetSeconds = incident.getSeverity().getTargetResponseSeconds();
            double estimatedSeconds = path.getEstimatedTimeSeconds();
            return estimatedSeconds <= targetSeconds;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Dispatch[%s -> %s via %s, Score: %.2f, ETA: %.2f min, Meets Target: %s]",
                unit.getId(), incident.getId(), dispatchCenter.getName(),
                score, path.getTotalTime(), willMeetTarget()
            );
        }
    }
    
    private static class UnitCandidate {
        final ResponseUnit unit;
        final Node dispatchCenter;
        final Path path;
        final double score;
        
        UnitCandidate(ResponseUnit unit, Node center, Path path, double score) {
            this.unit = unit;
            this.dispatchCenter = center;
            this.path = path;
            this.score = score;
        }
    }
} 
