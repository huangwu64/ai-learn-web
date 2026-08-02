package com.prompttraining.module.review.dto;

import lombok.Data;

/**
 * 拒绝审核请求（V3.2）
 */
@Data
public class ReviewRejectRequest {

    /** 审核备注（拒绝原因） */
    private String remark;
}
