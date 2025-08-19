package com.yourco.econdigest.openai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completion API 요청 DTO
 */
public class ChatCompletionRequest {
    
    private String model;
    private List<ChatMessage> messages;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    private Double temperature;
    
    @JsonProperty("top_p")
    private Double topP;
    
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    
    @JsonProperty("presence_penalty")
    private Double presencePenalty;
    
    private List<String> stop;
    
    private String user;
    
    // Structured Outputs 지원
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    // 기본 생성자
    public ChatCompletionRequest() {}
    
    // 필수 필드 생성자
    public ChatCompletionRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }
    
    // Getters and Setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }
    
    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }
    
    public Double getPresencePenalty() {
        return presencePenalty;
    }
    
    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }
    
    public List<String> getStop() {
        return stop;
    }
    
    public void setStop(List<String> stop) {
        this.stop = stop;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }
    
    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }
    
    /**
     * Structured Outputs를 위한 응답 형식 설정
     */
    public static class ResponseFormat {
        private String type;
        
        @JsonProperty("json_schema")
        private JsonSchema jsonSchema;
        
        public ResponseFormat() {}
        
        public ResponseFormat(String type) {
            this.type = type;
        }
        
        public ResponseFormat(JsonSchema jsonSchema) {
            this.type = "json_schema";
            this.jsonSchema = jsonSchema;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public JsonSchema getJsonSchema() {
            return jsonSchema;
        }
        
        public void setJsonSchema(JsonSchema jsonSchema) {
            this.jsonSchema = jsonSchema;
        }
        
        public static class JsonSchema {
            private String name;
            private String description;
            private Map<String, Object> schema;
            
            @JsonProperty("strict")
            private Boolean strict;
            
            public JsonSchema() {}
            
            public JsonSchema(String name, String description, Map<String, Object> schema) {
                this.name = name;
                this.description = description;
                this.schema = schema;
                this.strict = true; // Structured Outputs는 strict 모드 권장
            }
            
            public String getName() {
                return name;
            }
            
            public void setName(String name) {
                this.name = name;
            }
            
            public String getDescription() {
                return description;
            }
            
            public void setDescription(String description) {
                this.description = description;
            }
            
            public Map<String, Object> getSchema() {
                return schema;
            }
            
            public void setSchema(Map<String, Object> schema) {
                this.schema = schema;
            }
            
            public Boolean getStrict() {
                return strict;
            }
            
            public void setStrict(Boolean strict) {
                this.strict = strict;
            }
        }
    }
}