package com.yourco.econdigest.db;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=true"
})
class DatabaseSchemaTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testRequiredTablesExist() throws Exception {
        String[] requiredTables = {
            "articles", "summaries", "daily_digest", "dispatch_log"
        };

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            for (String tableName : requiredTables) {
                try (ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
                    assertTrue(rs.next(), "Table " + tableName + " should exist");
                    assertEquals(tableName.toUpperCase(), rs.getString("TABLE_NAME"));
                }
            }
        }
    }

    @Test
    void testArticlesTableStructure() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Test required columns exist
            String[] requiredColumns = {
                "ID", "SOURCE", "URL", "TITLE", "PUBLISHED_AT", "AUTHOR", "RAW_EXCERPT", "CREATED_AT"
            };
            
            List<String> actualColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "ARTICLES", null)) {
                while (rs.next()) {
                    actualColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Column " + column + " should exist in articles table");
            }
            
            // Test unique constraint on URL
            boolean hasUniqueConstraint = false;
            try (ResultSet rs = metaData.getIndexInfo(null, null, "ARTICLES", true, false)) {
                while (rs.next()) {
                    if ("URL".equals(rs.getString("COLUMN_NAME"))) {
                        hasUniqueConstraint = true;
                        break;
                    }
                }
            }
            assertTrue(hasUniqueConstraint, "URL column should have unique constraint");
        }
    }

    @Test
    void testSummariesTableStructure() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            String[] requiredColumns = {
                "ID", "ARTICLE_ID", "MODEL", "SUMMARY_TEXT", "WHY_IT_MATTERS", 
                "BULLETS", "GLOSSARY", "EVIDENCE_IDX", "SCORE", "CREATED_AT"
            };
            
            List<String> actualColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "SUMMARIES", null)) {
                while (rs.next()) {
                    actualColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Column " + column + " should exist in summaries table");
            }
        }
    }

    @Test
    void testDailyDigestTableStructure() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            String[] requiredColumns = {
                "ID", "DIGEST_DATE", "TITLE", "BODY_MARKDOWN", "TOTAL_ARTICLES", 
                "TOTAL_SUMMARIES", "CREATED_AT", "UPDATED_AT"
            };
            
            List<String> actualColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "DAILY_DIGEST", null)) {
                while (rs.next()) {
                    actualColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Column " + column + " should exist in daily_digest table");
            }
        }
    }

    @Test
    void testDispatchLogTableStructure() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            String[] requiredColumns = {
                "ID", "DIGEST_ID", "CHANNEL", "WEBHOOK_REF", "STATUS", 
                "RESPONSE_SNIPPET", "ATTEMPT_COUNT", "SENT_AT"
            };
            
            List<String> actualColumns = new ArrayList<>();
            try (ResultSet rs = metaData.getColumns(null, null, "DISPATCH_LOG", null)) {
                while (rs.next()) {
                    actualColumns.add(rs.getString("COLUMN_NAME"));
                }
            }
            
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Column " + column + " should exist in dispatch_log table");
            }
        }
    }

    @Test
    void testForeignKeyConstraints() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Test summaries -> articles foreign key
            boolean hasSummariesFK = false;
            try (ResultSet rs = metaData.getImportedKeys(null, null, "SUMMARIES")) {
                while (rs.next()) {
                    if ("ARTICLES".equals(rs.getString("PKTABLE_NAME")) && 
                        "ARTICLE_ID".equals(rs.getString("FKCOLUMN_NAME"))) {
                        hasSummariesFK = true;
                        break;
                    }
                }
            }
            assertTrue(hasSummariesFK, "summaries.article_id should reference articles.id");
            
            // Test dispatch_log -> daily_digest foreign key
            boolean hasDispatchLogFK = false;
            try (ResultSet rs = metaData.getImportedKeys(null, null, "DISPATCH_LOG")) {
                while (rs.next()) {
                    if ("DAILY_DIGEST".equals(rs.getString("PKTABLE_NAME")) && 
                        "DIGEST_ID".equals(rs.getString("FKCOLUMN_NAME"))) {
                        hasDispatchLogFK = true;
                        break;
                    }
                }
            }
            assertTrue(hasDispatchLogFK, "dispatch_log.digest_id should reference daily_digest.id");
        }
    }
}