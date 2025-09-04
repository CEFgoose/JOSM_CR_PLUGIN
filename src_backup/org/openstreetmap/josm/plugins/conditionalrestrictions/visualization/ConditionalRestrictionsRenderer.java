package org.openstreetmap.josm.plugins.conditionalrestrictions.visualization;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsPlugin;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction.RestrictionType;
import org.openstreetmap.josm.tools.Logging;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Main visualization renderer for conditional restrictions on the JOSM map.
 * 
 * Provides visual indicators for ways with conditional restrictions:
 * - Different colors and styles for different restriction types
 * - Active vs inactive state visualization based on current time
 * - Performance-optimized rendering for large datasets
 * - Integration with JOSM's map rendering pipeline
 * 
 * @author CEF
 */
public class ConditionalRestrictionsRenderer extends AbstractMapViewPaintable implements MapViewPaintable {
    
    private final ConditionalRestrictionsPlugin plugin;
    private final VisualizationSettings settings;
    private boolean enabled = true;
    
    // Cache for performance optimization
    private Bounds lastRenderBounds;
    private int lastZoomLevel = -1;
    private long lastUpdateTime = 0;
    private static final long CACHE_DURATION_MS = 5000; // 5 second cache
    
    /**
     * Creates a new conditional restrictions renderer
     * 
     * @param plugin The parent plugin instance
     */
    public ConditionalRestrictionsRenderer(ConditionalRestrictionsPlugin plugin) {
        this.plugin = plugin;
        this.settings = new VisualizationSettings();
    }
    
    /**
     * Main paint method called by JOSM's map rendering system
     */
    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        if (!enabled || plugin == null) {
            return;
        }
        
        // Performance optimization: check if we need to update
        int currentZoom = (int) (Math.log(mv.getState().getScale()) / Math.log(2));
        boolean needsUpdate = lastRenderBounds == null ||
                             !lastRenderBounds.equals(bbox) ||
                             currentZoom != lastZoomLevel ||
                             System.currentTimeMillis() - lastUpdateTime > CACHE_DURATION_MS;
        
        if (needsUpdate) {
            lastRenderBounds = bbox;
            lastZoomLevel = currentZoom;
            lastUpdateTime = System.currentTimeMillis();
        }
        
        // Get current restrictions from plugin
        Map<Way, List<ConditionalRestriction>> restrictions = plugin.getRestrictions();
        if (restrictions.isEmpty()) {
            return;
        }
        
        // Current time for evaluating time-based restrictions
        LocalDateTime currentTime = LocalDateTime.now();
        
        // Set up rendering hints for better quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        // Render restrictions by type for proper layering
        renderRestrictionsByType(g, mv, bbox, restrictions, currentTime);
        
        // Render special indicators (arrows, symbols)
        renderSpecialIndicators(g, mv, bbox, restrictions, currentTime);
    }
    
    /**
     * Renders restrictions grouped by type for proper visual layering
     */
    private void renderRestrictionsByType(Graphics2D g, MapView mv, Bounds bbox,
                                        Map<Way, List<ConditionalRestriction>> restrictions,
                                        LocalDateTime currentTime) {
        
        // Render in order of visual priority (background to foreground)
        RestrictionType[] renderOrder = {
            RestrictionType.PARKING,
            RestrictionType.SPEED,
            RestrictionType.HGV,
            RestrictionType.ACCESS,
            RestrictionType.BICYCLE,
            RestrictionType.FOOT,
            RestrictionType.ONEWAY,
            RestrictionType.OTHER
        };
        
        for (RestrictionType type : renderOrder) {
            renderRestrictionsOfType(g, mv, bbox, restrictions, currentTime, type);
        }
    }
    
    /**
     * Renders all restrictions of a specific type
     */
    private void renderRestrictionsOfType(Graphics2D g, MapView mv, Bounds bbox,
                                        Map<Way, List<ConditionalRestriction>> restrictions,
                                        LocalDateTime currentTime, RestrictionType targetType) {
        
        for (Map.Entry<Way, List<ConditionalRestriction>> entry : restrictions.entrySet()) {
            Way way = entry.getKey();
            List<ConditionalRestriction> wayRestrictions = entry.getValue();
            
            // Skip if way is not in current view bounds
            if (!way.getBBox().intersects(bbox)) {
                continue;
            }
            
            // Check if way has restrictions of target type
            for (ConditionalRestriction restriction : wayRestrictions) {
                if (restriction.getType() == targetType) {
                    renderWayRestriction(g, mv, way, restriction, currentTime);
                }
            }
        }
    }
    
    /**
     * Renders a single way with its conditional restriction
     */
    private void renderWayRestriction(Graphics2D g, MapView mv, Way way, 
                                    ConditionalRestriction restriction, LocalDateTime currentTime) {
        
        RestrictionStyle style = getStyleForRestriction(restriction, currentTime);
        if (style == null || !style.isVisible()) {
            return;
        }
        
        // Set up stroke and color
        g.setStroke(style.getStroke());
        g.setColor(style.getColor());
        
        // Draw the way path
        Point2D[] points = new Point2D[way.getNodesCount()];
        for (int i = 0; i < way.getNodesCount(); i++) {
            points[i] = mv.getPoint2D(way.getNode(i));
        }
        
        // Draw the path segments
        for (int i = 0; i < points.length - 1; i++) {
            if (points[i] != null && points[i + 1] != null) {
                g.drawLine(
                    (int) points[i].getX(), (int) points[i].getY(),
                    (int) points[i + 1].getX(), (int) points[i + 1].getY()
                );
            }
        }
        
        // Add overlay pattern for inactive restrictions
        if (!restriction.isActiveAt(currentTime) && settings.isShowInactiveRestrictions()) {
            renderInactiveOverlay(g, points, style);
        }
    }
    
    /**
     * Renders special indicators like arrows for oneway restrictions
     */
    private void renderSpecialIndicators(Graphics2D g, MapView mv, Bounds bbox,
                                       Map<Way, List<ConditionalRestriction>> restrictions,
                                       LocalDateTime currentTime) {
        
        if (!settings.isShowDirectionArrows()) {
            return;
        }
        
        for (Map.Entry<Way, List<ConditionalRestriction>> entry : restrictions.entrySet()) {
            Way way = entry.getKey();
            List<ConditionalRestriction> wayRestrictions = entry.getValue();
            
            // Skip if way is not in current view bounds
            if (!way.getBBox().intersects(bbox)) {
                continue;
            }
            
            // Check for oneway restrictions
            for (ConditionalRestriction restriction : wayRestrictions) {
                if (restriction.getType() == RestrictionType.ONEWAY && 
                    restriction.isActiveAt(currentTime)) {
                    renderDirectionArrows(g, mv, way, restriction);
                }
            }
        }
    }
    
    /**
     * Renders direction arrows for oneway restrictions
     */
    private void renderDirectionArrows(Graphics2D g, MapView mv, Way way, ConditionalRestriction restriction) {
        if (way.getNodesCount() < 2) {
            return;
        }
        
        // Determine arrow color based on restriction value
        Color arrowColor = "yes".equals(restriction.getRestrictionValue()) ? 
                          settings.getOnewayArrowColor() : 
                          settings.getNoOnewayArrowColor();
        
        g.setColor(arrowColor);
        g.setStroke(settings.getArrowStroke());
        
        // Draw arrows along the way at regular intervals
        Point2D[] points = new Point2D[way.getNodesCount()];
        for (int i = 0; i < way.getNodesCount(); i++) {
            points[i] = mv.getPoint2D(way.getNode(i));
        }
        
        // Calculate total way length for arrow spacing
        double totalLength = 0;
        for (int i = 0; i < points.length - 1; i++) {
            if (points[i] != null && points[i + 1] != null) {
                totalLength += points[i].distance(points[i + 1]);
            }
        }
        
        // Place arrows at regular intervals
        double arrowSpacing = Math.max(50, Math.min(200, totalLength / 5));
        double currentDistance = arrowSpacing / 2; // Start offset
        
        for (int i = 0; i < points.length - 1 && currentDistance < totalLength; i++) {
            if (points[i] == null || points[i + 1] == null) {
                continue;
            }
            
            double segmentLength = points[i].distance(points[i + 1]);
            if (currentDistance <= segmentLength) {
                // Calculate arrow position and direction
                double ratio = currentDistance / segmentLength;
                double x = points[i].getX() + ratio * (points[i + 1].getX() - points[i].getX());
                double y = points[i].getY() + ratio * (points[i + 1].getY() - points[i].getY());
                
                double angle = Math.atan2(
                    points[i + 1].getY() - points[i].getY(),
                    points[i + 1].getX() - points[i].getX()
                );
                
                drawArrow(g, (int) x, (int) y, angle);
                currentDistance += arrowSpacing;
            } else {
                currentDistance -= segmentLength;
            }
        }
    }
    
    /**
     * Draws a single directional arrow
     */
    private void drawArrow(Graphics2D g, int x, int y, double angle) {
        int arrowSize = settings.getArrowSize();
        double arrowAngle = Math.PI / 6; // 30 degrees
        
        // Arrow head points
        int x1 = (int) (x + arrowSize * Math.cos(angle - arrowAngle));
        int y1 = (int) (y + arrowSize * Math.sin(angle - arrowAngle));
        int x2 = (int) (x + arrowSize * Math.cos(angle + arrowAngle));
        int y2 = (int) (y + arrowSize * Math.sin(angle + arrowAngle));
        
        // Draw arrow
        g.drawLine(x, y, x1, y1);
        g.drawLine(x, y, x2, y2);
    }
    
    /**
     * Renders an overlay pattern for inactive restrictions
     */
    private void renderInactiveOverlay(Graphics2D g, Point2D[] points, RestrictionStyle style) {
        // Use a dashed pattern to indicate inactive state
        Stroke originalStroke = g.getStroke();
        Color originalColor = g.getColor();
        
        // Create semi-transparent dashed stroke
        float[] dashPattern = {10.0f, 10.0f};
        BasicStroke dashedStroke = new BasicStroke(
            style.getStroke().getLineWidth(),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            dashPattern,
            0.0f
        );
        
        g.setStroke(dashedStroke);
        g.setColor(new Color(style.getColor().getRed(), style.getColor().getGreen(), 
                           style.getColor().getBlue(), 128)); // 50% transparency
        
        // Redraw the path with dashed pattern
        for (int i = 0; i < points.length - 1; i++) {
            if (points[i] != null && points[i + 1] != null) {
                g.drawLine(
                    (int) points[i].getX(), (int) points[i].getY(),
                    (int) points[i + 1].getX(), (int) points[i + 1].getY()
                );
            }
        }
        
        // Restore original graphics state
        g.setStroke(originalStroke);
        g.setColor(originalColor);
    }
    
    /**
     * Gets the appropriate visual style for a restriction
     */
    private RestrictionStyle getStyleForRestriction(ConditionalRestriction restriction, LocalDateTime currentTime) {
        boolean isActive = restriction.isActiveAt(currentTime);
        return RestrictionStyle.getStyleForType(restriction.getType(), isActive, settings);
    }
    
    /**
     * Enables or disables the visualization
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        invalidate();
    }
    
    /**
     * Checks if visualization is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the visualization settings
     */
    public VisualizationSettings getSettings() {
        return settings;
    }
    
    /**
     * Forces a refresh of the visualization
     */
    public void refresh() {
        lastRenderBounds = null;
        lastUpdateTime = 0;
        invalidate();
    }
    
    /**
     * Invalidates cached rendering data
     */
    public void invalidate() {
        lastRenderBounds = null;
        lastZoomLevel = -1;
        lastUpdateTime = 0;
    }
    
    @Override
    public String toString() {
        return "ConditionalRestrictionsRenderer[enabled=" + enabled + "]";
    }
}