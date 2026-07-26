package com.prompttraining.module.session.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量删除会话请求（V2 新增）
 */
@Data
public class BatchDeleteRequest {

    @NotEmpty(message = "会话ID列表不能为空")
    @Size(max = 100, message = "一次最多删除100个会话")
    private List<String> ids;
}
