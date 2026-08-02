package com.prompttraining.module.admin;

import com.prompttraining.ai.config.AiConfigService;
import com.prompttraining.ai.config.dto.AiConfigResponse;
import com.prompttraining.ai.config.dto.AiConfigUpdateRequest;
import com.prompttraining.ai.deepseek.DeepSeekProvider;
import com.prompttraining.common.BusinessException;
import com.prompttraining.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 AI 配置接口（V3） - 需 ADMIN 角色
 */
@Slf4j
@Tag(name = "管理员AI配置")
@RestController
@RequestMapping("/api/v1/admin/ai-config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final DeepSeekProvider deepSeekProvider;

    @Operation(summary = "获取 AI 配置（Key 脱敏）")
    @GetMapping
    public Result<AiConfigResponse> getConfig() {
        return Result.ok(aiConfigService.toResponse());
    }

    @Operation(summary = "获取可用模型列表（V3.1）")
    @GetMapping("/models")
    public Result<Map<String, Object>> listModels() {
        List<String> models = deepSeekProvider.listModels();
        return Result.ok(Map.of("models", models));
    }

    @Operation(summary = "更新 AI 配置")
    @PutMapping
    public Result<?> updateConfig(@RequestBody AiConfigUpdateRequest request) {
        aiConfigService.updateConfig(request);
        log.info("管理员更新 AI 配置完成");
        return Result.ok(null, "AI 配置已更新");
    }

    @Operation(summary = "测试 AI 连接")
    @PostMapping("/test")
    public Result<?> testConnection(@RequestBody(required = false) AiConfigUpdateRequest request) {
        boolean ok = deepSeekProvider.healthCheck(request);
        if (!ok) {
            throw new BusinessException(500, "AI 连接失败，请检查 API 地址和 Key");
        }
        return Result.ok(Map.of("ok", true), "连接成功");
    }
}
