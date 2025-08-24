# RSS 고유 식별자 기반 중복 처리 구현

**작성일**: 2025-08-24  
**목적**: 발행일 정보가 없거나 부정확한 RSS 소스에 대해 URL 패턴 기반 고유 식별자를 추출하여 중복 처리를 개선

---

## 📋 개요

### 🎯 해결하고자 한 문제

1. **발행일 기반 필터링의 한계**
   - 일부 RSS 소스에서 발행일 정보가 누락되거나 부정확
   - 시간 기반 중복 제거가 제대로 작동하지 않음

2. **RSS별 고유 특성 활용 필요**
   - Investing.com: URL 마지막에 숫자 ID 증가 패턴
   - 매일경제: URL에 기사 번호 포함
   - KOTRA: dataIdx 파라미터로 기사 식별

### ✅ 구현한 솔루션

**다층 중복 처리 전략**:
1. **고유 ID 기반** (우선순위 1): RSS별 맞춤형 ID 추출
2. **URL 기반** (우선순위 2): 전체 URL 중복 체크
3. **제목 유사도** (우선순위 3): Jaccard similarity 기반

---

## 🏗️ 구현 내용

### 1. ArticleIdExtractor 유틸리티

```java
/**
 * RSS 소스별로 기사의 고유 식별자를 추출
 */
public static String extractUniqueId(String sourceCode, String url) {
    switch (sourceCode.toLowerCase()) {
        case "maeil_securities":
            return extractMaeilEconomyId(url);
        case "kotra_overseas":
            return extractKotraId(url);
        case "investing_news":
        case "investing_market": 
        case "investing_commodities":
            return extractInvestingId(url);
        default:
            return generateHashId(url);  // 기본: SHA-256 해시
    }
}
```

#### 소스별 ID 추출 전략

| RSS 소스 | URL 패턴 | 추출 방식 | 예시 |
|----------|----------|-----------|------|
| 매일경제 | `/news/.../숫자ID` | 정규식으로 8자리 이상 숫자 추출 | `maeil_10123456` |
| KOTRA | `?dataIdx=숫자` | dataIdx 파라미터 추출 | `kotra_123456` |
| Investing.com | `/...숫자ID` | URL 끝 7자리 이상 숫자 추출 | `investing_12345678` |
| 기타 소스 | 모든 URL | SHA-256 해시값 (12자) | `hash_3deaa5e7823d` |

### 2. ArticleDto 확장

```java
private String uniqueId;      // RSS별 고유 식별자 (중복 처리용)

public String getUniqueId() { return uniqueId; }
public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
```

### 3. RssFeedService 중복 제거 로직 개선

```java
// 고유 ID 기반 중복 제거 (우선순위 1)
if (article.getUniqueId() != null) {
    if (seenUniqueIds.contains(article.getUniqueId())) {
        isDuplicate = true;
        System.out.println("중복 제거 (고유ID): " + article.getUniqueId());
    } else {
        seenUniqueIds.add(article.getUniqueId());
    }
}

// URL 기반 중복 제거 (우선순위 2) 
// 제목 유사도 기반 중복 제거 (우선순위 3)
```

---

## 🧪 테스트 및 검증

### 단위 테스트 (ArticleIdExtractorTest)

```java
@Test
void should_extract_maeil_economy_id() {
    String id = ArticleIdExtractor.extractUniqueId(
        "maeil_securities", 
        "https://www.mk.co.kr/news/economy/10123456"
    );
    assertEquals("maeil_10123456", id);
}

@Test  
void should_extract_investing_id() {
    String id = ArticleIdExtractor.extractUniqueId(
        "investing_news",
        "https://www.investing.com/news/economy/fed-cuts-rates-12345678"
    );
    assertEquals("investing_12345678", id);
}
```

**테스트 결과**: ✅ 9개 테스트 모두 통과

### 통합 테스트 (RssUniqueIdIntegrationTest)

실제 RSS 피드 연동하여 고유 ID 생성 확인:
- 매일경제 RSS에서 기사 수집
- Investing.com RSS에서 기사 수집  
- 각 기사의 고유 ID 올바른 생성 검증

---

## 📊 RSS 소스 업데이트

### 변경사항

1. **The Economist 제거**: RSS 피드 없어서 제외
2. **Investing.com URL 변경**: `kr.investing.com` → `www.investing.com` (국제 버전)
3. **발행일 누락 처리**: 발행일이 없는 경우 현재 시간으로 설정

### 최종 RSS 소스 목록 (9개)

| 소스명 | URL | 고유ID 방식 |
|--------|-----|-------------|
| BBC Business | `https://feeds.bbci.co.uk/news/business/rss.xml` | hash |
| Financial Times | `https://www.ft.com/companies?format=rss` | hash |
| MarketWatch | `https://feeds.marketwatch.com/marketwatch/topstories/` | hash |
| Bloomberg | `https://feeds.bloomberg.com/economics/news.rss` | hash |
| Investing News | `https://www.investing.com/rss/news.rss` | investing_* |
| Investing Market | `https://www.investing.com/rss/market_overview.rss` | investing_* |
| Investing Commodities | `https://www.investing.com/rss/commodities.rss` | investing_* |
| KOTRA | `https://dream.kotra.or.kr/kotra/rssList.do?pBbsGroup=1` | kotra_* |
| 매일경제 | `https://www.mk.co.kr/rss/30800011/` | maeil_* |

---

## 🔍 실제 테스트 결과

### RSS 연결성 확인

```bash
# Investing.com 테스트
curl -s "https://www.investing.com/rss/news.rss" | head -30
# ✅ 정상 연결, 기사 수집 가능

# 매일경제 테스트  
curl -s "https://www.mk.co.kr/rss/30800011/" | head -20
# ✅ 정상 연결, <no>11400481</no> 형태 ID 확인

# KOTRA 테스트
curl -s "https://dream.kotra.or.kr/kotra/rssList.do?pBbsGroup=1" | head -15  
# ✅ 정상 연결, dataIdx=232432 형태 ID 확인
```

### 고유 ID 추출 예시

```
매일경제 기사 예시:
- URL: https://www.mk.co.kr/news/english/11400481
- UniqueId: maeil_11400481
- Title: Former NTS officials launch Korea's largest tax firm

Investing.com 기사 예시:
- URL: https://www.investing.com/news/stock-market-news/cocacola-explores-sale-4207944
- UniqueId: investing_4207944  
- Title: Coca-Cola explores sale of Costa Coffee, source says

KOTRA 기사 예시:
- URL: https://dream.kotra.or.kr/.../Page.do?dataIdx=232432
- UniqueId: kotra_232432
- Title: 카타르 스마트시티 산업 동향
```

---

## 🎯 기대 효과

### 중복 처리 개선
- **이전**: URL만으로 중복 체크 → 일부 중복 놓침
- **이후**: 고유 ID → URL → 제목 순차적 중복 체크 → 정확도 향상

### 성능 개선  
- **빠른 중복 체크**: 해시/ID 비교가 문자열 유사도보다 빠름
- **메모리 효율**: 중복 기사를 조기에 필터링

### 확장성
- **새로운 RSS 소스 추가 시**: `ArticleIdExtractor`에 패턴만 추가하면 됨
- **패턴 변경 대응**: 각 소스별 독립적인 ID 추출 로직

---

## 🚀 다음 단계

1. **실제 운영 환경 테스트**
   - 실제 배치 실행으로 중복 제거 효과 측정
   - RSS별 고유 ID 생성 통계 수집

2. **모니터링 개선**
   - 고유 ID 추출 실패 케이스 로깅
   - RSS별 중복 제거 통계 대시보드

3. **패턴 보완**
   - 새로운 RSS 소스 추가 시 ID 추출 패턴 개발
   - 기존 패턴의 정확도 지속적인 검증

---

**구현 완료일**: 2025-08-24  
**테스트 상태**: ✅ 단위 테스트 9개 통과  
**문서화**: 완료 ✅