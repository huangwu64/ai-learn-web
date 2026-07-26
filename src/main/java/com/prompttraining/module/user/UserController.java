package com.prompttraining.module.user;

import com.prompttraining.common.Result;
import com.prompttraining.module.user.entity.dto.ChangePasswordRequest;
import com.prompttraining.module.user.entity.dto.UpdateUserRequest;
import com.prompttraining.module.user.entity.dto.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口 - V2：返回真实登录用户，支持信息编辑和密码修改
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> getCurrentUser() {
        return Result.ok(userService.getCurrentUser());
    }

    @Operation(summary = "更新当前用户信息（V2 新增）")
    @PatchMapping("/me")
    public Result<UserInfoResponse> updateUser(@RequestBody UpdateUserRequest request) {
        return Result.ok(userService.updateUser(request), "更新成功");
    }

    @Operation(summary = "修改密码（V2 新增）")
    @PatchMapping("/me/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return Result.ok(null, "密码修改成功，请重新登录");
    }
}
