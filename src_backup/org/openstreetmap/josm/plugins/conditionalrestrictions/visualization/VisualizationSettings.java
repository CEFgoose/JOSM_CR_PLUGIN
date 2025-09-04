package org.openstreetmap.josm.plugins.conditionalrestrictions.visualization;

import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction.RestrictionType;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration settings for conditional restrictions visualization.
 * 
 * Allows users to customize how restrictions are displayed on the map,
 * including colors, line styles, visibility options, and special indicators.
 * 
 * @author CEF
 */
public class VisualizationSettings {
    
    // Basic rendering settings
    private float baseLineWidth = 4.0f;
    private boolean showInactiveRestrictions = true;
    private boolean showDirectionArrows = true;
    private boolean showLegend = true;
    
    // Arrow settings
    private Color onewayArrowColor = new Color(20, 120, 220);      // Blue for normal oneway
    private Color noOnewayArrowColor = new Color(220, 20, 20);     // Red for reversed oneway
    private BasicStroke arrowStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private int arrowSize = 8;
    
    // Visibility settings for each restriction type
    private final Map<RestrictionType, Boolean> typeVisibility;
    
    // Performance settings
    private int maxVisibleRestrictions = 1000;
    private boolean useDetailLevelOptimization = true;
    private int minZoomForDetails = 14;
    
    /**
     * Creates default visualization settings
     */
    public VisualizationSettings() {
        // Initialize visibility map with all types visible by default
        typeVisibility = new EnumMap<>(RestrictionType.class);
        for (RestrictionType type : RestrictionType.values()) {
            typeVisibility.put(type, true);
        }
    }
    
    /**
     * Creates a copy of another settings instance
     */
    public VisualizationSettings(VisualizationSettings other) {
        this.baseLineWidth = other.baseLineWidth;
        this.showInactiveRestrictions = other.showInactiveRestrictions;
        this.showDirectionArrows = other.showDirectionArrows;
        this.showLegend = other.showLegend;
        this.onewayArrowColor = other.onewayArrowColor;
        this.noOnewayArrowColor = other.noOnewayArrowColor;
        this.arrowStroke = other.arrowStroke;
        this.arrowSize = other.arrowSize;
        this.maxVisibleRestrictions = other.maxVisibleRestrictions;
        this.useDetailLevelOptimization = other.useDetailLevelOptimization;
        this.minZoomForDetails = other.minZoomForDetails;
        
        this.typeVisibility = new EnumMap<>(other.typeVisibility);
    }
    
    /**
     * Checks if restrictions of a specific type should be shown
     */
    public boolean isShowRestrictionsOfType(RestrictionType type) {
        return typeVisibility.getOrDefault(type, true);
    }
    
    /**
     * Sets whether restrictions of a specific type should be shown
     */
    public void setShowRestrictionsOfType(RestrictionType type, boolean show) {
        typeVisibility.put(type, show);
    }
    
    /**
     * Enables or disables all restriction types
     */
    public void setShowAllTypes(boolean show) {
        for (RestrictionType type : RestrictionType.values()) {
            typeVisibility.put(type, show);
        }
    }
    
    /**
     * Gets the count of currently visible restriction types
     */
    public int getVisibleTypeCount() {
        return (int) typeVisibility.values().stream().mapToInt(b -> b ? 1 : 0).sum();
    }
    
    /**
     * Resets all settings to defaults
     */
    public void resetToDefaults() {
        baseLineWidth = 4.0f;
        showInactiveRestrictions = true;
        showDirectionArrows = true;
        showLegend = true;
        onewayArrowColor = new Color(20, 120, 220);
        noOnewayArrowColor = new Color(220, 20, 20);
        arrowStroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        arrowSize = 8;
        maxVisibleRestrictions = 1000;
        useDetailLevelOptimization = true;
        minZoomForDetails = 14;
        
        setShowAllTypes(true);
    }
    
    /**
     * Creates a preset for showing only major restrictions (access, oneway)
     */
    public void applyMajorRestrictionsPreset() {
        setShowAllTypes(false);
        setShowRestrictionsOfType(RestrictionType.ACCESS, true);
        setShowRestrictionsOfType(RestrictionType.ONEWAY, true);
        setShowDirectionArrows(true);
        setShowInactiveRestrictions(false);
    }
    
    /**
     * Creates a preset for showing vehicle-specific restrictions
     */
    public void applyVehicleRestrictionsPreset() {
        setShowAllTypes(false);
        setShowRestrictionsOfType(RestrictionType.ACCESS, true);
        setShowRestrictionsOfType(RestrictionType.HGV, true);
        setShowRestrictionsOfType(RestrictionType.SPEED, true);
        setShowDirectionArrows(true);
        setShowInactiveRestrictions(true);
    }
    
    /**
     * Creates a preset for showing pedestrian and cycling restrictions
     */
    public void applyNonMotorizedPreset() {
        setShowAllTypes(false);
        setShowRestrictionsOfType(RestrictionType.BICYCLE, true);
        setShowRestrictionsOfType(RestrictionType.FOOT, true);
        setShowRestrictionsOfType(RestrictionType.ACCESS, true);
        setShowDirectionArrows(false);
        setShowInactiveRestrictions(true);
    }
    
    /**
     * Validates current settings and fixes any invalid values
     */
    public void validate() {
        // Ensure line width is within reasonable bounds
        if (baseLineWidth < 0.5f) baseLineWidth = 0.5f;
        if (baseLineWidth > 20.0f) baseLineWidth = 20.0f;
        
        // Ensure arrow size is reasonable
        if (arrowSize < 3) arrowSize = 3;
        if (arrowSize > 20) arrowSize = 20;
        
        // Ensure performance limits are reasonable
        if (maxVisibleRestrictions < 100) maxVisibleRestrictions = 100;
        if (maxVisibleRestrictions > 10000) maxVisibleRestrictions = 10000;
        
        if (minZoomForDetails < 10) minZoomForDetails = 10;
        if (minZoomForDetails > 20) minZoomForDetails = 20;
        
        // Ensure colors are not null
        if (onewayArrowColor == null) onewayArrowColor = new Color(20, 120, 220);
        if (noOnewayArrowColor == null) noOnewayArrowColor = new Color(220, 20, 20);
        if (arrowStroke == null) arrowStroke = new BasicStroke(2.0f);
    }
    
    // Getters and setters
    public float getBaseLineWidth() {
        return baseLineWidth;
    }
    
    public void setBaseLineWidth(float baseLineWidth) {
        this.baseLineWidth = Math.max(0.5f, Math.min(20.0f, baseLineWidth));
    }
    
    public boolean isShowInactiveRestrictions() {
        return showInactiveRestrictions;
    }
    
    public void setShowInactiveRestrictions(boolean showInactiveRestrictions) {
        this.showInactiveRestrictions = showInactiveRestrictions;
    }
    
    public boolean isShowDirectionArrows() {
        return showDirectionArrows;
    }
    
    public void setShowDirectionArrows(boolean showDirectionArrows) {
        this.showDirectionArrows = showDirectionArrows;
    }
    
    public boolean isShowLegend() {
        return showLegend;
    }
    
    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
    }
    
    public Color getOnewayArrowColor() {
        return onewayArrowColor;
    }
    
    public void setOnewayArrowColor(Color onewayArrowColor) {
        this.onewayArrowColor = onewayArrowColor != null ? onewayArrowColor : new Color(20, 120, 220);
    }
    
    public Color getNoOnewayArrowColor() {
        return noOnewayArrowColor;
    }
    
    public void setNoOnewayArrowColor(Color noOnewayArrowColor) {
        this.noOnewayArrowColor = noOnewayArrowColor != null ? noOnewayArrowColor : new Color(220, 20, 20);
    }
    
    public BasicStroke getArrowStroke() {
        return arrowStroke;
    }
    
    public void setArrowStroke(BasicStroke arrowStroke) {
        this.arrowStroke = arrowStroke != null ? arrowStroke : new BasicStroke(2.0f);
    }
    
    public int getArrowSize() {
        return arrowSize;
    }
    
    public void setArrowSize(int arrowSize) {
        this.arrowSize = Math.max(3, Math.min(20, arrowSize));
    }
    
    public int getMaxVisibleRestrictions() {
        return maxVisibleRestrictions;
    }
    
    public void setMaxVisibleRestrictions(int maxVisibleRestrictions) {
        this.maxVisibleRestrictions = Math.max(100, Math.min(10000, maxVisibleRestrictions));
    }
    
    public boolean isUseDetailLevelOptimization() {
        return useDetailLevelOptimization;
    }
    
    public void setUseDetailLevelOptimization(boolean useDetailLevelOptimization) {
        this.useDetailLevelOptimization = useDetailLevelOptimization;
    }
    
    public int getMinZoomForDetails() {
        return minZoomForDetails;
    }
    
    public void setMinZoomForDetails(int minZoomForDetails) {
        this.minZoomForDetails = Math.max(10, Math.min(20, minZoomForDetails));
    }
    
    /**
     * Gets a copy of the type visibility map
     */
    public Map<RestrictionType, Boolean> getTypeVisibility() {
        return new EnumMap<>(typeVisibility);
    }
    
    @Override
    public String toString() {
        return "VisualizationSettings[" +
               "baseWidth=" + baseLineWidth +
               ", showInactive=" + showInactiveRestrictions +
               ", showArrows=" + showDirectionArrows +
               ", visibleTypes=" + getVisibleTypeCount() + "/" + RestrictionType.values().length +
               "]";
    }
}