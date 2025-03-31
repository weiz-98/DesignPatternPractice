package com.example.demo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingUtil {

    /**
     * 解析 toolIdList 為 List<String>，以逗號分隔
     */
    public static List<String> splitToolList(String toolIdList) {
        if (toolIdList == null || toolIdList.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] arr = toolIdList.split(",");
        List<String> result = new ArrayList<>();
        for (String s : arr) {
            s = s.trim();
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 根據 recipeId 的後綴，將每個 tool 與相對應的 chamber 組合起來。
     * <p>
     * RecipeId 格式為：xxx.xx-xxxx.xxxx-{...}，後綴的 {} 可能為：
     * (1) {c}              -> 返回 ["%%"]
     * (2) {cEF}            -> 返回 ["E", "F"]
     * (3) {cEF}{c134}      -> 返回 ["E", "F", "1", "3", "4"]
     * (4) {c(3;2)}         -> 返回 ["3", "2"]
     * (5) 沒有大括號      -> 不做變換，直接返回 toolList
     * <p>
     * 最後，對於每個 tool，依序與所有擴展值組合成 "tool#chamber" 字串。
     */
    public static List<String> parsingChamber(List<String> toolList, String recipeId) {
        if (recipeId == null || !recipeId.contains("{")) {
            return toolList;
        }

        List<String> expansions = new ArrayList<>();
        // 使用正則表達式取得所有 {} 中的內容
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(recipeId);
        while (matcher.find()) {
            String content = matcher.group(1);  // 例如 "c", "cEF", "c(3;2)", "c134", etc.
            if (content.startsWith("c")) {
                content = content.substring(1); // 移除前導的 "c"
            }
            content = content.trim();
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

        // 若未找到任何 expansion，返回原始 toolList
        if (expansions.isEmpty()) {
            return toolList;
        }

        // 與每個 tool 組合，形成 "tool#expansion" 的格式
        List<String> result = new ArrayList<>();
        for (String tool : toolList) {
            for (String exp : expansions) {
                result.add(tool + "#" + exp);
            }
        }
        return result;
    }
}
