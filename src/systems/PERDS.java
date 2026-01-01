package system;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import core.algorithms.*;
import prediction.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class PERDS {
    
    private final EmergencyNetwork network;
    private final DijkstraPathfinder dijkstra;
    private final AStarPathfinder aStar;
    private final MultiCriteriaDispatch dispatcher;
    private final PredictiveAnalyzer predictor;
    private final AdaptiveLearningSystem learningSystem;
    
    private final PriorityQueue<Incident> incidentQueue;
    private final Map<String, ActiveDispatch> activeDispatches;
    private final List<CompletedDispatch> dispatchHistory;
    
    // SIMULATION MODE: Process instantly instead of real-time delays
    private final boolean simulationMode = true;
    private final PriorityQueue<ScheduledCompletion> completionQueue;
    private double simulatedTime = 0.0; // in minutes
    
    private boolean running;
    private LocalDateTime systemStartTime;
    private int totalIncidentsHandled;
    
    private final ScheduledExecutorService executor;
    
    public PERDS() {
        this.network = new EmergencyNetwork();
        this.dijkstra = new DijkstraPathfinder(network);
        this.aStar = new AStarPathfinder(network);
        this.predictor = new PredictiveAnalyzer();
        this.dispatcher = new MultiCriteriaDispatch(network);
        this.learningSystem = new AdaptiveLearningSystem(network, predictor);
        
        this.incidentQueue = new PriorityQueue<>();
        this.activeDispatches = new ConcurrentHashMap<>();
        this.dispatchHistory = new ArrayList<>();
        this.completionQueue = new PriorityQueue<>(
            Comparator.comparingDouble(ScheduledCompletion::getCompletionTime)
        );
        
        this.running = false;
        this.totalIncidentsHandled = 0;
        
        this.executor = Executors.newScheduledThreadPool(4);
    }
    
    public void initializeNetwork(NetworkConfiguration config) {
        System.out.println("Initializing Emergency Network...");
        
        for (NodeConfig nodeConfig : config.getNodes()) {
            Node node = new Node(
                nodeConfig.id,
                nodeConfig.name,
                nodeConfig.type,
                nodeConfig.location,
                nodeConfig.capacity
            );
            network.addNode(node);
        }
        
        for (EdgeConfig edgeConfig : config.getEdges()) {
            network.addEdge(edgeConfig.sourceId, edgeConfig.destId);
        }
        
        for (UnitDeployment deployment : config.getUnits()) {
            ResponseUnit unit = new ResponseUnit(deployment.unitId, deployment.type);
            Node node = network.getNode(deployment.nodeId);
            if (node != null) {
                node.addUnit(unit);
            }
        }
        
        System.out.println("Network initialized: " + network.getStatistics());
    }
    
    public void start() {
        if (running) {
            System.out.println("System already running");
            return;
        }
        
        running = true;
        systemStartTime = LocalDateTime.now();
        
        System.out.println("PERDS System Started at " + systemStartTime);
        System.out.println("Network Status: " + network.getStatistics());
        System.out.println("Simulation Mode: " + (simulationMode ? "ENABLED (Fast)" : "DISABLED (Real-time)"));
        
        if (!simulationMode) {
            startBackgroundTasks();
        }
    }
    
    private void startBackgroundTasks() {
        executor.scheduleAtFixedRate(
            this::performPredictivePositioning,
            5, 30, TimeUnit.SECONDS // Changed to seconds for demo
        );
        
        executor.scheduleAtFixedRate(
            this::optimizeNetwork,
            10, 60, TimeUnit.SECONDS // Changed to seconds for demo
        );
    }
    
    public synchronized void reportIncident(Incident incident) {
        if (!running) {
            System.out.println("System not running. Start system first.");
            return;
        }
        
        System.out.println("\n[INCIDENT REPORTED] " + incident);
        
        incidentQueue.offer(incident);
        
        Node nearestNode = findNearestNode(incident.getLocation());
        if (nearestNode != null) {
            predictor.recordIncident(incident, nearestNode.getId());
        }
        
        processNextIncident();
        
        totalIncidentsHandled++;
    }
    
    private synchronized void processNextIncident() {
        // First, process any completed dispatches in simulation mode
        if (simulationMode) {
            processCompletedDispatches();
        }
        
        if (incidentQueue.isEmpty()) {
            // Try to advance time if we're waiting for completions
            if (simulationMode) {
                advanceSimulatedTime();
            }
            return;
        }
        
        Incident incident = incidentQueue.poll();
        
        double[] weights = learningSystem.getOptimizedWeights();
        dispatcher.setWeights(weights[0], weights[1], weights[2], weights[3]);
        
        MultiCriteriaDispatch.DispatchDecision decision = 
            dispatcher.findOptimalUnit(incident);
        
        if (decision == null) {
            System.out.println("  [WARNING] No available units for incident: " + 
                incident.getId());
            incidentQueue.offer(incident);
            return;
        }
        
        executeDispatch(decision);
    }
    
    private void executeDispatch(MultiCriteriaDispatch.DispatchDecision decision) {
        ResponseUnit unit = decision.getUnit();
        Incident incident = decision.getIncident();
        
        unit.setStatus(ResponseUnit.UnitStatus.DISPATCHED);
        
        decision.getDispatchCenter().getBestAvailableUnit();
        
        ActiveDispatch active = new ActiveDispatch(
            decision, LocalDateTime.now(), simulatedTime
        );
        activeDispatches.put(unit.getId(), active);
        
        incident.setAssignedUnit(unit);
        incident.markResponseStarted();
        
        System.out.println("  [DISPATCHED] " + unit.getId() + " -> " + 
            incident.getId() + " (ETA: " + 
            String.format("%.2f", decision.getEstimatedResponseTime()) + " min, " +
            "Score: " + String.format("%.2f", decision.getScore()) + ")");
        
        scheduleDispatchCompletion(active);
    }
    
    private void scheduleDispatchCompletion(ActiveDispatch dispatch) {
        if (simulationMode) {
            // In simulation mode, add to priority queue for instant processing
            double completionTime = simulatedTime + dispatch.getEstimatedTime();
            completionQueue.offer(new ScheduledCompletion(dispatch, completionTime));
        } else {
            // Real-time mode (original behavior)
            executor.schedule(() -> {
                completeDispatch(dispatch);
            }, (long) (dispatch.getEstimatedTime() * 60), TimeUnit.SECONDS);
        }
    }
    
    private void processCompletedDispatches() {
        // Process completions up to current simulated time
        while (!completionQueue.isEmpty()) {
            ScheduledCompletion next = completionQueue.peek();
            
            // Check if this completion should happen now
            if (next.getCompletionTime() <= simulatedTime + 0.01) {
                ScheduledCompletion completion = completionQueue.poll();
                completeDispatch(completion.getDispatch());
            } else {
                // No more completions ready yet
                break;
            }
        }
    }
    
    private void advanceSimulatedTime() {
        // Advance time to next completion if queue is empty but completions pending
        if (incidentQueue.isEmpty() && !completionQueue.isEmpty()) {
            ScheduledCompletion next = completionQueue.peek();
            if (next != null && next.getCompletionTime() > simulatedTime) {
                simulatedTime = next.getCompletionTime();
                System.out.println("  [TIME ADVANCE] Simulated time: " + 
                    String.format("%.2f", simulatedTime) + " minutes");
                processCompletedDispatches();
            }
        }
    }
    
    private synchronized void completeDispatch(ActiveDispatch dispatch) {
        String unitId = dispatch.getUnit().getId();
        
        if (!activeDispatches.containsKey(unitId)) {
            return;
        }
        
        double actualTime = dispatch.getActualResponseTime();
        
        boolean successful = Math.random() < 
            (dispatch.wasResourceMatch() ? 0.90 : 0.75);
        
        dispatch.getIncident().markResolved();
        
        Node nearestNode = findNearestNode(dispatch.getIncident().getLocation());
        AdaptiveLearningSystem.DispatchOutcome outcome = 
            new AdaptiveLearningSystem.DispatchOutcome(
                dispatch.getUnit(),
                dispatch.getIncident(),
                nearestNode != null ? nearestNode.getId() : "unknown",
                actualTime,
                successful,
                dispatch.wasResourceMatch()
            );
        
        learningSystem.learnFromDispatch(outcome);
        
        ResponseUnit unit = dispatch.getUnit();
        unit.setStatus(ResponseUnit.UnitStatus.AVAILABLE);
        
        dispatch.getDispatchCenter().addUnit(unit);
        
        CompletedDispatch completed = new CompletedDispatch(dispatch, actualTime, successful);
        dispatchHistory.add(completed);
        activeDispatches.remove(unitId);
        
        System.out.println("  [COMPLETED] " + unit.getId() + " finished " + 
            dispatch.getIncident().getId() + " (Time: " + 
            String.format("%.2f", actualTime) + " min, Success: " + successful + ")");
        
        // Continue processing queue
        processNextIncident();
    }
    
    private void performPredictivePositioning() {
        if (!running) return;
        
        System.out.println("\n[PREDICTIVE POSITIONING] Analyzing demand patterns...");
        
        List<AdaptiveLearningSystem.RepositioningRecommendation> recommendations = 
            learningSystem.recommendRepositioning();
        
        if (recommendations.isEmpty()) {
            System.out.println("  No repositioning needed.");
            return;
        }
        
        System.out.println("  Found " + recommendations.size() + " repositioning opportunities:");
        
        for (AdaptiveLearningSystem.RepositioningRecommendation rec : recommendations) {
            if (rec.getConfidence() > 0.7 && rec.getUrgency() > 40.0) {
                repositionUnit(rec);
            }
        }
    }
    
    private void repositionUnit(AdaptiveLearningSystem.RepositioningRecommendation rec) {
        ResponseUnit unit = rec.getUnit();
        Node current = rec.getCurrentLocation();
        Node target = rec.getTargetLocation();
        
        current.getAllAvailableUnits().remove(unit);
        target.addUnit(unit);
        
        System.out.println("  [REPOSITIONED] " + unit.getId() + ": " + 
            current.getName() + " -> " + target.getName() + 
            " (Urgency: " + String.format("%.2f", rec.getUrgency()) + ")");
    }
    
    private void optimizeNetwork() {
        if (!running) return;
        
        System.out.println("\n[NETWORK OPTIMIZATION] Updating network state...");
        
        for (Edge edge : network.getAllEdges()) {
            double randomFactor = Math.random();
            edge.simulateTrafficUpdate(randomFactor);
        }
        
        System.out.println("  Network state updated.");
    }
    
    public void updateTrafficCondition(String edgeId, double congestionFactor) {
        network.updateEdgeCongestion(edgeId, congestionFactor);
        System.out.println("  [TRAFFIC UPDATE] Edge " + edgeId + 
            " congestion: " + String.format("%.2f", congestionFactor));
    }
    
    public void setRoadClosure(String edgeId, boolean blocked) {
        network.setEdgeBlocked(edgeId, blocked);
        System.out.println("  [ROAD " + (blocked ? "CLOSED" : "OPENED") + 
            "] Edge " + edgeId);
    }
    
    public SystemStatistics getStatistics() {
        // Force process any remaining completions before returning stats
        if (simulationMode) {
            System.out.println("\n[FINALIZING] Processing remaining dispatches...");
            int iterations = 0;
            while (!completionQueue.isEmpty() && iterations < 1000) {
                advanceSimulatedTime();
                processCompletedDispatches();
                iterations++;
            }
            if (!completionQueue.isEmpty()) {
                System.out.println("  [WARNING] " + completionQueue.size() + 
                    " dispatches still pending after max iterations");
            }
        }
        
        AdaptiveLearningSystem.SystemPerformanceMetrics perf = 
            learningSystem.getPerformanceMetrics();
        
        return new SystemStatistics(
            totalIncidentsHandled,
            activeDispatches.size(),
            incidentQueue.size(),
            dispatchHistory.size(),
            perf
        );
    }
    
    public Map<String, Double> getRiskAssessment() {
        Map<String, Double> risks = new HashMap<>();
        for (Node node : network.getAllNodes()) {
            risks.put(node.getId(), predictor.getRiskScore(node.getId()));
        }
        return risks;
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
    
    public void stop() {
        if (!running) return;
        
        running = false;
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nPERDS System Stopped");
        System.out.println("Final Statistics: " + getStatistics());
    }
    
    // Helper class for simulation mode scheduling
    private static class ScheduledCompletion {
        private final ActiveDispatch dispatch;
        private final double completionTime;
        
        ScheduledCompletion(ActiveDispatch dispatch, double completionTime) {
            this.dispatch = dispatch;
            this.completionTime = completionTime;
        }
        
        ActiveDispatch getDispatch() { return dispatch; }
        double getCompletionTime() { return completionTime; }
    }
    
    private static class ActiveDispatch {
        private final MultiCriteriaDispatch.DispatchDecision decision;
        private final LocalDateTime startTime;
        private final double simulatedStartTime;
        
        ActiveDispatch(MultiCriteriaDispatch.DispatchDecision decision, 
                      LocalDateTime startTime, double simulatedStartTime) {
            this.decision = decision;
            this.startTime = startTime;
            this.simulatedStartTime = simulatedStartTime;
        }
        
        ResponseUnit getUnit() { return decision.getUnit(); }
        Incident getIncident() { return decision.getIncident(); }
        Node getDispatchCenter() { return decision.getDispatchCenter(); }
        double getEstimatedTime() { return decision.getEstimatedResponseTime(); }
        boolean wasResourceMatch() { 
            return getUnit().isSuitableFor(getIncident().getType()); 
        }
        
        double getActualResponseTime() {
            // Use estimated time as actual in simulation mode
            return getEstimatedTime();
        }
    }
    
    private static class CompletedDispatch {
        private final ActiveDispatch dispatch;
        private final double actualTime;
        private final boolean successful;
        private final LocalDateTime completionTime;
        
        CompletedDispatch(ActiveDispatch dispatch, double actualTime, boolean successful) {
            this.dispatch = dispatch;
            this.actualTime = actualTime;
            this.successful = successful;
            this.completionTime = LocalDateTime.now();
        }
        
        public ActiveDispatch getDispatch() { return dispatch; }
        public double getActualTime() { return actualTime; }
        public boolean isSuccessful() { return successful; }
        public LocalDateTime getCompletionTime() { return completionTime; }
    }
    
    public static class SystemStatistics {
        public final int totalIncidents;
        public final int activeDispatches;
        public final int queuedIncidents;
        public final int completedDispatches;
        public final AdaptiveLearningSystem.SystemPerformanceMetrics performance;
        
        public SystemStatistics(int totalIncidents, int activeDispatches, 
                               int queuedIncidents, int completedDispatches,
                               AdaptiveLearningSystem.SystemPerformanceMetrics performance) {
            this.totalIncidents = totalIncidents;
            this.activeDispatches = activeDispatches;
            this.queuedIncidents = queuedIncidents;
            this.completedDispatches = completedDispatches;
            this.performance = performance;
        }
        
        @Override
        public String toString() {
            return String.format(
                "System[Total: %d, Active: %d, Queued: %d, Completed: %d]\n%s",
                totalIncidents, activeDispatches, queuedIncidents, 
                completedDispatches, performance
            );
        }
    }
    
    public static class NetworkConfiguration {
        private List<NodeConfig> nodes = new ArrayList<>();
        private List<EdgeConfig> edges = new ArrayList<>();
        private List<UnitDeployment> units = new ArrayList<>();
        
        public void addNode(NodeConfig node) { nodes.add(node); }
        public void addEdge(EdgeConfig edge) { edges.add(edge); }
        public void addUnit(UnitDeployment unit) { units.add(unit); }
        
        public List<NodeConfig> getNodes() { return nodes; }
        public List<EdgeConfig> getEdges() { return edges; }
        public List<UnitDeployment> getUnits() { return units; }
    }
    
    public static class NodeConfig {
        public String id, name;
        public Node.NodeType type;
        public Location location;
        public int capacity;
        
        public NodeConfig(String id, String name, Node.NodeType type, 
                         Location location, int capacity) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.location = location;
            this.capacity = capacity;
        }
    }
    
    public static class EdgeConfig {
        public String sourceId, destId;
        
        public EdgeConfig(String sourceId, String destId) {
            this.sourceId = sourceId;
            this.destId = destId;
        }
    }
    
    public static class UnitDeployment {
        public String unitId, nodeId;
        public ResponseUnit.UnitType type;
        
        public UnitDeployment(String unitId, String nodeId, ResponseUnit.UnitType type) {
            this.unitId = unitId;
            this.nodeId = nodeId;
            this.type = type;
        }
    }
}