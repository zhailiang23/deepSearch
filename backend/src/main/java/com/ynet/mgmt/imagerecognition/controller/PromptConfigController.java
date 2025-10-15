package com.ynet.mgmt.imagerecognition.controller;

import com.ynet.mgmt.imagerecognition.dto.PromptConfigDTO;
import com.ynet.mgmt.imagerecognition.service.PromptConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词配置控制器
 */
@RestController
@RequestMapping("/prompt-config")
@Tag(name = "提示词配置", description = "图片识别提示词配置管理接口")
public class PromptConfigController {

    private static final Logger logger = LoggerFactory.getLogger(PromptConfigController.class);

    private final PromptConfigService promptConfigService;

    public PromptConfigController(PromptConfigService promptConfigService) {
        this.promptConfigService = promptConfigService;
    }

    @GetMapping
    @Operation(summary = "获取所有提示词配置", description = "获取系统中所有的提示词配置")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<List<PromptConfigDTO>> getAllConfigs() {
        logger.info("获取所有提示词配置");
        List<PromptConfigDTO> configs = promptConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取配置", description = "根据配置ID获取指定的提示词配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public ResponseEntity<?> getConfigById(@PathVariable Long id) {
        logger.info("获取提示词配置: ID={}", id);
        try {
            PromptConfigDTO config = promptConfigService.getConfigById(id);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            logger.error("获取提示词配置失败: ID={}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/key/{configKey}")
    @Operation(summary = "根据键名获取配置", description = "根据配置键名获取指定的提示词配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public ResponseEntity<?> getConfigByKey(@PathVariable String configKey) {
        logger.info("获取提示词配置: KEY={}", configKey);
        try {
            PromptConfigDTO config = promptConfigService.getConfigByKey(configKey);
            return ResponseEntity.ok(config);
        } catch (RuntimeException e) {
            logger.error("获取提示词配置失败: KEY={}", configKey, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "创建新配置", description = "创建一个新的提示词配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> createConfig(@Valid @RequestBody PromptConfigDTO dto) {
        logger.info("创建提示词配置: KEY={}", dto.getConfigKey());
        try {
            PromptConfigDTO created = promptConfigService.createConfig(dto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            logger.error("创建提示词配置失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新配置", description = "更新指定ID的提示词配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "404", description = "配置不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody PromptConfigDTO dto) {
        logger.info("更新提示词配置: ID={}", id);
        try {
            PromptConfigDTO updated = promptConfigService.updateConfig(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            logger.error("更新提示词配置失败: ID={}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置", description = "删除指定ID的提示词配置")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public ResponseEntity<?> deleteConfig(@PathVariable Long id) {
        logger.info("删除提示词配置: ID={}", id);
        try {
            promptConfigService.deleteConfig(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("删除提示词配置失败: ID={}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
