package org.openstreetmap.josm.plugins.conditionalrestrictions.data;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a time-based condition for a conditional restriction.
 * 
 * Handles day-of-week and time-of-day conditions such as:
 * - Mo-Fr 07:00-19:00
 * - Sa 08:00-14:00
 * - Su 10:00-16:00
 * 
 * @author CEF
 */
public class TimeCondition {
    
    private Set<DayOfWeek> days;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean allDays;
    private boolean allHours;
    
    /**
     * Creates a new time condition
     */
    public TimeCondition() {
        this.days = EnumSet.noneOf(DayOfWeek.class);
        this.allDays = false;
        this.allHours = true;
    }
    
    /**
     * Adds a range of days to this condition
     * 
     * @param start Start day (inclusive)
     * @param end End day (inclusive)
     */
    public void addDayRange(DayOfWeek start, DayOfWeek end) {
        DayOfWeek current = start;
        while (true) {
            days.add(current);
            if (current == end) {
                break;
            }
            current = current.plus(1);
        }
        allDays = false;
    }
    
    /**
     * Adds a single day to this condition
     * 
     * @param day Day to add
     */
    public void addDay(DayOfWeek day) {
        days.add(day);
        allDays = false;
    }
    
    /**
     * Sets the time range for this condition
     * 
     * @param startTime Start time (inclusive)
     * @param endTime End time (exclusive)
     */
    public void setTimeRange(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.allHours = false;
    }
    
    /**
     * Checks if this condition is active at the given date/time
     * 
     * @param dateTime The date/time to check
     * @return true if the condition is active
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        // Check day of week
        if (!allDays && !days.contains(dateTime.getDayOfWeek())) {
            return false;
        }
        
        // Check time of day
        if (!allHours) {
            LocalTime time = dateTime.toLocalTime();
            
            if (startTime.isBefore(endTime)) {
                // Normal case: e.g., 07:00-19:00
                return !time.isBefore(startTime) && time.isBefore(endTime);
            } else {
                // Crosses midnight: e.g., 22:00-06:00
                return !time.isBefore(startTime) || time.isBefore(endTime);
            }
        }
        
        return true;
    }
    
    /**
     * Gets a human-readable description of this condition
     * 
     * @return Description string
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        
        // Days
        if (!allDays && !days.isEmpty()) {
            sb.append(formatDays());
        }
        
        // Times
        if (!allHours && startTime != null && endTime != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(startTime.toString()).append("-").append(endTime.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Formats the days of week into a readable string
     */
    private String formatDays() {
        if (days.size() == 7) {
            return "Every day";
        }
        
        // Check for weekdays
        Set<DayOfWeek> weekdays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        if (days.equals(weekdays)) {
            return "Mo-Fr";
        }
        
        // Check for weekend
        Set<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        if (days.equals(weekend)) {
            return "Sa-Su";
        }
        
        // Build custom string
        List<String> dayStrings = new ArrayList<>();
        for (DayOfWeek day : days) {
            dayStrings.add(dayAbbreviation(day));
        }
        
        return String.join(",", dayStrings);
    }
    
    /**
     * Gets the two-letter abbreviation for a day
     */
    private String dayAbbreviation(DayOfWeek day) {
        switch (day) {
            case MONDAY: return "Mo";
            case TUESDAY: return "Tu";
            case WEDNESDAY: return "We";
            case THURSDAY: return "Th";
            case FRIDAY: return "Fr";
            case SATURDAY: return "Sa";
            case SUNDAY: return "Su";
            default: return day.toString();
        }
    }
    
    // Getters
    public Set<DayOfWeek> getDays() { return EnumSet.copyOf(days); }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isAllDays() { return allDays; }
    public boolean isAllHours() { return allHours; }
    
    @Override
    public String toString() {
        return "TimeCondition[" + getDescription() + "]";
    }
}