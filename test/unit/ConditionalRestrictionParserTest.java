package org.openstreetmap.josm.plugins.conditionalrestrictions.test;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ParseException;
import org.openstreetmap.josm.plugins.conditionalrestrictions.parser.ConditionalRestrictionParser.ValidationResult;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.ConditionalRestriction;
import org.openstreetmap.josm.plugins.conditionalrestrictions.data.TimeCondition;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Unit tests for ConditionalRestrictionParser
 */
public class ConditionalRestrictionParserTest {
    
    private ConditionalRestrictionParser parser;
    
    @Before
    public void setUp() {
        parser = new ConditionalRestrictionParser();
    }
    
    @Test
    public void testSimpleTimeRestriction() throws ParseException {
        String tagValue = "no @ (Mo-Fr 07:00-19:00)";
        ConditionalRestriction restriction = parser.parse("access:conditional", tagValue);
        
        assertNotNull(restriction);
        assertEquals("access:conditional", restriction.getTagKey());
        assertEquals("no", restriction.getRestrictionValue());
        assertEquals(1, restriction.getTimeConditions().size());
        
        TimeCondition timeCondition = restriction.getTimeConditions().get(0);
        assertEquals(5, timeCondition.getDays().size());
        assertTrue(timeCondition.getDays().contains(DayOfWeek.MONDAY));
        assertTrue(timeCondition.getDays().contains(DayOfWeek.FRIDAY));
        assertFalse(timeCondition.getDays().contains(DayOfWeek.SATURDAY));
        
        assertEquals(LocalTime.of(7, 0), timeCondition.getStartTime());
        assertEquals(LocalTime.of(19, 0), timeCondition.getEndTime());
    }
    
    @Test
    public void testWeekendRestriction() throws ParseException {
        String tagValue = "yes @ (Sa-Su 10:00-16:00)";
        ConditionalRestriction restriction = parser.parse("oneway:conditional", tagValue);
        
        assertNotNull(restriction);
        assertEquals("oneway:conditional", restriction.getTagKey());
        assertEquals("yes", restriction.getRestrictionValue());
        
        TimeCondition timeCondition = restriction.getTimeConditions().get(0);
        assertEquals(2, timeCondition.getDays().size());
        assertTrue(timeCondition.getDays().contains(DayOfWeek.SATURDAY));
        assertTrue(timeCondition.getDays().contains(DayOfWeek.SUNDAY));
    }
    
    @Test
    public void testMaxspeedRestriction() throws ParseException {
        String tagValue = "30 @ (22:00-06:00)";
        ConditionalRestriction restriction = parser.parse("maxspeed:conditional", tagValue);
        
        assertNotNull(restriction);
        assertEquals("maxspeed:conditional", restriction.getTagKey());
        assertEquals("30", restriction.getRestrictionValue());
        
        TimeCondition timeCondition = restriction.getTimeConditions().get(0);
        assertEquals(LocalTime.of(22, 0), timeCondition.getStartTime());
        assertEquals(LocalTime.of(6, 0), timeCondition.getEndTime());
    }
    
    @Test
    public void testWeightRestriction() throws ParseException {
        String tagValue = "no @ (weight>7.5)";
        ConditionalRestriction restriction = parser.parse("hgv:conditional", tagValue);
        
        assertNotNull(restriction);
        assertEquals("hgv:conditional", restriction.getTagKey());
        assertEquals("no", restriction.getRestrictionValue());
        assertEquals(">", restriction.getWeightOperator());
        assertEquals(Double.valueOf(7.5), restriction.getWeightValue());
    }
    
    @Test
    public void testMultipleConditions() throws ParseException {
        String tagValue = "no @ (Mo-Fr 07:00-19:00; Sa 08:00-14:00)";
        ConditionalRestriction restriction = parser.parse("parking:conditional", tagValue);
        
        assertNotNull(restriction);
        assertEquals(2, restriction.getTimeConditions().size());
        
        // First condition: weekdays
        TimeCondition weekdayCondition = restriction.getTimeConditions().get(0);
        assertEquals(5, weekdayCondition.getDays().size());
        assertEquals(LocalTime.of(7, 0), weekdayCondition.getStartTime());
        assertEquals(LocalTime.of(19, 0), weekdayCondition.getEndTime());
        
        // Second condition: Saturday
        TimeCondition saturdayCondition = restriction.getTimeConditions().get(1);
        assertEquals(1, saturdayCondition.getDays().size());
        assertTrue(saturdayCondition.getDays().contains(DayOfWeek.SATURDAY));
        assertEquals(LocalTime.of(8, 0), saturdayCondition.getStartTime());
        assertEquals(LocalTime.of(14, 0), saturdayCondition.getEndTime());
    }
    
    @Test
    public void testValidation() {
        // Valid syntax
        ValidationResult result = parser.validate("access:conditional", "no @ (Mo-Fr 07:00-19:00)");
        assertTrue(result.isValid());
        
        // Invalid syntax - missing @
        result = parser.validate("access:conditional", "no (Mo-Fr 07:00-19:00)");
        assertFalse(result.isValid());
        
        // Invalid syntax - missing parentheses
        result = parser.validate("access:conditional", "no @ Mo-Fr 07:00-19:00");
        assertFalse(result.isValid());
        
        // Invalid time format
        result = parser.validate("access:conditional", "no @ (Mo-Fr 25:00-19:00)");
        assertFalse(result.isValid());
    }
    
    @Test
    public void testTimeConditionEvaluation() throws ParseException {
        String tagValue = "no @ (Mo-Fr 07:00-19:00)";
        ConditionalRestriction restriction = parser.parse("access:conditional", tagValue);
        
        // Test Monday 08:00 - should be active
        LocalDateTime mondayMorning = LocalDateTime.of(2023, 10, 16, 8, 0); // Monday
        assertTrue(restriction.isActiveAt(mondayMorning));
        
        // Test Monday 20:00 - should not be active
        LocalDateTime mondayEvening = LocalDateTime.of(2023, 10, 16, 20, 0); // Monday
        assertFalse(restriction.isActiveAt(mondayEvening));
        
        // Test Saturday 10:00 - should not be active
        LocalDateTime saturdayMorning = LocalDateTime.of(2023, 10, 14, 10, 0); // Saturday
        assertFalse(restriction.isActiveAt(saturdayMorning));
    }
    
    @Test(expected = ParseException.class)
    public void testInvalidSyntax() throws ParseException {
        parser.parse("access:conditional", "invalid syntax");
    }
    
    @Test(expected = ParseException.class) 
    public void testInvalidTimeFormat() throws ParseException {
        parser.parse("access:conditional", "no @ (Mo-Fr 25:00-19:00)");
    }
    
    @Test(expected = ParseException.class)
    public void testInvalidDayFormat() throws ParseException {
        parser.parse("access:conditional", "no @ (Xx-Fr 07:00-19:00)");
    }
}