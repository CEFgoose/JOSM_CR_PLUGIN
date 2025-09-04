package org.openstreetmap.josm.plugins.conditionalrestrictions;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.actions.AnalyzeConditionalRestrictionsAction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

/**
 * Dialog for displaying conditional restrictions analysis results
 * 
 * @author CEF
 */
public class ConditionalRestrictionsDialog extends ExtendedDialog {
    
    private final ConditionalRestrictionsPlugin plugin;
    private JTable restrictionsTable;
    private JTable errorsTable;
    private DefaultTableModel restrictionsModel;
    private DefaultTableModel errorsModel;
    
    public ConditionalRestrictionsDialog(ConditionalRestrictionsPlugin plugin) {
        super(MainApplication.getMainFrame(), 
              "Conditional Restrictions Analysis", 
              new String[]{"Close"});
              
        this.plugin = plugin;
        setupUI();
        loadData();
    }
    
    private void setupUI() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Restrictions tab
        tabbedPane.addTab("Parsed Restrictions", createRestrictionsPanel());
        
        // Validation errors tab  
        tabbedPane.addTab("Validation Errors", createErrorsPanel());
        
        setContent(tabbedPane, false);
        setSize(800, 600);
    }
    
    private JPanel createRestrictionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table for restrictions
        restrictionsModel = new DefaultTableModel(
            new String[]{"Way ID", "Tag", "Value", "Conditions", "Description"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        restrictionsTable = new JTable(restrictionsModel);
        restrictionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener to zoom to selected way
        restrictionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = restrictionsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String wayIdStr = (String) restrictionsModel.getValueAt(selectedRow, 0);
                    try {
                        long wayId = Long.parseLong(wayIdStr);
                        zoomToWay(wayId);
                    } catch (NumberFormatException ex) {
                        // Ignore
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(restrictionsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add info label
        JLabel infoLabel = new JLabel("Click on a row to zoom to the way in the map");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(infoLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createErrorsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Table for errors
        errorsModel = new DefaultTableModel(
            new String[]{"Way ID", "Tag", "Value", "Error"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        errorsTable = new JTable(errorsModel);
        errorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add selection listener
        errorsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = errorsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String wayIdStr = (String) errorsModel.getValueAt(selectedRow, 0);
                    try {
                        long wayId = Long.parseLong(wayIdStr);
                        zoomToWay(wayId);
                    } catch (NumberFormatException ex) {
                        // Ignore
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(errorsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add refresh button
        JButton refreshButton = new JButton("Refresh Validation");
        refreshButton.addActionListener(e -> {
            loadValidationErrors();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void loadData() {
        loadRestrictions();
        loadValidationErrors();
    }
    
    private void loadRestrictions() {
        restrictionsModel.setRowCount(0);
        
        Map<Way, List<ConditionalRestriction>> restrictions = plugin.getRestrictions();
        
        for (Map.Entry<Way, List<ConditionalRestriction>> entry : restrictions.entrySet()) {
            Way way = entry.getKey();
            
            for (ConditionalRestriction restriction : entry.getValue()) {
                restrictionsModel.addRow(new Object[]{
                    String.valueOf(way.getId()),
                    restriction.getTagKey(),
                    restriction.getRestrictionValue(),
                    restriction.getTimeConditions().size() + 
                        restriction.getGenericConditions().size() + " conditions",
                    restriction.getDescription()
                });
            }
        }
    }
    
    private void loadValidationErrors() {
        errorsModel.setRowCount(0);
        
        List<ConditionalRestrictionsPlugin.ValidationError> errors = plugin.validate();
        
        for (ConditionalRestrictionsPlugin.ValidationError error : errors) {
            errorsModel.addRow(new Object[]{
                String.valueOf(error.getWay().getId()),
                error.getTag(),
                error.getValue(),
                error.getMessage()
            });
        }
    }
    
    private void zoomToWay(long wayId) {
        // This would implement zooming to the specified way
        // For now, just show a message
        JOptionPane.showMessageDialog(this, 
            "Would zoom to way " + wayId + " (not implemented in this demo)",
            "Zoom to Way",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public ExtendedDialog showDialog() {
        setVisible(true);
        return this;
    }
    
    /**
     * Sets the analysis result to display in the dialog.
     * This method is used by AnalyzeConditionalRestrictionsAction to pass
     * comprehensive analysis results to the dialog.
     * 
     * @param result The analysis result to display
     */
    public void setAnalysisResult(AnalyzeConditionalRestrictionsAction.AnalysisResult result) {
        if (result == null) {
            return;
        }
        
        // Clear existing data
        restrictionsModel.setRowCount(0);
        errorsModel.setRowCount(0);
        
        // Load restrictions from analysis result
        Map<Way, List<ConditionalRestriction>> restrictions = result.getRestrictions();
        for (Map.Entry<Way, List<ConditionalRestriction>> entry : restrictions.entrySet()) {
            Way way = entry.getKey();
            
            for (ConditionalRestriction restriction : entry.getValue()) {
                restrictionsModel.addRow(new Object[]{
                    String.valueOf(way.getId()),
                    restriction.getTagKey(),
                    restriction.getRestrictionValue(),
                    restriction.getTimeConditions().size() + 
                        restriction.getGenericConditions().size() + " conditions",
                    restriction.getDescription()
                });
            }
        }
        
        // Load validation errors from analysis result
        List<ConditionalRestrictionsPlugin.ValidationError> errors = result.getValidationErrors();
        for (ConditionalRestrictionsPlugin.ValidationError error : errors) {
            errorsModel.addRow(new Object[]{
                String.valueOf(error.getWay().getId()),
                error.getTag(),
                error.getValue(),
                error.getMessage()
            });
        }
        
        // Update dialog title with analysis info
        String title = String.format("Conditional Restrictions Analysis - %d restrictions, %d errors (%.2fs)", 
                      restrictions.size(), 
                      errors.size(),
                      result.getAnalysisTime() / 1000.0);
        setTitle(title);
    }
}