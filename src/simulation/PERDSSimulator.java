 
package simulation;

import core.models.*;
import system.PERDS;
import java.util.*;

public class PERDSSimulator {
    
    private final PERDS system;
    private final Random random;
    private int incidentCounter;
    
    public PERDSSimulator() {
        this.system = new PERDS();
        this.random = new Random(42);
        this.incidentCounter = 1;
    }
    
    public void runFullDemo() {
        System.out.println("=".repeat(80));
        System.out.println("PREDICTIVE EMERGENCY RESPONSE DISPATCH SYSTEM (PERDS)");
        System.out.println("Comprehensive Demonstration");
        System.out.println("=".repeat(80));
        
        System.out.println("\nPHASE 1: Network Initialization");
        System.out.println("-".repeat(80));
        initializeRealisticNetwork();
        
        System.out.println("\nPHASE 2: System Startup");
        System.out.println("-".repeat(80));
        system.start();
        
        System.out.println("\nPHASE 3: Normal Operations Simulation");
        System.out.println("-".repeat(80));
        simulateNormalOperations(20);
        
        sleep(5000);
        
        System.out.println("\nPHASE 4: High-Load Stress Test");
        System.out.println("-".repeat(80));
        simulateHighLoad(10);
        
        sleep(5000);
        
        System.out.println("\nPHASE 5: Dynamic Network Updates");
        System.out.println("-".repeat(80));
        demonstrateDynamicUpdates();
        
        sleep(3000);
        
        System.out.println("\nPHASE 6: Performance Analysis");
        System.out.println("-".repeat(80));
        displayResults();
        
        System.out.println("\nPHASE 7: System Shutdown");
        System.out.println("-".repeat(80));
        system.stop();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DEMONSTRATION COMPLETE");
        System.out.println("=".repeat(80));
    }
    
    private void initializeRealisticNetwork() {
        PERDS.NetworkConfiguration config = new PERDS.NetworkConfiguration();
        
        String[][] cityData = {
            {"C1", "Central City", "51.5074", "-0.1278"},
            {"C2", "North Town", "51.6074", "-0.1278"},
            {"C3", "East Borough", "51.5074", "-0.0278"},
            {"C4", "South District", "51.4074", "-0.1278"},
            {"C5", "West Village", "51.5074", "-0.2278"},
            {"D1", "Central Dispatch", "51.5074", "-0.1278"},
            {"D2", "North Dispatch", "51.6074", "-0.1278"},
            {"D3", "East Dispatch", "51.5074", "-0.0278"},
            {"D4", "South Dispatch", "51.4074", "-0.1278"}
        };
        
        for (int i = 0; i < 5; i++) {
            config.addNode(new PERDS.NodeConfig(
                cityData[i][0],
                cityData[i][1],
                Node.NodeType.CITY,
                new Location(
                    Double.parseDouble(cityData[i][2]),
                    Double.parseDouble(cityData[i][3]),
                    cityData[i][1]
                ),
                0
            ));
        }
        
        for (int i = 5; i < 9; i++) {
            config.addNode(new PERDS.NodeConfig(
                cityData[i][0],
                cityData[i][1],
                Node.NodeType.DISPATCH_CENTER,
                new Location(
                    Double.parseDouble(cityData[i][2]),
                    Double.parseDouble(cityData[i][3]),
                    cityData[i][1]
                ),
                10
            ));
        }
        
        String[] nodes = {"C1", "C2", "C3", "C4", "C5", "D1", "D2", "D3", "D4"};
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                config.addEdge(new PERDS.EdgeConfig(nodes[i], nodes[j]));
            }
        }
        
        ResponseUnit.UnitType[] types = ResponseUnit.UnitType.values();
        int unitId = 1;
        
        for (int i = 1; i <= 4; i++) {
            String dispatchId = "D" + i;
            int unitsToDeploy = 3 + random.nextInt(3);
            
            for (int j = 0; j < unitsToDeploy; j++) {
                ResponseUnit.UnitType type = types[random.nextInt(types.length)];
                config.addUnit(new PERDS.UnitDeployment(
                    "UNIT-" + unitId++,
                    dispatchId,
                    type
                ));
            }
        }
        
        system.initializeNetwork(config);
    }
    
    private void simulateNormalOperations(int incidentCount) {
        System.out.println("Simulating " + incidentCount + " incidents...");
        
        for (int i = 0; i < incidentCount; i++) {
            Incident incident = generateRandomIncident();
            system.reportIncident(incident);
            sleep(random.nextInt(3000));
        }
    }
    
    private void simulateHighLoad(int simultaneousIncidents) {
        System.out.println("Generating " + simultaneousIncidents + 
            " simultaneous incidents...");
        
        for (int i = 0; i < simultaneousIncidents; i++) {
            Incident incident = generateRandomIncident();
            system.reportIncident(incident);
            sleep(100);
        }
    }
    
    private void demonstrateDynamicUpdates() {
        System.out.println("Demonstrating dynamic network updates...");
        
        System.out.println("\n[SCENARIO] Rush hour traffic on main routes");
        system.updateTrafficCondition("C1-C2", 2.5);
        system.updateTrafficCondition("C1-D1", 1.8);
        
        Incident congestionIncident = new Incident(
            "INC-" + incidentCounter++,
            Incident.IncidentType.TRAFFIC_ACCIDENT,
            Incident.Severity.HIGH,
            new Location(51.5574, -0.1278, "Highway Junction"),
            "Multi-vehicle accident blocking lanes"
        );
        system.reportIncident(congestionIncident);
        
        sleep(2000);
        
        System.out.println("\n[SCENARIO] Emergency road closure");
        system.setRoadClosure("C2-C3", true);
        
        Incident rerouteIncident = generateRandomIncident();
        system.reportIncident(rerouteIncident);
        
        sleep(2000);
        
        System.out.println("\n[SCENARIO] Traffic returning to normal");
        system.updateTrafficCondition("C1-C2", 1.0);
        system.updateTrafficCondition("C1-D1", 1.0);
        system.setRoadClosure("C2-C3", false);
    }
    
    private Incident generateRandomIncident() {
        Incident.IncidentType[] types = Incident.IncidentType.values();
        Incident.Severity[] severities = Incident.Severity.values();
        
        double lat = 51.4074 + (random.nextDouble() * 0.2);
        double lon = -0.2278 + (random.nextDouble() * 0.2);
        
        String[] descriptions = {
            "Emergency assistance required",
            "Immediate response needed",
            "Critical situation reported",
            "Urgent attention required",
            "Emergency services requested"
        };
        
        Incident.IncidentType type = types[random.nextInt(types.length)];
        Incident.Severity severity = severities[random.nextInt(severities.length)];
        
        if (random.nextDouble() < 0.3) {
            severity = Incident.Severity.CRITICAL;
        }
        
        return new Incident(
            "INC-" + incidentCounter++,
            type,
            severity,
            new Location(lat, lon, "Location-" + incidentCounter),
            descriptions[random.nextInt(descriptions.length)]
        );
    }
    
    private void displayResults() {
        PERDS.SystemStatistics stats = system.getStatistics();
        
        System.out.println("\nSYSTEM PERFORMANCE SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println(stats);
        
        System.out.println("\nRISK ASSESSMENT");
        System.out.println("-".repeat(80));
        Map<String, Double> risks = system.getRiskAssessment();
        List<Map.Entry<String, Double>> sortedRisks = new ArrayList<>(risks.entrySet());
        sortedRisks.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        System.out.println("Top 5 High-Risk Locations:");
        for (int i = 0; i < Math.min(5, sortedRisks.size()); i++) {
            Map.Entry<String, Double> entry = sortedRisks.get(i);
            System.out.println(String.format("  %d. %s: Risk Score %.2f", 
                i + 1, entry.getKey(), entry.getValue()));
        }
        
        System.out.println("\nKEY INSIGHTS");
        System.out.println("-".repeat(80));
        
        if (stats.performance.totalDispatches > 0) {
            System.out.println(String.format("• Average Response Time: %.2f minutes", 
                stats.performance.averageResponseTime));
            System.out.println(String.format("• Success Rate: %.1f%%", 
                stats.performance.successRate * 100));
            System.out.println(String.format("• Target Met Rate: %.1f%%", 
                stats.performance.targetMetRate * 100));
            
            if (stats.performance.improvementPercentage != 0) {
                System.out.println(String.format("• System Improvement: %.1f%%", 
                    stats.performance.improvementPercentage));
            }
            
            double[] weights = stats.performance.optimizedWeights;
            System.out.println("\n• Adaptive Weights (optimized through learning):");
            System.out.println(String.format("  - Distance Priority: %.1f%%", weights[0] * 100));
            System.out.println(String.format("  - Resource Match: %.1f%%", weights[1] * 100));
            System.out.println(String.format("  - Availability: %.1f%%", weights[2] * 100));
            System.out.println(String.format("  - Performance History: %.1f%%", weights[3] * 100));
        }
    }
    
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        PERDSSimulator simulator = new PERDSSimulator();
        simulator.runFullDemo();
    }
}