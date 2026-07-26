package com.prompttraining.module.session.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话实体
 */
@Data
@TableName("session")
public class Session {

    /** 主键，雪花ID 或 UUID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 所属用户 ID */
    private Long userId;

    /** 会话标题 */
    private String title;

    /** 使用的模型编码 */
    private String modelCode;

    /** 消息总数（冗余字段） */
    private Integer messageCount;

    /** 软删除标记 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
