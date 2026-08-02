package com.prompttraining.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理员入口路径响应（V3）
 */
@Data
@AllArgsConstructor
public class AdminEntryResponse {

    /** 管理员前端入口路径（可配置，默认 /admin） */
    private String path;
}
