package org.openstreetmap.josm.plugins.conditionalrestrictions.ui;

import org.openstreetmap.josm.plugins.conditionalrestrictions.routing.ConditionalRoutingGraph.VehicleProfile;
import org.openstreetmap.josm.plugins.conditionalrestrictions.utils.TimeUtils;
import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * UI panel for selecting conditional parameters such as vehicle type,
 * time conditions, and custom vehicle dimensions. Provides input validation
 * and real-time feedback for conditional restriction analysis.
 * 
 * @author CEF
 */
public class ConditionalParametersPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    // Vehicle selection components
    private JComboBox<VehicleProfile> vehicleProfileCombo;
    private JCheckBox useCustomDimensionsCheck;
    private JTextField customWeightField;
    private JTextField customHeightField;
    private JLabel weightLabel;
    private JLabel heightLabel;
    
    // Time condition components
    private JCheckBox useCustomTimeCheck;
    private JTextField dateTimeField;
    private JButton nowButton;
    private JLabel dateTimeLabel;
    private JLabel dateTimeFormatLabel;
    
    // Condition input components
    private JTextArea conditionTextArea;
    private JLabel conditionLabel;
    private JLabel syntaxStatusLabel;
    private JScrollPane conditionScrollPane;
    
    // Event listeners
    private List<ParameterChangeListener> listeners;
    
    /**
     * Interface for components that want to be notified of parameter changes
     */
    public interface ParameterChangeListener {
        /**
         * Called when any parameter changes
         * 
         * @param source The panel that changed
         */
        void onParametersChanged(ConditionalParametersPanel source);
    }
    
    /**
     * Creates a new conditional parameters panel
     */
    public ConditionalParametersPanel() {
        this.listeners = new ArrayList<>();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
        updateDimensionFields();
    }
    
    /**
     * Initializes all UI components
     */
    private void initializeComponents() {
        // Vehicle profile selection
        vehicleProfileCombo = new JComboBox<>(VehicleProfile.values());
        vehicleProfileCombo.setSelectedItem(VehicleProfile.CAR);
        vehicleProfileCombo.setToolTipText(tr("Select the vehicle type for routing analysis"));
        
        useCustomDimensionsCheck = new JCheckBox(tr("Use custom dimensions"));
        useCustomDimensionsCheck.setToolTipText(tr("Override default vehicle weight and height"));
        
        customWeightField = new JTextField(8);
        customWeightField.setToolTipText(tr("Vehicle weight in tonnes (e.g., 3.5)"));
        setupNumericFilter(customWeightField);
        
        customHeightField = new JTextField(8);
        customHeightField.setToolTipText(tr("Vehicle height in meters (e.g., 2.5)"));
        setupNumericFilter(customHeightField);
        
        weightLabel = new JLabel(tr("Weight (t):"));
        heightLabel = new JLabel(tr("Height (m):"));
        
        // Time condition components
        useCustomTimeCheck = new JCheckBox(tr("Use custom date/time"));
        useCustomTimeCheck.setToolTipText(tr("Specify a custom time for condition evaluation"));
        
        dateTimeField = new JTextField(16);
        dateTimeField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dateTimeField.setToolTipText(tr("Date and time in format: yyyy-MM-dd HH:mm"));
        
        nowButton = new JButton(tr("Now"));
        nowButton.setToolTipText(tr("Set to current date and time"));
        
        dateTimeLabel = new JLabel(tr("Date/Time:"));
        dateTimeFormatLabel = new JLabel(tr("Format: yyyy-MM-dd HH:mm"));
        dateTimeFormatLabel.setFont(dateTimeFormatLabel.getFont().deriveFont(Font.ITALIC, 10f));
        dateTimeFormatLabel.setForeground(Color.GRAY);
        
        // Condition input components
        conditionLabel = new JLabel(tr("Test Condition (optional):"));
        
        conditionTextArea = new JTextArea(4, 30);
        conditionTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        conditionTextArea.setToolTipText(tr("Enter a conditional restriction condition to test (e.g., Mo-Fr 07:00-19:00)"));
        conditionTextArea.setLineWrap(true);
        conditionTextArea.setWrapStyleWord(true);
        
        conditionScrollPane = new JScrollPane(conditionTextArea);
        conditionScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        conditionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        syntaxStatusLabel = new JLabel(" ");
        syntaxStatusLabel.setFont(syntaxStatusLabel.getFont().deriveFont(Font.ITALIC, 10f));
    }
    
    /**
     * Lays out components in the panel
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(tr("Conditional Parameters")));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Vehicle profile section
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel(tr("Vehicle Profile:")), gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(vehicleProfileCombo, gbc);
        
        // Custom dimensions
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(useCustomDimensionsCheck, gbc);
        
        gbc.gridy = 2;
        gbc.gridx = 0; gbc.gridwidth = 1;
        mainPanel.add(weightLabel, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(customWeightField, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(heightLabel, gbc);
        
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(customHeightField, gbc);
        
        // Time section
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(useCustomTimeCheck, gbc);
        
        gbc.gridy = 4;
        gbc.gridx = 0; gbc.gridwidth = 1;
        mainPanel.add(dateTimeLabel, gbc);
        
        gbc.gridx = 1; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(dateTimeField, gbc);
        
        gbc.gridx = 3; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(nowButton, gbc);
        
        gbc.gridx = 1; gbc.gridy = 5;
        gbc.gridwidth = 3;
        mainPanel.add(dateTimeFormatLabel, gbc);
        
        // Condition input section
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(conditionLabel, gbc);
        
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(conditionScrollPane, gbc);
        
        gbc.gridy = 8;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(syntaxStatusLabel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Sets up event handlers for all components
     */
    private void setupEventHandlers() {
        vehicleProfileCombo.addActionListener(e -> {
            updateDimensionFields();
            notifyListeners();
        });
        
        useCustomDimensionsCheck.addActionListener(e -> {
            updateDimensionFields();
            notifyListeners();
        });
        
        ActionListener dimensionListener = e -> notifyListeners();
        customWeightField.addActionListener(dimensionListener);
        customHeightField.addActionListener(dimensionListener);
        
        useCustomTimeCheck.addActionListener(e -> {
            updateTimeFields();
            notifyListeners();
        });
        
        dateTimeField.addActionListener(e -> {
            validateDateTime();
            notifyListeners();
        });
        
        nowButton.addActionListener(e -> {
            dateTimeField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            validateDateTime();
            notifyListeners();
        });
        
        // Add document listener for condition text area
        conditionTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                validateConditionSyntax();
                notifyListeners();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                validateConditionSyntax();
                notifyListeners();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                validateConditionSyntax();
                notifyListeners();
            }
        });
        
        // Initial validation
        updateTimeFields();
        validateDateTime();
        validateConditionSyntax();
    }
    
    /**
     * Sets up numeric input filter for text fields
     */
    private void setupNumericFilter(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (isNumeric(string)) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (isNumeric(text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
            
            private boolean isNumeric(String text) {
                return text.matches("[0-9.]*");
            }
        });
    }
    
    /**
     * Updates the dimension fields based on current settings
     */
    private void updateDimensionFields() {
        boolean customEnabled = useCustomDimensionsCheck.isSelected();
        
        customWeightField.setEnabled(customEnabled);
        customHeightField.setEnabled(customEnabled);
        weightLabel.setEnabled(customEnabled);
        heightLabel.setEnabled(customEnabled);
        
        if (!customEnabled) {
            VehicleProfile profile = (VehicleProfile) vehicleProfileCombo.getSelectedItem();
            if (profile != null) {
                customWeightField.setText(String.valueOf(profile.getDefaultWeight()));
                customHeightField.setText(String.valueOf(profile.getDefaultHeight()));
            }
        }
    }
    
    /**
     * Updates the time fields based on current settings
     */
    private void updateTimeFields() {
        boolean customEnabled = useCustomTimeCheck.isSelected();
        
        dateTimeField.setEnabled(customEnabled);
        dateTimeLabel.setEnabled(customEnabled);
        dateTimeFormatLabel.setEnabled(customEnabled);
        nowButton.setEnabled(customEnabled);
    }
    
    /**
     * Validates the date/time input
     */
    private void validateDateTime() {
        if (!useCustomTimeCheck.isSelected()) {
            dateTimeField.setBackground(Color.WHITE);
            return;
        }
        
        try {
            TimeUtils.parseDateTime(dateTimeField.getText());
            dateTimeField.setBackground(Color.WHITE);
            dateTimeField.setToolTipText(tr("Date and time in format: yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException e) {
            dateTimeField.setBackground(new Color(255, 200, 200));
            dateTimeField.setToolTipText(tr("Invalid date/time format: {0}", e.getMessage()));
        }
    }
    
    /**
     * Validates the condition syntax
     */
    private void validateConditionSyntax() {
        String condition = conditionTextArea.getText().trim();
        
        if (condition.isEmpty()) {
            syntaxStatusLabel.setText(" ");
            syntaxStatusLabel.setForeground(Color.BLACK);
            conditionTextArea.setBackground(Color.WHITE);
            return;
        }
        
        try {
            TimeUtils.validateConditionSyntax(condition);
            syntaxStatusLabel.setText(tr("✓ Valid condition syntax"));
            syntaxStatusLabel.setForeground(new Color(0, 128, 0));
            conditionTextArea.setBackground(Color.WHITE);
        } catch (Exception e) {
            syntaxStatusLabel.setText(tr("✗ Invalid syntax: {0}", e.getMessage()));
            syntaxStatusLabel.setForeground(Color.RED);
            conditionTextArea.setBackground(new Color(255, 250, 250));
        }
    }
    
    /**
     * Adds a parameter change listener
     */
    public void addParameterChangeListener(ParameterChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a parameter change listener
     */
    public void removeParameterChangeListener(ParameterChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all listeners of parameter changes
     */
    private void notifyListeners() {
        for (ParameterChangeListener listener : listeners) {
            try {
                listener.onParametersChanged(this);
            } catch (Exception e) {
                // Don't let listener exceptions break the UI
                e.printStackTrace();
            }
        }
    }
    
    // Public getters for current parameter values
    
    /**
     * Gets the currently selected vehicle profile
     */
    public VehicleProfile getSelectedVehicleProfile() {
        return (VehicleProfile) vehicleProfileCombo.getSelectedItem();
    }
    
    /**
     * Gets the custom weight value, or null if not using custom dimensions
     */
    public Double getCustomWeight() {
        if (!useCustomDimensionsCheck.isSelected()) {
            return null;
        }
        
        try {
            String text = customWeightField.getText().trim();
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets the custom height value, or null if not using custom dimensions
     */
    public Double getCustomHeight() {
        if (!useCustomDimensionsCheck.isSelected()) {
            return null;
        }
        
        try {
            String text = customHeightField.getText().trim();
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Gets the current date/time for evaluation
     */
    public LocalDateTime getDateTime() {
        if (!useCustomTimeCheck.isSelected()) {
            return LocalDateTime.now();
        }
        
        try {
            return TimeUtils.parseDateTime(dateTimeField.getText());
        } catch (DateTimeParseException e) {
            return LocalDateTime.now(); // Fallback to current time
        }
    }
    
    /**
     * Gets the test condition text
     */
    public String getTestCondition() {
        return conditionTextArea.getText().trim();
    }
    
    /**
     * Checks if the current parameters are valid
     */
    public boolean isValid() {
        // Check date/time validity
        if (useCustomTimeCheck.isSelected()) {
            try {
                TimeUtils.parseDateTime(dateTimeField.getText());
            } catch (DateTimeParseException e) {
                return false;
            }
        }
        
        // Check condition syntax validity
        String condition = conditionTextArea.getText().trim();
        if (!condition.isEmpty()) {
            try {
                TimeUtils.validateConditionSyntax(condition);
            } catch (Exception e) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Resets all parameters to default values
     */
    public void resetToDefaults() {
        vehicleProfileCombo.setSelectedItem(VehicleProfile.CAR);
        useCustomDimensionsCheck.setSelected(false);
        useCustomTimeCheck.setSelected(false);
        dateTimeField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        conditionTextArea.setText("");
        
        updateDimensionFields();
        updateTimeFields();
        validateDateTime();
        validateConditionSyntax();
        notifyListeners();
    }
}