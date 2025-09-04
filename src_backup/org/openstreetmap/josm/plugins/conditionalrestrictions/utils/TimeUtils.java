package org.openstreetmap.josm.plugins.conditionalrestrictions.utils;

import org.openstreetmap.josm.plugins.conditionalrestrictions.validation.ConditionalValidator;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for time parsing, evaluation, and validation in conditional restrictions.
 * Provides comprehensive support for OSM conditional time syntax including day ranges,
 * time ranges, and complex time conditions.
 * 
 * @author CEF
 */
public class TimeUtils {
    
    // Date/time formatters for parsing
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };
    
    private static final DateTimeFormatter[] TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm:ss")
    };
    
    // Regex patterns for validation
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
        "(\\d{1,2}:\\d{2})(?::\\d{2})?\\s*-\\s*(\\d{1,2}:\\d{2})(?:\\d{2})?"
    );
    
    private static final Pattern DAY_PATTERN = Pattern.compile(
        "\\b(Mo|Tu|We|Th|Fr|Sa|Su)\\b"
    );
    
    private static final Pattern DAY_RANGE_PATTERN = Pattern.compile(
        "(Mo|Tu|We|Th|Fr|Sa|Su)\\s*-\\s*(Mo|Tu|We|Th|Fr|Sa|Su)"
    );
    
    private static final Pattern MONTH_PATTERN = Pattern.compile(
        "\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\b"
    );
    
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
        "(\\d{1,2})\\.(\\d{1,2})\\s*-\\s*(\\d{1,2})\\.(\\d{1,2})"
    );
    
    // Day of week mappings
    private static final Map<String, DayOfWeek> DAY_ABBREVIATIONS = new HashMap<String, DayOfWeek>() {{
        put("Mo", DayOfWeek.MONDAY); put("Tu", DayOfWeek.TUESDAY); put("We", DayOfWeek.WEDNESDAY);
        put("Th", DayOfWeek.THURSDAY); put("Fr", DayOfWeek.FRIDAY); put("Sa", DayOfWeek.SATURDAY);
        put("Su", DayOfWeek.SUNDAY);
    }};
    
    private static final Map<String, Month> MONTH_ABBREVIATIONS = new HashMap<String, Month>() {{
        put("Jan", Month.JANUARY); put("Feb", Month.FEBRUARY); put("Mar", Month.MARCH);
        put("Apr", Month.APRIL); put("May", Month.MAY); put("Jun", Month.JUNE);
        put("Jul", Month.JULY); put("Aug", Month.AUGUST); put("Sep", Month.SEPTEMBER);
        put("Oct", Month.OCTOBER); put("Nov", Month.NOVEMBER); put("Dec", Month.DECEMBER);
    }};
    
    /**
     * Represents a parsed time condition with all its components
     */
    public static class ParsedTimeCondition {
        private final Set<DayOfWeek> days;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Set<Month> months;
        private final boolean spans24Hours;
        private final boolean spansMultipleDays;
        
        public ParsedTimeCondition(Set<DayOfWeek> days, LocalTime startTime, LocalTime endTime, 
                                 Set<Month> months) {
            this.days = days != null ? EnumSet.copyOf(days) : EnumSet.noneOf(DayOfWeek.class);
            this.startTime = startTime;
            this.endTime = endTime;
            this.months = months != null ? EnumSet.copyOf(months) : EnumSet.noneOf(Month.class);
            
            // Calculate spans
            this.spans24Hours = startTime == null || endTime == null || 
                              (startTime.equals(LocalTime.MIDNIGHT) && endTime.equals(LocalTime.MIDNIGHT));
            this.spansMultipleDays = startTime != null && endTime != null && 
                                   startTime.isAfter(endTime);
        }
        
        // Getters
        public Set<DayOfWeek> getDays() { return EnumSet.copyOf(days); }
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public Set<Month> getMonths() { return EnumSet.copyOf(months); }
        public boolean isSpans24Hours() { return spans24Hours; }
        public boolean isSpansMultipleDays() { return spansMultipleDays; }
        
        /**
         * Checks if this condition is active at the given date/time
         */
        public boolean isActiveAt(LocalDateTime dateTime) {
            // Check month
            if (!months.isEmpty() && !months.contains(dateTime.getMonth())) {
                return false;
            }
            
            // Check day of week
            if (!days.isEmpty() && !days.contains(dateTime.getDayOfWeek())) {
                return false;
            }
            
            // Check time
            if (!spans24Hours && startTime != null && endTime != null) {
                LocalTime time = dateTime.toLocalTime();
                
                if (spansMultipleDays) {
                    // Time range crosses midnight (e.g., 22:00-06:00)
                    return !time.isBefore(startTime) || time.isBefore(endTime);
                } else {
                    // Normal time range (e.g., 07:00-19:00)
                    return !time.isBefore(startTime) && time.isBefore(endTime);
                }
            }
            
            return true;
        }
    }
    
    /**
     * Parses a date/time string using multiple format attempts
     * 
     * @param dateTimeStr The date/time string to parse
     * @return Parsed LocalDateTime
     * @throws DateTimeParseException if no format could parse the string
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw new DateTimeParseException("Empty date/time string", dateTimeStr, 0);
        }
        
        String normalized = dateTimeStr.trim();
        DateTimeParseException lastException = null;
        
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException e) {
                lastException = e;
            }
        }
        
        throw new DateTimeParseException("Unable to parse date/time: " + dateTimeStr, 
                                       dateTimeStr, 0, lastException);
    }
    
    /**
     * Parses a time string (without date)
     * 
     * @param timeStr The time string to parse
     * @return Parsed LocalTime
     * @throws DateTimeParseException if no format could parse the string
     */
    public static LocalTime parseTime(String timeStr) throws DateTimeParseException {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new DateTimeParseException("Empty time string", timeStr, 0);
        }
        
        String normalized = timeStr.trim();
        DateTimeParseException lastException = null;
        
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(normalized, formatter);
            } catch (DateTimeParseException e) {
                lastException = e;
            }
        }
        
        throw new DateTimeParseException("Unable to parse time: " + timeStr, 
                                       timeStr, 0, lastException);
    }
    
    /**
     * Parses a day-of-week abbreviation
     * 
     * @param dayStr The day abbreviation (Mo, Tu, We, etc.)
     * @return DayOfWeek enum value
     * @throws IllegalArgumentException if the abbreviation is not recognized
     */
    public static DayOfWeek parseDay(String dayStr) throws IllegalArgumentException {
        DayOfWeek day = DAY_ABBREVIATIONS.get(dayStr);
        if (day == null) {
            throw new IllegalArgumentException("Unknown day abbreviation: " + dayStr);
        }
        return day;
    }
    
    /**
     * Parses a month abbreviation
     * 
     * @param monthStr The month abbreviation (Jan, Feb, Mar, etc.)
     * @return Month enum value
     * @throws IllegalArgumentException if the abbreviation is not recognized
     */
    public static Month parseMonth(String monthStr) throws IllegalArgumentException {
        Month month = MONTH_ABBREVIATIONS.get(monthStr);
        if (month == null) {
            throw new IllegalArgumentException("Unknown month abbreviation: " + monthStr);
        }
        return month;
    }
    
    /**
     * Parses a complete time condition string
     * 
     * @param conditionStr The condition string (e.g., "Mo-Fr 07:00-19:00")
     * @return ParsedTimeCondition object
     * @throws IllegalArgumentException if the condition syntax is invalid
     */
    public static ParsedTimeCondition parseTimeCondition(String conditionStr) 
            throws IllegalArgumentException {
        if (conditionStr == null || conditionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty condition string");
        }
        
        String condition = conditionStr.trim();
        Set<DayOfWeek> days = new HashSet<>();
        Set<Month> months = new HashSet<>();
        LocalTime startTime = null;
        LocalTime endTime = null;
        
        // Parse day ranges
        Matcher dayRangeMatcher = DAY_RANGE_PATTERN.matcher(condition);
        while (dayRangeMatcher.find()) {
            String startDay = dayRangeMatcher.group(1);
            String endDay = dayRangeMatcher.group(2);
            
            DayOfWeek start = parseDay(startDay);
            DayOfWeek end = parseDay(endDay);
            
            // Add all days in range
            DayOfWeek current = start;
            while (true) {
                days.add(current);
                if (current == end) break;
                current = current.plus(1);
            }
        }
        
        // Parse individual days
        Matcher dayMatcher = DAY_PATTERN.matcher(condition);
        while (dayMatcher.find()) {
            String dayStr = dayMatcher.group(1);
            // Only add if not part of a range
            if (!dayRangeMatcher.find(dayMatcher.start())) {
                days.add(parseDay(dayStr));
            }
        }
        dayMatcher.reset(); // Reset for next use
        
        // Parse months
        Matcher monthMatcher = MONTH_PATTERN.matcher(condition);
        while (monthMatcher.find()) {
            String monthStr = monthMatcher.group(1);
            months.add(parseMonth(monthStr));
        }
        
        // Parse time ranges
        Matcher timeMatcher = TIME_RANGE_PATTERN.matcher(condition);
        if (timeMatcher.find()) {
            String startTimeStr = timeMatcher.group(1);
            String endTimeStr = timeMatcher.group(2);
            
            try {
                startTime = parseTime(startTimeStr);
                endTime = parseTime(endTimeStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid time format: " + e.getMessage());
            }
        }
        
        return new ParsedTimeCondition(days, startTime, endTime, months);
    }
    
    /**
     * Validates condition syntax without full parsing
     * 
     * @param condition The condition string to validate
     * @throws IllegalArgumentException if the syntax is invalid
     */
    public static void validateConditionSyntax(String condition) throws IllegalArgumentException {
        if (condition == null || condition.trim().isEmpty()) {
            return; // Empty conditions are valid
        }
        
        // Use the ConditionalValidator for validation
        ConditionalValidator.validateCondition("test @ (" + condition + ")");
    }
    
    /**
     * Calculates the next time a condition will become active
     * 
     * @param condition The time condition
     * @param fromTime Starting time for calculation
     * @return Next activation time, or null if never active
     */
    public static LocalDateTime getNextActivation(ParsedTimeCondition condition, LocalDateTime fromTime) {
        if (condition.isActiveAt(fromTime)) {
            return fromTime; // Already active
        }
        
        LocalDateTime searchTime = fromTime;
        
        // Search for next activation within a reasonable time window (1 month)
        LocalDateTime searchLimit = fromTime.plus(31, ChronoUnit.DAYS);
        
        while (searchTime.isBefore(searchLimit)) {
            if (condition.isActiveAt(searchTime)) {
                return searchTime;
            }
            searchTime = searchTime.plus(1, ChronoUnit.MINUTES);
        }
        
        return null; // No activation found within search window
    }
    
    /**
     * Calculates when a currently active condition will end
     * 
     * @param condition The time condition
     * @param fromTime Starting time for calculation
     * @return End time, or null if not currently active or never ends
     */
    public static LocalDateTime getActivationEnd(ParsedTimeCondition condition, LocalDateTime fromTime) {
        if (!condition.isActiveAt(fromTime)) {
            return null; // Not currently active
        }
        
        LocalDateTime searchTime = fromTime.plus(1, ChronoUnit.MINUTES);
        
        // Search for end within a reasonable time window (24 hours)
        LocalDateTime searchLimit = fromTime.plus(24, ChronoUnit.HOURS);
        
        while (searchTime.isBefore(searchLimit)) {
            if (!condition.isActiveAt(searchTime)) {
                return searchTime;
            }
            searchTime = searchTime.plus(1, ChronoUnit.MINUTES);
        }
        
        return null; // No end found within search window
    }
    
    /**
     * Checks if two time conditions overlap
     * 
     * @param condition1 First condition
     * @param condition2 Second condition
     * @return true if conditions can be active simultaneously
     */
    public static boolean conditionsOverlap(ParsedTimeCondition condition1, ParsedTimeCondition condition2) {
        // Check if days overlap
        Set<DayOfWeek> days1 = condition1.getDays();
        Set<DayOfWeek> days2 = condition2.getDays();
        
        if (!days1.isEmpty() && !days2.isEmpty()) {
            boolean dayOverlap = false;
            for (DayOfWeek day : days1) {
                if (days2.contains(day)) {
                    dayOverlap = true;
                    break;
                }
            }
            if (!dayOverlap) {
                return false;
            }
        }
        
        // Check if months overlap
        Set<Month> months1 = condition1.getMonths();
        Set<Month> months2 = condition2.getMonths();
        
        if (!months1.isEmpty() && !months2.isEmpty()) {
            boolean monthOverlap = false;
            for (Month month : months1) {
                if (months2.contains(month)) {
                    monthOverlap = true;
                    break;
                }
            }
            if (!monthOverlap) {
                return false;
            }
        }
        
        // Check if times overlap (simplified check)
        if (!condition1.isSpans24Hours() && !condition2.isSpans24Hours()) {
            LocalTime start1 = condition1.getStartTime();
            LocalTime end1 = condition1.getEndTime();
            LocalTime start2 = condition2.getStartTime();
            LocalTime end2 = condition2.getEndTime();
            
            if (start1 != null && end1 != null && start2 != null && end2 != null) {
                // Simplified overlap check - doesn't handle all edge cases
                return !(end1.isBefore(start2) || end2.isBefore(start1));
            }
        }
        
        return true; // Assume overlap if we can't determine otherwise
    }
    
    /**
     * Formats a duration in a human-readable way
     * 
     * @param duration The duration to format
     * @return Human-readable duration string
     */
    public static String formatDuration(Duration duration) {
        if (duration == null) {
            return "unknown";
        }
        
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        
        if (hours == 0) {
            return minutes + " min";
        } else if (minutes == 0) {
            return hours + " h";
        } else {
            return hours + "h " + minutes + "m";
        }
    }
    
    /**
     * Gets the current day of week as a two-letter abbreviation
     * 
     * @return Day abbreviation (Mo, Tu, We, etc.)
     */
    public static String getCurrentDayAbbreviation() {
        return getDayAbbreviation(LocalDateTime.now().getDayOfWeek());
    }
    
    /**
     * Converts a DayOfWeek to two-letter abbreviation
     * 
     * @param day The day of week
     * @return Day abbreviation
     */
    public static String getDayAbbreviation(DayOfWeek day) {
        for (Map.Entry<String, DayOfWeek> entry : DAY_ABBREVIATIONS.entrySet()) {
            if (entry.getValue() == day) {
                return entry.getKey();
            }
        }
        return day.toString().substring(0, 2);
    }
    
    /**
     * Converts a Month to three-letter abbreviation
     * 
     * @param month The month
     * @return Month abbreviation
     */
    public static String getMonthAbbreviation(Month month) {
        for (Map.Entry<String, Month> entry : MONTH_ABBREVIATIONS.entrySet()) {
            if (entry.getValue() == month) {
                return entry.getKey();
            }
        }
        return month.toString().substring(0, 3);
    }
    
    /**
     * Checks if a given time is within business hours (Mo-Fr 09:00-17:00)
     * 
     * @param dateTime The time to check
     * @return true if within business hours
     */
    public static boolean isBusinessHours(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(17, 0));
    }
    
    /**
     * Creates a time condition for common scenarios
     * 
     * @param scenario The scenario type
     * @return ParsedTimeCondition for the scenario
     */
    public static ParsedTimeCondition createCommonCondition(String scenario) {
        switch (scenario.toLowerCase()) {
            case "business_hours":
                return new ParsedTimeCondition(
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                              DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                    LocalTime.of(9, 0), LocalTime.of(17, 0), EnumSet.noneOf(Month.class)
                );
            
            case "weekends":
                return new ParsedTimeCondition(
                    EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                    null, null, EnumSet.noneOf(Month.class)
                );
            
            case "night_hours":
                return new ParsedTimeCondition(
                    EnumSet.noneOf(DayOfWeek.class),
                    LocalTime.of(22, 0), LocalTime.of(6, 0), EnumSet.noneOf(Month.class)
                );
            
            default:
                throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
    }
}