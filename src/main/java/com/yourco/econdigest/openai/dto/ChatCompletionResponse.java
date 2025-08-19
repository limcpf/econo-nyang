package com.yourco.econdigest.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * OpenAI Chat Completion API 응답 DTO
 */
public class ChatCompletionResponse {
    
    private String id;
    private String object;
    private Long created;
    private String model;
    
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    
    private List<Choice> choices;
    private Usage usage;
    
    // 기본 생성자
    public ChatCompletionResponse() {}
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public Long getCreated() {
        return created;
    }
    
    public void setCreated(Long created) {
        this.created = created;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSystemFingerprint() {
        return systemFingerprint;
    }
    
    public void setSystemFingerprint(String systemFingerprint) {
        this.systemFingerprint = systemFingerprint;
    }
    
    public List<Choice> getChoices() {
        return choices;
    }
    
    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
    
    public Usage getUsage() {
        return usage;
    }
    
    public void setUsage(Usage usage) {
        this.usage = usage;
    }
    
    // 편의 메서드들
    public String getFirstChoiceContent() {
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            return choices.get(0).getMessage().getContent();
        }
        return null;
    }
    
    public boolean isFinished() {
        if (choices != null && !choices.isEmpty()) {
            return "stop".equals(choices.get(0).getFinishReason());
        }
        return false;
    }
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Instant getCreatedInstant() {
        return created != null ? Instant.ofEpochSecond(created) : null;
    }
    
    /**
     * Choice 클래스
     */
    public static class Choice {
        private Integer index;
        private ChatMessage message;
        
        @JsonProperty("logprobs")
        private Object logProbs;
        
        @JsonProperty("finish_reason")
        private String finishReason;
        
        public Choice() {}
        
        public Integer getIndex() {
            return index;
        }
        
        public void setIndex(Integer index) {
            this.index = index;
        }
        
        public ChatMessage getMessage() {
            return message;
        }
        
        public void setMessage(ChatMessage message) {
            this.message = message;
        }
        
        public Object getLogProbs() {
            return logProbs;
        }
        
        public void setLogProbs(Object logProbs) {
            this.logProbs = logProbs;
        }
        
        public String getFinishReason() {
            return finishReason;
        }
        
        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }
    
    /**
     * Usage 클래스 (토큰 사용량)
     */
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        
        @JsonProperty("prompt_tokens_details")
        private Object promptTokensDetails;
        
        @JsonProperty("completion_tokens_details")
        private Object completionTokensDetails;
        
        public Usage() {}
        
        public Integer getPromptTokens() {
            return promptTokens;
        }
        
        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }
        
        public Integer getCompletionTokens() {
            return completionTokens;
        }
        
        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }
        
        public Integer getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
        
        public Object getPromptTokensDetails() {
            return promptTokensDetails;
        }
        
        public void setPromptTokensDetails(Object promptTokensDetails) {
            this.promptTokensDetails = promptTokensDetails;
        }
        
        public Object getCompletionTokensDetails() {
            return completionTokensDetails;
        }
        
        public void setCompletionTokensDetails(Object completionTokensDetails) {
            this.completionTokensDetails = completionTokensDetails;
        }
        
        @Override
        public String toString() {
            return "Usage{" +
                    "promptTokens=" + promptTokens +
                    ", completionTokens=" + completionTokens +
                    ", totalTokens=" + totalTokens +
                    '}';
        }
    }
}