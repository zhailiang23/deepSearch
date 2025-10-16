package com.ynet.mgmt.common.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.util.StringUtils;

/**
 * 拼音转换工具类
 * 提供中文转拼音的功能，支持全拼、简拼和首字母提取
 *
 * @author system
 * @since 1.0.0
 */
public class PinyinUtils {

    /**
     * 拼音输出格式配置
     */
    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        // 设置为小写
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        // 不带声调
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        // ü 用 v 表示
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    /**
     * 将中文字符串转换为拼音全拼
     *
     * @param chinese 中文字符串
     * @return 拼音全拼，多音字取第一个读音，非中文字符保留
     * @example "取现" -> "quxian"
     */
    public static String toPinyin(String chinese) {
        if (!StringUtils.hasText(chinese)) {
            return "";
        }

        StringBuilder pinyin = new StringBuilder();
        char[] chars = chinese.toCharArray();

        for (char ch : chars) {
            // 判断是否为汉字
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]+")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        // 取第一个读音（多音字处理）
                        pinyin.append(pinyinArray[0]);
                    } else {
                        // 无法转换的汉字保留原字符
                        pinyin.append(ch);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 转换失败，保留原字符
                    pinyin.append(ch);
                }
            } else {
                // 非汉字字符直接保留（包括字母、数字、符号等）
                pinyin.append(ch);
            }
        }

        return pinyin.toString();
    }

    /**
     * 将中文字符串转换为拼音首字母
     *
     * @param chinese 中文字符串
     * @return 拼音首字母，非中文字符保留
     * @example "取现" -> "qx"
     */
    public static String toPinyinFirstLetter(String chinese) {
        if (!StringUtils.hasText(chinese)) {
            return "";
        }

        StringBuilder firstLetters = new StringBuilder();
        char[] chars = chinese.toCharArray();

        for (char ch : chars) {
            // 判断是否为汉字
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]+")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        // 取第一个读音的首字母
                        String pinyin = pinyinArray[0];
                        if (pinyin.length() > 0) {
                            firstLetters.append(pinyin.charAt(0));
                        }
                    } else {
                        // 无法转换的汉字保留原字符
                        firstLetters.append(ch);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    // 转换失败，保留原字符
                    firstLetters.append(ch);
                }
            } else {
                // 非汉字字符直接保留
                firstLetters.append(ch);
            }
        }

        return firstLetters.toString();
    }

    /**
     * 判断字符串是否包含中文字符
     *
     * @param str 待判断的字符串
     * @return true 如果包含中文，false 否则
     */
    public static boolean containsChinese(String str) {
        if (!StringUtils.hasText(str)) {
            return false;
        }
        return str.matches(".*[\\u4E00-\\u9FA5]+.*");
    }

    /**
     * 判断查询词是否可能是拼音
     * 用于判断是否需要启用拼音搜索
     *
     * @param query 查询词
     * @return true 如果查询词只包含字母（可能是拼音），false 否则
     */
    public static boolean isPossiblyPinyin(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        // 只包含字母（a-z, A-Z）被认为可能是拼音
        return query.matches("^[a-zA-Z]+$");
    }

    /**
     * 获取多音字的所有可能拼音组合
     * 注意：此方法会产生笛卡尔积，字符串过长可能导致组合数爆炸
     *
     * @param chinese 中文字符串
     * @param maxCombinations 最大组合数，超过则只返回第一个读音的组合
     * @return 所有可能的拼音组合
     * @example "取" -> ["qu"]，"乐" -> ["le", "yue"]
     */
    public static String[] toPinyinAllCombinations(String chinese, int maxCombinations) {
        if (!StringUtils.hasText(chinese)) {
            return new String[0];
        }

        char[] chars = chinese.toCharArray();
        java.util.List<String[]> allPinyin = new java.util.ArrayList<>();

        // 获取每个字符的所有拼音
        for (char ch : chars) {
            if (Character.toString(ch).matches("[\\u4E00-\\u9FA5]+")) {
                try {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        allPinyin.add(pinyinArray);
                    } else {
                        allPinyin.add(new String[]{String.valueOf(ch)});
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    allPinyin.add(new String[]{String.valueOf(ch)});
                }
            } else {
                allPinyin.add(new String[]{String.valueOf(ch)});
            }
        }

        // 计算组合数
        int totalCombinations = 1;
        for (String[] pinyin : allPinyin) {
            totalCombinations *= pinyin.length;
            // 防止组合数爆炸
            if (totalCombinations > maxCombinations) {
                // 超过最大组合数，只返回第一个读音的组合
                return new String[]{toPinyin(chinese)};
            }
        }

        // 生成所有组合（笛卡尔积）
        java.util.List<String> combinations = new java.util.ArrayList<>();
        generateCombinations(allPinyin, 0, new StringBuilder(), combinations);

        return combinations.toArray(new String[0]);
    }

    /**
     * 递归生成拼音组合（笛卡尔积）
     */
    private static void generateCombinations(java.util.List<String[]> allPinyin, int index,
                                            StringBuilder current, java.util.List<String> result) {
        if (index == allPinyin.size()) {
            result.add(current.toString());
            return;
        }

        String[] currentPinyin = allPinyin.get(index);
        for (String pinyin : currentPinyin) {
            int currentLength = current.length();
            current.append(pinyin);
            generateCombinations(allPinyin, index + 1, current, result);
            current.setLength(currentLength); // 回溯
        }
    }
}
