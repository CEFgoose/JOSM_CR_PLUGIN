package org.openstreetmap.josm.plugins.conditionalrestrictions.validation;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.plugins.conditionalrestrictions.ConditionalRestrictionsPlugin;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ParseException;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ValidationResult;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.*;
import java.util.regex.Pattern;

/**
 * JOSM validator integration for conditional restrictions. Validates conditional
 * restriction syntax according to OSM conditional restrictions specification
 * and provides suggestions for common syntax errors.
 * 
 * This validator integrates with JOSM's validation framework to provide
 * real-time feedback on conditional restriction tags in the map data.
 * 
 * @author CEF
 */
public class ConditionalValidator extends Test {
    
    // Error codes for different types of validation issues
    private static final int ERROR_INVALID_SYNTAX = 4001;
    private static final int ERROR_MALFORMED_CONDITION = 4002;
    private static final int ERROR_INCONSISTENT_VALUES = 4003;
    private static final int ERROR_DEPRECATED_SYNTAX = 4004;
    private static final int WARNING_UNUSUAL_CONDITION = 4005;
    private static final int WARNING_OVERLAPPING_CONDITIONS = 4006;
    
    // Common conditional tag patterns
    private static final Pattern CONDITIONAL_TAG_PATTERN = Pattern.compile(".*:conditional$");
    
    // Parser for validation
    private final ConditionalRestrictionParser parser;
    
    // Common mistakes and suggestions
    private static final Map<String, String> COMMON_FIXES = new HashMap<>();
    static {
        // Time format fixes
        COMMON_FIXES.put("Mo-Fr 7:00-19:00", "Mo-Fr 07:00-19:00");
        COMMON_FIXES.put("monday-friday", "Mo-Fr");
        COMMON_FIXES.put("Mon-Fri", "Mo-Fr");
        
        // Operator fixes
        COMMON_FIXES.put("weight>3.5", "weight > 3.5");
        COMMON_FIXES.put("maxweight>3.5", "weight > 3.5");
        COMMON_FIXES.put("height>2.5", "height > 2.5");
        
        // Common value corrections
        COMMON_FIXES.put("none", "no");
        COMMON_FIXES.put("private", "no");
        COMMON_FIXES.put("permit", "destination");
        
        // Bracket corrections
        COMMON_FIXES.put("Mo-Fr 07:00-19:00", "(Mo-Fr 07:00-19:00)");
    }
    
    // Known valid access values
    private static final Set<String> VALID_ACCESS_VALUES = Set.of(
        "yes", "no", "private", "permissive", "destination", "customers", 
        "delivery", "agricultural", "forestry", "emergency"
    );
    
    // Known valid oneway values
    private static final Set<String> VALID_ONEWAY_VALUES = Set.of(
        "yes", "no", "-1", "1", "true", "false"
    );
    
    /**
     * Creates a new conditional validator
     */
    public ConditionalValidator() {
        super(tr("Conditional Restrictions"),
              tr("Validates conditional restriction syntax and semantics"));
        this.parser = new ConditionalRestrictionParser();
    }
    
    @Override
    public void visit(Way way) {
        if (way.isDeleted() || way.isIncomplete()) {
            return;
        }
        
        // Check all conditional tags
        for (String key : way.keySet()) {
            if (CONDITIONAL_TAG_PATTERN.matcher(key).matches()) {
                String value = way.get(key);
                if (value != null && !value.trim().isEmpty()) {
                    validateConditionalTag(way, key, value);
                }
            }
        }
        
        // Check for conflicting conditional restrictions
        checkForConflictingRestrictions(way);
    }
    
    /**
     * Validates a single conditional tag
     * 
     * @param way The way containing the tag
     * @param key The tag key
     * @param value The tag value
     */
    private void validateConditionalTag(Way way, String key, String value) {
        try {
            // Basic syntax validation
            ValidationResult result = parser.validate(key, value);
            if (!result.isValid()) {
                String suggestion = suggestFix(value);
                
                // Build detailed description
                String description = tr("Invalid syntax in {0}: {1}", key, result.getMessage());
                if (suggestion != null) {
                    description += "\n" + tr("Suggestion: {0}", suggestion);
                }
                
                TestError error = TestError.builder(this, Severity.ERROR, ERROR_INVALID_SYNTAX)
                    .message(description)
                    .primitives(way)
                    .build();
                
                errors.add(error);
                return;
            }
            
            // Parse for semantic validation
            ConditionalRestriction restriction = parser.parse(key, value);
            validateSemantics(way, key, restriction);
            
        } catch (ParseException e) {
            // Create error with fix suggestion
            String suggestion = suggestFix(value);
            
            String description = tr("Parse error in {0}: {1}", key, e.getMessage());
            if (suggestion != null) {
                description += "\n" + tr("Suggestion: {0}", suggestion);
            }
            
            TestError error = TestError.builder(this, Severity.ERROR, ERROR_MALFORMED_CONDITION)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
    }
    
    /**
     * Validates the semantics of a parsed conditional restriction
     * 
     * @param way The way containing the restriction
     * @param key The tag key
     * @param restriction The parsed restriction
     */
    private void validateSemantics(Way way, String key, ConditionalRestriction restriction) {
        String baseTag = restriction.getBaseTag();
        String value = restriction.getRestrictionValue();
        
        // Validate access values
        if (isAccessTag(baseTag) && !VALID_ACCESS_VALUES.contains(value.toLowerCase())) {
            
            String description = tr("Unusual access value ''{0}'' in {1}. " +
                "Common values are: {2}", value, key, String.join(", ", VALID_ACCESS_VALUES));
            
            TestError error = TestError.builder(this, Severity.WARNING, WARNING_UNUSUAL_CONDITION)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
        
        // Validate oneway values
        if ("oneway".equals(baseTag) && !VALID_ONEWAY_VALUES.contains(value.toLowerCase())) {
            
            String description = tr("Invalid oneway value ''{0}'' in {1}. " +
                "Valid values are: {2}", value, key, String.join(", ", VALID_ONEWAY_VALUES));
            
            TestError error = TestError.builder(this, Severity.ERROR, ERROR_INCONSISTENT_VALUES)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
        
        // Validate speed values
        if ("maxspeed".equals(baseTag)) {
            validateSpeedValue(way, key, value);
        }
        
        // Check for deprecated syntax patterns
        checkDeprecatedSyntax(way, key, restriction);
        
        // Validate time conditions
        validateTimeConditions(way, key, restriction);
    }
    
    /**
     * Validates speed values in maxspeed:conditional tags
     */
    private void validateSpeedValue(Way way, String key, String value) {
        // Check for numeric speed values
        if (value.matches("\\d+")) {
            int speed = Integer.parseInt(value);
            if (speed > 200) {
                String description = tr("Speed limit of {0} km/h in {1} seems unusually high", 
                    speed, key);
                
                TestError error = TestError.builder(this, Severity.WARNING, WARNING_UNUSUAL_CONDITION)
                    .message(description)
                    .primitives(way)
                    .build();
                
                errors.add(error);
            }
        }
        
        // Check for common speed limit values with units
        if (value.matches("\\d+\\s*mph")) {
            String description = tr("Speed value ''{0}'' in {1} uses mph. " +
                "OSM typically uses km/h for maxspeed", value, key);
            
            TestError error = TestError.builder(this, Severity.WARNING, WARNING_UNUSUAL_CONDITION)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
    }
    
    /**
     * Checks for deprecated syntax patterns
     */
    private void checkDeprecatedSyntax(Way way, String key, ConditionalRestriction restriction) {
        // Check for old-style conditional syntax without @ symbol
        String originalValue = way.get(key);
        if (originalValue != null && !originalValue.contains("@")) {
            
            String description = tr("Tag {0} uses deprecated syntax. " +
                "Modern conditional restrictions should use the format: value @ (condition)", key);
            
            TestError error = TestError.builder(this, Severity.ERROR, ERROR_DEPRECATED_SYNTAX)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
    }
    
    /**
     * Validates time conditions for logical consistency
     */
    private void validateTimeConditions(Way way, String key, ConditionalRestriction restriction) {
        // Check for overlapping time ranges
        if (restriction.getTimeConditions().size() > 1) {
            // This is a simplified check - a full implementation would need
            // more sophisticated overlap detection
            
            String description = tr("Tag {0} has multiple time conditions. " +
                "Please verify they don't overlap unexpectedly", key);
            
            TestError error = TestError.builder(this, Severity.WARNING, WARNING_OVERLAPPING_CONDITIONS)
                .message(description)
                .primitives(way)
                .build();
            
            errors.add(error);
        }
    }
    
    /**
     * Checks for conflicting conditional restrictions on the same way
     */
    private void checkForConflictingRestrictions(Way way) {
        Map<String, List<String>> conditionalTags = new HashMap<>();
        
        // Collect all conditional tags by base type
        for (String key : way.keySet()) {
            if (CONDITIONAL_TAG_PATTERN.matcher(key).matches()) {
                String baseTag = key.replaceFirst(":conditional$", "");
                conditionalTags.computeIfAbsent(baseTag, k -> new ArrayList<>()).add(key);
            }
        }
        
        // Check for conflicts between conditional and non-conditional tags
        for (String baseTag : conditionalTags.keySet()) {
            if (way.hasKey(baseTag)) {
                String unconditionalValue = way.get(baseTag);
                List<String> conditionalKeys = conditionalTags.get(baseTag);
                
                // This could indicate a logical conflict
                String description = tr("Way has both {0}={1} and conditional tags {2}. " +
                    "Please verify this combination is intended", 
                    baseTag, unconditionalValue, String.join(", ", conditionalKeys));
                
                TestError error = TestError.builder(this, Severity.WARNING, WARNING_UNUSUAL_CONDITION)
                    .message(description)
                    .primitives(way)
                    .build();
                
                errors.add(error);
            }
        }
    }
    
    /**
     * Suggests a fix for common syntax errors
     * 
     * @param value The original tag value
     * @return Suggested correction, or null if no suggestion available
     */
    private String suggestFix(String value) {
        // Direct lookup for known fixes
        for (Map.Entry<String, String> fix : COMMON_FIXES.entrySet()) {
            if (value.contains(fix.getKey())) {
                return value.replace(fix.getKey(), fix.getValue());
            }
        }
        
        // Pattern-based fixes
        
        // Missing parentheses around condition
        if (value.contains("@") && !value.contains("(")) {
            String[] parts = value.split("@", 2);
            if (parts.length == 2) {
                return parts[0].trim() + " @ (" + parts[1].trim() + ")";
            }
        }
        
        // Extra spaces
        if (value.contains("  ")) {
            return value.replaceAll("\\s+", " ").trim();
        }
        
        // Wrong bracket types
        if (value.contains("[") || value.contains("{")) {
            return value.replace("[", "(").replace("]", ")")
                        .replace("{", "(").replace("}", ")");
        }
        
        return null;
    }
    
    /**
     * Checks if a tag represents an access restriction
     */
    private boolean isAccessTag(String baseTag) {
        Set<String> accessTags = Set.of(
            "access", "motor_vehicle", "vehicle", "bicycle", "foot", 
            "horse", "hgv", "bus", "taxi", "emergency", "delivery"
        );
        return accessTags.contains(baseTag.toLowerCase());
    }
    
    @Override
    public boolean isFixable(TestError testError) {
        // Only syntax errors can be automatically fixed
        return testError.getCode() == ERROR_INVALID_SYNTAX || 
               testError.getCode() == ERROR_MALFORMED_CONDITION;
    }
    
    @Override
    public Command fixError(TestError testError) {
        // This would implement automatic fixing of common syntax errors
        // For now, return null to indicate manual fixing is required
        return null;
    }
    
    /**
     * Gets suggestions for fixing validation errors
     * 
     * @param way The way with the error
     * @param key The problematic tag key
     * @param value The problematic tag value
     * @return List of suggested fixes
     */
    public List<String> getSuggestions(Way way, String key, String value) {
        List<String> suggestions = new ArrayList<>();
        
        String suggestion = suggestFix(value);
        if (suggestion != null) {
            suggestions.add(suggestion);
        }
        
        // Add context-specific suggestions
        String baseTag = key.replaceFirst(":conditional$", "");
        
        if (isAccessTag(baseTag)) {
            suggestions.add("Common access values: " + String.join(", ", VALID_ACCESS_VALUES));
        }
        
        if ("oneway".equals(baseTag)) {
            suggestions.add("Valid oneway values: " + String.join(", ", VALID_ONEWAY_VALUES));
        }
        
        if ("maxspeed".equals(baseTag)) {
            suggestions.add("Example: 30 @ (Mo-Fr 07:00-19:00)");
            suggestions.add("Remember to use km/h for speed values");
        }
        
        return suggestions;
    }
    
    /**
     * Static method to validate a conditional restriction string
     * Used by UI components for real-time validation
     * 
     * @param condition The condition string to validate
     * @throws IllegalArgumentException if the condition is invalid
     */
    public static void validateCondition(String condition) throws IllegalArgumentException {
        if (condition == null || condition.trim().isEmpty()) {
            return; // Empty conditions are valid
        }
        
        // Basic pattern checks
        if (!condition.contains("@")) {
            throw new IllegalArgumentException("Conditional restrictions must contain '@' symbol");
        }
        
        String[] parts = condition.split("@", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid conditional restriction format");
        }
        
        String conditionPart = parts[1].trim();
        if (!conditionPart.startsWith("(") || !conditionPart.endsWith(")")) {
            throw new IllegalArgumentException("Condition must be enclosed in parentheses");
        }
        
        // More detailed validation would be performed by the parser
        ConditionalRestrictionParser parser = new ConditionalRestrictionParser();
        ValidationResult result = parser.validate("test:conditional", condition);
        
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getMessage());
        }
    }
}