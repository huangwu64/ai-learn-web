package com.prompttraining.module.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.prompttraining.module.review.entity.ProfileChangeRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料变更审核 Mapper
 */
@Mapper
public interface ProfileChangeRequestMapper extends BaseMapper<ProfileChangeRequest> {
}
