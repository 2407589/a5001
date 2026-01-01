 
package core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Path {
    private final List<Node> nodes;
    private final List<Edge> edges;
    private double totalDistance;
    private double totalTime;
    private boolean isValid;
    
    public Path() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.totalDistance = 0.0;
        this.totalTime = 0.0;
        this.isValid = true;
    }
    
    public Path(List<Node> nodes, List<Edge> edges) {
        this.nodes = new ArrayList<>(nodes);
        this.edges = new ArrayList<>(edges);
        this.isValid = true;
        calculateMetrics();
    }
    
    public void addNode(Node node) {
        nodes.add(node);
    }
    
    public void addEdge(Edge edge) {
        edges.add(edge);
        totalDistance += edge.getBaseDistance();
        totalTime += edge.getCurrentTravelTime();
    }
    
    private void calculateMetrics() {
        totalDistance = 0.0;
        totalTime = 0.0;
        
        for (Edge edge : edges) {
            if (!edge.isTraversable()) {
                isValid = false;
                totalTime = Double.POSITIVE_INFINITY;
                return;
            }
            totalDistance += edge.getBaseDistance();
            totalTime += edge.getCurrentTravelTime();
        }
    }
    
    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
    
    public double getTotalDistance() {
        return totalDistance;
    }
    
    public double getTotalTime() {
        return totalTime;
    }
    
    public boolean isValid() {
        return isValid && !nodes.isEmpty();
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getEdgeCount() {
        return edges.size();
    }
    
    public Node getStartNode() {
        return nodes.isEmpty() ? null : nodes.get(0);
    }
    
    public Node getEndNode() {
        return nodes.isEmpty() ? null : nodes.get(nodes.size() - 1);
    }
    
    public Path reverse() {
        List<Node> reversedNodes = new ArrayList<>(nodes);
        Collections.reverse(reversedNodes);
        
        List<Edge> reversedEdges = new ArrayList<>(edges);
        Collections.reverse(reversedEdges);
        
        return new Path(reversedNodes, reversedEdges);
    }
    
    public double getEstimatedTimeMinutes() {
        return totalTime;
    }
    
    public double getEstimatedTimeSeconds() {
        return totalTime * 60.0;
    }
    
    @Override
    public String toString() {
        if (!isValid()) {
            return "Path[INVALID]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Path[");
        
        for (int i = 0; i < nodes.size(); i++) {
            sb.append(nodes.get(i).getName());
            if (i < nodes.size() - 1) {
                sb.append(" -> ");
            }
        }
        
        sb.append(String.format("] Distance: %.2f km, Time: %.2f min", 
            totalDistance, totalTime));
        
        return sb.toString();
    }
}