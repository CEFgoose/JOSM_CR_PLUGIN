package org.openstreetmap.josm.plugins.conditionalrestrictions.visualization;

import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction.RestrictionType;

import java.awt.*;

/**
 * Defines visual styles for different types of conditional restrictions.
 * 
 * Each restriction type has specific colors, stroke patterns, and visual indicators
 * to help users quickly identify and understand the restrictions on the map.
 * 
 * @author CEF
 */
public class RestrictionStyle {
    
    private final Color color;
    private final BasicStroke stroke;
    private final boolean visible;
    private final String description;
    
    /**
     * Creates a new restriction style
     */
    public RestrictionStyle(Color color, BasicStroke stroke, boolean visible, String description) {
        this.color = color;
        this.stroke = stroke;
        this.visible = visible;
        this.description = description;
    }
    
    /**
     * Gets the appropriate style for a restriction type and activity state
     */
    public static RestrictionStyle getStyleForType(RestrictionType type, boolean isActive, VisualizationSettings settings) {
        if (!settings.isShowRestrictionsOfType(type)) {
            return createInvisibleStyle();
        }
        
        // Base colors for different restriction types
        Color baseColor = getBaseColorForType(type);
        
        // Modify color intensity based on active state
        Color finalColor = isActive ? baseColor : createInactiveColor(baseColor);
        
        // Create stroke based on restriction type and settings
        BasicStroke stroke = createStrokeForType(type, isActive, settings);
        
        return new RestrictionStyle(finalColor, stroke, true, getDescriptionForType(type, isActive));
    }
    
    /**
     * Gets the base color for a restriction type
     */
    private static Color getBaseColorForType(RestrictionType type) {
        switch (type) {
            case ACCESS:
                return new Color(220, 20, 20);     // Red - major access restrictions
            case ONEWAY:
                return new Color(20, 120, 220);    // Blue - directional restrictions
            case SPEED:
                return new Color(255, 140, 0);     // Orange - speed limitations
            case HGV:
                return new Color(120, 20, 120);    // Purple - heavy vehicle restrictions
            case BICYCLE:
                return new Color(20, 180, 20);     // Green - bicycle restrictions
            case FOOT:
                return new Color(180, 180, 20);    // Yellow - pedestrian restrictions
            case PARKING:
                return new Color(140, 140, 140);   // Gray - parking restrictions
            case OTHER:
            default:
                return new Color(100, 100, 100);   // Dark gray - other restrictions
        }
    }
    
    /**
     * Creates an inactive color (faded version of the base color)
     */
    private static Color createInactiveColor(Color baseColor) {
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
        // Reduce saturation and brightness for inactive appearance
        return Color.getHSBColor(hsb[0], hsb[1] * 0.4f, hsb[2] * 0.7f + 0.3f);
    }
    
    /**
     * Creates the appropriate stroke for a restriction type
     */
    private static BasicStroke createStrokeForType(RestrictionType type, boolean isActive, VisualizationSettings settings) {
        float baseWidth = settings.getBaseLineWidth();
        
        // Adjust width based on restriction type importance
        float width = baseWidth;
        switch (type) {
            case ACCESS:
            case ONEWAY:
                width = baseWidth * 1.5f;  // Thicker for major restrictions
                break;
            case SPEED:
            case HGV:
                width = baseWidth * 1.2f;  // Slightly thicker for vehicle restrictions
                break;
            case BICYCLE:
            case FOOT:
            case PARKING:
                width = baseWidth;         // Normal thickness for specific restrictions
                break;
            case OTHER:
                width = baseWidth * 0.8f;  // Thinner for other restrictions
                break;
        }
        
        // Create different stroke patterns for different types
        float[] dashPattern = getDashPatternForType(type);
        
        if (dashPattern != null) {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, dashPattern, 0.0f);
        } else {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
    }
    
    /**
     * Gets the dash pattern for a restriction type (null for solid line)
     */
    private static float[] getDashPatternForType(RestrictionType type) {
        switch (type) {
            case PARKING:
                return new float[]{8.0f, 4.0f};        // Dashed for parking
            case SPEED:
                return new float[]{12.0f, 3.0f, 3.0f, 3.0f}; // Dash-dot for speed
            case OTHER:
                return new float[]{5.0f, 5.0f};        // Short dashes for other
            default:
                return null;                           // Solid line for major restrictions
        }
    }
    
    /**
     * Gets a description for the restriction type and state
     */
    private static String getDescriptionForType(RestrictionType type, boolean isActive) {
        String state = isActive ? "Active" : "Inactive";
        switch (type) {
            case ACCESS:
                return state + " Access Restriction";
            case ONEWAY:
                return state + " One-way Restriction";
            case SPEED:
                return state + " Speed Limit";
            case HGV:
                return state + " Heavy Vehicle Restriction";
            case BICYCLE:
                return state + " Bicycle Restriction";
            case FOOT:
                return state + " Pedestrian Restriction";
            case PARKING:
                return state + " Parking Restriction";
            case OTHER:
            default:
                return state + " Other Restriction";
        }
    }
    
    /**
     * Creates an invisible style for disabled restriction types
     */
    private static RestrictionStyle createInvisibleStyle() {
        return new RestrictionStyle(Color.BLACK, new BasicStroke(1.0f), false, "Hidden");
    }
    
    /**
     * Creates a preview style for legend display
     */
    public static RestrictionStyle createPreviewStyle(RestrictionType type, boolean isActive) {
        Color baseColor = getBaseColorForType(type);
        Color finalColor = isActive ? baseColor : createInactiveColor(baseColor);
        
        // Use a standard stroke for preview
        BasicStroke stroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        
        return new RestrictionStyle(finalColor, stroke, true, getDescriptionForType(type, isActive));
    }
    
    // Getters
    public Color getColor() {
        return color;
    }
    
    public BasicStroke getStroke() {
        return stroke;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "RestrictionStyle[" + description + ", visible=" + visible + "]";
    }
}