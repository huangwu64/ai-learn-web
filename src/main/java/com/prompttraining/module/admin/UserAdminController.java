package com.prompttraining.module.admin;

import com.prompttraining.common.Result;
import com.prompttraining.module.admin.dto.*;
import com.prompttraining.module.review.ProfileChangeRequestService;
import com.prompttraining.module.review.dto.ProfileChangeRequestResponse;
import com.prompttraining.module.review.dto.ReviewRejectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员用户管理与审核接口（V3.2） - 需 ADMIN 角色
 */
@Tag(name = "管理员用户管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final ProfileChangeRequestService reviewService;

    @Operation(summary = "用户列表（V3.2）")
    @GetMapping("/users")
    public Result<List<AdminUserResponse>> listUsers() {
        return Result.ok(userAdminService.listUsers());
    }

    @Operation(summary = "创建用户（V3.2）")
    @PostMapping("/users")
    public Result<AdminUserResponse> createUser(@RequestBody AdminCreateUserRequest request) {
        return Result.ok(userAdminService.createUser(request), "创建成功");
    }

    @Operation(summary = "删除用户（V3.2）")
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userAdminService.deleteUser(id);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "更新用户信息（V3.2，管理员直接生效）")
    @PutMapping("/users/{id}")
    public Result<AdminUserResponse> updateUser(@PathVariable Long id, @RequestBody AdminUpdateUserRequest request) {
        return Result.ok(userAdminService.updateUser(id, request), "更新成功");
    }

    @Operation(summary = "重置用户密码（V3.2）")
    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable Long id, @RequestBody AdminResetPasswordRequest request) {
        userAdminService.resetPassword(id, request);
        return Result.ok(null, "密码已重置");
    }

    @Operation(summary = "资料变更审核列表（V3.2）")
    @GetMapping("/reviews")
    public Result<List<ProfileChangeRequestResponse>> listReviews(
            @RequestParam(required = false) Integer status) {
        return Result.ok(reviewService.listAdmin(status));
    }

    @Operation(summary = "通过资料变更审核（V3.2）")
    @PostMapping("/reviews/{id}/approve")
    public Result<?> approveReview(@PathVariable Long id) {
        reviewService.approve(id);
        return Result.ok(null, "已通过，变更已同步到用户端");
    }

    @Operation(summary = "拒绝资料变更审核（V3.2）")
    @PostMapping("/reviews/{id}/reject")
    public Result<?> rejectReview(@PathVariable Long id, @RequestBody(required = false) ReviewRejectRequest request) {
        reviewService.reject(id, request != null ? request.getRemark() : null);
        return Result.ok(null, "已拒绝");
    }
}
