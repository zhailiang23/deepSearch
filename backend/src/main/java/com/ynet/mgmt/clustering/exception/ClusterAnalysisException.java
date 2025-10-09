package com.ynet.mgmt.clustering.exception;

/**
 * 聚类分析异常
 *
 * @author system
 * @since 1.0.0
 */
public class ClusterAnalysisException extends RuntimeException {

    public ClusterAnalysisException(String message) {
        super(message);
    }

    public ClusterAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
