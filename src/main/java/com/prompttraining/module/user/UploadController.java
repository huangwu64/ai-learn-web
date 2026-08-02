package com.prompttraining.module.user;

import com.prompttraining.common.BusinessException;
import com.prompttraining.common.Result;
import com.prompttraining.config.WebMvcConfig;
import com.prompttraining.module.review.ProfileChangeRequestService;
import com.prompttraining.module.review.dto.ProfileChangeRequestResponse;
import com.prompttraining.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口（V3.2）
 * 头像上传后生成待审核记录，管理员审核通过后才同步到用户资料
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    /** 允许的头像图片类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp"
    );

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024; // 5MB

    private final ProfileChangeRequestService reviewService;

    @Operation(summary = "上传头像（V3.2 新增，上传后进入审核）")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        // 校验文件类型与大小
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 png/jpg/gif/webp 图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException(400, "图片大小不能超过 5MB");
        }
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        // 保存文件：uploads/avatars/{uuid}.{ext}
        String ext = resolveExtension(contentType);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativeDir = WebMvcConfig.UPLOAD_DIR + "/avatars";
        try {
            Path dir = Paths.get(relativeDir);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            log.error("头像保存失败", e);
            throw new BusinessException(500, "头像上传失败，请重试");
        }

        String url = "/uploads/avatars/" + filename;
        // 生成待审核记录（头像变更需管理员审核）
        ProfileChangeRequestResponse review = reviewService.submit(userId, "avatar", url);
        log.info("头像上传成功并进入审核: userId={}, url={}, reviewId={}", userId, url, review.getId());

        return Result.ok(Map.of(
                "requestId", review.getId(),
                "url", url,
                "status", "pending"
        ), "头像已上传，等待管理员审核");
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }
}
