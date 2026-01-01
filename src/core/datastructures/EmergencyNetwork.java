 
package core.datastructures;

import core.models.*;
import java.util.*;

public class EmergencyNetwork {
    private final Map<String, Node> nodes;
    private final Map<String, List<Edge>> adjacencyList;
    private final Map<String, Edge> edges;
    private final Set<Node> dispatchCenters;
    private int totalNodes;
    private int totalEdges;
    
    public EmergencyNetwork() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.edges = new HashMap<>();
        this.dispatchCenters = new HashSet<>();
        this.totalNodes = 0;
        this.totalEdges = 0;
    }
    
    public void addNode(Node node) {
        if (!nodes.containsKey(node.getId())) {
            nodes.put(node.getId(), node);
            adjacencyList.put(node.getId(), new ArrayList<>());
            
            if (node.getType() == Node.NodeType.DISPATCH_CENTER) {
                dispatchCenters.add(node);
            }
            
            totalNodes++;
        }
    }
    
    public void removeNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null) return;
        
        List<Edge> connectedEdges = new ArrayList<>(adjacencyList.get(nodeId));
        for (Edge edge : connectedEdges) {
            removeEdge(edge.getId());
        }
        
        nodes.remove(nodeId);
        adjacencyList.remove(nodeId);
        dispatchCenters.remove(node);
        totalNodes--;
    }
    
    public void addEdge(Edge edge) {
        String sourceId = edge.getSource().getId();
        String destId = edge.getDestination().getId();
        
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(destId)) {
            throw new IllegalArgumentException("Both nodes must exist in network");
        }
        
        adjacencyList.get(sourceId).add(edge);
        adjacencyList.get(destId).add(edge);
        
        edges.put(edge.getId(), edge);
        totalEdges++;
    }
    
    public Edge addEdge(String sourceId, String destId) {
        Node source = nodes.get(sourceId);
        Node dest = nodes.get(destId);
        
        if (source == null || dest == null) {
            throw new IllegalArgumentException("Both nodes must exist");
        }
        
        Edge edge = new Edge(source, dest);
        addEdge(edge);
        return edge;
    }
    
    public void removeEdge(String edgeId) {
        Edge edge = edges.get(edgeId);
        if (edge == null) return;
        
        String sourceId = edge.getSource().getId();
        String destId = edge.getDestination().getId();
        
        adjacencyList.get(sourceId).remove(edge);
        adjacencyList.get(destId).remove(edge);
        edges.remove(edgeId);
        totalEdges--;
    }
    
    public void updateEdgeCongestion(String edgeId, double congestionFactor) {
        Edge edge = edges.get(edgeId);
        if (edge != null) {
            edge.setCongestionFactor(congestionFactor);
        }
    }
    
    public void setEdgeBlocked(String edgeId, boolean blocked) {
        Edge edge = edges.get(edgeId);
        if (edge != null) {
            edge.setBlocked(blocked);
        }
    }
    
    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    public Edge getEdge(String edgeId) {
        return edges.get(edgeId);
    }
    
    public List<Edge> getEdgesFromNode(String nodeId) {
        return new ArrayList<>(adjacencyList.getOrDefault(nodeId, new ArrayList<>()));
    }
    
    public List<Node> getNeighbors(String nodeId) {
        List<Edge> edges = adjacencyList.get(nodeId);
        if (edges == null) return new ArrayList<>();
        
        List<Node> neighbors = new ArrayList<>();
        Node currentNode = nodes.get(nodeId);
        
        for (Edge edge : edges) {
            if (edge.isTraversable()) {
                Node neighbor = edge.getOtherNode(currentNode);
                if (neighbor != null) {
                    neighbors.add(neighbor);
                }
            }
        }
        
        return neighbors;
    }
    
    public List<Node> getAvailableDispatchCenters() {
        List<Node> available = new ArrayList<>();
        for (Node center : dispatchCenters) {
            if (center.hasAvailableUnits()) {
                available.add(center);
            }
        }
        return available;
    }
    
    public Node findNearestDispatchCenter(Location location) {
        Node nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Node center : dispatchCenters) {
            if (center.hasAvailableUnits()) {
                double distance = center.getLocation().distanceTo(location);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = center;
                }
            }
        }
        
        return nearest;
    }
    
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }
    
    public Collection<Edge> getAllEdges() {
        return edges.values();
    }
    
    public int getNodeCount() {
        return totalNodes;
    }
    
    public int getEdgeCount() {
        return totalEdges;
    }
    
    public boolean isConnected() {
        if (nodes.isEmpty()) return true;
        
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        String startId = nodes.keySet().iterator().next();
        queue.offer(startId);
        visited.add(startId);
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            
            for (Node neighbor : getNeighbors(currentId)) {
                if (!visited.contains(neighbor.getId())) {
                    visited.add(neighbor.getId());
                    queue.offer(neighbor.getId());
                }
            }
        }
        
        return visited.size() == totalNodes;
    }
    
    public void clear() {
        nodes.clear();
        adjacencyList.clear();
        edges.clear();
        dispatchCenters.clear();
        totalNodes = 0;
        totalEdges = 0;
    }
    
    public NetworkStats getStatistics() {
        int availableUnits = 0;
        for (Node node : dispatchCenters) {
            availableUnits += node.getAvailableUnitCount();
        }
        
        return new NetworkStats(
            totalNodes,
            totalEdges,
            dispatchCenters.size(),
            availableUnits,
            isConnected()
        );
    }
    
    public static class NetworkStats {
        public final int totalNodes;
        public final int totalEdges;
        public final int dispatchCenters;
        public final int availableUnits;
        public final boolean isConnected;
        
        public NetworkStats(int totalNodes, int totalEdges, int dispatchCenters, 
                          int availableUnits, boolean isConnected) {
            this.totalNodes = totalNodes;
            this.totalEdges = totalEdges;
            this.dispatchCenters = dispatchCenters;
            this.availableUnits = availableUnits;
            this.isConnected = isConnected;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Network Stats: %d nodes, %d edges, %d dispatch centers, %d available units, connected: %s",
                totalNodes, totalEdges, dispatchCenters, availableUnits, isConnected
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("EmergencyNetwork[%d nodes, %d edges, %d dispatch centers]",
            totalNodes, totalEdges, dispatchCenters.size());
    }
}