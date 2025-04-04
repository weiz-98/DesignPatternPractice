package com.example.demo.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ParsingUtil 提供與 Recipe ID 解析、Tool List 分割等相關的靜態工具方法。
 * 其中重點為 parsingChamber 及 parseChamberGrouped，
 * 用於將 recipe ID (含 {} ) 轉換成對應的 tool-chamber 組合 (平鋪 or AND/OR 分組)。
 */
public class ToolChamberUtil {

    private static final Pattern EXPANSION_PATTERN = Pattern.compile("\\{(.*?)\\}");

    /**
     * 將逗號分隔的 toolIdList 字串分割為清單。
     * 例如 "JDTM16, JDTM17, JDTM20" -> ["JDTM16","JDTM17","JDTM20"]。
     */
    public static List<String> splitToolList(String toolIdList) {
        if (toolIdList == null || toolIdList.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Stream.of(toolIdList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------------------------
    // 1) parsingChamber: 將 recipeId 中每個大括號內容直接平舖 (適用於單純的 “平鋪” 功能)
    // --------------------------------------------------------------------------

    /**
     * 依據 recipeId 中的 {} 格式，將 toolList 與擴展出來的 chamber 逐一組合（平鋪）。
     * 若無大括號，直接回傳原 toolList（不帶 #chamber）。
     * <p>
     * 例如：
     * (1) {c} => 最終會將每個 tool + "#%%"
     * (2) {cEF} => 會展開成 E 與 F => tool + "#E" 以及 tool + "#F"
     * (3) {cEF}{c134} => 會先萃取 E,F,1,3,4 平舖
     * (4) {c(3;2)} => ["3","2"]
     * (5) 無大括號 => 原樣不動
     */
    public static List<String> parsingChamber(List<String> toolList, String recipeId) {
        if (recipeId == null || !recipeId.contains("{")) {
            return toolList;
        }

        List<String> expansions = extractExpansions(recipeId);
        if (expansions.isEmpty()) {
            return toolList;
        }

        // 依 “tool + # + expansion” 逐一組合
        List<String> result = new ArrayList<>();
        for (String tool : toolList) {
            for (String exp : expansions) {
                result.add(tool + "#" + exp);
            }
        }
        return result;
    }

    /**
     * 從 recipeId 中萃取所有大括號內容，再依不同格式 ("", "(...)", 其他) 拆分為多個字串。
     *
     * @return e.g. {cEF}{c134} => ["E","F","1","3","4"]
     */
    private static List<String> extractExpansions(String recipeId) {
        List<String> expansions = new ArrayList<>();
        Matcher matcher = EXPANSION_PATTERN.matcher(recipeId);
        while (matcher.find()) {
            // 取出大括號內的字串 (包含可能的 'c')
            String content = matcher.group(1);
            // 移除前導的 'c'
            if (content.startsWith("c")) {
                content = content.substring(1).trim();
            }
            // 依格式決定如何拆
            if (content.isEmpty()) {
                // {c} => "%%"
                expansions.add("%%");
            } else if (content.startsWith("(") && content.endsWith(")")) {
                // {c(3;2)} => -> ["3","2"]
                String inner = content.substring(1, content.length() - 1);
                for (String part : inner.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        expansions.add(trimmed);
                    }
                }
            } else {
                // e.g. "EF" => => ["E","F"]
                for (char ch : content.toCharArray()) {
                    expansions.add(String.valueOf(ch));
                }
            }
        }
        return expansions;
    }

    // --------------------------------------------------------------------------
    // 2) parseChamberGrouped: 返回 AND/OR 結構
    // --------------------------------------------------------------------------

    /**
     * 產生針對每個 tool 都有一組 bracketExpansions (AND)，
     * 其中每個 bracketExpansions[i] 內的字串列表表示 OR。
     * <p>
     * 例如 {cEF}{c134} => bracketExpansions=[ ["E","F"], ["1","3","4"] ]，
     * 對 toolList 中每個 tool 都返回相同的 bracketExpansions。
     * <p>
     * 若 recipeId 無大括號，代表不限定 chamber => expansions=[ [ ] ] (空 => 只檢查 tool 即可)。
     */
    public static Map<String, List<List<String>>> parseChamberGrouped(List<String> toolList, String recipeId) {
        // case: 無大括號 => 不限定 chamber
        if (recipeId == null || !recipeId.contains("{")) {
            return buildGroupedResultForNoBracket(toolList);
        }

        // 萃取 bracketExpansions => AND
        List<List<String>> bracketExpansions = extractBracketExpansionsGrouped(recipeId);

        // 每個 tool 都對應相同 bracketExpansions
        Map<String, List<List<String>>> result = new HashMap<>();
        for (String tool : toolList) {
            result.put(tool, bracketExpansions);
        }
        return result;
    }

    /**
     * 無大括號時，對於每個 tool => expansions=[ [ ] ]，表示一個 bracket (AND=1)，
     * 且其中 OR list 為 empty => 代表不指定 chamber。
     */
    private static Map<String, List<List<String>>> buildGroupedResultForNoBracket(List<String> toolList) {
        Map<String, List<List<String>>> result = new HashMap<>();
        for (String tool : toolList) {
            // bracketExpansions=[ single bracket => empty list => no chamber ]
            List<List<String>> expansions = new ArrayList<>();
            expansions.add(Collections.emptyList());
            result.put(tool, expansions);
        }
        return result;
    }

    /**
     * 萃取多個大括號 => 每個大括號內的字元列表就是 OR => 彼此之間 AND。
     * e.g. {cEF}{c134} => bracketExpansions=[ ["E","F"], ["1","3","4"] ]。
     */
    private static List<List<String>> extractBracketExpansionsGrouped(String recipeId) {
        List<List<String>> bracketExpansions = new ArrayList<>();
        Matcher matcher = EXPANSION_PATTERN.matcher(recipeId);
        while (matcher.find()) {
            String content = matcher.group(1);
            if (content.startsWith("c")) {
                content = content.substring(1).trim();
            }
            bracketExpansions.add(parseOneBracketForGroup(content));
        }
        return bracketExpansions;
    }

    /**
     * 解析單一大括號 => OR 列表。
     * - 若 content="" => ["%%"]
     * - 若 content="(3;2)" => => ["3","2"]
     * - 否則逐字拆
     */
    private static List<String> parseOneBracketForGroup(String content) {
        if (content.isEmpty()) {
            return Collections.singletonList("%%");
        }
        if (content.startsWith("(") && content.endsWith(")")) {
            String inner = content.substring(1, content.length() - 1);
            List<String> list = new ArrayList<>();
            for (String p : inner.split(";")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
            return list;
        }
        // e.g. "EF" => ["E","F"]
        List<String> list = new ArrayList<>();
        for (char ch : content.toCharArray()) {
            list.add(String.valueOf(ch));
        }
        return list;
    }
}
