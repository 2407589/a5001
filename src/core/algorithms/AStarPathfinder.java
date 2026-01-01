 
package core.algorithms;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import java.util.*;

public class AStarPathfinder {
    
    private final EmergencyNetwork network;
    private boolean useTimeWeight;
    private double heuristicWeight;
    
    public AStarPathfinder(EmergencyNetwork network) {
        this.network = network;
        this.useTimeWeight = true;
        this.heuristicWeight = 1.0;
    }
    
    public void setUseTimeWeight(boolean useTimeWeight) {
        this.useTimeWeight = useTimeWeight;
    }
    
    public void setHeuristicWeight(double weight) {
        this.heuristicWeight = Math.max(0.1, Math.min(weight, 2.0));
    }
    
    public Path findOptimalPath(String startId, String endId) {
        Node start = network.getNode(startId);
        Node end = network.getNode(endId);
        
        if (start == null || end == null) {
            return new Path();
        }
        
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        Set<String> inOpenSet = new HashSet<>();
        Set<String> closedSet = new HashSet<>();
        Map<String, Double> gScore = new HashMap<>();
        Map<String, Double> fScore = new HashMap<>();
        Map<String, Node> previous = new HashMap<>();
        Map<String, Edge> previousEdge = new HashMap<>();
        
        for (Node node : network.getAllNodes()) {
            gScore.put(node.getId(), Double.POSITIVE_INFINITY);
            fScore.put(node.getId(), Double.POSITIVE_INFINITY);
        }
        
        double startH = calculateHeuristic(start, end);
        gScore.put(startId, 0.0);
        fScore.put(startId, startH);
        
        openSet.offer(new AStarNode(start, 0.0, startH));
        inOpenSet.add(startId);
        
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            String currentId = current.node.getId();
            inOpenSet.remove(currentId);
            
            if (currentId.equals(endId)) {
                return reconstructPath(start, end, previous, previousEdge);
            }
            
            closedSet.add(currentId);
            
            List<Edge> edges = network.getEdgesFromNode(currentId);
            
            for (Edge edge : edges) {
                if (!edge.isTraversable()) {
                    continue;
                }
                
                Node neighbor = edge.getOtherNode(current.node);
                if (neighbor == null || closedSet.contains(neighbor.getId())) {
                    continue;
                }
                
                double edgeWeight = useTimeWeight ? 
                    edge.getCurrentTravelTime() : edge.getBaseDistance();
                double tentativeGScore = gScore.get(currentId) + edgeWeight;
                
                if (tentativeGScore < gScore.get(neighbor.getId())) {
                    previous.put(neighbor.getId(), current.node);
                    previousEdge.put(neighbor.getId(), edge);
                    gScore.put(neighbor.getId(), tentativeGScore);
                    
                    double h = calculateHeuristic(neighbor, end);
                    double f = tentativeGScore + (heuristicWeight * h);
                    fScore.put(neighbor.getId(), f);
                    
                    if (!inOpenSet.contains(neighbor.getId())) {
                        openSet.offer(new AStarNode(neighbor, tentativeGScore, h));
                        inOpenSet.add(neighbor.getId());
                    }
                }
            }
        }
        
        return new Path();
    }
    
    private double calculateHeuristic(Node from, Node goal) {
        double straightLineDistance = from.getLocation().distanceTo(goal.getLocation());
        
        if (useTimeWeight) {
            final double OPTIMAL_SPEED_KMH = 80.0;
            return (straightLineDistance / OPTIMAL_SPEED_KMH) * 60.0;
        } else {
            return straightLineDistance;
        }
    }
    
    public Path findPathWithWaypoints(List<String> nodeIds) {
        if (nodeIds.size() < 2) {
            return new Path();
        }
        
        List<Node> allNodes = new ArrayList<>();
        List<Edge> allEdges = new ArrayList<>();
        
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            Path segment = findOptimalPath(nodeIds.get(i), nodeIds.get(i + 1));
            
            if (!segment.isValid()) {
                return new Path();
            }
            
            List<Node> segmentNodes = segment.getNodes();
            if (i == 0) {
                allNodes.addAll(segmentNodes);
            } else {
                allNodes.addAll(segmentNodes.subList(1, segmentNodes.size()));
            }
            
            allEdges.addAll(segment.getEdges());
        }
        
        return new Path(allNodes, allEdges);
    }
    
    private Path reconstructPath(Node start, Node end, Map<String, Node> previous,
                                Map<String, Edge> previousEdge) {
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        
        Node current = end;
        while (current != null && !current.equals(start)) {
            nodes.add(current);
            Edge edge = previousEdge.get(current.getId());
            if (edge != null) edges.add(edge);
            current = previous.get(current.getId());
        }
        
        if (current != null) nodes.add(start);
        
        Collections.reverse(nodes);
        Collections.reverse(edges);
        
        return new Path(nodes, edges);
    }
    
    private static class AStarNode implements Comparable<AStarNode> {
        final Node node;
        final double g;
        final double h;
        
        AStarNode(Node node, double g, double h) {
            this.node = node;
            this.g = g;
            this.h = h;
        }
        
        double getF() {
            return g + h;
        }
        
        @Override
        public int compareTo(AStarNode other) {
            return Double.compare(this.getF(), other.getF());
        }
    }
}