 
package core.models;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Represents a node in the emergency network
 * Can be a city, dispatch center, or incident site
 */
public class Node {
    private final String id;
    private final String name;
    private final NodeType type;
    private final Location location;
    private final PriorityQueue<ResponseUnit> availableUnits;
    private int capacity;
    
    public enum NodeType {
        CITY,
        DISPATCH_CENTER,
        INCIDENT_SITE
    }
    
    public Node(String id, String name, NodeType type, Location location, int capacity) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.capacity = capacity;
        this.availableUnits = new PriorityQueue<>((u1, u2) -> 
            Double.compare(u2.getReadinessScore(), u1.getReadinessScore())
        );
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public NodeType getType() {
        return type;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public void addUnit(ResponseUnit unit) {
        if (availableUnits.size() < capacity) {
            availableUnits.offer(unit);
            unit.setCurrentNode(this);
        }
    }
    
    public ResponseUnit getBestAvailableUnit() {
        return availableUnits.poll();
    }
    
    public List<ResponseUnit> getAllAvailableUnits() {
        return new ArrayList<>(availableUnits);
    }
    
    public boolean hasAvailableUnits() {
        return !availableUnits.isEmpty();
    }
    
    public int getAvailableUnitCount() {
        return availableUnits.size();
    }
    
    public boolean canDispatch() {
        return type == NodeType.DISPATCH_CENTER && hasAvailableUnits();
    }
    
    @Override
    public String toString() {
        return String.format("Node[%s: %s (%s) - %d units available]", 
            id, name, type, availableUnits.size());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node)) return false;
        Node other = (Node) obj;
        return id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}