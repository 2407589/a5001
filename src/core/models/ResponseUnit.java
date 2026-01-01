 
package core.models;

/**
 * Represents an emergency response unit
 */
public class ResponseUnit {
    private final String id;
    private final UnitType type;
    private UnitStatus status;
    private Node currentNode;
    private double readinessScore;
    private int successfulDispatches;
    private double averageResponseTime;
    private int totalDispatches;
    
    public enum UnitType {
        AMBULANCE(1.0, "Medical Emergency"),
        FIRE_TRUCK(0.9, "Fire Incident"),
        POLICE_CAR(0.8, "Crime/Security"),
        HAZMAT(1.2, "Hazardous Materials"),
        RESCUE_HELICOPTER(1.5, "Critical/Remote");
        
        private final double priorityWeight;
        private final String specialization;
        
        UnitType(double priorityWeight, String specialization) {
            this.priorityWeight = priorityWeight;
            this.specialization = specialization;
        }
        
        public double getPriorityWeight() {
            return priorityWeight;
        }
        
        public String getSpecialization() {
            return specialization;
        }
    }
    
    public enum UnitStatus {
        AVAILABLE,
        DISPATCHED,
        RESPONDING,
        ON_SCENE,
        RETURNING,
        MAINTENANCE
    }
    
    public ResponseUnit(String id, UnitType type) {
        this.id = id;
        this.type = type;
        this.status = UnitStatus.AVAILABLE;
        this.readinessScore = 1.0;
        this.successfulDispatches = 0;
        this.averageResponseTime = 0.0;
        this.totalDispatches = 0;
    }
    
    public String getId() {
        return id;
    }
    
    public UnitType getType() {
        return type;
    }
    
    public UnitStatus getStatus() {
        return status;
    }
    
    public void setStatus(UnitStatus status) {
        this.status = status;
    }
    
    public Node getCurrentNode() {
        return currentNode;
    }
    
    public void setCurrentNode(Node node) {
        this.currentNode = node;
    }
    
    public double getReadinessScore() {
        if (status != UnitStatus.AVAILABLE) {
            return 0.0;
        }
        
        double score = 1.0;
        
        if (totalDispatches > 0) {
            double successRate = (double) successfulDispatches / totalDispatches;
            score *= (0.5 + 0.5 * successRate);
        }
        
        if (averageResponseTime > 0) {
            score *= Math.max(0.7, 1.0 - (averageResponseTime / 100.0));
        }
        
        score *= type.getPriorityWeight();
        
        this.readinessScore = score;
        return score;
    }
    
    public boolean isSuitableFor(Incident.IncidentType incidentType) {
        switch (incidentType) {
            case MEDICAL_EMERGENCY:
                return type == UnitType.AMBULANCE || type == UnitType.RESCUE_HELICOPTER;
            case FIRE:
                return type == UnitType.FIRE_TRUCK || type == UnitType.RESCUE_HELICOPTER;
            case CRIME:
            case TRAFFIC_ACCIDENT:
                return type == UnitType.POLICE_CAR || type == UnitType.AMBULANCE;
            case HAZMAT:
                return type == UnitType.HAZMAT || type == UnitType.FIRE_TRUCK;
            default:
                return true;
        }
    }
    
    public void updatePerformanceMetrics(boolean successful, double responseTime) {
        totalDispatches++;
        if (successful) {
            successfulDispatches++;
        }
        
        averageResponseTime = ((averageResponseTime * (totalDispatches - 1)) + responseTime) 
                            / totalDispatches;
        
        getReadinessScore();
    }
    
    public boolean isAvailable() {
        return status == UnitStatus.AVAILABLE;
    }
    
    public int getSuccessfulDispatches() {
        return successfulDispatches;
    }
    
    public double getAverageResponseTime() {
        return averageResponseTime;
    }
    
    public int getTotalDispatches() {
        return totalDispatches;
    }
    
    @Override
    public String toString() {
        return String.format("Unit[%s: %s - %s - Score: %.2f]", 
            id, type, status, readinessScore);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResponseUnit)) return false;
        ResponseUnit other = (ResponseUnit) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}