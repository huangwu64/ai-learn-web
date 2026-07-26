package com.prompttraining.module.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompttraining.module.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
