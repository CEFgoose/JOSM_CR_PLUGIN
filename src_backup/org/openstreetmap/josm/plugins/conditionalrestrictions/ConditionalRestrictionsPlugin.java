package org.openstreetmap.josm.plugins.conditionalrestrictions;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ParseException;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ValidationResult;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.actions.AnalyzeConditionalRestrictionsAction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.integration.GraphViewIntegration;
import org.openstreetmap.josm.plugins.conditionalrestrictions.visualization.ConditionalRestrictionsRenderer;
import org.openstreetmap.josm.plugins.conditionalrestrictions.visualization.LegendPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JOSM Plugin that extends GraphView to support conditional restrictions.
 * 
 * This plugin parses and visualizes conditional access restrictions such as
 * time-based access rules, vehicle restrictions, and other conditional tags
 * commonly used in OpenStreetMap.
 * 
 * @author CEF
 */
public class ConditionalRestrictionsPlugin extends Plugin {
    
    /**
     * Supported conditional tags
     */
    public static final String[] CONDITIONAL_TAGS = {
        "access:conditional",
        "oneway:conditional",
        "hgv:conditional",
        "maxspeed:conditional",
        "parking:conditional",
        "bicycle:conditional",
        "motor_vehicle:conditional",
        "foot:conditional"
    };
    
    private ConditionalRestrictionParser parser;
    private Map<Way, List<ConditionalRestriction>> restrictionCache;
    private ConditionalRestrictionsAction mainAction;
    private AnalyzeConditionalRestrictionsAction analyzeAction;
    private GraphViewIntegration graphViewIntegration;
    
    // Visualization components
    private ConditionalRestrictionsRenderer renderer;
    private LegendPanel legendPanel;
    private boolean visualizationEnabled = false;
    
    /**
     * Creates the plugin instance
     * 
     * @param info Plugin information
     */
    public ConditionalRestrictionsPlugin(PluginInformation info) {
        super(info);
        
        this.parser = new ConditionalRestrictionParser();
        this.restrictionCache = new ConcurrentHashMap<>();
        
        // Create main menu action
        mainAction = new ConditionalRestrictionsAction();
        MainApplication.getMenu().toolsMenu.add(mainAction);
        
        // Create analyze action
        analyzeAction = new AnalyzeConditionalRestrictionsAction(this);
        MainApplication.getMenu().toolsMenu.add(analyzeAction);
        
        // Create visualization toggle action
        ToggleVisualizationAction toggleAction = new ToggleVisualizationAction();
        MainApplication.getMenu().toolsMenu.add(toggleAction);
        
        // Initialize GraphView integration
        graphViewIntegration = new GraphViewIntegration(this);
        
        Logging.info("ConditionalRestrictions plugin loaded successfully");
    }
    
    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        super.mapFrameInitialized(oldFrame, newFrame);
        
        if (newFrame != null) {
            // Hook into GraphView if available
            tryConnectToGraphView();
            
            // Initialize visualization components
            initializeVisualization(newFrame);
        } else {
            // Clean up visualization when frame is closed
            cleanupVisualization();
        }
    }
    
    /**
     * Attempts to connect to the GraphView plugin if it's loaded
     */
    private void tryConnectToGraphView() {
        try {
            if (graphViewIntegration != null) {
                if (graphViewIntegration.isGraphViewAvailable()) {
                    Logging.info("ConditionalRestrictions: Successfully connected to GraphView plugin");
                    Logging.info("ConditionalRestrictions: Conditional routing features are now available in GraphView");
                } else {
                    Logging.info("ConditionalRestrictions: GraphView plugin not found - running independently");
                    Logging.info("ConditionalRestrictions: Conditional routing analysis available through plugin menu");
                }
            }
        } catch (Exception e) {
            Logging.warn("ConditionalRestrictions: Error during GraphView integration: " + e.getMessage());
        }
    }
    
    /**
     * Processes conditional restrictions for all ways in the current dataset
     */
    public void processDataSet(DataSet dataSet) {
        if (dataSet == null) {
            return;
        }
        
        restrictionCache.clear();
        
        for (Way way : dataSet.getWays()) {
            List<ConditionalRestriction> restrictions = parseWayRestrictions(way);
            if (!restrictions.isEmpty()) {
                restrictionCache.put(way, restrictions);
            }
        }
        
        Logging.info("Processed " + restrictionCache.size() + " ways with conditional restrictions");
    }
    
    /**
     * Parses all conditional restrictions from a way
     * 
     * @param way The way to parse
     * @return List of parsed conditional restrictions
     */
    private List<ConditionalRestriction> parseWayRestrictions(Way way) {
        List<ConditionalRestriction> restrictions = new ArrayList<>();
        
        for (String tagKey : CONDITIONAL_TAGS) {
            String tagValue = way.get(tagKey);
            if (tagValue != null && !tagValue.isEmpty()) {
                try {
                    ConditionalRestriction restriction = parser.parse(tagKey, tagValue);
                    if (restriction != null) {
                        restriction.setWay(way);
                        restrictions.add(restriction);
                    }
                } catch (ParseException e) {
                    Logging.warn("Failed to parse conditional restriction: " + 
                               tagKey + "=" + tagValue + " on way " + way.getId() + 
                               ": " + e.getMessage());
                }
            }
        }
        
        return restrictions;
    }
    
    /**
     * Gets all parsed conditional restrictions
     * 
     * @return Map of ways to their conditional restrictions
     */
    public Map<Way, List<ConditionalRestriction>> getRestrictions() {
        return new ConcurrentHashMap<>(restrictionCache);
    }
    
    /**
     * Gets the GraphView integration instance
     * 
     * @return The GraphView integration, or null if not available
     */
    public GraphViewIntegration getGraphViewIntegration() {
        return graphViewIntegration;
    }
    
    /**
     * Validates conditional restrictions in the current dataset
     * 
     * @return List of validation errors
     */
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        
        if (layer != null) {
            DataSet dataSet = layer.getDataSet();
            
            for (Way way : dataSet.getWays()) {
                for (String tagKey : CONDITIONAL_TAGS) {
                    String tagValue = way.get(tagKey);
                    if (tagValue != null && !tagValue.isEmpty()) {
                        ValidationResult result = parser.validate(tagKey, tagValue);
                        if (!result.isValid()) {
                            errors.add(new ValidationError(way, tagKey, tagValue, result.getMessage()));
                        }
                    }
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Initializes the visualization components for the map frame
     */
    private void initializeVisualization(MapFrame mapFrame) {
        try {
            // Create the renderer
            renderer = new ConditionalRestrictionsRenderer(this);
            
            // Create the legend panel
            legendPanel = new LegendPanel(renderer.getSettings());
            
            // Set up legend change listeners
            legendPanel.addLegendChangeListener(new LegendPanel.LegendChangeListener() {
                @Override
                public void onVisibilityChanged(ConditionalRestriction.RestrictionType type, boolean visible) {
                    // Refresh the renderer when visibility changes
                    if (renderer != null) {
                        renderer.refresh();
                    }
                }
                
                @Override
                public void onSettingsChanged() {
                    // Refresh the renderer when settings change
                    if (renderer != null) {
                        renderer.refresh();
                    }
                }
            });
            
            // Add the renderer to the map view
            if (mapFrame.mapView != null) {
                mapFrame.mapView.addTemporaryLayer(renderer);
                visualizationEnabled = true;
                Logging.info("ConditionalRestrictions: Map visualization initialized");
            }
            
            // Add legend panel to the map frame (if there's a side panel available)
            // Note: This would need to be adapted based on JOSM's UI structure
            
        } catch (Exception e) {
            Logging.warn("ConditionalRestrictions: Failed to initialize visualization: " + e.getMessage());
        }
    }
    
    /**
     * Cleans up visualization components
     */
    private void cleanupVisualization() {
        try {
            if (renderer != null && visualizationEnabled) {
                // Remove renderer from map view
                MapFrame mapFrame = MainApplication.getMap();
                if (mapFrame != null && mapFrame.mapView != null) {
                    mapFrame.mapView.removeTemporaryLayer(renderer);
                }
                visualizationEnabled = false;
            }
            
            renderer = null;
            legendPanel = null;
            
            Logging.info("ConditionalRestrictions: Visualization cleanup completed");
        } catch (Exception e) {
            Logging.warn("ConditionalRestrictions: Error during visualization cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Toggles the visualization on/off
     */
    public void toggleVisualization() {
        if (renderer != null) {
            renderer.setEnabled(!renderer.isEnabled());
            Logging.info("ConditionalRestrictions: Visualization " + 
                        (renderer.isEnabled() ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Gets the current visualization renderer
     */
    public ConditionalRestrictionsRenderer getRenderer() {
        return renderer;
    }
    
    /**
     * Gets the legend panel
     */
    public LegendPanel getLegendPanel() {
        return legendPanel;
    }
    
    /**
     * Checks if visualization is currently enabled
     */
    public boolean isVisualizationEnabled() {
        return visualizationEnabled && renderer != null && renderer.isEnabled();
    }
    
    /**
     * Cleanup method called when plugin is being shut down
     */
    public void cleanup() {
        cleanupVisualization();
        
        if (graphViewIntegration != null) {
            graphViewIntegration.disconnect();
        }
    }
    
    /**
     * Main action for the plugin
     */
    private class ConditionalRestrictionsAction extends JosmAction {
        
        ConditionalRestrictionsAction() {
            super("Conditional Restrictions", 
                  "conditional", 
                  "Analyze conditional restrictions in the current data", 
                  null, 
                  false);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
            
            if (layer == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "Please open an OSM data layer first.",
                    "No Data Layer",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Process the current dataset
            processDataSet(layer.getDataSet());
            
            // Show results dialog
            ConditionalRestrictionsDialog dialog = new ConditionalRestrictionsDialog(
                ConditionalRestrictionsPlugin.this
            );
            dialog.showDialog();
        }
    }
    
    /**
     * Validation error class
     */
    public static class ValidationError {
        private final Way way;
        private final String tag;
        private final String value;
        private final String message;
        
        public ValidationError(Way way, String tag, String value, String message) {
            this.way = way;
            this.tag = tag;
            this.value = value;
            this.message = message;
        }
        
        public Way getWay() { return way; }
        public String getTag() { return tag; }
        public String getValue() { return value; }
        public String getMessage() { return message; }
    }
    
    /**
     * Action to toggle visualization on/off
     */
    private class ToggleVisualizationAction extends JosmAction {
        
        ToggleVisualizationAction() {
            super("Toggle Conditional Restrictions Visualization", 
                  "conditional", 
                  "Toggle the map visualization of conditional restrictions", 
                  null, 
                  false);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            toggleVisualization();
            
            // Update menu text based on current state
            if (isVisualizationEnabled()) {
                putValue(NAME, "Hide Conditional Restrictions Visualization");
            } else {
                putValue(NAME, "Show Conditional Restrictions Visualization");
            }
        }
    }
}