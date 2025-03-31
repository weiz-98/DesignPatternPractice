package com.example.demo.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParsingUtilTest {

    @Test
    void splitToolList_nullOrEmpty() {
        assertTrue(ParsingUtil.splitToolList(null).isEmpty());
        assertTrue(ParsingUtil.splitToolList("").isEmpty());
        assertTrue(ParsingUtil.splitToolList("   ").isEmpty());
    }

    @Test
    void splitToolList_normal() {
        List<String> expected = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        assertEquals(expected, ParsingUtil.splitToolList("JDTM16, JDTM17, JDTM20"));
    }

    @Test
    void parsingChamber_noBrackets() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        assertEquals(tools, ParsingUtil.parsingChamber(tools, "xxx.xx-xxxx.xxxx-"));
    }

    @Test
    void parsingChamber_case1_bracketsEmpty() {
        List<String> tools = Arrays.asList("JDTM16", "JDTM17", "JDTM20");
        String recipeId = "xxx.xx-xxxx.xxxx-{c}";
        List<String> expected = Arrays.asList("JDTM16#%%", "JDTM17#%%", "JDTM20#%%");
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
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
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
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
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
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
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
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
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
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
        assertEquals(expected, ParsingUtil.parsingChamber(tools, recipeId));
    }
}
