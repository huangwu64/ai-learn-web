package com.prompttraining.module.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompttraining.module.session.entity.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话 Mapper
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
