package com.ynet.mgmt.imagerecognition.service;

import com.ynet.mgmt.imagerecognition.dto.BatchConversionRequest;
import com.ynet.mgmt.imagerecognition.dto.BatchConversionResponse;

/**
 * 批量数据转换服务接口
 */
public interface BatchConversionService {

    /**
     * 批量识别数据库中的图片
     *
     * @param request 批量转换请求
     * @return 批量转换响应
     */
    BatchConversionResponse batchRecognizeImages(BatchConversionRequest request);
}
