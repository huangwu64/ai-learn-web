package com.prompttraining.module.session;

import com.prompttraining.module.session.entity.Session;
import com.prompttraining.module.session.entity.dto.SessionListResponse;
import com.prompttraining.module.session.entity.dto.SessionResponse;

import java.util.List;

/**
 * 会话服务接口
 */
public interface SessionService {

    /**
     * 创建新会话（关联当前登录用户）
     */
    SessionResponse createSession(String modelCode);

    /**
     * 获取当前用户会话列表
     */
    List<SessionListResponse> listSessions();

    /**
     * 获取会话详情
     */
    SessionResponse getSession(String sessionId);

    /**
     * 更新会话标题
     */
    void updateTitle(String sessionId, String title);

    /**
     * 删除会话（软删除）
     */
    void deleteSession(String sessionId);

    /**
     * 搜索会话（按标题模糊搜索，V2 新增）
     */
    List<SessionListResponse> searchSessions(String keyword);

    /**
     * 批量删除会话（V2 新增）
     */
    int batchDeleteSessions(List<String> ids);

    /**
     * 获取会话实体（内部使用）
     */
    Session getById(String sessionId);
}
