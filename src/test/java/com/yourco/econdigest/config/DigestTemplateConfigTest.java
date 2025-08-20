package com.yourco.econdigest.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DigestTemplateConfig 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class DigestTemplateConfigTest {

    @Autowired
    private DigestTemplateConfig digestTemplateConfig;

    @Test
    void testTemplatesLoaded() {
        Map<String, DigestTemplateConfig.Template> templates = digestTemplateConfig.getTemplates();
        
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        
        // 기본 템플릿 확인
        assertTrue(templates.containsKey("default"));
        DigestTemplateConfig.Template defaultTemplate = templates.get("default");
        assertNotNull(defaultTemplate.getTitle());
        assertNotNull(defaultTemplate.getHeader());
        assertNotNull(defaultTemplate.getArticleItem());
        assertNotNull(defaultTemplate.getFooter());
    }

    @Test
    void testFormatsConfiguration() {
        Map<String, DigestTemplateConfig.Format> formats = digestTemplateConfig.getFormats();
        
        assertNotNull(formats);
        
        // 마크다운 포맷 확인
        if (formats.containsKey("markdown")) {
            DigestTemplateConfig.Format markdownFormat = formats.get("markdown");
            assertNotNull(markdownFormat.getFileExtension());
            assertNotNull(markdownFormat.getMimeType());
        }
    }

    @Test
    void testDeliveryConfiguration() {
        DigestTemplateConfig.Delivery delivery = digestTemplateConfig.getDelivery();
        
        if (delivery != null) {
            assertNotNull(delivery.getDefaultDeliveryTimes());
            assertTrue(delivery.getMinArticlesForDelivery() >= 0);
            assertTrue(delivery.getMaxArticlesPerDigest() > 0);
        }
    }

    @Test
    void testTemplateVariables() {
        Map<String, DigestTemplateConfig.Template> templates = digestTemplateConfig.getTemplates();
        DigestTemplateConfig.Template defaultTemplate = templates.get("default");
        
        if (defaultTemplate != null && defaultTemplate.getArticleItem() != null) {
            String articleItem = defaultTemplate.getArticleItem();
            
            // 템플릿 변수가 포함되어 있는지 확인
            assertTrue(articleItem.contains("{{title}}") || 
                      articleItem.contains("{{importance}}") ||
                      articleItem.contains("{{aiSummary}}"));
        }
    }

    @Test
    void testMultipleTemplates() {
        Map<String, DigestTemplateConfig.Template> templates = digestTemplateConfig.getTemplates();
        
        // 여러 템플릿이 설정되어 있는지 확인
        assertTrue(templates.size() >= 1);
        
        for (Map.Entry<String, DigestTemplateConfig.Template> entry : templates.entrySet()) {
            String templateName = entry.getKey();
            DigestTemplateConfig.Template template = entry.getValue();
            
            assertNotNull(templateName);
            assertNotNull(template);
            assertNotNull(template.getTitle());
        }
    }
}