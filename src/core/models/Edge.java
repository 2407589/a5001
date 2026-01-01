 
package core.models;

public class Edge {
    private final String id;
    private final Node source;
    private final Node destination;
    private final double baseDistance;
    private double currentTravelTime;
    private double congestionFactor;
    private boolean isBlocked;
    private RoadCondition condition;
    
    public enum RoadCondition {
        EXCELLENT(0.9),
        GOOD(1.0),
        FAIR(1.1),
        POOR(1.3),
        CLOSED(Double.POSITIVE_INFINITY);
        
        private final double speedMultiplier;
        
        RoadCondition(double speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
        }
        
        public double getSpeedMultiplier() {
            return speedMultiplier;
        }
    }
    
    public Edge(Node source, Node destination) {
        this.id = source.getId() + "-" + destination.getId();
        this.source = source;
        this.destination = destination;
        this.baseDistance = source.getLocation().distanceTo(destination.getLocation());
        this.congestionFactor = 1.0;
        this.isBlocked = false;
        this.condition = RoadCondition.GOOD;
        calculateTravelTime();
    }
    
    public Edge(String id, Node source, Node destination, double baseDistance) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.baseDistance = baseDistance;
        this.congestionFactor = 1.0;
        this.isBlocked = false;
        this.condition = RoadCondition.GOOD;
        calculateTravelTime();
    }
    
    public String getId() {
        return id;
    }
    
    public Node getSource() {
        return source;
    }
    
    public Node getDestination() {
        return destination;
    }
    
    public double getBaseDistance() {
        return baseDistance;
    }
    
    public double getCurrentTravelTime() {
        return currentTravelTime;
    }
    
    public double getCongestionFactor() {
        return congestionFactor;
    }
    
    public void setCongestionFactor(double factor) {
        this.congestionFactor = Math.max(0.5, Math.min(factor, 3.0));
        calculateTravelTime();
    }
    
    public boolean isBlocked() {
        return isBlocked;
    }
    
    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        if (blocked) {
            this.condition = RoadCondition.CLOSED;
        }
        calculateTravelTime();
    }
    
    public RoadCondition getCondition() {
        return condition;
    }
    
    public void setCondition(RoadCondition condition) {
        this.condition = condition;
        if (condition == RoadCondition.CLOSED) {
            this.isBlocked = true;
        }
        calculateTravelTime();
    }
    
    private void calculateTravelTime() {
        if (isBlocked || condition == RoadCondition.CLOSED) {
            currentTravelTime = Double.POSITIVE_INFINITY;
            return;
        }
        
        final double AVERAGE_SPEED_KMH = 60.0;
        double baseTimeMinutes = (baseDistance / AVERAGE_SPEED_KMH) * 60.0;
        currentTravelTime = baseTimeMinutes * congestionFactor * condition.getSpeedMultiplier();
    }
    
    public double getWeight() {
        return currentTravelTime;
    }
    
    public double getDistanceWeight() {
        return baseDistance;
    }
    
    public boolean isTraversable() {
        return !isBlocked && condition != RoadCondition.CLOSED;
    }
    
    public Node getOtherNode(Node node) {
        if (node.equals(source)) {
            return destination;
        } else if (node.equals(destination)) {
            return source;
        }
        return null;
    }
    
    public void simulateTrafficUpdate(double randomFactor) {
        double newCongestion = 1.0 + (randomFactor * 0.5);
        setCongestionFactor(newCongestion);
    }
    
    @Override
    public String toString() {
        return String.format("Edge[%s -> %s: %.2f km, %.2f min, congestion: %.2f]", 
            source.getName(), destination.getName(), baseDistance, 
            currentTravelTime, congestionFactor);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Edge)) return false;
        Edge other = (Edge) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}