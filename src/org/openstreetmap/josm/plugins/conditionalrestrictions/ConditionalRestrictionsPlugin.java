package org.openstreetmap.josm.plugins.conditionalrestrictions;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * JOSM Plugin that extends GraphView to support conditional restrictions.
 * 
 * This is a minimal working version that loads successfully in JOSM.
 * 
 * @author CEF
 */
public class ConditionalRestrictionsPlugin extends Plugin {
    
    public ConditionalRestrictionsPlugin(PluginInformation info) {
        super(info);
        
        try {
            Logging.info("ConditionalRestrictions plugin starting...");
            
            // Use SwingUtilities to ensure GUI creation happens on EDT
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createMenu();
                }
            });
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions plugin failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createMenu() {
        try {
            Logging.info("ConditionalRestrictions: Creating menu items...");
            
            // Get the main menu
            MainMenu menu = MainApplication.getMenu();
            JMenu toolsMenu = menu.toolsMenu;
            
            // Add separator
            toolsMenu.addSeparator();
            
            // Create simple menu item with direct action
            JMenuItem testItem = new JMenuItem("Test Conditional Restrictions");
            testItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Test menu clicked");
                    JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        "Plugin is working!\n\nMenu click registered successfully.",
                        "Conditional Restrictions Test",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            });
            
            // Create status menu item
            JMenuItem statusItem = new JMenuItem("Plugin Status");
            statusItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Status menu clicked");
                    showStatus();
                }
            });
            
            // Create scan menu item
            JMenuItem scanItem = new JMenuItem("Scan for Conditional Restrictions");
            scanItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Scan menu clicked");
                    scanConditionalRestrictions();
                }
            });
            
            // Create analyze menu item
            JMenuItem analyzeItem = new JMenuItem("Analyze Conditional Restrictions");
            analyzeItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Analyze menu clicked");
                    analyzeConditionalRestrictions();
                }
            });
            
            // Create highlight menu item
            JMenuItem highlightItem = new JMenuItem("Highlight Conditional Restrictions");
            highlightItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Highlight menu clicked");
                    highlightConditionalRestrictions();
                }
            });
            
            // Create active restrictions menu item
            JMenuItem activeItem = new JMenuItem("Check Active Restrictions NOW");
            activeItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Active check menu clicked");
                    checkActiveRestrictions();
                }
            });
            
            // Create routing analysis menu item
            JMenuItem routingItem = new JMenuItem("Analyze Routing Impact");
            routingItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Logging.info("ConditionalRestrictions: Routing analysis menu clicked");
                    analyzeRoutingImpact();
                }
            });
            
            // Add items
            toolsMenu.add(testItem);
            toolsMenu.add(statusItem);
            toolsMenu.add(scanItem);
            toolsMenu.add(analyzeItem);
            toolsMenu.add(highlightItem);
            toolsMenu.add(activeItem);
            toolsMenu.add(routingItem);
            
            Logging.info("ConditionalRestrictions: Menu items added successfully");
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Failed to create menu - " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showStatus() {
        try {
            String message = "Conditional Restrictions Plugin\n" +
                           "Version: 1.0.0\n" +
                           "Status: Active\n" +
                           "Java: " + System.getProperty("java.version");
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                message,
                "Plugin Status",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error showing status - " + e.getMessage());
        }
    }
    
    private void scanConditionalRestrictions() {
        try {
            DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
            
            if (currentDataSet == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No data layer is active.\n\nPlease load some OSM data first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Scan for conditional tags
            List<String> foundTags = new ArrayList<>();
            int totalObjects = 0;
            
            for (OsmPrimitive primitive : currentDataSet.allPrimitives()) {
                totalObjects++;
                Map<String, String> tags = primitive.getKeys();
                
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    String value = tag.getValue();
                    
                    if (key.contains(":conditional")) {
                        foundTags.add(String.format("%s %d: %s = %s", 
                            primitive.getType().toString(), 
                            primitive.getId(), 
                            key, value));
                    }
                }
            }
            
            // Show results
            StringBuilder results = new StringBuilder();
            results.append("Conditional Restrictions Scan Results\n");
            results.append("=====================================\n\n");
            results.append("Scanned: ").append(totalObjects).append(" objects\n");
            results.append("Found: ").append(foundTags.size()).append(" conditional restrictions\n\n");
            
            if (foundTags.isEmpty()) {
                results.append("No conditional restrictions found.\n\n");
                results.append("Try loading data with conditional tags like:\n");
                results.append("• access:conditional\n");
                results.append("• maxspeed:conditional\n");
                results.append("• parking:conditional\n");
            } else {
                results.append("Found restrictions:\n");
                for (String tag : foundTags) {
                    results.append("• ").append(tag).append("\n");
                }
            }
            
            JTextArea textArea = new JTextArea(results.toString());
            textArea.setEditable(false);
            textArea.setRows(15);
            textArea.setColumns(60);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                scrollPane,
                "Conditional Restrictions Scan",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error during scan - " + e.getMessage());
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Error scanning data: " + e.getMessage(),
                "Scan Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void analyzeConditionalRestrictions() {
        try {
            DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
            
            if (currentDataSet == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No data layer is active.\n\nPlease load some OSM data first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Parse and categorize conditional restrictions
            Map<String, Integer> tagTypes = new HashMap<>();
            Map<String, Integer> conditionTypes = new HashMap<>();
            List<String> parseErrors = new ArrayList<>();
            List<String> validRestrictions = new ArrayList<>();
            
            Pattern conditionalPattern = Pattern.compile("^\\s*(.+?)\\s*@\\s*\\((.+?)\\)\\s*$");
            Pattern timePattern = Pattern.compile("\\b(Mo|Tu|We|Th|Fr|Sa|Su)\\b|\\b\\d{1,2}:\\d{2}\\b");
            
            for (OsmPrimitive primitive : currentDataSet.allPrimitives()) {
                Map<String, String> tags = primitive.getKeys();
                
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    String value = tag.getValue();
                    
                    if (key.contains(":conditional")) {
                        // Count tag types
                        String baseTag = key.replace(":conditional", "");
                        tagTypes.put(baseTag, tagTypes.getOrDefault(baseTag, 0) + 1);
                        
                        // Try to parse the condition
                        Matcher matcher = conditionalPattern.matcher(value);
                        if (matcher.matches()) {
                            String restriction = matcher.group(1).trim();
                            String condition = matcher.group(2).trim();
                            
                            validRestrictions.add(String.format("%s %d: %s=%s @ (%s)", 
                                primitive.getType().toString(), primitive.getId(), 
                                baseTag, restriction, condition));
                            
                            // Categorize condition types
                            if (timePattern.matcher(condition).find()) {
                                conditionTypes.put("time-based", conditionTypes.getOrDefault("time-based", 0) + 1);
                            } else if (condition.contains("weight")) {
                                conditionTypes.put("weight-based", conditionTypes.getOrDefault("weight-based", 0) + 1);
                            } else if (condition.contains("height")) {
                                conditionTypes.put("height-based", conditionTypes.getOrDefault("height-based", 0) + 1);
                            } else {
                                conditionTypes.put("other", conditionTypes.getOrDefault("other", 0) + 1);
                            }
                        } else {
                            parseErrors.add(String.format("%s %d: %s = %s", 
                                primitive.getType().toString(), primitive.getId(), key, value));
                        }
                    }
                }
            }
            
            // Build analysis results
            StringBuilder analysis = new StringBuilder();
            analysis.append("Conditional Restrictions Analysis\n");
            analysis.append("==================================\n\n");
            
            analysis.append("Tag Types Found:\n");
            for (Map.Entry<String, Integer> entry : tagTypes.entrySet()) {
                analysis.append(String.format("• %s: %d occurrences\n", entry.getKey(), entry.getValue()));
            }
            
            analysis.append("\nCondition Types:\n");
            for (Map.Entry<String, Integer> entry : conditionTypes.entrySet()) {
                analysis.append(String.format("• %s: %d occurrences\n", entry.getKey(), entry.getValue()));
            }
            
            analysis.append("\nValid Restrictions (first 10):\n");
            int count = 0;
            for (String restriction : validRestrictions) {
                if (count >= 10) {
                    analysis.append(String.format("... and %d more\n", validRestrictions.size() - 10));
                    break;
                }
                analysis.append("• ").append(restriction).append("\n");
                count++;
            }
            
            if (!parseErrors.isEmpty()) {
                analysis.append("\nParse Errors (first 5):\n");
                count = 0;
                for (String error : parseErrors) {
                    if (count >= 5) {
                        analysis.append(String.format("... and %d more\n", parseErrors.size() - 5));
                        break;
                    }
                    analysis.append("• ").append(error).append("\n");
                    count++;
                }
            }
            
            JTextArea textArea = new JTextArea(analysis.toString());
            textArea.setEditable(false);
            textArea.setRows(20);
            textArea.setColumns(70);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                scrollPane,
                "Conditional Restrictions Analysis",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error during analysis - " + e.getMessage());
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Error analyzing data: " + e.getMessage(),
                "Analysis Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void highlightConditionalRestrictions() {
        try {
            DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
            
            if (currentDataSet == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No data layer is active.\n\nPlease load some OSM data first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            // Find objects with conditional restrictions
            List<OsmPrimitive> conditionalObjects = new ArrayList<>();
            Map<String, Integer> highlightStats = new HashMap<>();
            
            for (OsmPrimitive primitive : currentDataSet.allPrimitives()) {
                boolean hasConditional = false;
                Map<String, String> tags = primitive.getKeys();
                
                for (String key : tags.keySet()) {
                    if (key.contains(":conditional")) {
                        hasConditional = true;
                        String baseTag = key.replace(":conditional", "");
                        highlightStats.put(baseTag, highlightStats.getOrDefault(baseTag, 0) + 1);
                    }
                }
                
                if (hasConditional) {
                    conditionalObjects.add(primitive);
                }
            }
            
            if (conditionalObjects.isEmpty()) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No conditional restrictions found to highlight.",
                    "Nothing to Highlight",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // Clear current selection and select all conditional objects
            currentDataSet.clearSelection();
            currentDataSet.setSelected(conditionalObjects);
            
            // Show what was highlighted
            StringBuilder message = new StringBuilder();
            message.append("SUCCESS! Highlighted ").append(conditionalObjects.size()).append(" objects with conditional restrictions!\n\n");
            message.append("Selected objects by type:\n");
            for (Map.Entry<String, Integer> entry : highlightStats.entrySet()) {
                message.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            message.append("\nThe selected objects are now highlighted on the map in magenta/purple.\n\n");
            message.append("Tips:\n");
            message.append("• Use the Selection panel (View → Selection) to see details\n");
            message.append("• Click elsewhere on map to clear selection\n");
            message.append("• Double-click selected object to see its tags");
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                message.toString(),
                "Objects Highlighted on Map",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error during highlighting - " + e.getMessage());
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Error highlighting objects: " + e.getMessage(),
                "Highlight Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private void checkActiveRestrictions() {
        try {
            DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
            
            if (currentDataSet == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No data layer is active.\n\nPlease load some OSM data first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek currentDay = now.getDayOfWeek();
            LocalTime currentTime = now.toLocalTime();
            
            List<String> activeRestrictions = new ArrayList<>();
            List<String> inactiveRestrictions = new ArrayList<>();
            List<String> unknownRestrictions = new ArrayList<>();
            
            Pattern conditionalPattern = Pattern.compile("^\\s*(.+?)\\s*@\\s*\\((.+?)\\)\\s*$");
            Pattern timeRangePattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
            Pattern dayRangePattern = Pattern.compile("\\b(Mo|Tu|We|Th|Fr|Sa|Su)(?:-(Mo|Tu|We|Th|Fr|Sa|Su))?\\b");
            
            for (OsmPrimitive primitive : currentDataSet.allPrimitives()) {
                Map<String, String> tags = primitive.getKeys();
                
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    String value = tag.getValue();
                    
                    if (key.contains(":conditional")) {
                        Matcher matcher = conditionalPattern.matcher(value);
                        if (matcher.matches()) {
                            String restriction = matcher.group(1).trim();
                            String condition = matcher.group(2).trim();
                            
                            boolean isActive = evaluateTimeCondition(condition, currentDay, currentTime);
                            String status = isActive ? "🔴 ACTIVE" : "⚪ INACTIVE";
                            
                            String restrictionInfo = String.format("%s %s %d: %s=%s @ (%s)", 
                                status, primitive.getType().toString(), primitive.getId(), 
                                key.replace(":conditional", ""), restriction, condition);
                            
                            if (isActive) {
                                activeRestrictions.add(restrictionInfo);
                            } else {
                                inactiveRestrictions.add(restrictionInfo);
                            }
                        } else {
                            unknownRestrictions.add(String.format("❓ %s %d: %s = %s", 
                                primitive.getType().toString(), primitive.getId(), key, value));
                        }
                    }
                }
            }
            
            // Build results
            StringBuilder results = new StringBuilder();
            results.append("Active Restrictions Check\n");
            results.append("========================\n");
            results.append("Current time: ").append(now.format(DateTimeFormatter.ofPattern("EEE MMM dd, HH:mm"))).append("\n\n");
            
            if (activeRestrictions.isEmpty() && inactiveRestrictions.isEmpty()) {
                results.append("No conditional restrictions found in current data.\n");
            } else {
                results.append("ACTIVE NOW (").append(activeRestrictions.size()).append("):\n");
                for (String restriction : activeRestrictions) {
                    results.append("").append(restriction).append("\n");
                }
                
                results.append("\nINACTIVE NOW (").append(inactiveRestrictions.size()).append("):\n");
                int count = 0;
                for (String restriction : inactiveRestrictions) {
                    if (count >= 10) {
                        results.append("... and ").append(inactiveRestrictions.size() - 10).append(" more inactive\n");
                        break;
                    }
                    results.append("").append(restriction).append("\n");
                    count++;
                }
            }
            
            if (!unknownRestrictions.isEmpty()) {
                results.append("\nUNKNOWN/UNPARSEABLE (").append(unknownRestrictions.size()).append("):\n");
                for (int i = 0; i < Math.min(5, unknownRestrictions.size()); i++) {
                    results.append("").append(unknownRestrictions.get(i)).append("\n");
                }
                if (unknownRestrictions.size() > 5) {
                    results.append("... and ").append(unknownRestrictions.size() - 5).append(" more\n");
                }
            }
            
            JTextArea textArea = new JTextArea(results.toString());
            textArea.setEditable(false);
            textArea.setRows(20);
            textArea.setColumns(80);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                scrollPane,
                "Active Restrictions Right Now",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error checking active restrictions - " + e.getMessage());
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Error checking restrictions: " + e.getMessage(),
                "Check Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private boolean evaluateTimeCondition(String condition, DayOfWeek currentDay, LocalTime currentTime) {
        try {
            // Simple time-based evaluation
            Pattern timeRangePattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
            Pattern dayRangePattern = Pattern.compile("\\b(Mo|Tu|We|Th|Fr|Sa|Su)(?:-(Mo|Tu|We|Th|Fr|Sa|Su))?\\b");
            
            boolean dayMatches = true; // Assume day matches unless we find day conditions
            boolean timeMatches = true; // Assume time matches unless we find time conditions
            
            // Check day conditions
            Matcher dayMatcher = dayRangePattern.matcher(condition);
            if (dayMatcher.find()) {
                dayMatches = false; // Now we need to explicitly match
                do {
                    String startDay = dayMatcher.group(1);
                    String endDay = dayMatcher.group(2);
                    
                    DayOfWeek start = parseDayOfWeek(startDay);
                    DayOfWeek end = endDay != null ? parseDayOfWeek(endDay) : start;
                    
                    if (isDayInRange(currentDay, start, end)) {
                        dayMatches = true;
                        break;
                    }
                } while (dayMatcher.find());
            }
            
            // Check time conditions
            Matcher timeMatcher = timeRangePattern.matcher(condition);
            if (timeMatcher.find()) {
                timeMatches = false; // Now we need to explicitly match
                do {
                    String startTimeStr = timeMatcher.group(1);
                    String endTimeStr = timeMatcher.group(2);
                    
                    LocalTime startTime = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("H:mm"));
                    LocalTime endTime = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("H:mm"));
                    
                    if (isTimeInRange(currentTime, startTime, endTime)) {
                        timeMatches = true;
                        break;
                    }
                } while (timeMatcher.find());
            }
            
            return dayMatches && timeMatches;
            
        } catch (Exception e) {
            // If we can't parse it, assume it's not active
            return false;
        }
    }
    
    private DayOfWeek parseDayOfWeek(String dayStr) {
        switch (dayStr) {
            case "Mo": return DayOfWeek.MONDAY;
            case "Tu": return DayOfWeek.TUESDAY;
            case "We": return DayOfWeek.WEDNESDAY;
            case "Th": return DayOfWeek.THURSDAY;
            case "Fr": return DayOfWeek.FRIDAY;
            case "Sa": return DayOfWeek.SATURDAY;
            case "Su": return DayOfWeek.SUNDAY;
            default: return DayOfWeek.MONDAY;
        }
    }
    
    private boolean isDayInRange(DayOfWeek current, DayOfWeek start, DayOfWeek end) {
        int currentValue = current.getValue();
        int startValue = start.getValue();
        int endValue = end.getValue();
        
        if (startValue <= endValue) {
            return currentValue >= startValue && currentValue <= endValue;
        } else {
            // Range crosses week boundary (e.g., Sa-Mo)
            return currentValue >= startValue || currentValue <= endValue;
        }
    }
    
    private boolean isTimeInRange(LocalTime current, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            // Normal range (e.g., 09:00-17:00)
            return !current.isBefore(start) && current.isBefore(end);
        } else {
            // Range crosses midnight (e.g., 22:00-06:00)
            return !current.isBefore(start) || current.isBefore(end);
        }
    }
    
    private void analyzeRoutingImpact() {
        try {
            DataSet currentDataSet = MainApplication.getLayerManager().getEditDataSet();
            
            if (currentDataSet == null) {
                JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    "No data layer is active.\n\nPlease load some OSM data first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek currentDay = now.getDayOfWeek();
            LocalTime currentTime = now.toLocalTime();
            
            // Analyze routing impact of conditional restrictions
            Map<String, Integer> impactStats = new HashMap<>();
            List<String> blockedRoutes = new ArrayList<>();
            List<String> timeRestrictedRoutes = new ArrayList<>();
            List<String> affectedWays = new ArrayList<>();
            
            Pattern conditionalPattern = Pattern.compile("^\\s*(.+?)\\s*@\\s*\\((.+?)\\)\\s*$");
            
            // First pass: identify all ways with conditional restrictions
            Map<Long, Way> restrictedWays = new HashMap<>();
            for (OsmPrimitive primitive : currentDataSet.allPrimitives()) {
                if (primitive instanceof Way) {
                    Way way = (Way) primitive;
                    Map<String, String> tags = way.getKeys();
                    
                    boolean hasConditionalRestriction = false;
                    for (String key : tags.keySet()) {
                        if (key.contains(":conditional")) {
                            hasConditionalRestriction = true;
                            break;
                        }
                    }
                    
                    if (hasConditionalRestriction) {
                        restrictedWays.put(way.getId(), way);
                    }
                }
            }
            
            // Second pass: analyze routing impact
            for (Way way : restrictedWays.values()) {
                Map<String, String> tags = way.getKeys();
                String wayType = getWayType(way);
                String wayName = tags.get("name");
                if (wayName == null) wayName = "unnamed " + wayType;
                
                boolean currentlyBlocked = false;
                boolean hasTimeRestrictions = false;
                
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    String key = tag.getKey();
                    String value = tag.getValue();
                    
                    if (key.contains(":conditional")) {
                        Matcher matcher = conditionalPattern.matcher(value);
                        if (matcher.matches()) {
                            String restriction = matcher.group(1).trim();
                            String condition = matcher.group(2).trim();
                            
                            // Check if restriction blocks access
                            if (isAccessBlockingRestriction(key, restriction)) {
                                boolean isActive = evaluateTimeCondition(condition, currentDay, currentTime);
                                if (isActive) {
                                    currentlyBlocked = true;
                                }
                                hasTimeRestrictions = true;
                            }
                            
                            // Count impact types
                            String impactType = getImpactType(key, restriction);
                            impactStats.put(impactType, impactStats.getOrDefault(impactType, 0) + 1);
                        }
                    }
                }
                
                String wayInfo = String.format("%s (ID: %d, Type: %s)", wayName, way.getId(), wayType);
                affectedWays.add(wayInfo);
                
                if (currentlyBlocked) {
                    blockedRoutes.add("🚫 " + wayInfo + " - BLOCKED NOW");
                }
                if (hasTimeRestrictions && !currentlyBlocked) {
                    timeRestrictedRoutes.add("⏰ " + wayInfo + " - Time-restricted");
                }
            }
            
            // Build routing analysis results
            StringBuilder analysis = new StringBuilder();
            analysis.append("Routing Impact Analysis\n");
            analysis.append("======================\n");
            analysis.append("Current time: ").append(now.format(DateTimeFormatter.ofPattern("EEE MMM dd, HH:mm"))).append("\n\n");
            
            analysis.append("ROUTING SUMMARY:\n");
            analysis.append("• Total ways with conditional restrictions: ").append(restrictedWays.size()).append("\n");
            analysis.append("• Currently blocked for routing: ").append(blockedRoutes.size()).append("\n");
            analysis.append("• Time-restricted routes: ").append(timeRestrictedRoutes.size()).append("\n\n");
            
            if (!impactStats.isEmpty()) {
                analysis.append("RESTRICTION TYPES:\n");
                for (Map.Entry<String, Integer> entry : impactStats.entrySet()) {
                    analysis.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                analysis.append("\n");
            }
            
            if (!blockedRoutes.isEmpty()) {
                analysis.append("CURRENTLY BLOCKED ROUTES:\n");
                for (String route : blockedRoutes) {
                    analysis.append(route).append("\n");
                }
                analysis.append("\n");
            }
            
            if (!timeRestrictedRoutes.isEmpty()) {
                analysis.append("TIME-RESTRICTED ROUTES (first 10):\n");
                for (int i = 0; i < Math.min(10, timeRestrictedRoutes.size()); i++) {
                    analysis.append(timeRestrictedRoutes.get(i)).append("\n");
                }
                if (timeRestrictedRoutes.size() > 10) {
                    analysis.append("... and ").append(timeRestrictedRoutes.size() - 10).append(" more\n");
                }
                analysis.append("\n");
            }
            
            analysis.append("ROUTING RECOMMENDATIONS:\n");
            analysis.append("• Routes marked 🚫 should be avoided for navigation\n");
            analysis.append("• Routes marked ⏰ may become blocked at certain times\n");
            analysis.append("• Consider alternative routes during restricted periods\n");
            analysis.append("• Emergency vehicles may have different restrictions\n");
            
            JTextArea textArea = new JTextArea(analysis.toString());
            textArea.setEditable(false);
            textArea.setRows(25);
            textArea.setColumns(80);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                scrollPane,
                "Routing Impact Analysis",
                JOptionPane.INFORMATION_MESSAGE
            );
            
        } catch (Exception e) {
            Logging.error("ConditionalRestrictions: Error during routing analysis - " + e.getMessage());
            JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                "Error analyzing routing impact: " + e.getMessage(),
                "Routing Analysis Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    private String getWayType(Way way) {
        Map<String, String> tags = way.getKeys();
        
        if (tags.containsKey("highway")) {
            return "highway (" + tags.get("highway") + ")";
        } else if (tags.containsKey("railway")) {
            return "railway (" + tags.get("railway") + ")";
        } else if (tags.containsKey("waterway")) {
            return "waterway (" + tags.get("waterway") + ")";
        } else if (tags.containsKey("cycleway")) {
            return "cycleway";
        } else if (tags.containsKey("footway")) {
            return "footway";
        } else {
            return "way";
        }
    }
    
    private boolean isAccessBlockingRestriction(String key, String restriction) {
        // Check if this restriction would block routing access
        String baseKey = key.replace(":conditional", "");
        
        // Access restrictions
        if (baseKey.equals("access") || baseKey.equals("motor_vehicle") || 
            baseKey.equals("vehicle") || baseKey.equals("foot") || 
            baseKey.equals("bicycle")) {
            return restriction.equals("no") || restriction.equals("private") || 
                   restriction.equals("destination");
        }
        
        // Turn restrictions
        if (baseKey.startsWith("turn:") || baseKey.contains("turn")) {
            return restriction.equals("no");
        }
        
        // Through traffic restrictions
        if (baseKey.equals("through_traffic")) {
            return restriction.equals("no");
        }
        
        return false;
    }
    
    private String getImpactType(String key, String restriction) {
        String baseKey = key.replace(":conditional", "");
        
        if (baseKey.equals("access") || baseKey.equals("motor_vehicle") || baseKey.equals("vehicle")) {
            return "Vehicle access restrictions";
        } else if (baseKey.equals("bicycle")) {
            return "Bicycle access restrictions";
        } else if (baseKey.equals("foot")) {
            return "Pedestrian access restrictions";
        } else if (baseKey.contains("turn")) {
            return "Turn restrictions";
        } else if (baseKey.equals("maxspeed")) {
            return "Speed limit changes";
        } else if (baseKey.equals("parking")) {
            return "Parking restrictions";
        } else if (baseKey.equals("through_traffic")) {
            return "Through traffic restrictions";
        } else {
            return "Other restrictions (" + baseKey + ")";
        }
    }
}