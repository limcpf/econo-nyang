package com.yourco.econyang.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static LocalDate getYesterday() {
        return LocalDate.now(KST).minusDays(1);
    }
    
    public static LocalDate getYesterday(ZoneId zoneId) {
        return LocalDate.now(zoneId).minusDays(1);
    }
    
    public static ZonedDateTime nowKst() {
        return ZonedDateTime.now(KST);
    }
    
    public static LocalDateTime nowKstLocal() {
        return LocalDateTime.now(KST);
    }
    
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
    
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }
    
    public static boolean isValidDateString(String dateStr) {
        try {
            parseDate(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}