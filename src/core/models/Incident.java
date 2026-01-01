 
package core.models;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents an emergency incident
 */
public class Incident implements Comparable<Incident> {
    private final String id;
    private final IncidentType type;
    private final Severity severity;
    private final Location location;
    private final LocalDateTime timestamp;
    private IncidentStatus status;
    private String description;
    private ResponseUnit assignedUnit;
    private LocalDateTime responseTime;
    private LocalDateTime resolutionTime;
    
    public enum IncidentType {
        MEDICAL_EMERGENCY(1.0),
        FIRE(0.95),
        CRIME(0.85),
        TRAFFIC_ACCIDENT(0.80),
        HAZMAT(0.98),
        NATURAL_DISASTER(1.0),
        OTHER(0.70);
        
        private final double urgencyWeight;
        
        IncidentType(double urgencyWeight) {
            this.urgencyWeight = urgencyWeight;
        }
        
        public double getUrgencyWeight() {
            return urgencyWeight;
        }
    }
    
    public enum Severity {
        CRITICAL(4, 180),
        HIGH(3, 300),
        MEDIUM(2, 600),
        LOW(1, 900);
        
        private final int level;
        private final int targetResponseSeconds;
        
        Severity(int level, int targetResponseSeconds) {
            this.level = level;
            this.targetResponseSeconds = targetResponseSeconds;
        }
        
        public int getLevel() {
            return level;
        }
        
        public int getTargetResponseSeconds() {
            return targetResponseSeconds;
        }
    }
    
    public enum IncidentStatus {
        REPORTED,
        ASSIGNED,
        RESPONDING,
        ON_SCENE,
        RESOLVED,
        CANCELLED
    }
    
    public Incident(String id, IncidentType type, Severity severity, 
                   Location location, String description) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.status = IncidentStatus.REPORTED;
    }
    
    public String getId() {
        return id;
    }
    
    public IncidentType getType() {
        return type;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public IncidentStatus getStatus() {
        return status;
    }
    
    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ResponseUnit getAssignedUnit() {
        return assignedUnit;
    }
    
    public void setAssignedUnit(ResponseUnit unit) {
        this.assignedUnit = unit;
        this.status = IncidentStatus.ASSIGNED;
    }
    
    public void markResponseStarted() {
        this.responseTime = LocalDateTime.now();
        this.status = IncidentStatus.RESPONDING;
    }
    
    public void markResolved() {
        this.resolutionTime = LocalDateTime.now();
        this.status = IncidentStatus.RESOLVED;
    }
    
    public double getPriority() {
        double basePriority = severity.getLevel() * 100.0;
        long minutesWaiting = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
        double timeUrgency = Math.min(minutesWaiting * 2.0, 100.0);
        double typeWeight = type.getUrgencyWeight() * 50.0;
        return basePriority + timeUrgency + typeWeight;
    }
    
    public long getActualResponseTime() {
        if (responseTime == null) {
            return -1;
        }
        return ChronoUnit.SECONDS.between(timestamp, responseTime);
    }
    
    public long getTotalResolutionTime() {
        if (resolutionTime == null) {
            return -1;
        }
        return ChronoUnit.SECONDS.between(timestamp, resolutionTime);
    }
    
    public boolean metTargetTime() {
        long actualTime = getActualResponseTime();
        if (actualTime < 0) {
            return false;
        }
        return actualTime <= severity.getTargetResponseSeconds();
    }
    
    @Override
    public int compareTo(Incident other) {
        return Double.compare(other.getPriority(), this.getPriority());
    }
    
    @Override
    public String toString() {
        return String.format("Incident[%s: %s - %s at %s - Priority: %.2f]", 
            id, type, severity, location.getAddress(), getPriority());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Incident)) return false;
        Incident other = (Incident) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}