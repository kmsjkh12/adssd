package CDCD;

import java.util.*;
public class ValidationResult {
    private final List<String> missingParameters = new ArrayList<>();
    private final List<String> invalidPlaceholders = new ArrayList<>();

    public void addMissingParameter(String param) {
        missingParameters.add(param);
    }

    public void addInvalidPlaceholder(String placeholder) {
        invalidPlaceholders.add(placeholder);
    }

    public boolean isValid() {
        return missingParameters.isEmpty() && invalidPlaceholders.isEmpty();
    }

    @Override
    public String toString() {
        if (isValid()) {
            return "✅ [성공] 모든 파라미터가 유효하고 올바르게 설정되었습니다.";
        }
        StringBuilder sb = new StringBuilder("🚨 [실패] 검증에 실패했습니다.\n");
        if (!invalidPlaceholders.isEmpty()) {
            sb.append("  - 잘못된 형식의 파라미터가 XML에 존재합니다: ").append(invalidPlaceholders).append("\n");
        }
        if (!missingParameters.isEmpty()) {
            sb.append("  - 다음 파라미터가 Java 코드에 정의되지 않았습니다: ").append(missingParameters).append("\n");
        }
        return sb.toString();
    }
}