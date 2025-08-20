package com.yourco.econdigest.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL-specific tests that only run when connected to PostgreSQL
 * Enable with: -Dtest.postgresql=true
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "test.postgresql", matches = "true")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true"
})
class PostgreSQLSpecificTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testJSONBSupport() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Test JSONB operations on glossary column
            String insertSql = "INSERT INTO articles (source, url, title, published_at, raw_excerpt) " +
                "VALUES ('test', 'http://test.com/unique1', 'Test Article', now(), 'Test content')";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.executeUpdate();
            }
            
            // Get the article ID
            long articleId;
            String selectSql = "SELECT id FROM articles WHERE url = 'http://test.com/unique1'";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                articleId = rs.getLong("id");
            }
            
            // Insert summary with JSONB glossary
            String summaryInsertSql = "INSERT INTO summaries (article_id, model, summary_text, why_it_matters, bullets, glossary, score) " +
                "VALUES (?, 'gpt-4o-mini', 'Test summary', 'Test importance', " +
                "ARRAY['point1', 'point2', 'point3'], " +
                "'[{\"term\": \"GDP\", \"definition\": \"국내총생산\"}]'::JSONB, 85.0)";
            
            try (PreparedStatement stmt = conn.prepareStatement(summaryInsertSql)) {
                stmt.setLong(1, articleId);
                stmt.executeUpdate();
            }
            
            // Test JSONB query
            String jsonbQuerySql = "SELECT glossary->0->>'term' as term, glossary->0->>'definition' as definition " +
                "FROM summaries WHERE article_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(jsonbQuerySql)) {
                stmt.setLong(1, articleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals("GDP", rs.getString("term"));
                    assertEquals("국내총생산", rs.getString("definition"));
                }
            }
        }
    }

    @Test
    void testArraySupport() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Test array operations on bullets and evidence_idx columns
            String insertSql = "INSERT INTO articles (source, url, title, published_at, raw_excerpt) " +
                "VALUES ('test', 'http://test.com/unique2', 'Test Article 2', now(), 'Test content 2')";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.executeUpdate();
            }
            
            // Get the article ID
            long articleId;
            String selectSql = "SELECT id FROM articles WHERE url = 'http://test.com/unique2'";
            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                articleId = rs.getLong("id");
            }
            
            // Insert summary with arrays
            String summaryInsertSql = "INSERT INTO summaries (article_id, model, summary_text, why_it_matters, bullets, evidence_idx, score) " +
                "VALUES (?, 'gpt-4o-mini', 'Test summary', 'Test importance', " +
                "ARRAY['첫 번째 포인트', '두 번째 포인트', '세 번째 포인트'], " +
                "ARRAY[1, 3, 5], 75.5)";
            
            try (PreparedStatement stmt = conn.prepareStatement(summaryInsertSql)) {
                stmt.setLong(1, articleId);
                stmt.executeUpdate();
            }
            
            // Test array query
            String arrayQuerySql = "SELECT bullets, evidence_idx, array_length(bullets, 1) as bullets_count " +
                "FROM summaries WHERE article_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(arrayQuerySql)) {
                stmt.setLong(1, articleId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    
                    // Test bullets array
                    String[] bullets = (String[]) rs.getArray("bullets").getArray();
                    assertEquals(3, bullets.length);
                    assertEquals("첫 번째 포인트", bullets[0]);
                    
                    // Test evidence_idx array
                    Integer[] evidenceIdx = (Integer[]) rs.getArray("evidence_idx").getArray();
                    assertEquals(3, evidenceIdx.length);
                    assertEquals(Integer.valueOf(1), evidenceIdx[0]);
                    
                    // Test array_length function
                    assertEquals(3, rs.getInt("bullets_count"));
                }
            }
        }
    }

    @Test
    void testTrigramExtension() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Test if pg_trgm extension is available
            String extensionCheckSql = "SELECT EXISTS (" +
                "SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm'" +
                ") as extension_exists";
            
            try (PreparedStatement stmt = conn.prepareStatement(extensionCheckSql);
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                boolean extensionExists = rs.getBoolean("extension_exists");
                assertTrue(extensionExists, "pg_trgm extension should be available");
            }
        }
    }

    @Test
    void testUpdatedAtTrigger() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            // Insert a daily digest
            String insertSql = "INSERT INTO daily_digest (digest_date, title, body_markdown, total_articles, total_summaries) " +
                "VALUES (CURRENT_DATE, 'Test Digest', 'Test content', 5, 3)";
            
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.executeUpdate();
            }
            
            // Get the created record
            long digestId;
            java.sql.Timestamp createdAt;
            String selectSql = "SELECT id, created_at, updated_at FROM daily_digest " +
                "WHERE title = 'Test Digest'";
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql);
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                digestId = rs.getLong("id");
                createdAt = rs.getTimestamp("created_at");
                java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                
                // Initially, created_at and updated_at should be similar
                long timeDiff = Math.abs(updatedAt.getTime() - createdAt.getTime());
                assertTrue(timeDiff < 1000, "created_at and updated_at should be similar initially");
            }
            
            // Wait a moment and update the record
            Thread.sleep(1000);
            
            String updateSql = "UPDATE daily_digest SET title = 'Updated Test Digest' " +
                "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setLong(1, digestId);
                stmt.executeUpdate();
            }
            
            // Check that updated_at was automatically updated
            String checkUpdateSql = "SELECT created_at, updated_at FROM daily_digest " +
                "WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(checkUpdateSql)) {
                stmt.setLong(1, digestId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next());
                    java.sql.Timestamp newCreatedAt = rs.getTimestamp("created_at");
                    java.sql.Timestamp newUpdatedAt = rs.getTimestamp("updated_at");
                    
                    // created_at should not change
                    assertEquals(createdAt, newCreatedAt);
                    
                    // updated_at should be newer
                    assertTrue(newUpdatedAt.after(newCreatedAt), 
                        "updated_at should be automatically updated by trigger");
                }
            }
        }
    }
}