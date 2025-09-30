package com.ynet.mgmt.sensitiveWord.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 敏感词检测器
 * 使用AC自动机（Aho-Corasick）算法实现高效的多模式字符串匹配
 *
 * 算法特点：
 * - 时间复杂度：O(n)，n为待检测文本长度
 * - 支持一次性检测多个敏感词
 * - 支持子串匹配
 *
 * @author system
 * @since 1.0.0
 */
public class SensitiveWordDetector {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordDetector.class);

    /**
     * Trie树的根节点
     */
    private final TrieNode root;

    /**
     * 敏感词列表（用于快速查询）
     */
    private final Set<String> sensitiveWords;

    /**
     * 构造函数
     *
     * @param words 敏感词列表
     */
    public SensitiveWordDetector(Collection<String> words) {
        this.root = new TrieNode();
        this.sensitiveWords = new HashSet<>(words);
        buildTrie(words);
        buildFailureLinks();
        log.info("敏感词检测器初始化完成，共加载 {} 个敏感词", words.size());
    }

    /**
     * 检测文本中是否包含敏感词
     *
     * @param text 待检测文本
     * @return 检测结果
     */
    public DetectionResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return DetectionResult.pass();
        }

        List<String> detectedWords = new ArrayList<>();
        TrieNode current = root;

        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));

            // 沿着失败链接查找匹配
            while (current != root && !current.children.containsKey(c)) {
                current = current.fail;
            }

            // 移动到下一个节点
            if (current.children.containsKey(c)) {
                current = current.children.get(c);
            }

            // 检查当前节点及其失败链接上是否有匹配的词
            TrieNode temp = current;
            while (temp != root) {
                if (temp.isEndOfWord && temp.word != null) {
                    if (!detectedWords.contains(temp.word)) {
                        detectedWords.add(temp.word);
                    }
                }
                temp = temp.fail;
            }
        }

        if (detectedWords.isEmpty()) {
            return DetectionResult.pass();
        } else {
            return DetectionResult.reject(detectedWords);
        }
    }

    /**
     * 快速检查文本是否包含敏感词（不返回具体敏感词）
     *
     * @param text 待检测文本
     * @return 是否包含敏感词
     */
    public boolean contains(String text) {
        return !detect(text).isPassed();
    }

    /**
     * 获取检测器中的敏感词数量
     *
     * @return 敏感词数量
     */
    public int getWordCount() {
        return sensitiveWords.size();
    }

    /**
     * 构建Trie树
     */
    private void buildTrie(Collection<String> words) {
        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }

            TrieNode current = root;
            String lowerWord = word.toLowerCase().trim();

            for (char c : lowerWord.toCharArray()) {
                current = current.children.computeIfAbsent(c, k -> new TrieNode());
            }

            current.isEndOfWord = true;
            current.word = lowerWord;
        }
    }

    /**
     * 构建失败链接（AC自动机的核心）
     * 使用BFS遍历Trie树，为每个节点建立失败链接
     */
    private void buildFailureLinks() {
        Queue<TrieNode> queue = new LinkedList<>();

        // 根节点的所有子节点的失败链接指向根节点
        for (TrieNode child : root.children.values()) {
            child.fail = root;
            queue.offer(child);
        }

        // BFS遍历构建失败链接
        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();

            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();

                // 沿着父节点的失败链接查找
                TrieNode fail = current.fail;
                while (fail != root && !fail.children.containsKey(c)) {
                    fail = fail.fail;
                }

                // 设置失败链接
                if (fail.children.containsKey(c) && fail.children.get(c) != child) {
                    child.fail = fail.children.get(c);
                } else {
                    child.fail = root;
                }

                queue.offer(child);
            }
        }
    }

    /**
     * Trie树节点
     */
    private static class TrieNode {
        /**
         * 子节点映射
         */
        Map<Character, TrieNode> children = new HashMap<>();

        /**
         * 失败指针（AC自动机）
         */
        TrieNode fail = null;

        /**
         * 是否为词的结尾
         */
        boolean isEndOfWord = false;

        /**
         * 如果是词的结尾，存储完整的词
         */
        String word = null;
    }

    /**
     * 检测结果
     */
    public static class DetectionResult {
        /**
         * 是否通过检测（不包含敏感词）
         */
        private final boolean passed;

        /**
         * 检测到的敏感词列表
         */
        private final List<String> detectedWords;

        private DetectionResult(boolean passed, List<String> detectedWords) {
            this.passed = passed;
            this.detectedWords = detectedWords != null ? Collections.unmodifiableList(detectedWords) : Collections.emptyList();
        }

        /**
         * 创建通过检测的结果
         */
        public static DetectionResult pass() {
            return new DetectionResult(true, Collections.emptyList());
        }

        /**
         * 创建检测失败的结果
         */
        public static DetectionResult reject(List<String> detectedWords) {
            return new DetectionResult(false, detectedWords);
        }

        public boolean isPassed() {
            return passed;
        }

        public List<String> getDetectedWords() {
            return detectedWords;
        }

        public boolean hasDetectedWords() {
            return !detectedWords.isEmpty();
        }

        @Override
        public String toString() {
            return "DetectionResult{" +
                    "passed=" + passed +
                    ", detectedWords=" + detectedWords +
                    '}';
        }
    }
}