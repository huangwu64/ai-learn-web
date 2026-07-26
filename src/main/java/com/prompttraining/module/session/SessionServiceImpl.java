package com.prompttraining.module.session;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.common.BusinessException;
import com.prompttraining.common.Constant;
import com.prompttraining.module.message.MessageMapper;
import com.prompttraining.module.message.entity.Message;
import com.prompttraining.module.session.entity.Session;
import com.prompttraining.module.session.entity.dto.SessionListResponse;
import com.prompttraining.module.session.entity.dto.SessionResponse;
import com.prompttraining.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;

    @Override
    @Transactional
    public SessionResponse createSession(String modelCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        Session session = new Session();
        session.setUserId(userId);
        session.setTitle("新对话");
        session.setModelCode(modelCode != null ? modelCode : Constant.DEFAULT_MODEL_CODE);
        session.setMessageCount(0);
        sessionMapper.insert(session);
        return toResponse(session);
    }

    @Override
    public List<SessionListResponse> listSessions() {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUserId, userId)
               .orderByDesc(Session::getUpdatedAt);

        List<Session> sessions = sessionMapper.selectList(wrapper);

        return sessions.stream().map(session -> {
            SessionListResponse resp = new SessionListResponse();
            resp.setId(session.getId());
            resp.setTitle(session.getTitle());
            resp.setMessageCount(session.getMessageCount());
            resp.setModelCode(session.getModelCode());
            resp.setUpdatedAt(formatDateTime(session.getUpdatedAt()));

            // 获取最后一条消息摘要（截取前50字）
            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getSessionId, session.getId())
                      .orderByDesc(Message::getCreatedAt)
                      .last("LIMIT 1");
            Message lastMsg = messageMapper.selectOne(msgWrapper);
            if (lastMsg != null) {
                String content = lastMsg.getContent();
                resp.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            } else {
                resp.setLastMessage("");
            }

            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public SessionResponse getSession(String sessionId) {
        Session session = getById(sessionId);
        return toResponse(session);
    }

    @Override
    public void updateTitle(String sessionId, String title) {
        if (StrUtil.isBlank(title)) {
            return;
        }
        Session session = getById(sessionId);
        // 标题截取前50字
        String truncated = title.length() > 50 ? title.substring(0, 50) : title;
        session.setTitle(truncated);
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        Session session = getById(sessionId);
        // 软删除会话
        sessionMapper.deleteById(sessionId);
        // 软删除该会话下所有消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId);
        messageMapper.delete(wrapper);
    }

    @Override
    public List<SessionListResponse> searchSessions(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return List.of();
        }
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUserId, userId)
               .like(Session::getTitle, keyword)
               .orderByDesc(Session::getUpdatedAt);

        List<Session> sessions = sessionMapper.selectList(wrapper);

        return sessions.stream().map(session -> {
            SessionListResponse resp = new SessionListResponse();
            resp.setId(session.getId());
            resp.setTitle(session.getTitle());
            resp.setMessageCount(session.getMessageCount());
            resp.setModelCode(session.getModelCode());
            resp.setUpdatedAt(formatDateTime(session.getUpdatedAt()));

            LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(Message::getSessionId, session.getId())
                      .orderByDesc(Message::getCreatedAt)
                      .last("LIMIT 1");
            Message lastMsg = messageMapper.selectOne(msgWrapper);
            if (lastMsg != null) {
                String content = lastMsg.getContent();
                resp.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            } else {
                resp.setLastMessage("");
            }

            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int batchDeleteSessions(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        int deletedCount = 0;
        for (String id : ids) {
            Session session = sessionMapper.selectById(id);
            if (session == null) continue;
            if (!session.getUserId().equals(userId)) {
                throw new BusinessException(403, "部分会话不属于当前用户，操作已拒绝");
            }
            // 软删除会话
            sessionMapper.deleteById(id);
            // 软删除会话下所有消息
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Message::getSessionId, id);
            messageMapper.delete(wrapper);
            deletedCount++;
        }
        return deletedCount;
    }

    @Override
    public Session getById(String sessionId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "会话不存在");
        }
        // V2: 校验会话所有权（数据隔离）
        checkOwnership(session);
        return session;
    }

    /**
     * 校验会话所有权
     */
    private void checkOwnership(Session session) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && !userId.equals(session.getUserId())) {
            throw new BusinessException(403, "无权访问该会话");
        }
    }

    private SessionResponse toResponse(Session session) {
        SessionResponse resp = new SessionResponse();
        resp.setId(session.getId());
        resp.setTitle(session.getTitle());
        resp.setModelCode(session.getModelCode());
        resp.setMessageCount(session.getMessageCount());
        resp.setCreatedAt(session.getCreatedAt());
        resp.setUpdatedAt(session.getUpdatedAt());
        return resp;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }
}
