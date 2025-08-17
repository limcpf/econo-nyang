package com.yourco.econdigest.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class TextUtils {
    
    private TextUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    public static String truncateSafe(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        String truncated = text.substring(0, maxLength - 3);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace > maxLength * 0.8) {
            truncated = truncated.substring(0, lastSpace);
        }
        return truncated + "...";
    }
    
    public static List<String> splitSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> result = new ArrayList<>();
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                // Remove trailing punctuation for clean sentence
                String cleanSentence = trimmed.replaceAll("[.!?]+$", "");
                result.add(cleanSentence);
            }
        }
        
        return result;
    }
    
    public static String calculateHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    public static String removeExtraWhitespace(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}