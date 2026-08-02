package com.prompttraining.module.admin;

import com.prompttraining.common.Result;
import com.prompttraining.config.AdminConfig;
import com.prompttraining.module.admin.dto.AdminEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开接口（V3） - 无需认证
 */
@Tag(name = "公开接口")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final AdminConfig adminConfig;

    @Operation(summary = "获取管理员入口路径")
    @GetMapping("/admin-entry")
    public Result<AdminEntryResponse> getAdminEntry() {
        return Result.ok(new AdminEntryResponse(adminConfig.getEntryPath()));
    }
}
