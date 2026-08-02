package com.prompttraining.module.admin;

import com.prompttraining.common.Result;
import com.prompttraining.module.admin.dto.AdminLoginRequest;
import com.prompttraining.module.admin.dto.AdminLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员认证接口（V3）
 * - 登录无需认证
 * - 退出需 ADMIN 角色
 */
@Slf4j
@Tag(name = "管理员认证")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return Result.ok(response, "登录成功");
    }

    @Operation(summary = "管理员退出")
    @PostMapping("/logout")
    public Result<?> logout() {
        // 管理员 Token 为无状态 JWT，退出由前端清除本地 Token
        return Result.ok(null, "已退出登录");
    }
}
