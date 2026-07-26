package com.prompttraining.module.session;

import com.prompttraining.common.Result;
import com.prompttraining.module.session.entity.dto.BatchDeleteRequest;
import com.prompttraining.module.session.entity.dto.SessionCreateRequest;
import com.prompttraining.module.session.entity.dto.SessionListResponse;
import com.prompttraining.module.session.entity.dto.SessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理接口 - V2：按真实用户过滤，新增搜索和批量删除
 */
@Tag(name = "会话管理")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "创建新会话")
    @PostMapping
    public Result<SessionResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        SessionResponse response = sessionService.createSession(request.getModelCode());
        return Result.ok(response);
    }

    @Operation(summary = "获取会话列表")
    @GetMapping
    public Result<List<SessionListResponse>> listSessions() {
        List<SessionListResponse> list = sessionService.listSessions();
        return Result.ok(list);
    }

    @Operation(summary = "获取会话详情")
    @GetMapping("/{id}")
    public Result<SessionResponse> getSession(@PathVariable("id") String sessionId) {
        SessionResponse response = sessionService.getSession(sessionId);
        return Result.ok(response);
    }

    @Operation(summary = "更新会话标题")
    @PatchMapping("/{id}")
    public Result<?> updateSession(@PathVariable("id") String sessionId, @RequestBody UpdateTitleRequest request) {
        sessionService.updateTitle(sessionId, request.getTitle());
        return Result.ok();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<?> deleteSession(@PathVariable("id") String sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.ok(null, "删除成功");
    }

    @Operation(summary = "搜索会话（V2 新增）")
    @GetMapping("/search")
    public Result<List<SessionListResponse>> searchSessions(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        List<SessionListResponse> list = sessionService.searchSessions(keyword);
        return Result.ok(list);
    }

    @Operation(summary = "批量删除会话（V2 新增）")
    @PostMapping("/batch-delete")
    public Result<?> batchDeleteSessions(@Valid @RequestBody BatchDeleteRequest request) {
        int count = sessionService.batchDeleteSessions(request.getIds());
        return Result.ok(Map.of("deletedCount", count), "成功删除 " + count + " 个会话");
    }
}
