package com.yourco.econdigest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 다이제스트 템플릿 설정
 */
@Component
@ConfigurationProperties(prefix = "digest")
public class DigestTemplateConfig {

    private Map<String, Template> templates;
    private Map<String, Format> formats;
    private Delivery delivery;

    public Map<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, Template> templates) {
        this.templates = templates;
    }

    public Map<String, Format> getFormats() {
        return formats;
    }

    public void setFormats(Map<String, Format> formats) {
        this.formats = formats;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public static class Template {
        private String title;
        private String subtitle;
        private String header;
        private String articleItem;
        private String footer;
        private Map<String, String> sectionHeaders;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getHeader() {
            return header;
        }

        public void setHeader(String header) {
            this.header = header;
        }

        public String getArticleItem() {
            return articleItem;
        }

        public void setArticleItem(String articleItem) {
            this.articleItem = articleItem;
        }

        public String getFooter() {
            return footer;
        }

        public void setFooter(String footer) {
            this.footer = footer;
        }

        public Map<String, String> getSectionHeaders() {
            return sectionHeaders;
        }

        public void setSectionHeaders(Map<String, String> sectionHeaders) {
            this.sectionHeaders = sectionHeaders;
        }
    }

    public static class Format {
        private String fileExtension;
        private String mimeType;
        private String wrapperTemplate;

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getWrapperTemplate() {
            return wrapperTemplate;
        }

        public void setWrapperTemplate(String wrapperTemplate) {
            this.wrapperTemplate = wrapperTemplate;
        }
    }

    public static class Delivery {
        private List<String> defaultDeliveryTimes;
        private boolean enableWeekendDelivery;
        private boolean enableHolidayDelivery;
        private int minArticlesForDelivery;
        private int maxArticlesPerDigest;

        public List<String> getDefaultDeliveryTimes() {
            return defaultDeliveryTimes;
        }

        public void setDefaultDeliveryTimes(List<String> defaultDeliveryTimes) {
            this.defaultDeliveryTimes = defaultDeliveryTimes;
        }

        public boolean isEnableWeekendDelivery() {
            return enableWeekendDelivery;
        }

        public void setEnableWeekendDelivery(boolean enableWeekendDelivery) {
            this.enableWeekendDelivery = enableWeekendDelivery;
        }

        public boolean isEnableHolidayDelivery() {
            return enableHolidayDelivery;
        }

        public void setEnableHolidayDelivery(boolean enableHolidayDelivery) {
            this.enableHolidayDelivery = enableHolidayDelivery;
        }

        public int getMinArticlesForDelivery() {
            return minArticlesForDelivery;
        }

        public void setMinArticlesForDelivery(int minArticlesForDelivery) {
            this.minArticlesForDelivery = minArticlesForDelivery;
        }

        public int getMaxArticlesPerDigest() {
            return maxArticlesPerDigest;
        }

        public void setMaxArticlesPerDigest(int maxArticlesPerDigest) {
            this.maxArticlesPerDigest = maxArticlesPerDigest;
        }
    }
}