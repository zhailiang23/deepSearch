package com.ynet.mgmt.queryunderstanding.processor.preprocessing;

import com.ynet.mgmt.queryunderstanding.context.QueryContext;
import com.ynet.mgmt.queryunderstanding.processor.AbstractQueryProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 文本标准化处理器
 * 负责清洗和标准化查询文本：
 * - 去除多余空白
 * - 去除特殊字符（保留中文、英文、数字）
 * - 转换全角字符为半角
 * - 统一大小写（可选）
 *
 * @author deepSearch
 * @since 2025-01-16
 */
@Slf4j
@Component
public class TextNormalizationProcessor extends AbstractQueryProcessor {

    /**
     * 特殊字符正则模式（保留中文、英文、数字、常用标点）
     */
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s\\u3000-\\u303F\\uFF00-\\uFFEF.,;:?!()\\[\\]<>-]");

    /**
     * 多个空白字符的正则模式
     */
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");

    /**
     * 是否转换为小写
     */
    private boolean convertToLowerCase = false;

    public TextNormalizationProcessor() {
        this.priority = 100; // 最高优先级，第一个执行
        this.timeout = 500L; // 500ms 超时
    }

    @Override
    protected void doProcess(QueryContext context) {
        String query = context.getOriginalQuery();
        if (query == null || query.isEmpty()) {
            log.debug("原始查询为空，跳过标准化");
            return;
        }

        String normalized = query;

        // 1. 去除前后空白
        normalized = normalized.trim();

        // 2. 全角转半角
        normalized = convertFullWidthToHalfWidth(normalized);

        // 3. 去除特殊字符（可配置）
        // normalized = removeSpecialChars(normalized);

        // 4. 合并多个连续空白为单个空格
        normalized = MULTIPLE_SPACES_PATTERN.matcher(normalized).replaceAll(" ");

        // 5. 转换为小写（可配置）
        if (convertToLowerCase) {
            normalized = toLowerCase(normalized);
        }

        context.setNormalizedQuery(normalized);
        log.debug("文本标准化完成: \"{}\" -> \"{}\"", query, normalized);
    }

    /**
     * 全角字符转半角字符
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    private String convertFullWidthToHalfWidth(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 全角空格转换为半角空格
            if (c == '\u3000') {
                sb.append(' ');
            }
            // 其他全角字符（0xFF01-0xFF5E）转换为半角（0x21-0x7E）
            else if (c >= '\uFF01' && c <= '\uFF5E') {
                sb.append((char) (c - 0xFEE0));
            }
            // 其他字符不变
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 去除特殊字符
     *
     * @param input 输入字符串
     * @return 处理后的字符串
     */
    private String removeSpecialChars(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return SPECIAL_CHARS_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * 智能转换小写（保留中文）
     *
     * @param input 输入字符串
     * @return 转换后的字符串
     */
    private String toLowerCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 只转换英文字符为小写，保留中文
        return input.toLowerCase();
    }

    @Override
    public String getName() {
        return "TextNormalizationProcessor";
    }

    /**
     * 设置是否转换为小写
     *
     * @param convertToLowerCase true 表示转换
     */
    public void setConvertToLowerCase(boolean convertToLowerCase) {
        this.convertToLowerCase = convertToLowerCase;
    }
}
