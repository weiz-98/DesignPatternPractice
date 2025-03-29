package com.example.demo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingUtil {
    /**
     * 解析 recipeId => 找到 bracket {c...} => 依規則產生多個 tool#chamber.
     * (以下為簡化示範，可依需求擴充.)
     */
    public static List<String> parsingChamber(List<String> toolList, String recipeId) {
        // 若沒 recipeId => 直接回傳不改
        if (recipeId == null || recipeId.trim().isEmpty()) {
            return toolList;
        }
        List<String> expansions = parseBrackets(recipeId);
        if (expansions.isEmpty()) {
            return toolList; // 沒 {c...} 就不變
        }

        // 有 expansions => 組合
        List<String> result = new ArrayList<>();
        for (String chamber : expansions) {
            for (String tool : toolList) {
                if ("%%".equals(chamber)) {
                    // {c} => #%%
                    result.add(tool + "#%%");
                } else {
                    // 其他 => #chamber
                    result.add(tool + "#" + chamber);
                }
            }
        }
        return result;
    }

    /**
     * 正則找出 {c...}，並解析成 expansions (ex: {cEF} => ["E","F"], {c(5;6)} => ["5","6"], {c{3;2}} => ["3","2"], {c} => ["%%"])
     */
    public static List<String> parseBrackets(String recipeId) {
        Pattern pattern = Pattern.compile("\\{c(.*?)\\}");
        Matcher matcher = pattern.matcher(recipeId);

        List<String> expansions = new ArrayList<>();
        while (matcher.find()) {
            String insideC = matcher.group(1); // ex: "", "EF", "(5;6)", "{3;2}"
            if (insideC == null) continue;
            insideC = insideC.trim();

            if (insideC.isEmpty()) {
                expansions.add("%%"); // {c} => "%%"
                continue;
            }
            // 大括號型 {c{3;2}}
            if (insideC.startsWith("{") && insideC.endsWith("}")) {
                String content = insideC.substring(1, insideC.length() - 1);
                String[] arr = content.split(";");
                expansions.addAll(Arrays.asList(arr));
                continue;
            }
            // 小括號型 {c(5;6)}
            if (insideC.startsWith("(") && insideC.endsWith(")")) {
                String content = insideC.substring(1, insideC.length() - 1);
                String[] arr = content.split(";");
                expansions.addAll(Arrays.asList(arr));
                continue;
            }
            // 一般型 ex: {cEF} => "EF" => ['E','F']
            char[] chars = insideC.toCharArray();
            for (char c : chars) {
                expansions.add(String.valueOf(c));
            }
        }
        return expansions;
    }
    /**
     * 把 "JDTM16,JDTM17,JDTM20" 拆成 ["JDTM16","JDTM17","JDTM20"]
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
}
