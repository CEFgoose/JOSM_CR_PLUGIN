package org.openstreetmap.josm.plugins.conditionalrestrictions.test;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.openstreetmap.josm.plugins.conditionalrestrictions.utils.TimeUtils;
import org.openstreetmap.josm.plugins.conditionalrestrictions.utils.TimeUtils.ParsedTimeCondition;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.EnumSet;

/**
 * Comprehensive unit tests for TimeUtils functionality.
 * Tests time parsing, evaluation, edge cases, and complex time conditions
 * with thorough coverage of the OSM conditional syntax.
 * 
 * @author CEF
 */
public class TimeUtilsTest {
    
    private LocalDateTime mondayMorning;
    private LocalDateTime mondayEvening;
    private LocalDateTime saturdayAfternoon;
    private LocalDateTime sundayNight;
    private LocalDateTime fridayLateNight;
    private LocalDateTime tuesdayEarlyMorning;
    
    @Before
    public void setUp() {
        // Set up various test times (October 2023)
        mondayMorning = LocalDateTime.of(2023, 10, 16, 8, 0);     // Monday 08:00
        mondayEvening = LocalDateTime.of(2023, 10, 16, 20, 0);    // Monday 20:00
        saturdayAfternoon = LocalDateTime.of(2023, 10, 14, 14, 0); // Saturday 14:00
        sundayNight = LocalDateTime.of(2023, 10, 15, 23, 30);     // Sunday 23:30
        fridayLateNight = LocalDateTime.of(2023, 10, 13, 23, 0);  // Friday 23:00
        tuesdayEarlyMorning = LocalDateTime.of(2023, 10, 17, 5, 0); // Tuesday 05:00
    }
    
    @Test
    public void testParseDateTime() {
        // Test various date/time formats
        LocalDateTime expected = LocalDateTime.of(2023, 10, 16, 14, 30);
        
        assertEquals(expected, TimeUtils.parseDateTime("2023-10-16 14:30"));
        assertEquals(expected, TimeUtils.parseDateTime("16.10.2023 14:30"));
        assertEquals(expected, TimeUtils.parseDateTime("16/10/2023 14:30"));
        assertEquals(expected, TimeUtils.parseDateTime("10/16/2023 14:30"));
        
        // Test with seconds
        LocalDateTime expectedWithSeconds = LocalDateTime.of(2023, 10, 16, 14, 30, 45);
        assertEquals(expectedWithSeconds, TimeUtils.parseDateTime("2023-10-16 14:30:45"));
    }
    
    @Test(expected = DateTimeParseException.class)
    public void testParseDateTimeInvalid() {
        TimeUtils.parseDateTime("invalid date");
    }
    
    @Test(expected = DateTimeParseException.class)
    public void testParseDateTimeEmpty() {
        TimeUtils.parseDateTime("");
    }
    
    @Test(expected = DateTimeParseException.class)
    public void testParseDateTimeNull() {
        TimeUtils.parseDateTime(null);
    }
    
    @Test
    public void testParseTime() {
        // Test various time formats
        LocalTime expected = LocalTime.of(14, 30);
        
        assertEquals(expected, TimeUtils.parseTime("14:30"));
        assertEquals(expected, TimeUtils.parseTime("14:30:00"));
        
        // Test single digit hours
        LocalTime expectedSingleDigit = LocalTime.of(8, 15);
        assertEquals(expectedSingleDigit, TimeUtils.parseTime("8:15"));
        assertEquals(expectedSingleDigit, TimeUtils.parseTime("8:15:00"));
    }
    
    @Test(expected = DateTimeParseException.class)
    public void testParseTimeInvalid() {
        TimeUtils.parseTime("25:00");
    }
    
    @Test(expected = DateTimeParseException.class)
    public void testParseTimeEmpty() {
        TimeUtils.parseTime("");
    }
    
    @Test
    public void testParseDay() {
        assertEquals(DayOfWeek.MONDAY, TimeUtils.parseDay("Mo"));
        assertEquals(DayOfWeek.TUESDAY, TimeUtils.parseDay("Tu"));
        assertEquals(DayOfWeek.WEDNESDAY, TimeUtils.parseDay("We"));
        assertEquals(DayOfWeek.THURSDAY, TimeUtils.parseDay("Th"));
        assertEquals(DayOfWeek.FRIDAY, TimeUtils.parseDay("Fr"));
        assertEquals(DayOfWeek.SATURDAY, TimeUtils.parseDay("Sa"));
        assertEquals(DayOfWeek.SUNDAY, TimeUtils.parseDay("Su"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseDayInvalid() {
        TimeUtils.parseDay("Xx");
    }
    
    @Test
    public void testParseMonth() {
        assertEquals(Month.JANUARY, TimeUtils.parseMonth("Jan"));
        assertEquals(Month.FEBRUARY, TimeUtils.parseMonth("Feb"));
        assertEquals(Month.MARCH, TimeUtils.parseMonth("Mar"));
        assertEquals(Month.APRIL, TimeUtils.parseMonth("Apr"));
        assertEquals(Month.MAY, TimeUtils.parseMonth("May"));
        assertEquals(Month.JUNE, TimeUtils.parseMonth("Jun"));
        assertEquals(Month.JULY, TimeUtils.parseMonth("Jul"));
        assertEquals(Month.AUGUST, TimeUtils.parseMonth("Aug"));
        assertEquals(Month.SEPTEMBER, TimeUtils.parseMonth("Sep"));
        assertEquals(Month.OCTOBER, TimeUtils.parseMonth("Oct"));
        assertEquals(Month.NOVEMBER, TimeUtils.parseMonth("Nov"));
        assertEquals(Month.DECEMBER, TimeUtils.parseMonth("Dec"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseMonthInvalid() {
        TimeUtils.parseMonth("Xxx");
    }
    
    @Test
    public void testParseTimeConditionBasic() {
        // Test basic day range with time
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00");
        
        Set<DayOfWeek> expectedDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        assertEquals(expectedDays, condition.getDays());
        assertEquals(LocalTime.of(7, 0), condition.getStartTime());
        assertEquals(LocalTime.of(19, 0), condition.getEndTime());
        assertFalse(condition.isSpans24Hours());
        assertFalse(condition.isSpansMultipleDays());
    }
    
    @Test
    public void testParseTimeConditionWeekend() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Sa-Su 10:00-16:00");
        
        Set<DayOfWeek> expectedDays = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertEquals(expectedDays, condition.getDays());
        assertEquals(LocalTime.of(10, 0), condition.getStartTime());
        assertEquals(LocalTime.of(16, 0), condition.getEndTime());
    }
    
    @Test
    public void testParseTimeConditionSingleDay() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo 08:00-17:00");
        
        Set<DayOfWeek> expectedDays = EnumSet.of(DayOfWeek.MONDAY);
        assertEquals(expectedDays, condition.getDays());
        assertEquals(LocalTime.of(8, 0), condition.getStartTime());
        assertEquals(LocalTime.of(17, 0), condition.getEndTime());
    }
    
    @Test
    public void testParseTimeConditionOvernightTime() {
        // Test time range that crosses midnight
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("22:00-06:00");
        
        assertEquals(LocalTime.of(22, 0), condition.getStartTime());
        assertEquals(LocalTime.of(6, 0), condition.getEndTime());
        assertTrue("Should span multiple days", condition.isSpansMultipleDays());
        assertFalse("Should not span 24 hours", condition.isSpans24Hours());
    }
    
    @Test
    public void testParseTimeConditionWithMonths() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00 Apr-Oct");
        
        Set<DayOfWeek> expectedDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        assertEquals(expectedDays, condition.getDays());
        
        Set<Month> expectedMonths = EnumSet.of(
            Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, 
            Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER
        );
        assertEquals(expectedMonths, condition.getMonths());
    }
    
    @Test
    public void testParseTimeConditionTimeOnly() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("07:00-19:00");
        
        assertTrue("Days should be empty", condition.getDays().isEmpty());
        assertEquals(LocalTime.of(7, 0), condition.getStartTime());
        assertEquals(LocalTime.of(19, 0), condition.getEndTime());
    }
    
    @Test
    public void testParseTimeConditionDaysOnly() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr");
        
        Set<DayOfWeek> expectedDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        assertEquals(expectedDays, condition.getDays());
        assertNull("Start time should be null", condition.getStartTime());
        assertNull("End time should be null", condition.getEndTime());
        assertTrue("Should span 24 hours", condition.isSpans24Hours());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseTimeConditionEmpty() {
        TimeUtils.parseTimeCondition("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testParseTimeConditionNull() {
        TimeUtils.parseTimeCondition(null);
    }
    
    @Test
    public void testIsActiveAtBasicCondition() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00");
        
        // Monday 08:00 - should be active
        assertTrue("Monday morning should be active", condition.isActiveAt(mondayMorning));
        
        // Monday 20:00 - should not be active
        assertFalse("Monday evening should not be active", condition.isActiveAt(mondayEvening));
        
        // Saturday 14:00 - should not be active
        assertFalse("Saturday should not be active", condition.isActiveAt(saturdayAfternoon));
    }
    
    @Test
    public void testIsActiveAtOvernightCondition() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("22:00-06:00");
        
        // Friday 23:00 - should be active
        assertTrue("Friday 23:00 should be active", condition.isActiveAt(fridayLateNight));
        
        // Tuesday 05:00 - should be active
        assertTrue("Tuesday 05:00 should be active", condition.isActiveAt(tuesdayEarlyMorning));
        
        // Monday 08:00 - should not be active
        assertFalse("Monday 08:00 should not be active", condition.isActiveAt(mondayMorning));
        
        // Saturday 14:00 - should not be active
        assertFalse("Saturday 14:00 should not be active", condition.isActiveAt(saturdayAfternoon));
    }
    
    @Test
    public void testIsActiveAtWeekendCondition() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Sa-Su");
        
        // Saturday - should be active
        assertTrue("Saturday should be active", condition.isActiveAt(saturdayAfternoon));
        
        // Sunday - should be active
        assertTrue("Sunday should be active", condition.isActiveAt(sundayNight));
        
        // Monday - should not be active
        assertFalse("Monday should not be active", condition.isActiveAt(mondayMorning));
    }
    
    @Test
    public void testIsActiveAtMonthCondition() {
        // Test condition that's only active in summer months
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Jun-Aug");
        
        LocalDateTime julyDate = LocalDateTime.of(2023, 7, 15, 12, 0);
        LocalDateTime octoberDate = LocalDateTime.of(2023, 10, 15, 12, 0);
        
        assertTrue("July should be active", condition.isActiveAt(julyDate));
        assertFalse("October should not be active", condition.isActiveAt(octoberDate));
    }
    
    @Test
    public void testIsActiveAt24HourCondition() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr");
        
        // Should be active any time on weekdays
        assertTrue("Monday should be active", condition.isActiveAt(mondayMorning));
        assertTrue("Monday evening should be active", condition.isActiveAt(mondayEvening));
        
        // Should not be active on weekends
        assertFalse("Saturday should not be active", condition.isActiveAt(saturdayAfternoon));
    }
    
    @Test
    public void testValidateConditionSyntax() {
        // Valid conditions should not throw
        TimeUtils.validateConditionSyntax("Mo-Fr 07:00-19:00");
        TimeUtils.validateConditionSyntax("Sa-Su");
        TimeUtils.validateConditionSyntax("22:00-06:00");
        TimeUtils.validateConditionSyntax("");
        
        // Invalid conditions would throw (tested elsewhere)
    }
    
    @Test
    public void testGetNextActivation() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00");
        
        // From Monday 05:00, next activation should be Monday 07:00
        LocalDateTime fromTime = LocalDateTime.of(2023, 10, 16, 5, 0);
        LocalDateTime nextActivation = TimeUtils.getNextActivation(condition, fromTime);
        assertNotNull("Should find next activation", nextActivation);
        assertEquals(LocalDateTime.of(2023, 10, 16, 7, 0), nextActivation);
        
        // From Monday 08:00 (already active), should return current time
        LocalDateTime alreadyActive = TimeUtils.getNextActivation(condition, mondayMorning);
        assertEquals("Already active should return same time", mondayMorning, alreadyActive);
        
        // From Saturday, next activation should be Monday
        LocalDateTime fromSaturday = LocalDateTime.of(2023, 10, 14, 10, 0);
        LocalDateTime nextFromSaturday = TimeUtils.getNextActivation(condition, fromSaturday);
        assertNotNull("Should find next Monday activation", nextFromSaturday);
        assertEquals(DayOfWeek.MONDAY, nextFromSaturday.getDayOfWeek());
        assertEquals(7, nextFromSaturday.getHour());
    }
    
    @Test
    public void testGetActivationEnd() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00");
        
        // From Monday 08:00 (active), end should be Monday 19:00
        LocalDateTime endTime = TimeUtils.getActivationEnd(condition, mondayMorning);
        assertNotNull("Should find end time", endTime);
        assertEquals(LocalDateTime.of(2023, 10, 16, 19, 0), endTime);
        
        // From Saturday (not active), should return null
        LocalDateTime notActiveEnd = TimeUtils.getActivationEnd(condition, saturdayAfternoon);
        assertNull("Not active should return null", notActiveEnd);
    }
    
    @Test
    public void testConditionsOverlap() {
        ParsedTimeCondition condition1 = TimeUtils.parseTimeCondition("Mo-Fr 07:00-19:00");
        ParsedTimeCondition condition2 = TimeUtils.parseTimeCondition("Mo-We 08:00-17:00");
        ParsedTimeCondition condition3 = TimeUtils.parseTimeCondition("Sa-Su 10:00-16:00");
        
        // Conditions 1 and 2 should overlap (Mo-We, overlapping times)
        assertTrue("Weekday conditions should overlap", 
                  TimeUtils.conditionsOverlap(condition1, condition2));
        
        // Conditions 1 and 3 should not overlap (different days)
        assertFalse("Weekday and weekend conditions should not overlap", 
                   TimeUtils.conditionsOverlap(condition1, condition3));
        
        // Conditions 2 and 3 should not overlap
        assertFalse("Different day conditions should not overlap", 
                   TimeUtils.conditionsOverlap(condition2, condition3));
    }
    
    @Test
    public void testFormatDuration() {
        assertEquals("30 min", TimeUtils.formatDuration(Duration.ofMinutes(30)));
        assertEquals("2 h", TimeUtils.formatDuration(Duration.ofHours(2)));
        assertEquals("2h 30m", TimeUtils.formatDuration(Duration.ofMinutes(150)));
        assertEquals("unknown", TimeUtils.formatDuration(null));
    }
    
    @Test
    public void testGetCurrentDayAbbreviation() {
        String dayAbbr = TimeUtils.getCurrentDayAbbreviation();
        assertNotNull("Day abbreviation should not be null", dayAbbr);
        assertTrue("Day abbreviation should be 2 characters", dayAbbr.length() == 2);
    }
    
    @Test
    public void testGetDayAbbreviation() {
        assertEquals("Mo", TimeUtils.getDayAbbreviation(DayOfWeek.MONDAY));
        assertEquals("Tu", TimeUtils.getDayAbbreviation(DayOfWeek.TUESDAY));
        assertEquals("We", TimeUtils.getDayAbbreviation(DayOfWeek.WEDNESDAY));
        assertEquals("Th", TimeUtils.getDayAbbreviation(DayOfWeek.THURSDAY));
        assertEquals("Fr", TimeUtils.getDayAbbreviation(DayOfWeek.FRIDAY));
        assertEquals("Sa", TimeUtils.getDayAbbreviation(DayOfWeek.SATURDAY));
        assertEquals("Su", TimeUtils.getDayAbbreviation(DayOfWeek.SUNDAY));
    }
    
    @Test
    public void testGetMonthAbbreviation() {
        assertEquals("Jan", TimeUtils.getMonthAbbreviation(Month.JANUARY));
        assertEquals("Feb", TimeUtils.getMonthAbbreviation(Month.FEBRUARY));
        assertEquals("Dec", TimeUtils.getMonthAbbreviation(Month.DECEMBER));
    }
    
    @Test
    public void testIsBusinessHours() {
        // Monday 10:00 - should be business hours
        LocalDateTime mondayBusinessTime = LocalDateTime.of(2023, 10, 16, 10, 0);
        assertTrue("Monday 10:00 should be business hours", 
                  TimeUtils.isBusinessHours(mondayBusinessTime));
        
        // Monday 18:00 - should not be business hours
        LocalDateTime mondayAfterHours = LocalDateTime.of(2023, 10, 16, 18, 0);
        assertFalse("Monday 18:00 should not be business hours", 
                   TimeUtils.isBusinessHours(mondayAfterHours));
        
        // Saturday 10:00 - should not be business hours
        assertFalse("Saturday should not be business hours", 
                   TimeUtils.isBusinessHours(saturdayAfternoon));
        
        // Sunday 10:00 - should not be business hours
        LocalDateTime sundayMorning = LocalDateTime.of(2023, 10, 15, 10, 0);
        assertFalse("Sunday should not be business hours", 
                   TimeUtils.isBusinessHours(sundayMorning));
    }
    
    @Test
    public void testCreateCommonConditions() {
        // Test business hours condition
        ParsedTimeCondition businessHours = TimeUtils.createCommonCondition("business_hours");
        Set<DayOfWeek> expectedBusinessDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        assertEquals(expectedBusinessDays, businessHours.getDays());
        assertEquals(LocalTime.of(9, 0), businessHours.getStartTime());
        assertEquals(LocalTime.of(17, 0), businessHours.getEndTime());
        
        // Test weekends condition
        ParsedTimeCondition weekends = TimeUtils.createCommonCondition("weekends");
        Set<DayOfWeek> expectedWeekendDays = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertEquals(expectedWeekendDays, weekends.getDays());
        assertNull("Weekend condition should not have time limits", weekends.getStartTime());
        
        // Test night hours condition
        ParsedTimeCondition nightHours = TimeUtils.createCommonCondition("night_hours");
        assertEquals(LocalTime.of(22, 0), nightHours.getStartTime());
        assertEquals(LocalTime.of(6, 0), nightHours.getEndTime());
        assertTrue("Night hours should span multiple days", nightHours.isSpansMultipleDays());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateCommonConditionInvalid() {
        TimeUtils.createCommonCondition("unknown_scenario");
    }
    
    @Test
    public void testComplexConditionEvaluation() {
        // Test a complex condition: "Mo-Fr 07:00-09:00,17:00-19:00"
        // This would be parsed as separate conditions in real usage,
        // but we can test the overlap logic
        ParsedTimeCondition morningRush = TimeUtils.parseTimeCondition("Mo-Fr 07:00-09:00");
        ParsedTimeCondition eveningRush = TimeUtils.parseTimeCondition("Mo-Fr 17:00-19:00");
        
        // Test morning rush hour
        LocalDateTime morningRushTime = LocalDateTime.of(2023, 10, 16, 8, 0);
        assertTrue("Should be active during morning rush", morningRush.isActiveAt(morningRushTime));
        assertFalse("Should not be active during morning rush", eveningRush.isActiveAt(morningRushTime));
        
        // Test evening rush hour
        LocalDateTime eveningRushTime = LocalDateTime.of(2023, 10, 16, 18, 0);
        assertFalse("Should not be active during evening rush", morningRush.isActiveAt(eveningRushTime));
        assertTrue("Should be active during evening rush", eveningRush.isActiveAt(eveningRushTime));
        
        // Test midday (neither condition active)
        LocalDateTime middayTime = LocalDateTime.of(2023, 10, 16, 12, 0);
        assertFalse("Should not be active at midday", morningRush.isActiveAt(middayTime));
        assertFalse("Should not be active at midday", eveningRush.isActiveAt(middayTime));
    }
    
    @Test
    public void testEdgeCaseMidnight() {
        ParsedTimeCondition midnightCondition = TimeUtils.parseTimeCondition("00:00-00:00");
        
        assertTrue("Midnight to midnight should span 24 hours", midnightCondition.isSpans24Hours());
        
        // Should be active at any time
        assertTrue("Should be active at any time", midnightCondition.isActiveAt(mondayMorning));
        assertTrue("Should be active at any time", midnightCondition.isActiveAt(saturdayAfternoon));
    }
    
    @Test
    public void testEdgeCaseOneMinuteBefore() {
        ParsedTimeCondition condition = TimeUtils.parseTimeCondition("07:00-19:00");
        
        // 06:59 should not be active
        LocalDateTime oneMinuteBefore = LocalDateTime.of(2023, 10, 16, 6, 59);
        assertFalse("One minute before should not be active", condition.isActiveAt(oneMinuteBefore));
        
        // 07:00 should be active
        LocalDateTime exactStart = LocalDateTime.of(2023, 10, 16, 7, 0);
        assertTrue("Exact start time should be active", condition.isActiveAt(exactStart));
        
        // 19:00 should not be active (end time is exclusive)
        LocalDateTime exactEnd = LocalDateTime.of(2023, 10, 16, 19, 0);
        assertFalse("Exact end time should not be active", condition.isActiveAt(exactEnd));
        
        // 18:59 should be active
        LocalDateTime oneMinuteBeforeEnd = LocalDateTime.of(2023, 10, 16, 18, 59);
        assertTrue("One minute before end should be active", condition.isActiveAt(oneMinuteBeforeEnd));
    }
}