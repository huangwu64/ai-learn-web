package com.prompttraining.module.user;

import com.prompttraining.common.Result;
import com.prompttraining.module.review.ProfileChangeRequestService;
import com.prompttraining.module.review.dto.ProfileChangeRequestResponse;
import com.prompttraining.module.review.dto.ProfileChangeRequestSubmit;
import com.prompttraining.module.user.entity.dto.ChangePasswordRequest;
import com.prompttraining.module.user.entity.dto.UpdateUserRequest;
import com.prompttraining.module.user.entity.dto.UserInfoResponse;
import com.prompttraining.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口 - V2：返回真实登录用户，支持信息编辑和密码修改
 * V3.2：昵称/用户名等资料变更走管理员审核
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ProfileChangeRequestService reviewService;

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

    @Operation(summary = "提交资料变更审核（V3.2 新增，昵称/用户名/头像需管理员审核）")
    @PostMapping("/me/profile-request")
    public Result<ProfileChangeRequestResponse> submitProfileChange(@RequestBody ProfileChangeRequestSubmit request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(reviewService.submit(userId, request.getFieldName(), request.getNewValue()), "已提交，等待管理员审核");
    }

    @Operation(summary = "查看我的资料变更审核记录（V3.2 新增）")
    @GetMapping("/me/profile-requests")
    public Result<List<ProfileChangeRequestResponse>> listMyProfileChanges() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.ok(reviewService.listByUser(userId));
    }
}
