package core.models;

/**
 * Represents a geographical location with coordinates
 * Used for nodes in the emergency network and incident locations
 */
public class Location {
    private final double latitude;
    private final double longitude;
    private final String address;
    
    public Location(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public String getAddress() {
        return address;
    }
    
    /**
     * Calculate Haversine distance to another location (in kilometers)
     * Used as heuristic for A* algorithm
     * Time Complexity: O(1)
     */
    public double distanceTo(Location other) {
        final int EARTH_RADIUS = 6371; // kilometers
        
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    @Override
    public String toString() {
        return String.format("Location(%.4f, %.4f) - %s", latitude, longitude, address);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Location)) return false;
        Location other = (Location) obj;
        return Double.compare(latitude, other.latitude) == 0 &&
               Double.compare(longitude, other.longitude) == 0;
    }
    
    @Override
    public int hashCode() {
        return Double.hashCode(latitude) * 31 + Double.hashCode(longitude);
    }
} 
