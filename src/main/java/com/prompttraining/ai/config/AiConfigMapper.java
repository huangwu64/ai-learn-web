package com.prompttraining.ai.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 动态配置 Mapper
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {
}
