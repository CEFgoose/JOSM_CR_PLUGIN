package org.openstreetmap.josm.plugins.conditionalrestrictions.visualization;

import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction.RestrictionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Legend panel that displays visual indicators for conditional restrictions.
 * 
 * Shows the meaning of different colors, patterns, and symbols used in the
 * map visualization. Allows users to toggle visibility of restriction types.
 * 
 * @author CEF
 */
public class LegendPanel extends JPanel {
    
    private final VisualizationSettings settings;
    private final List<LegendChangeListener> listeners;
    private boolean isCollapsed = false;
    
    // UI Components
    private JPanel contentPanel;
    private JButton toggleButton;
    private final List<LegendItem> legendItems;
    
    /**
     * Interface for listening to legend changes
     */
    public interface LegendChangeListener {
        void onVisibilityChanged(RestrictionType type, boolean visible);
        void onSettingsChanged();
    }
    
    /**
     * Creates a new legend panel
     */
    public LegendPanel(VisualizationSettings settings) {
        this.settings = settings;
        this.listeners = new ArrayList<>();
        this.legendItems = new ArrayList<>();
        
        initializeUI();
        updateLegend();
    }
    
    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Conditional Restrictions Legend"));
        setBackground(Color.WHITE);
        
        // Create toggle button for collapsing/expanding
        toggleButton = new JButton("⏷");
        toggleButton.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        toggleButton.setMargin(new Insets(2, 4, 2, 4));
        toggleButton.addActionListener(e -> toggleCollapsed());
        
        // Create header panel with title and toggle button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(new JLabel("Legend"), BorderLayout.CENTER);
        headerPanel.add(toggleButton, BorderLayout.EAST);
        
        // Create content panel for legend items
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        // Add settings panel at bottom
        add(createSettingsPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * Creates the settings panel with global options
     */
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        panel.setBackground(Color.WHITE);
        
        // Show inactive restrictions checkbox
        JCheckBox showInactiveBox = new JCheckBox("Show Inactive", settings.isShowInactiveRestrictions());
        showInactiveBox.setBackground(Color.WHITE);
        showInactiveBox.addActionListener(e -> {
            settings.setShowInactiveRestrictions(showInactiveBox.isSelected());
            fireSettingsChanged();
        });
        
        // Show direction arrows checkbox
        JCheckBox showArrowsBox = new JCheckBox("Show Arrows", settings.isShowDirectionArrows());
        showArrowsBox.setBackground(Color.WHITE);
        showArrowsBox.addActionListener(e -> {
            settings.setShowDirectionArrows(showArrowsBox.isSelected());
            fireSettingsChanged();
        });
        
        panel.add(showInactiveBox);
        panel.add(showArrowsBox);
        
        return panel;
    }
    
    /**
     * Updates the legend items based on current settings
     */
    public void updateLegend() {
        contentPanel.removeAll();
        legendItems.clear();
        
        if (isCollapsed) {
            revalidate();
            repaint();
            return;
        }
        
        // Create legend items for each restriction type
        for (RestrictionType type : RestrictionType.values()) {
            LegendItem activeItem = new LegendItem(type, true, settings);
            LegendItem inactiveItem = new LegendItem(type, false, settings);
            
            legendItems.add(activeItem);
            legendItems.add(inactiveItem);
            
            contentPanel.add(activeItem);
            if (settings.isShowInactiveRestrictions()) {
                contentPanel.add(inactiveItem);
            }
            contentPanel.add(Box.createVerticalStrut(3));
        }
        
        // Add explanation text
        JPanel explanationPanel = createExplanationPanel();
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(explanationPanel);
        
        revalidate();
        repaint();
    }
    
    /**
     * Creates an explanation panel with usage information
     */
    private JPanel createExplanationPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Usage"));
        
        String[] explanations = {
            "• Solid lines: Currently active restrictions",
            "• Faded lines: Inactive restrictions",
            "• Arrows: Direction of oneway restrictions",
            "• Different colors: Different restriction types",
            "• Click legend items to toggle visibility"
        };
        
        for (String text : explanations) {
            JLabel label = new JLabel(text);
            label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
        }
        
        return panel;
    }
    
    /**
     * Toggles the collapsed state of the legend
     */
    private void toggleCollapsed() {
        isCollapsed = !isCollapsed;
        toggleButton.setText(isCollapsed ? "⏵" : "⏷");
        contentPanel.setVisible(!isCollapsed);
        updateLegend();
    }
    
    /**
     * Adds a listener for legend changes
     */
    public void addLegendChangeListener(LegendChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener for legend changes
     */
    public void removeLegendChangeListener(LegendChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Fires visibility changed event
     */
    private void fireVisibilityChanged(RestrictionType type, boolean visible) {
        for (LegendChangeListener listener : listeners) {
            listener.onVisibilityChanged(type, visible);
        }
    }
    
    /**
     * Fires settings changed event
     */
    private void fireSettingsChanged() {
        for (LegendChangeListener listener : listeners) {
            listener.onSettingsChanged();
        }
        updateLegend();
    }
    
    /**
     * Individual legend item component
     */
    private class LegendItem extends JPanel {
        private final RestrictionType type;
        private final boolean isActive;
        private final VisualizationSettings settings;
        private JCheckBox visibilityBox;
        
        public LegendItem(RestrictionType type, boolean isActive, VisualizationSettings settings) {
            this.type = type;
            this.isActive = isActive;
            this.settings = settings;
            
            initializeItem();
        }
        
        private void initializeItem() {
            setLayout(new BorderLayout(5, 0));
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            
            // Create visual preview
            LegendPreview preview = new LegendPreview(type, isActive);
            preview.setPreferredSize(new Dimension(40, 20));
            
            // Create description label
            RestrictionStyle style = RestrictionStyle.createPreviewStyle(type, isActive);
            JLabel descLabel = new JLabel(style.getDescription());
            descLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            
            // Create visibility checkbox (only for active items)
            if (isActive) {
                visibilityBox = new JCheckBox();
                visibilityBox.setSelected(settings.isShowRestrictionsOfType(type));
                visibilityBox.setBackground(Color.WHITE);
                visibilityBox.addActionListener(e -> {
                    settings.setShowRestrictionsOfType(type, visibilityBox.isSelected());
                    fireVisibilityChanged(type, visibilityBox.isSelected());
                });
                
                add(visibilityBox, BorderLayout.WEST);
            }
            
            add(preview, BorderLayout.CENTER);
            add(descLabel, BorderLayout.EAST);
            
            // Add click handler for toggling visibility
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isActive && visibilityBox != null) {
                        visibilityBox.setSelected(!visibilityBox.isSelected());
                        settings.setShowRestrictionsOfType(type, visibilityBox.isSelected());
                        fireVisibilityChanged(type, visibilityBox.isSelected());
                    }
                }
            });
        }
    }
    
    /**
     * Preview component that draws a sample of the restriction style
     */
    private static class LegendPreview extends JComponent {
        private final RestrictionType type;
        private final boolean isActive;
        
        public LegendPreview(RestrictionType type, boolean isActive) {
            this.type = type;
            this.isActive = isActive;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            RestrictionStyle style = RestrictionStyle.createPreviewStyle(type, isActive);
            
            g2.setColor(style.getColor());
            g2.setStroke(style.getStroke());
            
            // Draw a sample line
            int y = getHeight() / 2;
            g2.drawLine(5, y, getWidth() - 5, y);
            
            // Add arrow for oneway restrictions
            if (type == RestrictionType.ONEWAY && isActive) {
                int arrowX = getWidth() - 10;
                int arrowSize = 4;
                g2.drawLine(arrowX, y, arrowX - arrowSize, y - arrowSize/2);
                g2.drawLine(arrowX, y, arrowX - arrowSize, y + arrowSize/2);
            }
            
            g2.dispose();
        }
    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        if (isCollapsed) {
            return new Dimension(preferred.width, 50);
        }
        return new Dimension(Math.max(250, preferred.width), preferred.height);
    }
}