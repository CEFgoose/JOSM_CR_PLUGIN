package org.openstreetmap.josm.plugins.conditionalrestrictions.data;

import org.openstreetmap.josm.data.osm.Way;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete conditional restriction parsed from an OSM tag.
 * 
 * Contains the restriction value and all associated conditions.
 * 
 * @author CEF
 */
public class ConditionalRestriction {
    
    private final String tagKey;
    private final String restrictionValue;
    private Way way;
    
    private List<TimeCondition> timeConditions;
    private List<String> genericConditions;
    
    // Vehicle dimension conditions
    private String weightOperator;
    private Double weightValue;
    private String heightOperator;
    private Double heightValue;
    
    /**
     * Creates a new conditional restriction
     * 
     * @param tagKey The OSM tag key (e.g., "access:conditional")
     * @param restrictionValue The restriction value (e.g., "no", "yes", "30")
     */
    public ConditionalRestriction(String tagKey, String restrictionValue) {
        this.tagKey = tagKey;
        this.restrictionValue = restrictionValue;
        this.timeConditions = new ArrayList<>();
        this.genericConditions = new ArrayList<>();
    }
    
    /**
     * Gets the base tag type (e.g., "access" from "access:conditional")
     */
    public String getBaseTag() {
        return tagKey.replaceFirst(":conditional$", "");
    }
    
    /**
     * Adds a time condition to this restriction
     */
    public void addTimeCondition(TimeCondition condition) {
        timeConditions.add(condition);
    }
    
    /**
     * Adds a generic condition (non-parsed condition string)
     */
    public void addGenericCondition(String condition) {
        genericConditions.add(condition);
    }
    
    /**
     * Sets a weight condition
     * 
     * @param operator Comparison operator (">", ">=", "<", "<=")
     * @param value Weight value in tonnes
     */
    public void setWeightCondition(String operator, double value) {
        this.weightOperator = operator;
        this.weightValue = value;
    }
    
    /**
     * Sets a height condition
     * 
     * @param operator Comparison operator (">", ">=", "<", "<=")
     * @param value Height value in meters
     */
    public void setHeightCondition(String operator, double value) {
        this.heightOperator = operator;
        this.heightValue = value;
    }
    
    /**
     * Checks if this restriction is currently active based on time conditions
     * 
     * @param dateTime The date/time to check against
     * @return true if any time condition is currently active
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        // If there are no time conditions, the restriction is always active
        if (timeConditions.isEmpty()) {
            return true;
        }
        
        // Check if any time condition is active
        for (TimeCondition condition : timeConditions) {
            if (condition.isActiveAt(dateTime)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if this restriction applies to a vehicle with given dimensions
     * 
     * @param vehicleWeight Vehicle weight in tonnes (null if unknown)
     * @param vehicleHeight Vehicle height in meters (null if unknown)
     * @return true if the restriction applies to this vehicle
     */
    public boolean appliesToVehicle(Double vehicleWeight, Double vehicleHeight) {
        // Check weight condition
        if (weightOperator != null && weightValue != null && vehicleWeight != null) {
            boolean weightMatches = false;
            switch (weightOperator) {
                case ">":
                    weightMatches = vehicleWeight > weightValue;
                    break;
                case ">=":
                    weightMatches = vehicleWeight >= weightValue;
                    break;
                case "<":
                    weightMatches = vehicleWeight < weightValue;
                    break;
                case "<=":
                    weightMatches = vehicleWeight <= weightValue;
                    break;
            }
            if (!weightMatches) {
                return false;
            }
        }
        
        // Check height condition
        if (heightOperator != null && heightValue != null && vehicleHeight != null) {
            boolean heightMatches = false;
            switch (heightOperator) {
                case ">":
                    heightMatches = vehicleHeight > heightValue;
                    break;
                case ">=":
                    heightMatches = vehicleHeight >= heightValue;
                    break;
                case "<":
                    heightMatches = vehicleHeight < heightValue;
                    break;
                case "<=":
                    heightMatches = vehicleHeight <= heightValue;
                    break;
            }
            if (!heightMatches) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets a human-readable description of this restriction
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getBaseTag()).append(" = ").append(restrictionValue);
        
        List<String> conditions = new ArrayList<>();
        
        // Add time conditions
        for (TimeCondition tc : timeConditions) {
            conditions.add(tc.getDescription());
        }
        
        // Add weight/height conditions
        if (weightOperator != null && weightValue != null) {
            conditions.add("weight " + weightOperator + " " + weightValue + "t");
        }
        if (heightOperator != null && heightValue != null) {
            conditions.add("height " + heightOperator + " " + heightValue + "m");
        }
        
        // Add generic conditions
        conditions.addAll(genericConditions);
        
        if (!conditions.isEmpty()) {
            sb.append(" when: ").append(String.join(", ", conditions));
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the restriction type for routing purposes
     */
    public RestrictionType getType() {
        String baseTag = getBaseTag().toLowerCase();
        
        switch (baseTag) {
            case "access":
            case "motor_vehicle":
            case "vehicle":
                return RestrictionType.ACCESS;
            case "oneway":
                return RestrictionType.ONEWAY;
            case "maxspeed":
                return RestrictionType.SPEED;
            case "hgv":
            case "goods":
                return RestrictionType.HGV;
            case "bicycle":
                return RestrictionType.BICYCLE;
            case "foot":
                return RestrictionType.FOOT;
            case "parking":
                return RestrictionType.PARKING;
            default:
                return RestrictionType.OTHER;
        }
    }
    
    // Getters
    public String getTagKey() { return tagKey; }
    public String getRestrictionValue() { return restrictionValue; }
    public Way getWay() { return way; }
    public void setWay(Way way) { this.way = way; }
    public List<TimeCondition> getTimeConditions() { return new ArrayList<>(timeConditions); }
    public List<String> getGenericConditions() { return new ArrayList<>(genericConditions); }
    public String getWeightOperator() { return weightOperator; }
    public Double getWeightValue() { return weightValue; }
    public String getHeightOperator() { return heightOperator; }
    public Double getHeightValue() { return heightValue; }
    
    @Override
    public String toString() {
        return "ConditionalRestriction[" + tagKey + " = " + restrictionValue + 
               " @ (" + timeConditions.size() + " conditions)]";
    }
    
    /**
     * Enumeration of restriction types
     */
    public enum RestrictionType {
        ACCESS,    // General access restrictions
        ONEWAY,    // One-way restrictions
        SPEED,     // Speed limitations
        HGV,       // Heavy goods vehicle restrictions
        BICYCLE,   // Bicycle-specific restrictions
        FOOT,      // Pedestrian restrictions
        PARKING,   // Parking restrictions
        OTHER      // Other restriction types
    }
}