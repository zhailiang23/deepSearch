package com.ynet.mgmt.imagerecognition.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.imagerecognition.config.SiliconFlowVisionProperties;
import com.ynet.mgmt.imagerecognition.dto.ActivityInfo;
import com.ynet.mgmt.imagerecognition.dto.VisionApiRequest;
import com.ynet.mgmt.imagerecognition.dto.VisionApiResponse;
import com.ynet.mgmt.imagerecognition.service.PromptConfigService;
import com.ynet.mgmt.imagerecognition.service.SiliconFlowVisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 硅基流动 Vision API 服务实现
 */
@Service
public class SiliconFlowVisionServiceImpl implements SiliconFlowVisionService {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowVisionServiceImpl.class);

    private final SiliconFlowVisionProperties properties;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PromptConfigService promptConfigService;

    /**
     * 默认提示词模板（作为后备方案）
     */
    private static final String DEFAULT_PROMPT_TEMPLATE =
        "你是一个专业的图片文字识别助手。请仔细识别图片中的所有文字内容,并严格按照以下JSON格式返回结果。\n\n" +
        "**重要要求:**\n" +
        "1. 必须返回有效的JSON格式数据\n" +
        "2. 不要添加任何解释性文字\n" +
        "3. 不要使用markdown代码块标记\n" +
        "4. 直接返回纯JSON对象\n\n" +
        "**JSON格式要求:**\n" +
        "{\n" +
        "  \"name\": \"从图片中识别到的活动名称,如果无法识别则填写'未识别到活动名称'\",\n" +
        "  \"descript\": \"活动描述,如果无法直接识别则根据活动名称和其他文字信息生成简洁描述(不超过100字)\",\n" +
        "  \"link\": \"活动链接地址,如果无法识别则填写空字符串\",\n" +
        "  \"startDate\": \"活动开始时间,格式YYYY-MM-DD,如果无法识别则填写空字符串\",\n" +
        "  \"endDate\": \"活动结束时间,格式YYYY-MM-DD,如果无法识别则填写空字符串\",\n" +
        "  \"status\": \"活动状态(如:进行中、即将开始、已结束、已取消),如果无法识别则填写'未知'\",\n" +
        "  \"all\": \"图片中识别到的所有文字内容,保持原始格式和换行\"\n" +
        "}\n\n" +
        "请严格按照上述JSON格式返回识别结果:";

    public SiliconFlowVisionServiceImpl(SiliconFlowVisionProperties properties,
                                       ObjectMapper objectMapper,
                                       PromptConfigService promptConfigService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.promptConfigService = promptConfigService;
    }

    @PostConstruct
    public void init() {
        // 初始化RestTemplate并配置超时
        try {
            // 使用SimpleClientHttpRequestFactory配置超时
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(properties.getTimeout().intValue());
            factory.setReadTimeout(properties.getTimeout().intValue());
            this.restTemplate = new RestTemplate(factory);
        } catch (Exception e) {
            logger.warn("无法配置RestTemplate超时，将使用默认RestTemplate", e);
            this.restTemplate = new RestTemplate();
        }

        logger.info("初始化硅基流动Vision服务: model={}, url={}, timeout={}ms",
                properties.getModel(), properties.getApiUrl(), properties.getTimeout());
    }

    @Override
    public ActivityInfo recognizeImage(MultipartFile file) throws Exception {
        logger.info("开始识别图片: {}", file.getOriginalFilename());

        // 验证图片文件
        validateImageFile(file);

        // 将图片转换为 Base64
        String base64Image = encodeImageToBase64(file);

        // 构建 Vision API 请求
        VisionApiRequest request = buildVisionApiRequest(base64Image, file.getContentType());

        // 调用 Vision API
        VisionApiResponse response = callVisionApi(request);

        // 解析响应并返回活动信息
        return parseActivityInfo(response);
    }

    @Override
    public VisionApiResponse callVisionApi(VisionApiRequest request) throws Exception {
        logger.info("调用硅基流动 Vision API");

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        // 构建请求实体
        HttpEntity<VisionApiRequest> requestEntity = new HttpEntity<>(request, headers);

        try {
            // 发送请求
            ResponseEntity<VisionApiResponse> responseEntity = restTemplate.exchange(
                properties.getApiUrl(),
                HttpMethod.POST,
                requestEntity,
                VisionApiResponse.class
            );

            VisionApiResponse response = responseEntity.getBody();
            if (response == null) {
                throw new RuntimeException("Vision API 返回空响应");
            }

            logger.info("Vision API 调用成功, 响应ID: {}", response.getId());
            return response;

        } catch (Exception e) {
            logger.error("调用 Vision API 失败", e);
            throw new RuntimeException("调用 Vision API 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String encodeImageToBase64(MultipartFile file) throws Exception {
        try {
            byte[] imageBytes = file.getBytes();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            // 根据文件类型构建 data URL
            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType)) {
                contentType = getContentTypeFromFileName(file.getOriginalFilename());
            }

            return String.format("data:%s;base64,%s", contentType, base64String);
        } catch (IOException e) {
            logger.error("转换图片为 Base64 失败", e);
            throw new RuntimeException("转换图片为 Base64 失败", e);
        }
    }

    @Override
    public void validateImageFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > properties.getImage().getMaxFileSize()) {
            throw new IllegalArgumentException(
                String.format("图片文件大小超过限制，最大允许 %d 字节",
                            properties.getImage().getMaxFileSize())
            );
        }

        // 检查文件格式
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (!isValidImageFormat(contentType, originalFilename)) {
            throw new IllegalArgumentException(
                String.format("不支持的图片格式，支持的格式: %s",
                            properties.getImage().getSupportedFormats())
            );
        }

        // 检查图片尺寸
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();

                SiliconFlowVisionProperties.ImageConfig imageConfig = properties.getImage();
                if (width < imageConfig.getMinWidth() || height < imageConfig.getMinHeight() ||
                    width > imageConfig.getMaxWidth() || height > imageConfig.getMaxHeight()) {
                    throw new IllegalArgumentException(
                        String.format("图片尺寸不符合要求，当前: %dx%d，要求: %dx%d 到 %dx%d",
                                    width, height,
                                    imageConfig.getMinWidth(), imageConfig.getMinHeight(),
                                    imageConfig.getMaxWidth(), imageConfig.getMaxHeight())
                    );
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取图片文件，可能文件已损坏");
        }
    }

    /**
     * 构建 Vision API 请求
     */
    private VisionApiRequest buildVisionApiRequest(String base64Image, String contentType) {
        // 从数据库获取提示词,如果获取失败则使用默认提示词
        String promptTemplate = getPromptTemplate();

        // 创建文本内容
        VisionApiRequest.Content textContent = new VisionApiRequest.Content("text", promptTemplate);

        // 创建图片内容
        VisionApiRequest.ImageUrl imageUrl = new VisionApiRequest.ImageUrl(base64Image);
        VisionApiRequest.Content imageContent = new VisionApiRequest.Content("image_url", imageUrl);

        // 创建消息
        VisionApiRequest.Message message = new VisionApiRequest.Message("user",
            Arrays.asList(textContent, imageContent));

        // 创建请求（QVQ-72B-Preview 不支持 response_format 参数）
        return new VisionApiRequest(
            properties.getModel(),
            Arrays.asList(message),
            properties.getTemperature(),
            properties.getMaxTokens(),
            properties.getTopP(),
            properties.getStream()
        );
    }

    /**
     * 获取提示词模板
     * 优先从数据库读取,如果读取失败则使用默认模板
     */
    private String getPromptTemplate() {
        try {
            String promptFromDb = promptConfigService.getEnabledPromptContent(
                PromptConfigServiceImpl.DEFAULT_IMAGE_RECOGNITION_KEY
            );
            if (StringUtils.hasText(promptFromDb)) {
                logger.debug("使用数据库配置的提示词");
                return promptFromDb;
            }
        } catch (Exception e) {
            logger.warn("从数据库读取提示词失败,使用默认提示词: {}", e.getMessage());
        }
        logger.debug("使用默认提示词");
        return DEFAULT_PROMPT_TEMPLATE;
    }

    /**
     * 解析 Vision API 响应，提取活动信息
     */
    private ActivityInfo parseActivityInfo(VisionApiResponse response) throws Exception {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RuntimeException("Vision API 响应中没有选择项");
        }

        VisionApiResponse.Choice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new RuntimeException("Vision API 响应中没有内容");
        }

        String content = choice.getMessage().getContent().trim();
        logger.info("Vision API 返回内容: {}", content);

        try {
            // 尝试解析 JSON
            return objectMapper.readValue(content, ActivityInfo.class);
        } catch (Exception e) {
            logger.warn("解析 JSON 失败，尝试从文本中提取信息: {}", e.getMessage());

            // 如果 JSON 解析失败，创建一个包含原始文本的 ActivityInfo
            ActivityInfo fallbackInfo = new ActivityInfo();
            fallbackInfo.setAll(content);
            fallbackInfo.setName("无法识别");
            fallbackInfo.setDescript("图片识别结果解析失败");
            return fallbackInfo;
        }
    }

    /**
     * 检查是否为有效的图片格式
     */
    private boolean isValidImageFormat(String contentType, String filename) {
        List<String> supportedFormats = Arrays.asList(
            properties.getImage().getSupportedFormats().toLowerCase().split(",")
        );

        // 检查 Content-Type
        if (StringUtils.hasText(contentType)) {
            if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
                return supportedFormats.contains("jpg") || supportedFormats.contains("jpeg");
            }
            if (contentType.equals("image/png")) {
                return supportedFormats.contains("png");
            }
        }

        // 检查文件扩展名
        if (StringUtils.hasText(filename)) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            return supportedFormats.contains(extension);
        }

        return false;
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
}