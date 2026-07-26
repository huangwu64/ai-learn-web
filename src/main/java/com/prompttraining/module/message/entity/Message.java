package com.prompttraining.module.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID */
    private String sessionId;

    /** 角色：user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** token 消耗数 */
    private Integer tokenCount;

    /** 生成此消息的模型编码（用户消息为 NULL） */
    private String modelCode;

    /** 软删除标记 */
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
