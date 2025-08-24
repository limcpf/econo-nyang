package com.yourco.econyang.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 본문 품질 필터링 서비스
 * 제목과 관련 없는 내용, 홍보성 콘텐츠, 구독 요구 등을 감지하여 제외
 */
@Service
public class ContentQualityFilterService {
    
    // 홍보성/구독 요구 키워드 (한글/영어)
    private static final Set<String> PROMOTIONAL_KEYWORDS = new HashSet<>(Arrays.asList(
        // 구독 관련
        "subscribe", "subscription", "구독", "회원가입", "가입하기",
        "sign up", "register", "등록하기", "회원등록",
        
        // 뉴스레터 관련  
        "newsletter", "뉴스레터", "email updates", "이메일 구독",
        
        // 광고성
        "advertisement", "sponsored", "광고", "협찬", "후원",
        "paid content", "유료 콘텐츠", "프리미엄",
        
        // 홍보성
        "click here", "여기를 클릭", "자세히 보기", "more info",
        "visit our website", "웹사이트 방문", "홈페이지",
        
        // 소셜 미디어
        "follow us", "팔로우", "like us", "좋아요", "share",
        "공유하기", "twitter", "facebook", "instagram"
    ));
    
    // 구독/가입 요구 패턴 (정규식)
    private static final Pattern[] SUBSCRIPTION_PATTERNS = {
        Pattern.compile(".*무료.*구독.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*free.*subscription.*", Pattern.CASE_INSENSITIVE), 
        Pattern.compile(".*join.*today.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*지금.*가입.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*sign.*up.*now.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*이메일.*받아보세요.*", Pattern.CASE_INSENSITIVE)
    };
    
    // 광고성 문구 패턴
    private static final Pattern[] PROMOTIONAL_PATTERNS = {
        Pattern.compile(".*\\d+%.*할인.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\d+%.*off.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*특가.*행사.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*limited.*time.*offer.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*제한.*시간.*", Pattern.CASE_INSENSITIVE)
    };
    
    // 뉴스와 관련 없는 콘텐츠 키워드
    private static final Set<String> NON_NEWS_KEYWORDS = new HashSet<>(Arrays.asList(
        "recipe", "레시피", "요리법", "맛집",
        "horoscope", "운세", "별자리", 
        "game", "게임", "오락",
        "celebrity", "연예인", "가십",
        "sports", "스포츠", "축구", "야구" // 경제뉴스에서 제외할 스포츠
    ));
    
    /**
     * 본문 콘텐츠의 품질을 검사하여 사용 가능한지 판단
     */
    public ContentQualityResult checkContentQuality(String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ContentQualityResult(false, "본문 내용이 없음");
        }
        
        if (title == null || title.trim().isEmpty()) {
            return new ContentQualityResult(false, "제목이 없음");
        }
        
        String normalizedContent = content.toLowerCase().trim();
        String normalizedTitle = title.toLowerCase().trim();
        
        // 1. 본문이 너무 짧은지 검사 (50자 미만)
        if (content.trim().length() < 50) {
            return new ContentQualityResult(false, "본문이 너무 짧음 (" + content.trim().length() + "자)");
        }
        
        // 2. 홍보성 키워드 검사
        for (String keyword : PROMOTIONAL_KEYWORDS) {
            if (normalizedContent.contains(keyword.toLowerCase())) {
                return new ContentQualityResult(false, "홍보성 키워드 감지: " + keyword);
            }
        }
        
        // 3. 구독/가입 요구 패턴 검사
        for (Pattern pattern : SUBSCRIPTION_PATTERNS) {
            if (pattern.matcher(normalizedContent).matches()) {
                return new ContentQualityResult(false, "구독 요구 패턴 감지: " + pattern.pattern());
            }
        }
        
        // 4. 광고성 패턴 검사
        for (Pattern pattern : PROMOTIONAL_PATTERNS) {
            if (pattern.matcher(normalizedContent).matches()) {
                return new ContentQualityResult(false, "광고성 패턴 감지: " + pattern.pattern());
            }
        }
        
        // 5. 뉴스와 관련 없는 콘텐츠 검사
        for (String keyword : NON_NEWS_KEYWORDS) {
            if (normalizedContent.contains(keyword.toLowerCase())) {
                // 단, 제목에도 해당 키워드가 있으면 의도된 뉴스일 수 있으므로 허용
                if (!normalizedTitle.contains(keyword.toLowerCase())) {
                    return new ContentQualityResult(false, "뉴스 외 콘텐츠 감지: " + keyword);
                }
            }
        }
        
        // 6. 제목과 본문의 관련성 검사
        double relevanceScore = calculateTitleContentRelevance(normalizedTitle, normalizedContent);
        if (relevanceScore < 0.1) { // 관련성이 10% 미만
            return new ContentQualityResult(false, "제목-본문 관련성 부족 (" + String.format("%.1f", relevanceScore * 100) + "%)");
        }
        
        return new ContentQualityResult(true, "품질 검사 통과");
    }
    
    /**
     * 제목과 본문의 관련성 점수 계산 (간단한 키워드 기반)
     */
    private double calculateTitleContentRelevance(String title, String content) {
        if (title.isEmpty() || content.isEmpty()) {
            return 0.0;
        }
        
        // 제목에서 의미있는 단어들 추출 (2글자 이상, 특수문자 제외)
        String[] titleWords = title.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ")
                                  .split("\\s+");
        
        Set<String> meaningfulTitleWords = new HashSet<>();
        for (String word : titleWords) {
            word = word.trim();
            if (word.length() >= 2) {
                meaningfulTitleWords.add(word.toLowerCase());
            }
        }
        
        if (meaningfulTitleWords.isEmpty()) {
            return 0.0;
        }
        
        // 본문에서 제목 키워드가 언급되는 비율 계산
        int matches = 0;
        for (String titleWord : meaningfulTitleWords) {
            if (content.contains(titleWord)) {
                matches++;
            }
        }
        
        return (double) matches / meaningfulTitleWords.size();
    }
    
    /**
     * 본문 품질 검사 결과를 담는 클래스
     */
    public static class ContentQualityResult {
        private final boolean isQualified;
        private final String reason;
        
        public ContentQualityResult(boolean isQualified, String reason) {
            this.isQualified = isQualified;
            this.reason = reason;
        }
        
        public boolean isQualified() {
            return isQualified;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return String.format("ContentQualityResult{qualified=%s, reason='%s'}", isQualified, reason);
        }
    }
}