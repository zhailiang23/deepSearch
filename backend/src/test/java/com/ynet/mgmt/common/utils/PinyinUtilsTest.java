package com.ynet.mgmt.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PinyinUtils 工具类测试
 */
class PinyinUtilsTest {

    @Test
    void testToPinyin() {
        // 测试基本拼音转换
        assertEquals("quxian", PinyinUtils.toPinyin("取现"));
        assertEquals("zhuanzhang", PinyinUtils.toPinyin("转账"));
        assertEquals("yue", PinyinUtils.toPinyin("余额"));
        assertEquals("quxian", PinyinUtils.toPinyin("曲线"));

        // 测试混合字符
        assertEquals("quxian123", PinyinUtils.toPinyin("取现123"));
        assertEquals("abc", PinyinUtils.toPinyin("abc"));

        // 测试空字符串
        assertEquals("", PinyinUtils.toPinyin(""));
        assertEquals("", PinyinUtils.toPinyin(null));
    }

    @Test
    void testToPinyinFirstLetter() {
        // 测试首字母提取
        assertEquals("qx", PinyinUtils.toPinyinFirstLetter("取现"));
        assertEquals("zz", PinyinUtils.toPinyinFirstLetter("转账"));
        assertEquals("ye", PinyinUtils.toPinyinFirstLetter("余额"));
        assertEquals("qx", PinyinUtils.toPinyinFirstLetter("曲线"));

        // 测试混合字符
        assertEquals("qx123", PinyinUtils.toPinyinFirstLetter("取现123"));
        assertEquals("abc", PinyinUtils.toPinyinFirstLetter("abc"));

        // 测试空字符串
        assertEquals("", PinyinUtils.toPinyinFirstLetter(""));
        assertEquals("", PinyinUtils.toPinyinFirstLetter(null));
    }

    @Test
    void testContainsChinese() {
        // 测试包含中文
        assertTrue(PinyinUtils.containsChinese("取现"));
        assertTrue(PinyinUtils.containsChinese("abc取现123"));

        // 测试不包含中文
        assertFalse(PinyinUtils.containsChinese("abc123"));
        assertFalse(PinyinUtils.containsChinese(""));
        assertFalse(PinyinUtils.containsChinese(null));
    }

    @Test
    void testIsPossiblyPinyin() {
        // 测试可能是拼音
        assertTrue(PinyinUtils.isPossiblyPinyin("quxian"));
        assertTrue(PinyinUtils.isPossiblyPinyin("qx"));
        assertTrue(PinyinUtils.isPossiblyPinyin("abc"));

        // 测试不是拼音
        assertFalse(PinyinUtils.isPossiblyPinyin("取现"));
        assertFalse(PinyinUtils.isPossiblyPinyin("quxian123"));
        assertFalse(PinyinUtils.isPossiblyPinyin(""));
        assertFalse(PinyinUtils.isPossiblyPinyin(null));
    }

    @Test
    void testToPinyinAllCombinations() {
        // 测试单音字
        String[] result1 = PinyinUtils.toPinyinAllCombinations("取", 10);
        assertEquals(1, result1.length);
        assertEquals("qu", result1[0]);

        // 测试混合字符
        String[] result2 = PinyinUtils.toPinyinAllCombinations("取现", 10);
        assertTrue(result2.length >= 1);

        // 测试空字符串
        String[] result3 = PinyinUtils.toPinyinAllCombinations("", 10);
        assertEquals(0, result3.length);
    }
}
