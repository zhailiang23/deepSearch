package com.ynet.mgmt.imagerecognition.service.impl;

import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.dto.BatchConversionRequest;
import com.ynet.mgmt.imagerecognition.dto.BatchConversionResponse;
import com.ynet.mgmt.imagerecognition.dto.ImageRecognitionResult;
import com.ynet.mgmt.imagerecognition.service.ActivityIndexService;
import com.ynet.mgmt.imagerecognition.service.BatchConversionService;
import com.ynet.mgmt.imagerecognition.service.SiliconFlowVisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 批量数据转换服务实现
 */
@Service
public class BatchConversionServiceImpl implements BatchConversionService {

    private static final Logger logger = LoggerFactory.getLogger(BatchConversionServiceImpl.class);

    private final SiliconFlowVisionService visionService;
    private final ActivityIndexService activityIndexService;

    // 缩略图尺寸
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    public BatchConversionServiceImpl(SiliconFlowVisionService visionService,
                                     ActivityIndexService activityIndexService) {
        this.visionService = visionService;
        this.activityIndexService = activityIndexService;
    }

    @Override
    public BatchConversionResponse batchRecognizeImages(BatchConversionRequest request) {
        logger.info("开始批量识别图片: 数据库={}:{}/{}, 表={}, 索引={}",
                    request.getDbHost(), request.getDbPort(), request.getDbName(),
                    request.getTableName(), request.getIndexName());

        List<ImageRecognitionResult> results = new ArrayList<>();
        Connection connection = null;
        int insertedCount = 0;
        int skippedCount = 0;

        try {
            // 1. 连接数据库
            connection = connectToDatabase(request);
            logger.info("成功连接到数据库");

            // 2. 查询图片路径列表
            List<ImageRecord> imageRecords = queryImagePaths(connection, request);
            logger.info("从数据库查询到 {} 条图片记录", imageRecords.size());

            if (imageRecords.isEmpty()) {
                return BatchConversionResponse.error("未找到任何图片记录");
            }

            // 3. 循环处理每张图片
            for (ImageRecord record : imageRecords) {
                try {
                    ProcessedImageData processedData = processImage(record);
                    results.add(processedData.getResult());
                    logger.info("成功处理图片: id={}, path={}", record.getId(), record.getImagePath());

                    // 4. 如果指定了索引名称，则将数据插入到Elasticsearch
                    if (StringUtils.hasText(request.getIndexName()) && processedData.getResult().isSuccess()) {
                        boolean inserted = activityIndexService.insertActivity(
                            record.getId(),
                            processedData.getActivityInfo(),
                            request.getIndexName(),
                            record.getImagePath()
                        );

                        if (inserted) {
                            insertedCount++;
                            logger.info("活动信息已插入索引: id={}, index={}", record.getId(), request.getIndexName());
                        } else {
                            skippedCount++;
                            logger.info("活动信息已跳过（文档已存在）: id={}, index={}", record.getId(), request.getIndexName());
                        }
                    }
                } catch (Exception e) {
                    logger.error("处理图片失败: id={}, path={}, error={}",
                                record.getId(), record.getImagePath(), e.getMessage());
                    results.add(ImageRecognitionResult.failure(
                        record.getId(),
                        record.getImagePath(),
                        e.getMessage()
                    ));
                }
            }

            logger.info("批量识别完成: 总数={}, 成功={}, 失败={}, 插入={}, 跳过={}",
                       results.size(),
                       results.stream().filter(ImageRecognitionResult::isSuccess).count(),
                       results.stream().filter(r -> !r.isSuccess()).count(),
                       insertedCount,
                       skippedCount);

            BatchConversionResponse response = BatchConversionResponse.success(results);
            response.setInsertedCount(insertedCount);
            response.setSkippedCount(skippedCount);
            return response;

        } catch (Exception e) {
            logger.error("批量识别过程中发生错误", e);
            return BatchConversionResponse.error("批量识别失败: " + e.getMessage());
        } finally {
            // 关闭数据库连接
            if (connection != null) {
                try {
                    connection.close();
                    logger.info("数据库连接已关闭");
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
    }

    /**
     * 连接到指定的数据库
     */
    private Connection connectToDatabase(BatchConversionRequest request) throws SQLException {
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
                                      request.getDbHost(),
                                      request.getDbPort(),
                                      request.getDbName());

        logger.info("连接数据库: {}", jdbcUrl);

        return DriverManager.getConnection(
            jdbcUrl,
            request.getDbUsername(),
            request.getDbPassword()
        );
    }

    /**
     * 查询图片路径列表
     */
    private List<ImageRecord> queryImagePaths(Connection connection, BatchConversionRequest request)
            throws SQLException {
        List<ImageRecord> records = new ArrayList<>();

        // 构建查询语句
        String sql = buildQuerySql(request);
        logger.info("执行查询: {}", sql);

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String id = resultSet.getString(request.getPrimaryKeyColumn());
                String imagePath = resultSet.getString(request.getImagePathColumn());

                // 优先使用 imageUrlColumn，如果存在的话
                String imageUrl = null;
                if (StringUtils.hasText(request.getImageUrlColumn())) {
                    try {
                        imageUrl = resultSet.getString(request.getImageUrlColumn());
                    } catch (SQLException e) {
                        // 忽略列不存在的错误
                        logger.debug("列 {} 不存在或无法读取", request.getImageUrlColumn());
                    }
                }

                // 优先使用URL，如果没有则使用path
                String finalPath = StringUtils.hasText(imageUrl) ? imageUrl : imagePath;

                if (StringUtils.hasText(finalPath)) {
                    records.add(new ImageRecord(id, finalPath));
                }
            }
        }

        return records;
    }

    /**
     * 构建查询 SQL
     */
    private String buildQuerySql(BatchConversionRequest request) {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(request.getPrimaryKeyColumn()).append(", ");
        sql.append(request.getImagePathColumn());

        if (StringUtils.hasText(request.getImageUrlColumn())) {
            sql.append(", ").append(request.getImageUrlColumn());
        }

        sql.append(" FROM ").append(request.getTableName());
        sql.append(" WHERE ").append(request.getImagePathColumn()).append(" IS NOT NULL");
        sql.append(" AND ").append(request.getImagePathColumn()).append(" != ''");

        return sql.toString();
    }

    /**
     * 处理单张图片
     */
    private ProcessedImageData processImage(ImageRecord record) throws Exception {
        // 1. 从本地磁盘读取图片
        File imageFile = new File(record.getImagePath());
        if (!imageFile.exists()) {
            throw new RuntimeException("图片文件不存在: " + record.getImagePath());
        }

        // 2. 创建缩略图
        String thumbnailBase64 = createThumbnail(imageFile);

        // 3. 将图片文件转换为 MultipartFile
        MultipartFile multipartFile = convertFileToMultipartFile(imageFile);

        // 4. 调用视觉识别服务
        var activityInfo = visionService.recognizeImage(multipartFile);

        // 5. 提取所有文本内容
        String recognizedText = activityInfo.getAll();
        if (!StringUtils.hasText(recognizedText)) {
            recognizedText = String.format(
                "名称: %s\n描述: %s\n链接: %s\n开始日期: %s\n结束日期: %s\n状态: %s",
                activityInfo.getName(),
                activityInfo.getDescript(),
                activityInfo.getLink(),
                activityInfo.getStartDate(),
                activityInfo.getEndDate(),
                activityInfo.getStatus()
            );
        }

        ImageRecognitionResult result = ImageRecognitionResult.success(
            record.getId(),
            record.getImagePath(),
            thumbnailBase64,
            recognizedText
        );

        return new ProcessedImageData(result, activityInfo);
    }

    /**
     * 创建图片缩略图并转换为 Base64
     */
    private String createThumbnail(File imageFile) throws Exception {
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new RuntimeException("无法读取图片文件");
        }

        // 计算缩略图尺寸，保持宽高比
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        double ratio = Math.min(
            (double) THUMBNAIL_WIDTH / width,
            (double) THUMBNAIL_HEIGHT / height
        );

        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        // 创建缩略图
        BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // 转换为 Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", baos);
        byte[] thumbnailBytes = baos.toByteArray();
        String base64String = Base64.getEncoder().encodeToString(thumbnailBytes);

        return "data:image/jpeg;base64," + base64String;
    }

    /**
     * 将 File 转换为 MultipartFile
     */
    private MultipartFile convertFileToMultipartFile(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        byte[] fileBytes = input.readAllBytes();
        input.close();

        String contentType = getContentTypeFromFileName(file.getName());

        return new CustomMultipartFile(
            file.getName(),
            file.getName(),
            contentType,
            fileBytes
        );
    }

    /**
     * 自定义 MultipartFile 实现
     */
    private static class CustomMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public CustomMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }

    /**
     * 根据文件名获取 Content-Type
     */
    private String getContentTypeFromFileName(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "image/jpeg";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            default:
                return "image/jpeg";
        }
    }

    /**
     * 图片记录内部类
     */
    private static class ImageRecord {
        private final String id;
        private final String imagePath;

        public ImageRecord(String id, String imagePath) {
            this.id = id;
            this.imagePath = imagePath;
        }

        public String getId() {
            return id;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    /**
     * 处理后的图片数据内部类
     * 包含识别结果和活动信息
     */
    private static class ProcessedImageData {
        private final ImageRecognitionResult result;
        private final ActivityInfo activityInfo;

        public ProcessedImageData(ImageRecognitionResult result, ActivityInfo activityInfo) {
            this.result = result;
            this.activityInfo = activityInfo;
        }

        public ImageRecognitionResult getResult() {
            return result;
        }

        public ActivityInfo getActivityInfo() {
            return activityInfo;
        }
    }
}
