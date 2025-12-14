package CDCD;

import java.util.List;

class SearchResultItem {
    String keyword;     // 예: hello_3
    String filePath;    // 예: .../UserMapper.java
    List<String> codes; // 발견된 코드 내용
    ValidationResult validationResult; // [추가됨] 이 파일/메서드에 대한 검증 결과
    public SearchResultItem(String keyword, String filePath, List<String> codes,ValidationResult validationResult) {
        this.keyword = keyword;
        this.filePath = filePath;
        this.codes = codes;
        this.validationResult = validationResult;
    }
    
    // UI 출력을 위한 toString 오버라이딩
    @Override
    public String toString() {
        return "▶ [" + keyword + "] Found in: " + filePath;
    }
}