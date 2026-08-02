package com.prompttraining.module.review.dto;

import lombok.Data;

/**
 * 提交资料变更审核请求（V3.2）
 * 用户修改头像/昵称/用户名时，提交后进入管理员审核队列
 */
@Data
public class ProfileChangeRequestSubmit {

    /** 变更字段：avatar / nickname / username */
    private String fieldName;

    /** 新值 */
    private String newValue;
}
