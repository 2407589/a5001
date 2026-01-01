 
package analysis;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import core.algorithms.*;
import java.util.*;
import java.io.*;

public class PerformanceAnalyzer {
    
    private final PrintWriter csvWriter;
    
    public PerformanceAnalyzer(String outputFile) throws IOException {
        this.csvWriter = new PrintWriter(new FileWriter(outputFile));
    }
    
    public void runCompleteAnalysis() {
        System.out.println("=".repeat(80));
        System.out.println("PERDS PERFORMANCE ANALYSIS");
        System.out.println("=".repeat(80));
        
        System.out.println("\n[TEST 1] Pathfinding Algorithm Comparison");
        testPathfindingPerformance();
        
        System.out.println("\n[TEST 2] Network Scalability Analysis");
        testNetworkScalability();
        
        System.out.println("\n[TEST 3] System Load Testing");
        testSystemLoad();
        
        System.out.println("\n[TEST 4] Dynamic Update Performance");
        testDynamicUpdates();
        
        System.out.println("\nAnalysis complete. Results saved.");
        csvWriter.close();
    }
    
    private void testPathfindingPerformance() {
        int[] networkSizes = {10, 25, 50, 100, 200};
        
        csvWriter.println("Network Size,Algorithm,Time (ms),Nodes Explored,Path Length");
        
        for (int size : networkSizes) {
            EmergencyNetwork network = createTestNetwork(size);
            DijkstraPathfinder dijkstra = new DijkstraPathfinder(network);
            AStarPathfinder aStar = new AStarPathfinder(network);
            
            long dijkstraStart = System.nanoTime();
            Path dijkstraPath = dijkstra.findShortestPath("N0", "N" + (size - 1));
            long dijkstraTime = (System.nanoTime() - dijkstraStart) / 1_000_000;
            
            long aStarStart = System.nanoTime();
            Path aStarPath = aStar.findOptimalPath("N0", "N" + (size - 1));
            long aStarTime = (System.nanoTime() - aStarStart) / 1_000_000;
            
            csvWriter.println(String.format("%d,Dijkstra,%d,N/A,%d", 
                size, dijkstraTime, dijkstraPath.getNodeCount()));
            csvWriter.println(String.format("%d,A*,%d,N/A,%d", 
                size, aStarTime, aStarPath.getNodeCount()));
            
            System.out.println(String.format("  Size %d: Dijkstra=%dms, A*=%dms (%.1f%% improvement)",
                size, dijkstraTime, aStarTime, 
                ((dijkstraTime - aStarTime) / (double) dijkstraTime) * 100));
        }
        
        csvWriter.flush();
    }
    
    private void testNetworkScalability() {
        int[] sizes = {50, 100, 200, 500, 1000};
        
        csvWriter.println("\nNetwork Size,Operation,Time (ms)");
        
        for (int size : sizes) {
            long constructStart = System.nanoTime();
            EmergencyNetwork network = createTestNetwork(size);
            long constructTime = (System.nanoTime() - constructStart) / 1_000_000;
            
            csvWriter.println(String.format("%d,Construction,%d", size, constructTime));
            
            long lookupStart = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                network.getNode("N" + (i % size));
            }
            long lookupTime = (System.nanoTime() - lookupStart) / 1_000_000;
            
            csvWriter.println(String.format("%d,Lookup (1000x),%d", size, lookupTime));
            
            long connectStart = System.nanoTime();
            boolean connected = network.isConnected();
            long connectTime = (System.nanoTime() - connectStart) / 1_000_000;
            
            csvWriter.println(String.format("%d,Connectivity Check,%d", size, connectTime));
            
            System.out.println(String.format("  Size %d: Construct=%dms, Lookup=%dms, Connect=%dms",
                size, constructTime, lookupTime, connectTime));
        }
        
        csvWriter.flush();
    }
    
    private void testSystemLoad() {
        int[] incidentCounts = {10, 50, 100, 200, 500};
        EmergencyNetwork network = createTestNetwork(50);
        
        csvWriter.println("\nIncident Count,Processing Time (ms),Avg per Incident (ms)");
        
        for (int count : incidentCounts) {
            List<Incident> incidents = generateTestIncidents(count, network);
            PriorityQueue<Incident> queue = new PriorityQueue<>(incidents);
            
            long processStart = System.nanoTime();
            
            while (!queue.isEmpty()) {
                Incident incident = queue.poll();
                incident.getPriority();
            }
            
            long processTime = (System.nanoTime() - processStart) / 1_000_000;
            double avgTime = (double) processTime / count;
            
            csvWriter.println(String.format("%d,%d,%.3f", count, processTime, avgTime));
            
            System.out.println(String.format("  %d incidents: %dms total (%.3fms avg)",
                count, processTime, avgTime));
        }
        
        csvWriter.flush();
    }
    
    private void testDynamicUpdates() {
        EmergencyNetwork network = createTestNetwork(100);
        Collection<Edge> edges = network.getAllEdges();
        
        csvWriter.println("\nUpdate Type,Count,Time (ms),Avg per Update (ns)");
        
        int updateCount = 1000;
        List<Edge> edgeList = new ArrayList<>(edges);
        Random random = new Random();
        
        long updateStart = System.nanoTime();
        for (int i = 0; i < updateCount; i++) {
            Edge edge = edgeList.get(random.nextInt(edgeList.size()));
            edge.setCongestionFactor(1.0 + random.nextDouble());
        }
        long updateTime = (System.nanoTime() - updateStart) / 1_000_000;
        long avgTimeNs = (System.nanoTime() - updateStart) / updateCount;
        
        csvWriter.println(String.format("Edge Weight Update,%d,%d,%d", 
            updateCount, updateTime, avgTimeNs));
        
        System.out.println(String.format("  Edge updates: %d updates in %dms (%dns avg)",
            updateCount, updateTime, avgTimeNs));
        
        updateStart = System.nanoTime();
        for (int i = 0; i < updateCount; i++) {
            Edge edge = edgeList.get(random.nextInt(edgeList.size()));
            edge.setBlocked(i % 2 == 0);
        }
        updateTime = (System.nanoTime() - updateStart) / 1_000_000;
        avgTimeNs = (System.nanoTime() - updateStart) / updateCount;
        
        csvWriter.println(String.format("Edge Blocking,%d,%d,%d", 
            updateCount, updateTime, avgTimeNs));
        
        System.out.println(String.format("  Block/unblock: %d updates in %dms (%dns avg)",
            updateCount, updateTime, avgTimeNs));
        
        csvWriter.flush();
    }
    
    public void generateComplexityReport(String filename) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(filename));
        
        writer.println("PERDS - Algorithm Complexity Analysis");
        writer.println("=".repeat(80));
        writer.println();
        
        writer.println("1. CORE DATA STRUCTURES");
        writer.println("-".repeat(80));
        writer.println("EmergencyNetwork (Graph):");
        writer.println("  Representation: Adjacency List + HashMap");
        writer.println("  Space Complexity: O(V + E)");
        writer.println("  Operations:");
        writer.println("    - Add Node: O(1) average");
        writer.println("    - Add Edge: O(1) average");
        writer.println("    - Get Node: O(1) average (HashMap)");
        writer.println("    - Get Neighbors: O(degree(v))");
        writer.println("    - Check Connectivity: O(V + E) BFS");
        writer.println();
        
        writer.println("PriorityQueue (Binary Heap):");
        writer.println("  Space Complexity: O(n)");
        writer.println("  Operations:");
        writer.println("    - Insert: O(log n)");
        writer.println("    - Extract-Min: O(log n)");
        writer.println("    - Peek: O(1)");
        writer.println();
        
        writer.println("2. PATHFINDING ALGORITHMS");
        writer.println("-".repeat(80));
        writer.println("Dijkstra's Algorithm:");
        writer.println("  Time Complexity: O((V + E) log V)");
        writer.println("  Space Complexity: O(V)");
        writer.println();
        
        writer.println("A* Algorithm:");
        writer.println("  Time Complexity: O(E log V) average");
        writer.println("  Space Complexity: O(V)");
        writer.println();
        
        writer.close();
        System.out.println("Complexity report saved to: " + filename);
    }
    
    private EmergencyNetwork createTestNetwork(int size) {
        EmergencyNetwork network = new EmergencyNetwork();
        
        for (int i = 0; i < size; i++) {
            Node node = new Node(
                "N" + i,
                "Node " + i,
                i % 10 == 0 ? Node.NodeType.DISPATCH_CENTER : Node.NodeType.CITY,
                new Location(51.5 + (i / 10) * 0.01, -0.1 + (i % 10) * 0.01, "Loc " + i),
                5
            );
            network.addNode(node);
        }
        
        for (int i = 0; i < size - 1; i++) {
            for (int j = 1; j <= 3 && i + j < size; j++) {
                network.addEdge("N" + i, "N" + (i + j));
            }
        }
        
        return network;
    }
    
    private List<Incident> generateTestIncidents(int count, EmergencyNetwork network) {
        List<Incident> incidents = new ArrayList<>();
        Random random = new Random();
        
        Incident.IncidentType[] types = Incident.IncidentType.values();
        Incident.Severity[] severities = Incident.Severity.values();
        
        for (int i = 0; i < count; i++) {
            Incident incident = new Incident(
                "INC-" + i,
                types[random.nextInt(types.length)],
                severities[random.nextInt(severities.length)],
                new Location(51.5 + random.nextDouble() * 0.1, 
                           -0.1 + random.nextDouble() * 0.1, 
                           "Test Location"),
                "Test incident " + i
            );
            incidents.add(incident);
        }
        
        return incidents;
    }
    
    public static void main(String[] args) {
        try {
            PerformanceAnalyzer analyzer = new PerformanceAnalyzer("performance_results.csv");
            
            analyzer.runCompleteAnalysis();
            analyzer.generateComplexityReport("complexity_analysis.txt");
            
            System.out.println("\nPerformance analysis complete!");
            System.out.println("Results saved to:");
            System.out.println("  - performance_results.csv");
            System.out.println("  - complexity_analysis.txt");
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}