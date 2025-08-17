package com.yourco.econdigest.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextUtilsTest {

    @Test
    void truncate_shouldTruncateToMaxLength() {
        String text = "This is a long text that needs to be truncated";
        String result = TextUtils.truncate(text, 20);
        
        assertEquals("This is a long te...", result);
        assertEquals(20, result.length());
    }
    
    @Test
    void truncate_shouldReturnOriginalIfShorter() {
        String text = "Short text";
        String result = TextUtils.truncate(text, 20);
        
        assertEquals(text, result);
    }
    
    @Test
    void truncate_shouldHandleNullInput() {
        String result = TextUtils.truncate(null, 20);
        assertNull(result);
    }
    
    @Test
    void truncateSafe_shouldTruncateAtWordBoundary() {
        String text = "This is a long text that needs to be truncated";
        String result = TextUtils.truncateSafe(text, 20);
        
        assertTrue(result.endsWith("..."));
        assertTrue(result.length() <= 20);
    }
    
    @Test
    void splitSentences_shouldSplitCorrectly() {
        String text = "First sentence. Second sentence! Third sentence? Fourth sentence.";
        List<String> sentences = TextUtils.splitSentences(text);
        
        assertEquals(4, sentences.size());
        assertEquals("First sentence", sentences.get(0));
        assertEquals("Second sentence", sentences.get(1));
        assertEquals("Third sentence", sentences.get(2));
        assertEquals("Fourth sentence", sentences.get(3));
    }
    
    @Test
    void splitSentences_shouldHandleEmptyInput() {
        List<String> sentences = TextUtils.splitSentences("");
        assertTrue(sentences.isEmpty());
        
        sentences = TextUtils.splitSentences(null);
        assertTrue(sentences.isEmpty());
    }
    
    @Test
    void calculateHash_shouldReturnConsistentHash() {
        String input = "test input";
        String hash1 = TextUtils.calculateHash(input);
        String hash2 = TextUtils.calculateHash(input);
        
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 character hex string
    }
    
    @Test
    void calculateHash_shouldReturnDifferentHashForDifferentInput() {
        String input1 = "test input 1";
        String input2 = "test input 2";
        
        String hash1 = TextUtils.calculateHash(input1);
        String hash2 = TextUtils.calculateHash(input2);
        
        assertNotEquals(hash1, hash2);
    }
    
    @Test
    void isBlank_shouldReturnTrueForBlankStrings() {
        assertTrue(TextUtils.isBlank(null));
        assertTrue(TextUtils.isBlank(""));
        assertTrue(TextUtils.isBlank("   "));
        assertTrue(TextUtils.isBlank("\t\n"));
    }
    
    @Test
    void isBlank_shouldReturnFalseForNonBlankStrings() {
        assertFalse(TextUtils.isBlank("test"));
        assertFalse(TextUtils.isBlank(" test "));
        assertFalse(TextUtils.isBlank("a"));
    }
    
    @Test
    void isNotBlank_shouldBeOppositeOfIsBlank() {
        assertTrue(TextUtils.isNotBlank("test"));
        assertFalse(TextUtils.isNotBlank(null));
        assertFalse(TextUtils.isNotBlank(""));
    }
    
    @Test
    void removeExtraWhitespace_shouldNormalizeWhitespace() {
        String input = "This   has    multiple\t\tspaces  and\ntabs";
        String result = TextUtils.removeExtraWhitespace(input);
        
        assertEquals("This has multiple spaces and tabs", result);
    }
    
    @Test
    void removeExtraWhitespace_shouldHandleNullInput() {
        String result = TextUtils.removeExtraWhitespace(null);
        assertNull(result);
    }
}