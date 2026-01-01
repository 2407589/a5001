 
package core.algorithms;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import java.util.*;

public class DijkstraPathfinder {
    
    private final EmergencyNetwork network;
    private boolean useTimeWeight;
    
    public DijkstraPathfinder(EmergencyNetwork network) {
        this.network = network;
        this.useTimeWeight = true;
    }
    
    public void setUseTimeWeight(boolean useTimeWeight) {
        this.useTimeWeight = useTimeWeight;
    }
    
    public Path findShortestPath(String startId, String endId) {
        Node start = network.getNode(startId);
        Node end = network.getNode(endId);
        
        if (start == null || end == null) {
            return new Path();
        }
        
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, Node> previous = new HashMap<>();
        Map<String, Edge> previousEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        for (Node node : network.getAllNodes()) {
            distances.put(node.getId(), Double.POSITIVE_INFINITY);
        }
        distances.put(startId, 0.0);
        
        pq.offer(new NodeDistance(start, 0.0));
        
        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String currentId = current.node.getId();
            
            if (visited.contains(currentId)) {
                continue;
            }
            
            visited.add(currentId);
            
            if (currentId.equals(endId)) {
                break;
            }
            
            List<Edge> edges = network.getEdgesFromNode(currentId);
            
            for (Edge edge : edges) {
                if (!edge.isTraversable()) {
                    continue;
                }
                
                Node neighbor = edge.getOtherNode(current.node);
                if (neighbor == null || visited.contains(neighbor.getId())) {
                    continue;
                }
                
                double edgeWeight = useTimeWeight ? 
                    edge.getCurrentTravelTime() : edge.getBaseDistance();
                
                double newDistance = distances.get(currentId) + edgeWeight;
                
                if (newDistance < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), newDistance);
                    previous.put(neighbor.getId(), current.node);
                    previousEdge.put(neighbor.getId(), edge);
                    pq.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }
        
        return reconstructPath(start, end, previous, previousEdge);
    }
    
    public Map<String, Path> findAllShortestPaths(String startId) {
        Node start = network.getNode(startId);
        if (start == null) {
            return new HashMap<>();
        }
        
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>();
        Map<String, Double> distances = new HashMap<>();
        Map<String, Node> previous = new HashMap<>();
        Map<String, Edge> previousEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        for (Node node : network.getAllNodes()) {
            distances.put(node.getId(), Double.POSITIVE_INFINITY);
        }
        distances.put(startId, 0.0);
        pq.offer(new NodeDistance(start, 0.0));
        
        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            String currentId = current.node.getId();
            
            if (visited.contains(currentId)) {
                continue;
            }
            visited.add(currentId);
            
            List<Edge> edges = network.getEdgesFromNode(currentId);
            
            for (Edge edge : edges) {
                if (!edge.isTraversable()) continue;
                
                Node neighbor = edge.getOtherNode(current.node);
                if (neighbor == null || visited.contains(neighbor.getId())) {
                    continue;
                }
                
                double edgeWeight = useTimeWeight ? 
                    edge.getCurrentTravelTime() : edge.getBaseDistance();
                double newDistance = distances.get(currentId) + edgeWeight;
                
                if (newDistance < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), newDistance);
                    previous.put(neighbor.getId(), current.node);
                    previousEdge.put(neighbor.getId(), edge);
                    pq.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }
        
        Map<String, Path> paths = new HashMap<>();
        for (String nodeId : distances.keySet()) {
            if (!nodeId.equals(startId) && 
                distances.get(nodeId) != Double.POSITIVE_INFINITY) {
                Node end = network.getNode(nodeId);
                Path path = reconstructPath(start, end, previous, previousEdge);
                paths.put(nodeId, path);
            }
        }
        
        return paths;
    }
    
    private Path reconstructPath(Node start, Node end, 
                                 Map<String, Node> previous,
                                 Map<String, Edge> previousEdge) {
        Path path = new Path();
        
        if (!previous.containsKey(end.getId()) && !start.equals(end)) {
            return path;
        }
        
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        
        Node current = end;
        
        while (current != null && !current.equals(start)) {
            nodes.add(current);
            
            Edge edge = previousEdge.get(current.getId());
            if (edge != null) {
                edges.add(edge);
            }
            
            current = previous.get(current.getId());
        }
        
        if (current != null) {
            nodes.add(start);
        }
        
        Collections.reverse(nodes);
        Collections.reverse(edges);
        
        return new Path(nodes, edges);
    }
    
    private static class NodeDistance implements Comparable<NodeDistance> {
        final Node node;
        final double distance;
        
        NodeDistance(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}