package org.openstreetmap.josm.plugins.conditionalrestrictions.actions;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsDialog;
import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsPlugin;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.routing.ConditionalRoutingGraph;
import org.openstreetmap.josm.plugins.conditionalrestrictions.utils.TimeUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Menu action to trigger comprehensive conditional restriction analysis.
 * This action processes the current OSM data layer, analyzes all conditional restrictions,
 * builds a routing graph, and displays detailed results to the user.
 * 
 * The action integrates with GraphView's action system where possible and provides
 * its own comprehensive analysis when GraphView is not available.
 * 
 * @author CEF
 */
public class AnalyzeConditionalRestrictionsAction extends JosmAction {
    
    private static final String ACTION_NAME = "Analyze Conditional Restrictions";
    private static final String ACTION_DESCRIPTION = "Analyze conditional restrictions in the current data layer";
    private static final String ICON_NAME = "conditional";
    
    private ConditionalRestrictionsPlugin plugin;
    private ConditionalRestrictionsDialog analysisDialog;
    
    /**
     * Creates the analyze action
     * 
     * @param plugin The plugin instance
     */
    public AnalyzeConditionalRestrictionsAction(ConditionalRestrictionsPlugin plugin) {
        super(
            ACTION_NAME,
            new ImageProvider(ICON_NAME),
            ACTION_DESCRIPTION,
            Shortcut.registerShortcut(
                "tools:conditional_restrictions", 
                "Analyze conditional restrictions",
                KeyEvent.VK_R, 
                Shortcut.CTRL_SHIFT
            ),
            false,  // Not a toggle action
            "conditional-restrictions-analyze",
            true    // Install adapters
        );
        
        this.plugin = plugin;
        setEnabled(true);
        
        Logging.debug("AnalyzeConditionalRestrictionsAction created");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Logging.info("Starting conditional restrictions analysis...");
        
        OsmDataLayer layer = MainApplication.getLayerManager().getEditLayer();
        if (layer == null) {
            showNoDataLayerWarning();
            return;
        }
        
        DataSet dataSet = layer.getDataSet();
        if (dataSet == null || dataSet.getWays().isEmpty()) {
            showEmptyDataSetWarning();
            return;
        }
        
        try {
            // Perform comprehensive analysis
            AnalysisResult result = performAnalysis(dataSet);
            
            // Show results in dialog
            showAnalysisResults(result);
            
        } catch (Exception ex) {
            Logging.error("Error during conditional restrictions analysis", ex);
            showAnalysisError(ex);
        }
    }
    
    /**
     * Performs comprehensive analysis of conditional restrictions
     * 
     * @param dataSet The data set to analyze
     * @return Analysis results
     */
    private AnalysisResult performAnalysis(DataSet dataSet) {
        Logging.info("Analyzing conditional restrictions in dataset with " + 
                    dataSet.getWays().size() + " ways");
        
        long startTime = System.currentTimeMillis();
        
        // Process conditional restrictions
        plugin.processDataSet(dataSet);
        Map<Way, List<ConditionalRestriction>> restrictions = plugin.getRestrictions();
        
        // Validate restrictions
        List<ConditionalRestrictionsPlugin.ValidationError> validationErrors = plugin.validate();
        
        // Build routing graph
        ConditionalRoutingGraph routingGraph = new ConditionalRoutingGraph(plugin);
        routingGraph.buildGraph(dataSet);
        
        // Analyze current state
        LocalDateTime now = LocalDateTime.now();
        AnalysisSnapshot currentSnapshot = analyzeCurrentState(restrictions, now);
        
        // Calculate statistics
        AnalysisStatistics statistics = calculateStatistics(restrictions, validationErrors);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        Logging.info("Analysis completed in " + analysisTime + "ms. Found " + 
                    restrictions.size() + " ways with conditional restrictions");
        
        return new AnalysisResult(
            restrictions,
            validationErrors,
            routingGraph,
            currentSnapshot,
            statistics,
            analysisTime
        );
    }
    
    /**
     * Analyzes the current state of all conditional restrictions
     * 
     * @param restrictions Map of way restrictions
     * @param evaluationTime Time to evaluate restrictions at
     * @return Current state snapshot
     */
    private AnalysisSnapshot analyzeCurrentState(Map<Way, List<ConditionalRestriction>> restrictions, 
                                                LocalDateTime evaluationTime) {
        int activeRestrictions = 0;
        int inactiveRestrictions = 0;
        int totalConditions = 0;
        
        for (List<ConditionalRestriction> wayRestrictions : restrictions.values()) {
            for (ConditionalRestriction restriction : wayRestrictions) {
                totalConditions++;
                if (restriction.isActiveAt(evaluationTime)) {
                    activeRestrictions++;
                } else {
                    inactiveRestrictions++;
                }
            }
        }
        
        return new AnalysisSnapshot(
            evaluationTime,
            activeRestrictions,
            inactiveRestrictions,
            totalConditions
        );
    }
    
    /**
     * Calculates comprehensive statistics about the restrictions
     * 
     * @param restrictions Map of way restrictions
     * @param validationErrors List of validation errors
     * @return Analysis statistics
     */
    private AnalysisStatistics calculateStatistics(Map<Way, List<ConditionalRestriction>> restrictions,
                                                  List<ConditionalRestrictionsPlugin.ValidationError> validationErrors) {
        int totalWays = restrictions.size();
        int totalRestrictions = 0;
        int timeBasedRestrictions = 0;
        int vehicleRestrictions = 0;
        int accessRestrictions = 0;
        int speedRestrictions = 0;
        
        for (List<ConditionalRestriction> wayRestrictions : restrictions.values()) {
            for (ConditionalRestriction restriction : wayRestrictions) {
                totalRestrictions++;
                
                String tagKey = restriction.getTagKey();
                if (tagKey.contains("access")) {
                    accessRestrictions++;
                }
                if (tagKey.contains("maxspeed")) {
                    speedRestrictions++;
                }
                if (tagKey.contains("hgv") || tagKey.contains("motor_vehicle") || 
                    tagKey.contains("bicycle") || tagKey.contains("foot")) {
                    vehicleRestrictions++;
                }
                if (!restriction.getTimeConditions().isEmpty()) {
                    timeBasedRestrictions++;
                }
            }
        }
        
        return new AnalysisStatistics(
            totalWays,
            totalRestrictions,
            timeBasedRestrictions,
            vehicleRestrictions,
            accessRestrictions,
            speedRestrictions,
            validationErrors.size()
        );
    }
    
    /**
     * Shows the analysis results in a dialog
     * 
     * @param result The analysis result to display
     */
    private void showAnalysisResults(AnalysisResult result) {
        if (analysisDialog != null) {
            analysisDialog.dispose();
        }
        
        analysisDialog = new ConditionalRestrictionsDialog(plugin);
        analysisDialog.setAnalysisResult(result);
        analysisDialog.showDialog();
    }
    
    /**
     * Shows warning when no data layer is available
     */
    private void showNoDataLayerWarning() {
        JOptionPane.showMessageDialog(
            MainApplication.getMainFrame(),
            tr("Please open an OSM data layer first.\n\n" +
               "You need to have map data loaded to analyze conditional restrictions."),
            tr("No Data Layer"),
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    /**
     * Shows warning when data set is empty
     */
    private void showEmptyDataSetWarning() {
        JOptionPane.showMessageDialog(
            MainApplication.getMainFrame(),
            tr("The current data layer is empty.\n\n" +
               "Please download or load some map data to analyze."),
            tr("Empty Data Set"),
            JOptionPane.WARNING_MESSAGE
        );
    }
    
    /**
     * Shows error dialog when analysis fails
     * 
     * @param exception The exception that occurred
     */
    private void showAnalysisError(Exception exception) {
        String message = tr("An error occurred during conditional restrictions analysis:\n\n{0}\n\n" +
                           "Please check the JOSM log for more details.", 
                           exception.getMessage());
        
        JOptionPane.showMessageDialog(
            MainApplication.getMainFrame(),
            message,
            tr("Analysis Error"),
            JOptionPane.ERROR_MESSAGE
        );
    }
    
    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.getLayerManager().getEditLayer() != null);
    }
    
    /**
     * Container for analysis results
     */
    public static class AnalysisResult {
        private final Map<Way, List<ConditionalRestriction>> restrictions;
        private final List<ConditionalRestrictionsPlugin.ValidationError> validationErrors;
        private final ConditionalRoutingGraph routingGraph;
        private final AnalysisSnapshot currentSnapshot;
        private final AnalysisStatistics statistics;
        private final long analysisTime;
        
        public AnalysisResult(Map<Way, List<ConditionalRestriction>> restrictions,
                            List<ConditionalRestrictionsPlugin.ValidationError> validationErrors,
                            ConditionalRoutingGraph routingGraph,
                            AnalysisSnapshot currentSnapshot,
                            AnalysisStatistics statistics,
                            long analysisTime) {
            this.restrictions = restrictions;
            this.validationErrors = validationErrors;
            this.routingGraph = routingGraph;
            this.currentSnapshot = currentSnapshot;
            this.statistics = statistics;
            this.analysisTime = analysisTime;
        }
        
        // Getters
        public Map<Way, List<ConditionalRestriction>> getRestrictions() { return restrictions; }
        public List<ConditionalRestrictionsPlugin.ValidationError> getValidationErrors() { return validationErrors; }
        public ConditionalRoutingGraph getRoutingGraph() { return routingGraph; }
        public AnalysisSnapshot getCurrentSnapshot() { return currentSnapshot; }
        public AnalysisStatistics getStatistics() { return statistics; }
        public long getAnalysisTime() { return analysisTime; }
    }
    
    /**
     * Snapshot of restriction states at a specific time
     */
    public static class AnalysisSnapshot {
        private final LocalDateTime evaluationTime;
        private final int activeRestrictions;
        private final int inactiveRestrictions;
        private final int totalConditions;
        
        public AnalysisSnapshot(LocalDateTime evaluationTime, int activeRestrictions, 
                              int inactiveRestrictions, int totalConditions) {
            this.evaluationTime = evaluationTime;
            this.activeRestrictions = activeRestrictions;
            this.inactiveRestrictions = inactiveRestrictions;
            this.totalConditions = totalConditions;
        }
        
        public LocalDateTime getEvaluationTime() { return evaluationTime; }
        public int getActiveRestrictions() { return activeRestrictions; }
        public int getInactiveRestrictions() { return inactiveRestrictions; }
        public int getTotalConditions() { return totalConditions; }
        
        public String getActivationPercentage() {
            if (totalConditions == 0) return "0%";
            return String.format("%.1f%%", (activeRestrictions * 100.0) / totalConditions);
        }
    }
    
    /**
     * Statistical information about analyzed restrictions
     */
    public static class AnalysisStatistics {
        private final int totalWays;
        private final int totalRestrictions;
        private final int timeBasedRestrictions;
        private final int vehicleRestrictions;
        private final int accessRestrictions;
        private final int speedRestrictions;
        private final int validationErrors;
        
        public AnalysisStatistics(int totalWays, int totalRestrictions, int timeBasedRestrictions,
                                int vehicleRestrictions, int accessRestrictions, int speedRestrictions,
                                int validationErrors) {
            this.totalWays = totalWays;
            this.totalRestrictions = totalRestrictions;
            this.timeBasedRestrictions = timeBasedRestrictions;
            this.vehicleRestrictions = vehicleRestrictions;
            this.accessRestrictions = accessRestrictions;
            this.speedRestrictions = speedRestrictions;
            this.validationErrors = validationErrors;
        }
        
        // Getters
        public int getTotalWays() { return totalWays; }
        public int getTotalRestrictions() { return totalRestrictions; }
        public int getTimeBasedRestrictions() { return timeBasedRestrictions; }
        public int getVehicleRestrictions() { return vehicleRestrictions; }
        public int getAccessRestrictions() { return accessRestrictions; }
        public int getSpeedRestrictions() { return speedRestrictions; }
        public int getValidationErrors() { return validationErrors; }
        
        public double getAverageRestrictionsPerWay() {
            return totalWays > 0 ? (double) totalRestrictions / totalWays : 0.0;
        }
        
        public String getValidationSuccessRate() {
            if (totalRestrictions == 0) return "100%";
            double successRate = ((totalRestrictions - validationErrors) * 100.0) / totalRestrictions;
            return String.format("%.1f%%", successRate);
        }
    }
}