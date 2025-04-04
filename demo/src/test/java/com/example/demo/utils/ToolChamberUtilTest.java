package com.example.demo.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ToolChamberUtilTest {

    @Test
    void splitToolList_nullOrEmpty() {
        assertTrue(ToolChamberUtil.splitToolList(null).isEmpty());
        assertTrue(ToolChamberUtil.splitToolList("").isEmpty());
        assertTrue(ToolChamberUtil.splitToolList("   ").isEmpty());
    }

    @Test
    void splitToolList_normal() {
        List<String> expected = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        assertEquals(expected, ToolChamberUtil.splitToolList("JDTM16, JDTM17, JDTM20"));
    }

    @Test
    void parsingChamber_noBrackets() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        assertEquals(tools, ToolChamberUtil.parsingChamber(tools, "xxx.xx-xxxx.xxxx-"));
    }

    @Test
    void parsingChamber_case1_bracketsEmpty() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{c}";
        List<String> expected = Arrays.asList("JDTM16#%%", "JDTM17#%%", "JDTM20#%%");
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    @Test
    void parsingChamber_case2_singleLetters() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{cEF}";
        List<String> expected = Arrays.asList(
                "JDTM16#E", "JDTM16#F",
                "JDTM17#E", "JDTM17#F",
                "JDTM20#E", "JDTM20#F"
        );
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    @Test
    void parsingChamber_case3_multipleGroupsWithoutParen() {
        // 情形 (3): {cEF}{c134}{cAB}
        // {cEF} -> ["E","F"]
        // {c134} -> ["1","3","4"]
        // {cAB} -> ["A","B"]
        // 最終 expansions = ["E", "F", "1", "3", "4", "A", "B"]
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{cEF}{c134}{cAB}";
        List<String> expected = Arrays.asList(
                // For JDTM16
                "JDTM16#E", "JDTM16#F", "JDTM16#1", "JDTM16#3", "JDTM16#4", "JDTM16#A", "JDTM16#B",
                // For JDTM17
                "JDTM17#E", "JDTM17#F", "JDTM17#1", "JDTM17#3", "JDTM17#4", "JDTM17#A", "JDTM17#B",
                // For JDTM20
                "JDTM20#E", "JDTM20#F", "JDTM20#1", "JDTM20#3", "JDTM20#4", "JDTM20#A", "JDTM20#B"
        );
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    @Test
    void parsingChamber_case4_multipleGroupsWithParen() {
        // 情形 (4): {c(3;2)}  -> 返回 ["3", "2"]
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{c(3;2)}";
        List<String> expected = Arrays.asList(
                "JDTM16#3", "JDTM16#2",
                "JDTM17#3", "JDTM17#2",
                "JDTM20#3", "JDTM20#2"
        );
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    @Test
    void parsingChamber_caseExtraMultipleParens() {
        // 模擬情形：{c(2;3)}{c(A;C)}{c(B;D)}
        // {c(2;3)} -> ["2", "3"]
        // {c(A;C)} -> ["A", "C"]
        // {c(B;D)} -> ["B", "D"]
        // 最終 expansions = ["2", "3", "A", "C", "B", "D"]
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{c(2;3)}{c(A;C)}{c(B;D)}";
        List<String> expected = Arrays.asList(
                // For JDTM16
                "JDTM16#2", "JDTM16#3", "JDTM16#A", "JDTM16#C", "JDTM16#B", "JDTM16#D",
                // For JDTM17
                "JDTM17#2", "JDTM17#3", "JDTM17#A", "JDTM17#C", "JDTM17#B", "JDTM17#D",
                // For JDTM20
                "JDTM20#2", "JDTM20#3", "JDTM20#A", "JDTM20#C", "JDTM20#B", "JDTM20#D"
        );
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    @Test
    void parsingChamber_case5_singleGroupLetters() {
        // 情形 (5): {cEF} -> 返回 ["E", "F"]
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{cEF}";
        List<String> expected = Arrays.asList(
                "JDTM16#E", "JDTM16#F",
                "JDTM17#E", "JDTM17#F",
                "JDTM20#E", "JDTM20#F"
        );
        assertEquals(expected, ToolChamberUtil.parsingChamber(tools, recipeId));
    }

    /**
     * 測試場景：
     * 1. 沒有括號 => 每個 tool 產生 expansions=[ [ ] ] (空list => 不指定 chamber)
     * 2. {c} => expansions=[ ["%%"] ]
     * 3. {cEF}{c134} => expansions=[ ["E","F"], ["1","3","4"] ]
     * 4. {c(3;2)} => expansions=[ ["3","2"] ]
     * 5. {cEF} 單大括號多字母 => expansions=[ ["E","F"] ]
     */
    @Test
    void parseChamberGrouped_noBracket() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);

        log.info("noBracket result: {}", result);
        assertEquals(3, result.size());
        for (String tool : tools) {
            assertTrue(result.containsKey(tool));
            List<List<String>> expansions = result.get(tool);
            // expansions 只有1個 bracket，且該 bracket 是 emptyList => 代表 "不指定 chamber"
            assertEquals(1, expansions.size());
            assertTrue(expansions.getFirst().isEmpty());
        }
    }

    @Test
    void parseChamberGrouped_case1_bracketsEmpty() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17");
        String recipeId = "xxx.xx-xxxx.xxxx-{c}";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);

        log.info("{c} result: {}", result);
        for (String tool : tools) {
            assertTrue(result.containsKey(tool));
            List<List<String>> bracketExps = result.get(tool);
            assertEquals(1, bracketExps.size());
            assertEquals(Collections.singletonList("%%"), bracketExps.get(0));
        }
    }

    @Test
    void parseChamberGrouped_case2_multipleBracketsNoParen() {
        List<String> tools = Arrays.asList("T1", "T2");
        String recipeId = "xxx.xx-xxxx.xxxx-{cEF}{c134}";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);

        log.info("{cEF}{c134} result: {}", result);
        for (String t : tools) {
            List<List<String>> expansions = result.get(t);
            assertEquals(2, expansions.size());
            assertEquals(Arrays.asList("E", "F"), expansions.get(0));
            assertEquals(Arrays.asList("1", "3", "4"), expansions.get(1));
        }
    }

    @Test
    void parseChamberGrouped_case3_hasParen() {
        List<String> tools = Collections.singletonList("JDTM16");
        String recipeId = "xxx.xx-xxxx.xxxx-{c(3;2;4)}";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);

        log.info("{c(3;2;4)} result: {}", result);
        assertTrue(result.containsKey("JDTM16"));
        List<List<String>> expansions = result.get("JDTM16");
        assertEquals(1, expansions.size());
        assertEquals(Arrays.asList("3", "2", "4"), expansions.get(0));
    }

    @Test
    void parseChamberGrouped_case4_singleBracketLetters() {
        List<String> tools = Arrays.asList("T1", "T2", "T3");
        String recipeId = "xxx.xx-xxxx.xxxx-{cEF}";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);

        log.info("{cEF} result: {}", result);
        for (String t : tools) {
            List<List<String>> expansions = result.get(t);
            assertEquals(1, expansions.size());
            assertEquals(Arrays.asList("E", "F"), expansions.get(0));
        }
    }

    @Test
    void parseChamberGrouped_caseExtra() {
        List<String> tools = Arrays.asList("T1", "T2");
        String recipeId = "xxx.xx-xxxx.xxxx-{c(2;3)}{c(A;C)}{c(B;D)}";
        Map<String, List<List<String>>> result = ToolChamberUtil.parseChamberGrouped(tools, recipeId);
        log.info("{c(2;3)}{c(A;C)}{c(B;D)} result: {}", result);
        for (String t : tools) {
            List<List<String>> expansions = result.get(t);
            assertEquals(3, expansions.size());

            assertEquals(Arrays.asList("2", "3"), expansions.get(0));
            assertEquals(Arrays.asList("A", "C"), expansions.get(1));
            assertEquals(Arrays.asList("B", "D"), expansions.get(2));
        }
    }
}
