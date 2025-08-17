package com.yourco.econdigest.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void getYesterday_shouldReturnPreviousDay() {
        LocalDate yesterday = TimeUtils.getYesterday();
        LocalDate expectedYesterday = LocalDate.now(TimeUtils.KST).minusDays(1);
        
        assertEquals(expectedYesterday, yesterday);
    }
    
    @Test
    void getYesterday_withZoneId_shouldReturnPreviousDayInGivenZone() {
        ZoneId utc = ZoneId.of("UTC");
        LocalDate yesterday = TimeUtils.getYesterday(utc);
        LocalDate expectedYesterday = LocalDate.now(utc).minusDays(1);
        
        assertEquals(expectedYesterday, yesterday);
    }
    
    @Test
    void formatDate_shouldReturnCorrectFormat() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        String formatted = TimeUtils.formatDate(date);
        
        assertEquals("2023-12-25", formatted);
    }
    
    @Test
    void parseDate_shouldParseCorrectly() {
        String dateStr = "2023-12-25";
        LocalDate parsed = TimeUtils.parseDate(dateStr);
        
        assertEquals(LocalDate.of(2023, 12, 25), parsed);
    }
    
    @Test
    void isValidDateString_shouldReturnTrueForValidDate() {
        assertTrue(TimeUtils.isValidDateString("2023-12-25"));
        assertTrue(TimeUtils.isValidDateString("2000-01-01"));
    }
    
    @Test
    void isValidDateString_shouldReturnFalseForInvalidDate() {
        assertFalse(TimeUtils.isValidDateString("invalid-date"));
        assertFalse(TimeUtils.isValidDateString("2023-13-40"));
        assertFalse(TimeUtils.isValidDateString(""));
        assertFalse(TimeUtils.isValidDateString(null));
    }
    
    @Test
    void nowKst_shouldReturnCurrentTimeInKST() {
        assertNotNull(TimeUtils.nowKst());
        assertEquals(TimeUtils.KST, TimeUtils.nowKst().getZone());
    }
    
    @Test
    void nowKstLocal_shouldReturnCurrentLocalDateTime() {
        assertNotNull(TimeUtils.nowKstLocal());
    }
}