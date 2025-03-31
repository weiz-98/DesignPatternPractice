package com.example.demo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParsingUtil {

    private static final Pattern EXPANSION_PATTERN = Pattern.compile("\\{(.*?)\\}");

    public static List<String> splitToolList(String toolIdList) {
        if (toolIdList == null || toolIdList.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 使用 stream 語法簡化
        return Stream.of(toolIdList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * RecipeId 格式為：xxx.xx-xxxx.xxxx-{...}，
     * (1) {c}              -> 返回 ["%%"]
     * (2) {cEF}            -> 返回 ["E", "F"]
     * (3) {cEF}{c134}      -> 返回 ["E", "F", "1", "3", "4"]
     * (4) {c(3;2)}         -> 返回 ["3", "2"]
     * (5) 沒有大括號      -> 不做變換，直接返回 toolList
     */
    public static List<String> parsingChamber(List<String> toolList, String recipeId) {
        if (recipeId == null || !recipeId.contains("{")) {
            return toolList;
        }

        List<String> expansions = extractExpansions(recipeId);
        if (expansions.isEmpty()) {
            return toolList;
        }

        List<String> result = new ArrayList<>();
        for (String tool : toolList) {
            for (String exp : expansions) {
                result.add(tool + "#" + exp);
            }
        }
        return result;
    }

    /**
     * 從 recipeId 中提取所有大括號內的擴展內容，依據不同格式進行拆分：
     * - 若內容為空 (例如 {c}) 則返回 "%%"
     * - 若內容包含括號 (例如 {c(3;2)}) 則分割後返回 ["3", "2"]
     * - 否則將內容拆成單個字元返回 (例如 {cEF} -> ["E", "F"])
     */
    private static List<String> extractExpansions(String recipeId) {
        List<String> expansions = new ArrayList<>();
        Matcher matcher = EXPANSION_PATTERN.matcher(recipeId);
        while (matcher.find()) {
            String content = matcher.group(1); // 例如 "c", "cEF", "c(3;2)", "c134", 等
            if (content.startsWith("c")) {
                content = content.substring(1).trim(); // 移除前導的 "c" 與多餘空白
            }
            if (content.isEmpty()) {
                // 情形 {c}
                expansions.add("%%");
            } else if (content.startsWith("(") && content.endsWith(")")) {
                // 情形 {c(3;2)} 或 {c(A;B;C)}
                String inner = content.substring(1, content.length() - 1);
                String[] parts = inner.split(";");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        expansions.add(trimmed);
                    }
                }
            } else {
                // 情形 {cEF}、{c134}：直接拆分成單個字元
                for (char ch : content.toCharArray()) {
                    expansions.add(String.valueOf(ch));
                }
            }
        }
        return expansions;
    }
}
