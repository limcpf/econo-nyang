package com.yourco.econdigest.openai.dto;

/**
 * OpenAI Chat Message DTO
 */
public class ChatMessage {
    
    private String role;
    private String content;
    private String name;
    
    // 기본 생성자
    public ChatMessage() {}
    
    // 필수 필드 생성자
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // 이름 포함 생성자
    public ChatMessage(String role, String content, String name) {
        this.role = role;
        this.content = content;
        this.name = name;
    }
    
    // 정적 팩토리 메서드들
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }
    
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }
    
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
    
    public static ChatMessage function(String content, String name) {
        return new ChatMessage("function", content, name);
    }
    
    // Getters and Setters
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "ChatMessage{" +
                "role='" + role + '\'' +
                ", content='" + (content != null && content.length() > 50 ? 
                    content.substring(0, 50) + "..." : content) + '\'' +
                (name != null ? ", name='" + name + '\'' : "") +
                '}';
    }
}