package org.openstreetmap.josm.plugins.conditionalrestrictions.parser;

import org.openstreetmap.josm.plugins.conditionalrestrictions.data.TimeCondition;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.tools.Logging;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for OpenStreetMap conditional restriction syntax.
 * 
 * Supports parsing of conditional tags in the format:
 * tag:conditional = value @ (condition)
 * 
 * Example: access:conditional = no @ (Mo-Fr 07:00-19:00)
 * 
 * @author CEF
 */
public class ConditionalRestrictionParser {
    
    // Regex patterns for parsing
    private static final Pattern MAIN_PATTERN = Pattern.compile(
        "^\\s*([^@]+)\\s*@\\s*\\(([^)]+)\\)\\s*$"
    );
    
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})"
    );
    
    private static final Pattern DAY_RANGE_PATTERN = Pattern.compile(
        "(Mo|Tu|We|Th|Fr|Sa|Su)(?:-(Mo|Tu|We|Th|Fr|Sa|Su))?"
    );
    
    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
        "weight\\s*([<>]=?)\\s*([\\d.]+)"
    );
    
    private static final Pattern HEIGHT_PATTERN = Pattern.compile(
        "height\\s*([<>]=?)\\s*([\\d.]+)"
    );
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    
    /**
     * Parses a conditional restriction tag value
     * 
     * @param tagKey The tag key (e.g., "access:conditional")
     * @param tagValue The tag value (e.g., "no @ (Mo-Fr 07:00-19:00)")
     * @return Parsed ConditionalRestriction or null if parsing fails
     * @throws ParseException if the syntax is invalid
     */
    public ConditionalRestriction parse(String tagKey, String tagValue) throws ParseException {
        if (tagKey == null || tagValue == null || tagValue.trim().isEmpty()) {
            throw new ParseException("Tag key and value must not be empty");
        }
        
        Matcher mainMatcher = MAIN_PATTERN.matcher(tagValue);
        if (!mainMatcher.matches()) {
            throw new ParseException("Invalid conditional restriction syntax: " + tagValue);
        }
        
        String restrictionValue = mainMatcher.group(1).trim();
        String conditionString = mainMatcher.group(2).trim();
        
        ConditionalRestriction restriction = new ConditionalRestriction(tagKey, restrictionValue);
        
        // Parse conditions (can be multiple, separated by semicolons)
        String[] conditions = conditionString.split(";");
        
        for (String condition : conditions) {
            condition = condition.trim();
            
            if (isTimeCondition(condition)) {
                TimeCondition timeCondition = parseTimeCondition(condition);
                restriction.addTimeCondition(timeCondition);
            } else if (isWeightCondition(condition)) {
                parseWeightCondition(condition, restriction);
            } else if (isHeightCondition(condition)) {
                parseHeightCondition(condition, restriction);
            } else {
                // Generic condition
                restriction.addGenericCondition(condition);
            }
        }
        
        return restriction;
    }
    
    /**
     * Validates the syntax of a conditional restriction without fully parsing it
     * 
     * @param tagKey The tag key
     * @param tagValue The tag value
     * @return ValidationResult indicating if the syntax is valid
     */
    public ValidationResult validate(String tagKey, String tagValue) {
        try {
            parse(tagKey, tagValue);
            return new ValidationResult(true, "Valid syntax");
        } catch (ParseException e) {
            return new ValidationResult(false, e.getMessage());
        }
    }
    
    /**
     * Checks if a condition string represents a time condition
     */
    private boolean isTimeCondition(String condition) {
        return TIME_RANGE_PATTERN.matcher(condition).find() || 
               DAY_RANGE_PATTERN.matcher(condition).find();
    }
    
    /**
     * Checks if a condition string represents a weight condition
     */
    private boolean isWeightCondition(String condition) {
        return WEIGHT_PATTERN.matcher(condition).matches();
    }
    
    /**
     * Checks if a condition string represents a height condition
     */
    private boolean isHeightCondition(String condition) {
        return HEIGHT_PATTERN.matcher(condition).matches();
    }
    
    /**
     * Parses a time-based condition
     */
    private TimeCondition parseTimeCondition(String condition) throws ParseException {
        TimeCondition timeCondition = new TimeCondition();
        
        // Parse day ranges
        Matcher dayMatcher = DAY_RANGE_PATTERN.matcher(condition);
        while (dayMatcher.find()) {
            String startDay = dayMatcher.group(1);
            String endDay = dayMatcher.group(2);
            
            DayOfWeek start = parseDayOfWeek(startDay);
            DayOfWeek end = endDay != null ? parseDayOfWeek(endDay) : start;
            
            timeCondition.addDayRange(start, end);
        }
        
        // Parse time ranges
        Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(condition);
        if (timeMatcher.find()) {
            String startTime = timeMatcher.group(1);
            String endTime = timeMatcher.group(2);
            
            try {
                LocalTime start = LocalTime.parse(startTime, TIME_FORMATTER);
                LocalTime end = LocalTime.parse(endTime, TIME_FORMATTER);
                timeCondition.setTimeRange(start, end);
            } catch (DateTimeParseException e) {
                throw new ParseException("Invalid time format: " + e.getMessage());
            }
        }
        
        return timeCondition;
    }
    
    /**
     * Parses a weight condition
     */
    private void parseWeightCondition(String condition, ConditionalRestriction restriction) 
            throws ParseException {
        Matcher matcher = WEIGHT_PATTERN.matcher(condition);
        if (matcher.matches()) {
            String operator = matcher.group(1);
            String value = matcher.group(2);
            
            try {
                double weight = Double.parseDouble(value);
                restriction.setWeightCondition(operator, weight);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid weight value: " + value);
            }
        }
    }
    
    /**
     * Parses a height condition
     */
    private void parseHeightCondition(String condition, ConditionalRestriction restriction) 
            throws ParseException {
        Matcher matcher = HEIGHT_PATTERN.matcher(condition);
        if (matcher.matches()) {
            String operator = matcher.group(1);
            String value = matcher.group(2);
            
            try {
                double height = Double.parseDouble(value);
                restriction.setHeightCondition(operator, height);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid height value: " + value);
            }
        }
    }
    
    /**
     * Converts day abbreviation to DayOfWeek enum
     */
    private DayOfWeek parseDayOfWeek(String day) throws ParseException {
        switch (day) {
            case "Mo": return DayOfWeek.MONDAY;
            case "Tu": return DayOfWeek.TUESDAY;
            case "We": return DayOfWeek.WEDNESDAY;
            case "Th": return DayOfWeek.THURSDAY;
            case "Fr": return DayOfWeek.FRIDAY;
            case "Sa": return DayOfWeek.SATURDAY;
            case "Su": return DayOfWeek.SUNDAY;
            default: throw new ParseException("Invalid day abbreviation: " + day);
        }
    }
    
    /**
     * Custom exception for parsing errors
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}