package test;

import core.datastructures.EmergencyNetwork;
import core.models.*;
import core.algorithms.*;
import prediction.*;
import org.junit.jupiter.api.*;
import org.w3c.dom.Node;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class PERDSTests {
    
    private EmergencyNetwork network;
    private Node nodeA, nodeB, nodeC;
    private Edge edgeAB, edgeBC;
    
    @BeforeEach
    void setUp() {
        network = new EmergencyNetwork();
        
        nodeA = new Node("A", "Node A", Node.NodeType.DISPATCH_CENTER,
            new Location(51.5074, -0.1278, "Location A"), 5);
        nodeB = new Node("B", "Node B", Node.NodeType.CITY,
            new Location(51.6074, -0.1278, "Location B"), 5);
        nodeC = new Node("C", "Node C", Node.NodeType.DISPATCH_CENTER,
            new Location(51.5074, -0.0278, "Location C"), 5);
        
        network.addNode(nodeA);
        network.addNode(nodeB);
        network.addNode(nodeC);
        
        edgeAB = network.addEdge("A", "B");
        edgeBC = network.addEdge("B", "C");
    }
    
    @Test
    @DisplayName("Test Haversine Distance Calculation")
    void testHaversineDistance() {
        Location loc1 = new Location(51.5074, -0.1278, "London");
        Location loc2 = new Location(48.8566, 2.3522, "Paris");
        
        double distance = loc1.distanceTo(loc2);
        
        assertTrue(distance > 340 && distance < 350, 
            "Distance should be approximately 344 km");
    }
    
    @Test
    @DisplayName("Test Location Equality")
    void testLocationEquality() {
        Location loc1 = new Location(51.5074, -0.1278, "A");
        Location loc2 = new Location(51.5074, -0.1278, "B");
        
        assertEquals(loc1, loc2, "Locations with same coordinates should be equal");
    }
    
    @Test
    @DisplayName("Test Network Node Addition")
    void testNodeAddition() {
        assertEquals(3, network.getNodeCount(), "Should have 3 nodes");
        assertNotNull(network.getNode("A"), "Node A should exist");
    }
    
    @Test
    @DisplayName("Test Network Edge Addition")
    void testEdgeAddition() {
        assertEquals(2, network.getEdgeCount(), "Should have 2 edges");
        
        List<Node> neighborsA = network.getNeighbors("A");
        assertTrue(neighborsA.contains(nodeB), "B should be neighbor of A");
    }
    
    @Test
    @DisplayName("Test Network Connectivity")
    void testNetworkConnectivity() {
        assertTrue(network.isConnected(), "Network should be connected");
        
        Node isolated = new Node("D", "Node D", Node.NodeType.CITY,
            new Location(52.0, -1.0, "Isolated"), 5);
        network.addNode(isolated);
        
        assertFalse(network.isConnected(), 
            "Network should be disconnected with isolated node");
    }
    
    @Test
    @DisplayName("Test Dynamic Edge Weight Update")
    void testDynamicEdgeUpdate() {
        double initialTime = edgeAB.getCurrentTravelTime();
        
        edgeAB.setCongestionFactor(2.0);
        
        double newTime = edgeAB.getCurrentTravelTime();
        assertTrue(newTime > initialTime, 
            "Travel time should increase with congestion");
    }
    
    @Test
    @DisplayName("Test Edge Blocking")
    void testEdgeBlocking() {
        assertTrue(edgeAB.isTraversable(), "Edge should initially be traversable");
        
        edgeAB.setBlocked(true);
        
        assertFalse(edgeAB.isTraversable(), "Blocked edge should not be traversable");
        assertEquals(Double.POSITIVE_INFINITY, edgeAB.getCurrentTravelTime(),
            "Blocked edge should have infinite travel time");
    }
    
    @Test
    @DisplayName("Test Incident Priority Calculation")
    void testIncidentPriority() {
        Incident critical = new Incident("I1", Incident.IncidentType.MEDICAL_EMERGENCY,
            Incident.Severity.CRITICAL, 
            new Location(51.5, -0.1, "Test"), "Critical emergency");
        
        Incident low = new Incident("I2", Incident.IncidentType.OTHER,
            Incident.Severity.LOW,
            new Location(51.5, -0.1, "Test"), "Low priority");
        
        assertTrue(critical.getPriority() > low.getPriority(),
            "Critical incident should have higher priority");
    }
    
    @Test
    @DisplayName("Test Incident Priority Queue Ordering")
    void testIncidentQueueOrdering() {
        PriorityQueue<Incident> queue = new PriorityQueue<>();
        
        Incident low = new Incident("I1", Incident.IncidentType.OTHER,
            Incident.Severity.LOW, new Location(51.5, -0.1, "Test"), "Low");
        Incident high = new Incident("I2", Incident.IncidentType.FIRE,
            Incident.Severity.HIGH, new Location(51.5, -0.1, "Test"), "High");
        Incident critical = new Incident("I3", Incident.IncidentType.MEDICAL_EMERGENCY,
            Incident.Severity.CRITICAL, new Location(51.5, -0.1, "Test"), "Critical");
        
        queue.offer(low);
        queue.offer(high);
        queue.offer(critical);
        
        assertEquals(critical, queue.poll(), "Critical should be first");
        assertEquals(high, queue.poll(), "High should be second");
        assertEquals(low, queue.poll(), "Low should be last");
    }
    
    @Test
    @DisplayName("Test Unit Readiness Score")
    void testUnitReadinessScore() {
        ResponseUnit unit = new ResponseUnit("U1", ResponseUnit.UnitType.AMBULANCE);
        
        double initialScore = unit.getReadinessScore();
        assertEquals(1.0, initialScore, 0.01, "New unit should have perfect readiness");
        
        unit.updatePerformanceMetrics(true, 5.0);
        
        assertTrue(unit.getReadinessScore() > 0.5, "Score should remain positive");
    }
    
    @Test
    @DisplayName("Test Unit Suitability for Incident Type")
    void testUnitSuitability() {
        ResponseUnit ambulance = new ResponseUnit("U1", ResponseUnit.UnitType.AMBULANCE);
        ResponseUnit fireTruck = new ResponseUnit("U2", ResponseUnit.UnitType.FIRE_TRUCK);
        
        assertTrue(ambulance.isSuitableFor(Incident.IncidentType.MEDICAL_EMERGENCY),
            "Ambulance should be suitable for medical emergency");
assertFalse(fireTruck.isSuitableFor(Incident.IncidentType.MEDICAL_EMERGENCY),
"Fire truck should not be suitable for medical emergency");
}
@Test
@DisplayName("Test Dijkstra Shortest Path")
void testDijkstraShortestPath() {
    DijkstraPathfinder dijkstra = new DijkstraPathfinder(network);
    
    Path path = dijkstra.findShortestPath("A", "C");
    
    assertTrue(path.isValid(), "Path should be valid");
    assertEquals(nodeA, path.getStartNode(), "Should start at A");
    assertEquals(nodeC, path.getEndNode(), "Should end at C");
}

@Test
@DisplayName("Test A* Optimal Path")
void testAStarOptimalPath() {
    AStarPathfinder aStar = new AStarPathfinder(network);
    
    Path path = aStar.findOptimalPath("A", "C");
    
    assertTrue(path.isValid(), "Path should be valid");
    assertEquals(3, path.getNodeCount(), "Should find path A-B-C");
}

@Test
@DisplayName("Test Multi-Criteria Unit Selection")
void testMultiCriteriaDispatch() {
    ResponseUnit ambulance = new ResponseUnit("U1", ResponseUnit.UnitType.AMBULANCE);
    ResponseUnit fire = new ResponseUnit("U2", ResponseUnit.UnitType.FIRE_TRUCK);
    nodeA.addUnit(ambulance);
    nodeA.addUnit(fire);
    
    MultiCriteriaDispatch dispatcher = new MultiCriteriaDispatch(network);
    
    Incident medical = new Incident("I1", Incident.IncidentType.MEDICAL_EMERGENCY,
        Incident.Severity.HIGH, nodeB.getLocation(), "Medical emergency");
    
    MultiCriteriaDispatch.DispatchDecision decision = 
        dispatcher.findOptimalUnit(medical);
    
    assertNotNull(decision, "Should find a dispatch decision");
    assertEquals(ambulance, decision.getUnit(), 
        "Should select ambulance for medical emergency");
}

@Test
@DisplayName("Test Historical Incident Recording")
void testHistoricalRecording() {
    PredictiveAnalyzer predictor = new PredictiveAnalyzer();
    
    Incident incident = new Incident("I1", Incident.IncidentType.FIRE,
        Incident.Severity.HIGH, nodeA.getLocation(), "Fire");
    
    predictor.recordIncident(incident, "A");
    
    PredictiveAnalyzer.PredictionStatistics stats = predictor.getStatistics();
    assertEquals(1, stats.totalIncidents, "Should have recorded 1 incident");
}

@Test
@DisplayName("Test Hotspot Prediction")
void testHotspotPrediction() {
    PredictiveAnalyzer predictor = new PredictiveAnalyzer();
    
    for (int i = 0; i < 10; i++) {
        Incident incident = new Incident("I" + i, Incident.IncidentType.FIRE,
            Incident.Severity.HIGH, nodeA.getLocation(), "Fire");
        predictor.recordIncident(incident, "A");
    }
    
    List<PredictiveAnalyzer.HotSpot> hotspots = 
        predictor.predictHighDemandAreas(LocalDateTime.now());
    
    assertFalse(hotspots.isEmpty(), "Should predict hotspots");
}
} 
