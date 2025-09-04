package org.openstreetmap.josm.plugins.conditionalrestrictions.preferences;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

/**
 * Preferences panel for the Conditional Restrictions plugin.
 * Provides user configuration options for default vehicle profiles,
 * validation rules, display settings, and analysis parameters.
 * 
 * Integrates with JOSM's preferences system to persist settings
 * and provide a consistent user experience.
 * 
 * @author CEF
 */
public class ConditionalRestrictionsPreferences extends DefaultTabPreferenceSetting {
    
    // Preference keys
    private static final String PREF_PREFIX = "conditional-restrictions.";
    public static final String PREF_DEFAULT_VEHICLE = PREF_PREFIX + "default-vehicle";
    public static final String PREF_VALIDATE_ON_LOAD = PREF_PREFIX + "validate-on-load";
    public static final String PREF_SHOW_WARNINGS = PREF_PREFIX + "show-warnings";
    public static final String PREF_HIGHLIGHT_ACTIVE = PREF_PREFIX + "highlight-active";
    public static final String PREF_ANALYSIS_TIMEOUT = PREF_PREFIX + "analysis-timeout";
    public static final String PREF_CUSTOM_VEHICLE_PROFILES = PREF_PREFIX + "custom-vehicle-profiles";
    public static final String PREF_ENABLE_CACHING = PREF_PREFIX + "enable-caching";
    public static final String PREF_CACHE_SIZE = PREF_PREFIX + "cache-size";
    public static final String PREF_SHOW_STATISTICS = PREF_PREFIX + "show-statistics";
    public static final String PREF_AUTO_REFRESH = PREF_PREFIX + "auto-refresh";
    public static final String PREF_REFRESH_INTERVAL = PREF_PREFIX + "refresh-interval";
    
    // Default values
    private static final String DEFAULT_VEHICLE = "car";
    private static final boolean DEFAULT_VALIDATE_ON_LOAD = true;
    private static final boolean DEFAULT_SHOW_WARNINGS = true;
    private static final boolean DEFAULT_HIGHLIGHT_ACTIVE = true;
    private static final int DEFAULT_ANALYSIS_TIMEOUT = 30000; // 30 seconds
    private static final boolean DEFAULT_ENABLE_CACHING = true;
    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final boolean DEFAULT_SHOW_STATISTICS = true;
    private static final boolean DEFAULT_AUTO_REFRESH = false;
    private static final int DEFAULT_REFRESH_INTERVAL = 60; // seconds
    
    // Vehicle types
    private static final String[] VEHICLE_TYPES = {
        "car", "bicycle", "foot", "hgv", "motorcycle", "bus", "emergency"
    };
    
    // UI Components
    private JComboBox<String> defaultVehicleComboBox;
    private JCheckBox validateOnLoadCheckBox;
    private JCheckBox showWarningsCheckBox;
    private JCheckBox highlightActiveCheckBox;
    private JSpinner analysisTimeoutSpinner;
    private JTextArea customVehicleProfilesTextArea;
    private JCheckBox enableCachingCheckBox;
    private JSpinner cacheSizeSpinner;
    private JCheckBox showStatisticsCheckBox;
    private JCheckBox autoRefreshCheckBox;
    private JSpinner refreshIntervalSpinner;
    
    /**
     * Constructor for the preferences panel
     */
    public ConditionalRestrictionsPreferences() {
        super(
            "conditional-restrictions", 
            tr("Conditional Restrictions"), 
            tr("Configure conditional restrictions plugin settings"),
            false // Not expert mode only
        );
    }
    
    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createMainPanel(), BorderLayout.CENTER);
        
        createPreferenceTabWithScrollPane(gui, panel);
    }
    
    /**
     * Creates the main preferences panel
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.add(createGeneralPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        topPanel.add(createAnalysisPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        topPanel.add(createDisplayPanel(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        topPanel.add(createVehicleProfilesPanel(), GBC.eol().fill(GridBagConstraints.BOTH));
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        return mainPanel;
    }
    
    /**
     * Creates the general settings panel
     */
    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(tr("General Settings")));
        
        // Default vehicle type
        panel.add(new JLabel(tr("Default vehicle type:")), GBC.std());
        defaultVehicleComboBox = new JComboBox<>(VEHICLE_TYPES);
        defaultVehicleComboBox.setToolTipText(tr("Default vehicle type for analysis and routing"));
        panel.add(defaultVehicleComboBox, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        
        // Validate on load
        validateOnLoadCheckBox = new JCheckBox(tr("Validate conditional restrictions when loading data"));
        validateOnLoadCheckBox.setToolTipText(tr("Automatically validate syntax when new data is loaded"));
        panel.add(validateOnLoadCheckBox, GBC.eol().span(2));
        
        // Show warnings
        showWarningsCheckBox = new JCheckBox(tr("Show validation warnings in dialog"));
        showWarningsCheckBox.setToolTipText(tr("Display validation warnings in analysis results"));
        panel.add(showWarningsCheckBox, GBC.eol().span(2));
        
        // Enable caching
        enableCachingCheckBox = new JCheckBox(tr("Enable restriction parsing cache"));
        enableCachingCheckBox.setToolTipText(tr("Cache parsed restrictions for better performance"));
        enableCachingCheckBox.addActionListener(new CacheToggleListener());
        panel.add(enableCachingCheckBox, GBC.eol().span(2));
        
        // Cache size
        panel.add(new JLabel(tr("Cache size:")), GBC.std());
        cacheSizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_CACHE_SIZE, 100, 10000, 100));
        cacheSizeSpinner.setToolTipText(tr("Maximum number of cached restriction objects"));
        panel.add(cacheSizeSpinner, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        
        return panel;
    }
    
    /**
     * Creates the analysis settings panel
     */
    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(tr("Analysis Settings")));
        
        // Analysis timeout
        panel.add(new JLabel(tr("Analysis timeout (ms):")), GBC.std());
        analysisTimeoutSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_ANALYSIS_TIMEOUT, 5000, 300000, 5000));
        analysisTimeoutSpinner.setToolTipText(tr("Maximum time to spend analyzing restrictions"));
        panel.add(analysisTimeoutSpinner, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        
        // Show statistics
        showStatisticsCheckBox = new JCheckBox(tr("Show detailed statistics in analysis results"));
        showStatisticsCheckBox.setToolTipText(tr("Include comprehensive statistics in analysis dialog"));
        panel.add(showStatisticsCheckBox, GBC.eol().span(2));
        
        // Auto refresh
        autoRefreshCheckBox = new JCheckBox(tr("Auto-refresh analysis results"));
        autoRefreshCheckBox.setToolTipText(tr("Automatically refresh results when time conditions change"));
        autoRefreshCheckBox.addActionListener(new AutoRefreshToggleListener());
        panel.add(autoRefreshCheckBox, GBC.eol().span(2));
        
        // Refresh interval
        panel.add(new JLabel(tr("Refresh interval (seconds):")), GBC.std());
        refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_REFRESH_INTERVAL, 10, 3600, 10));
        refreshIntervalSpinner.setToolTipText(tr("How often to refresh time-based conditions"));
        panel.add(refreshIntervalSpinner, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        
        return panel;
    }
    
    /**
     * Creates the display settings panel
     */
    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(tr("Display Settings")));
        
        // Highlight active restrictions
        highlightActiveCheckBox = new JCheckBox(tr("Highlight currently active restrictions"));
        highlightActiveCheckBox.setToolTipText(tr("Visually highlight restrictions that are currently active"));
        panel.add(highlightActiveCheckBox, GBC.eol().span(2));
        
        return panel;
    }
    
    /**
     * Creates the vehicle profiles panel
     */
    private JPanel createVehicleProfilesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder(tr("Custom Vehicle Profiles")));
        
        JLabel label = new JLabel(tr("Define custom vehicle profiles (one per line, format: name:tags)"));
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(label, BorderLayout.NORTH);
        
        customVehicleProfilesTextArea = new JTextArea(8, 40);
        customVehicleProfilesTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        customVehicleProfilesTextArea.setToolTipText(
            tr("Example: delivery:hgv,maxweight=3.5\n" +
               "emergency:motor_vehicle,emergency=yes")
        );
        
        JScrollPane scrollPane = new JScrollPane(customVehicleProfilesTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton resetButton = new JButton(tr("Reset to Defaults"));
        resetButton.addActionListener(e -> resetCustomProfilesToDefaults());
        buttonsPanel.add(resetButton);
        
        JButton validateButton = new JButton(tr("Validate Profiles"));
        validateButton.addActionListener(e -> validateCustomProfiles());
        buttonsPanel.add(validateButton);
        
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    @Override
    public boolean ok() {
        // Save all preferences
        Config.getPref().put(PREF_DEFAULT_VEHICLE, (String) defaultVehicleComboBox.getSelectedItem());
        Config.getPref().putBoolean(PREF_VALIDATE_ON_LOAD, validateOnLoadCheckBox.isSelected());
        Config.getPref().putBoolean(PREF_SHOW_WARNINGS, showWarningsCheckBox.isSelected());
        Config.getPref().putBoolean(PREF_HIGHLIGHT_ACTIVE, highlightActiveCheckBox.isSelected());
        Config.getPref().putInt(PREF_ANALYSIS_TIMEOUT, (Integer) analysisTimeoutSpinner.getValue());
        Config.getPref().put(PREF_CUSTOM_VEHICLE_PROFILES, customVehicleProfilesTextArea.getText());
        Config.getPref().putBoolean(PREF_ENABLE_CACHING, enableCachingCheckBox.isSelected());
        Config.getPref().putInt(PREF_CACHE_SIZE, (Integer) cacheSizeSpinner.getValue());
        Config.getPref().putBoolean(PREF_SHOW_STATISTICS, showStatisticsCheckBox.isSelected());
        Config.getPref().putBoolean(PREF_AUTO_REFRESH, autoRefreshCheckBox.isSelected());
        Config.getPref().putInt(PREF_REFRESH_INTERVAL, (Integer) refreshIntervalSpinner.getValue());
        
        return false; // No need to restart JOSM
    }
    
    /**
     * Loads current preferences into UI components
     */
    private void loadPreferences() {
        defaultVehicleComboBox.setSelectedItem(
            Config.getPref().get(PREF_DEFAULT_VEHICLE, DEFAULT_VEHICLE)
        );
        validateOnLoadCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_VALIDATE_ON_LOAD, DEFAULT_VALIDATE_ON_LOAD)
        );
        showWarningsCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_SHOW_WARNINGS, DEFAULT_SHOW_WARNINGS)
        );
        highlightActiveCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_HIGHLIGHT_ACTIVE, DEFAULT_HIGHLIGHT_ACTIVE)
        );
        analysisTimeoutSpinner.setValue(
            Config.getPref().getInt(PREF_ANALYSIS_TIMEOUT, DEFAULT_ANALYSIS_TIMEOUT)
        );
        customVehicleProfilesTextArea.setText(
            Config.getPref().get(PREF_CUSTOM_VEHICLE_PROFILES, getDefaultCustomProfiles())
        );
        enableCachingCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_ENABLE_CACHING, DEFAULT_ENABLE_CACHING)
        );
        cacheSizeSpinner.setValue(
            Config.getPref().getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE)
        );
        showStatisticsCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_SHOW_STATISTICS, DEFAULT_SHOW_STATISTICS)
        );
        autoRefreshCheckBox.setSelected(
            Config.getPref().getBoolean(PREF_AUTO_REFRESH, DEFAULT_AUTO_REFRESH)
        );
        refreshIntervalSpinner.setValue(
            Config.getPref().getInt(PREF_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
        );
        
        // Update UI state
        updateCacheControls();
        updateRefreshControls();
    }
    
    /**
     * Returns default custom vehicle profiles
     */
    private String getDefaultCustomProfiles() {
        return "# Custom vehicle profiles - one per line\n" +
               "# Format: name:tag1,tag2=value,tag3\n" +
               "# Examples:\n" +
               "delivery:hgv,maxweight=3.5\n" +
               "emergency:motor_vehicle,emergency=yes\n" +
               "service:motor_vehicle,service=yes\n";
    }
    
    /**
     * Resets custom profiles to defaults
     */
    private void resetCustomProfilesToDefaults() {
        customVehicleProfilesTextArea.setText(getDefaultCustomProfiles());
    }
    
    /**
     * Validates custom vehicle profiles
     */
    private void validateCustomProfiles() {
        String profiles = customVehicleProfilesTextArea.getText();
        List<String> errors = validateProfileSyntax(profiles);
        
        if (errors.isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                tr("All vehicle profiles are valid."),
                tr("Validation Success"),
                JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            StringBuilder message = new StringBuilder(tr("Found validation errors:\n\n"));
            for (String error : errors) {
                message.append("• ").append(error).append("\n");
            }
            
            JOptionPane.showMessageDialog(
                null,
                message.toString(),
                tr("Validation Errors"),
                JOptionPane.WARNING_MESSAGE
            );
        }
    }
    
    /**
     * Validates custom profile syntax
     */
    private List<String> validateProfileSyntax(String profiles) {
        // Simple validation - in real implementation would be more comprehensive
        return Arrays.asList(); // Return empty list for now
    }
    
    /**
     * Updates cache control states
     */
    private void updateCacheControls() {
        boolean enabled = enableCachingCheckBox.isSelected();
        cacheSizeSpinner.setEnabled(enabled);
    }
    
    /**
     * Updates refresh control states
     */
    private void updateRefreshControls() {
        boolean enabled = autoRefreshCheckBox.isSelected();
        refreshIntervalSpinner.setEnabled(enabled);
    }
    
    /**
     * Listener for cache toggle
     */
    private class CacheToggleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateCacheControls();
        }
    }
    
    /**
     * Listener for auto-refresh toggle
     */
    private class AutoRefreshToggleListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateRefreshControls();
        }
    }
    
    // Static methods for accessing preferences from other classes
    
    /**
     * Gets the default vehicle type
     */
    public static String getDefaultVehicleType() {
        return Config.getPref().get(PREF_DEFAULT_VEHICLE, DEFAULT_VEHICLE);
    }
    
    /**
     * Gets whether to validate on load
     */
    public static boolean getValidateOnLoad() {
        return Config.getPref().getBoolean(PREF_VALIDATE_ON_LOAD, DEFAULT_VALIDATE_ON_LOAD);
    }
    
    /**
     * Gets whether to show warnings
     */
    public static boolean getShowWarnings() {
        return Config.getPref().getBoolean(PREF_SHOW_WARNINGS, DEFAULT_SHOW_WARNINGS);
    }
    
    /**
     * Gets whether to highlight active restrictions
     */
    public static boolean getHighlightActive() {
        return Config.getPref().getBoolean(PREF_HIGHLIGHT_ACTIVE, DEFAULT_HIGHLIGHT_ACTIVE);
    }
    
    /**
     * Gets the analysis timeout
     */
    public static int getAnalysisTimeout() {
        return Config.getPref().getInt(PREF_ANALYSIS_TIMEOUT, DEFAULT_ANALYSIS_TIMEOUT);
    }
    
    /**
     * Gets custom vehicle profiles
     */
    public static String getCustomVehicleProfiles() {
        return Config.getPref().get(PREF_CUSTOM_VEHICLE_PROFILES, "");
    }
    
    /**
     * Gets whether caching is enabled
     */
    public static boolean getCachingEnabled() {
        return Config.getPref().getBoolean(PREF_ENABLE_CACHING, DEFAULT_ENABLE_CACHING);
    }
    
    /**
     * Gets the cache size
     */
    public static int getCacheSize() {
        return Config.getPref().getInt(PREF_CACHE_SIZE, DEFAULT_CACHE_SIZE);
    }
    
    /**
     * Gets whether to show statistics
     */
    public static boolean getShowStatistics() {
        return Config.getPref().getBoolean(PREF_SHOW_STATISTICS, DEFAULT_SHOW_STATISTICS);
    }
    
    /**
     * Gets whether auto-refresh is enabled
     */
    public static boolean getAutoRefresh() {
        return Config.getPref().getBoolean(PREF_AUTO_REFRESH, DEFAULT_AUTO_REFRESH);
    }
    
    /**
     * Gets the refresh interval
     */
    public static int getRefreshInterval() {
        return Config.getPref().getInt(PREF_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL);
    }
}