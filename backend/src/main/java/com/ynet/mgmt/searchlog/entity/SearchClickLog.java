package com.ynet.mgmt.searchlog.entity;

import com.ynet.mgmt.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 搜索点击日志实体类
 * 记录用户对搜索结果的点击行为，支持一次搜索多次点击记录
 *
 * @author Claude Code AI
 * @version 1.0
 * @since 2025-09-27
 */
@Entity
@Table(name = "search_click_logs", indexes = {
    @Index(name = "idx_search_click_logs_search_log_id", columnList = "search_log_id"),
    @Index(name = "idx_search_click_logs_click_time", columnList = "click_time"),
    @Index(name = "idx_search_click_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_search_click_logs_document_id", columnList = "document_id"),
    @Index(name = "idx_search_click_logs_search_click_seq", columnList = "search_log_id, click_sequence"),
    @Index(name = "idx_search_click_logs_user_click_time", columnList = "user_id, click_time DESC")
})
@Comment("搜索点击日志表")
public class SearchClickLog extends BaseEntity {

    /**
     * 关联的搜索日志
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_log_id", nullable = false, foreignKey = @ForeignKey(name = "fk_search_click_logs_search_log"))
    @Comment("关联搜索日志ID")
    @NotNull(message = "关联搜索日志不能为空")
    private SearchLog searchLog;

    /**
     * 点击的文档ID
     */
    @Column(name = "document_id", length = 200, nullable = false)
    @Comment("点击的文档ID")
    @NotBlank(message = "文档ID不能为空")
    private String documentId;

    /**
     * 文档索引名称
     */
    @Column(name = "document_index", length = 100)
    @Comment("文档索引名称")
    private String documentIndex;

    /**
     * 文档标题
     */
    @Lob
    @Column(name = "document_title")
    @Comment("文档标题")
    private String documentTitle;

    /**
     * 点击位置（第几条结果，从1开始）
     */
    @Column(name = "click_position", nullable = false)
    @Comment("点击位置（第几条结果，从1开始）")
    @Min(value = 1, message = "点击位置必须大于0")
    @NotNull(message = "点击位置不能为空")
    private Integer clickPosition;

    /**
     * 点击文档的相关性分数
     */
    @Column(name = "click_score", precision = 10, scale = 6)
    @Comment("点击文档的相关性分数")
    private BigDecimal clickScore;

    /**
     * 同一次搜索中的点击顺序（1,2,3...）
     */
    @Column(name = "click_sequence", nullable = false)
    @Comment("同一次搜索中的点击顺序（1,2,3...）")
    @Min(value = 1, message = "点击顺序必须大于0")
    @NotNull(message = "点击顺序不能为空")
    private Integer clickSequence;

    /**
     * 具体点击时间
     */
    @Column(name = "click_time", nullable = false)
    @Comment("具体点击时间")
    @NotNull(message = "点击时间不能为空")
    private LocalDateTime clickTime;

    /**
     * 点击用户ID
     */
    @Column(name = "user_id")
    @Comment("点击用户ID")
    private Long userId;

    /**
     * 用户会话ID
     */
    @Column(name = "session_id", length = 64)
    @Comment("用户会话ID")
    private String sessionId;

    /**
     * 点击IP地址
     */
    @Column(name = "ip_address", length = 45)
    @Comment("点击IP地址")
    private String ipAddress;

    // ========== 构造函数 ==========

    /**
     * 默认构造函数
     */
    public SearchClickLog() {
    }

    /**
     * 构造函数
     *
     * @param searchLog      关联的搜索日志
     * @param documentId     文档ID
     * @param clickPosition  点击位置
     * @param clickSequence  点击顺序
     */
    public SearchClickLog(SearchLog searchLog, String documentId, Integer clickPosition, Integer clickSequence) {
        this.searchLog = searchLog;
        this.documentId = documentId;
        this.clickPosition = clickPosition;
        this.clickSequence = clickSequence;
        this.clickTime = LocalDateTime.now();
    }

    /**
     * 构造函数
     *
     * @param searchLog      关联的搜索日志
     * @param documentId     文档ID
     * @param documentTitle  文档标题
     * @param clickPosition  点击位置
     * @param clickSequence  点击顺序
     * @param clickScore     点击分数
     */
    public SearchClickLog(SearchLog searchLog, String documentId, String documentTitle,
                         Integer clickPosition, Integer clickSequence, BigDecimal clickScore) {
        this.searchLog = searchLog;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.clickPosition = clickPosition;
        this.clickSequence = clickSequence;
        this.clickScore = clickScore;
        this.clickTime = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 判断是否为第一次点击
     *
     * @return 是否为第一次点击
     */
    public boolean isFirstClick() {
        return clickSequence != null && clickSequence == 1;
    }

    /**
     * 判断是否为热门位置点击（前3位）
     *
     * @return 是否为热门位置
     */
    public boolean isTopPosition() {
        return clickPosition != null && clickPosition <= 3;
    }

    /**
     * 获取点击时间与搜索日志创建时间的间隔（秒）
     *
     * @return 时间间隔（秒）
     */
    public long getClickDelaySeconds() {
        if (clickTime == null || searchLog == null || searchLog.getCreatedAt() == null) {
            return 0;
        }
        return java.time.Duration.between(searchLog.getCreatedAt(), clickTime).getSeconds();
    }

    /**
     * 判断是否为快速点击（5秒内）
     *
     * @return 是否为快速点击
     */
    public boolean isQuickClick() {
        return getClickDelaySeconds() <= 5;
    }

    /**
     * 设置用户信息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param ipAddress IP地址
     */
    public void setUserInfo(Long userId, String sessionId, String ipAddress) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
    }

    /**
     * 更新点击时间为当前时间
     */
    public void updateClickTime() {
        this.clickTime = LocalDateTime.now();
    }

    // ========== Getter和Setter ==========

    public SearchLog getSearchLog() {
        return searchLog;
    }

    public void setSearchLog(SearchLog searchLog) {
        this.searchLog = searchLog;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentIndex() {
        return documentIndex;
    }

    public void setDocumentIndex(String documentIndex) {
        this.documentIndex = documentIndex;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public Integer getClickPosition() {
        return clickPosition;
    }

    public void setClickPosition(Integer clickPosition) {
        this.clickPosition = clickPosition;
    }

    public BigDecimal getClickScore() {
        return clickScore;
    }

    public void setClickScore(BigDecimal clickScore) {
        this.clickScore = clickScore;
    }

    public Integer getClickSequence() {
        return clickSequence;
    }

    public void setClickSequence(Integer clickSequence) {
        this.clickSequence = clickSequence;
    }

    public LocalDateTime getClickTime() {
        return clickTime;
    }

    public void setClickTime(LocalDateTime clickTime) {
        this.clickTime = clickTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    // ========== equals, hashCode, toString ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SearchClickLog that = (SearchClickLog) o;
        return Objects.equals(searchLog, that.searchLog) &&
               Objects.equals(documentId, that.documentId) &&
               Objects.equals(clickPosition, that.clickPosition) &&
               Objects.equals(clickSequence, that.clickSequence) &&
               Objects.equals(clickTime, that.clickTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), searchLog, documentId, clickPosition, clickSequence, clickTime);
    }

    @Override
    public String toString() {
        return "SearchClickLog{" +
               "id=" + getId() +
               ", searchLogId=" + (searchLog != null ? searchLog.getId() : null) +
               ", documentId='" + documentId + '\'' +
               ", documentTitle='" + documentTitle + '\'' +
               ", clickPosition=" + clickPosition +
               ", clickSequence=" + clickSequence +
               ", clickTime=" + clickTime +
               ", userId=" + userId +
               ", createdAt=" + getCreatedAt() +
               '}';
    }
}