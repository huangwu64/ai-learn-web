package com.prompttraining.module.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.prompttraining.common.BusinessException;
import com.prompttraining.module.review.dto.ProfileChangeRequestResponse;
import com.prompttraining.module.review.entity.ProfileChangeRequest;
import com.prompttraining.module.user.UserMapper;
import com.prompttraining.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户资料变更审核服务（V3.2）
 * 用户提交的头像/昵称/用户名变更需管理员审核通过后才同步到用户资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileChangeRequestService {

    private final ProfileChangeRequestMapper mapper;
    private final UserMapper userMapper;

    /**
     * 用户提交资料变更审核请求
     */
    @Transactional
    public ProfileChangeRequestResponse submit(Long userId, String fieldName, String newValue) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new BusinessException(400, "请指定要变更的字段");
        }
        if (newValue == null || newValue.isBlank()) {
            throw new BusinessException(400, "新值不能为空");
        }
        fieldName = fieldName.trim();
        newValue = newValue.trim();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String oldValue;
        switch (fieldName) {
            case "nickname":
                if (newValue.length() > 64) throw new BusinessException(400, "昵称最长64位");
                oldValue = user.getNickname();
                break;
            case "username":
                if (newValue.length() > 64) throw new BusinessException(400, "用户名最长64位");
                checkUsernameUnique(newValue, userId);
                oldValue = user.getUsername();
                break;
            case "avatar":
                if (newValue.length() > 255) throw new BusinessException(400, "头像地址过长");
                oldValue = user.getAvatarUrl();
                break;
            default:
                throw new BusinessException(400, "不支持的变更字段: " + fieldName);
        }

        ProfileChangeRequest req = new ProfileChangeRequest();
        req.setUserId(userId);
        req.setFieldName(fieldName);
        req.setOldValue(oldValue);
        req.setNewValue(newValue);
        req.setStatus(ProfileChangeRequest.STATUS_PENDING);
        mapper.insert(req);
        log.info("用户提交资料变更审核: userId={}, field={}, 新值={}", userId, fieldName, newValue);
        return toResponse(req, user.getUsername(), user.getNickname());
    }

    /**
     * 查询当前用户的审核记录（按时间倒序）
     */
    public List<ProfileChangeRequestResponse> listByUser(Long userId) {
        List<ProfileChangeRequest> list = mapper.selectList(
                new LambdaQueryWrapper<ProfileChangeRequest>()
                        .eq(ProfileChangeRequest::getUserId, userId)
                        .orderByDesc(ProfileChangeRequest::getCreatedAt)
                        .last("LIMIT 50")
        );
        return list.stream().map(r -> toResponse(r, null, null)).collect(Collectors.toList());
    }

    /**
     * 管理员查询审核列表
     */
    public List<ProfileChangeRequestResponse> listAdmin(Integer status) {
        LambdaQueryWrapper<ProfileChangeRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ProfileChangeRequest::getStatus, status);
        }
        wrapper.orderByAsc(ProfileChangeRequest::getStatus)
                .orderByDesc(ProfileChangeRequest::getCreatedAt)
                .last("LIMIT 200");
        List<ProfileChangeRequest> list = mapper.selectList(wrapper);
        return list.stream().map(r -> {
            User u = userMapper.selectById(r.getUserId());
            String username = u != null ? u.getUsername() : "已删除用户";
            String nickname = u != null ? u.getNickname() : "";
            return toResponse(r, username, nickname);
        }).collect(Collectors.toList());
    }

    /**
     * 通过审核：将变更应用到用户资料
     */
    @Transactional
    public ProfileChangeRequestResponse approve(Long id) {
        ProfileChangeRequest req = getRequest(id);
        if (req.getStatus() != ProfileChangeRequest.STATUS_PENDING) {
            throw new BusinessException(400, "该申请已处理");
        }
        User user = userMapper.selectById(req.getUserId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在，无法应用变更");
        }

        switch (req.getFieldName()) {
            case "nickname" -> user.setNickname(req.getNewValue());
            case "username" -> {
                checkUsernameUnique(req.getNewValue(), user.getId());
                user.setUsername(req.getNewValue());
            }
            case "avatar" -> user.setAvatarUrl(req.getNewValue());
            default -> throw new BusinessException(400, "不支持的变更字段");
        }
        userMapper.updateById(user);

        req.setStatus(ProfileChangeRequest.STATUS_APPROVED);
        req.setReviewedAt(LocalDateTime.now());
        mapper.updateById(req);
        log.info("审核通过资料变更: id={}, userId={}, field={}", id, user.getId(), req.getFieldName());
        return toResponse(req, user.getUsername(), user.getNickname());
    }

    /**
     * 拒绝审核
     */
    @Transactional
    public ProfileChangeRequestResponse reject(Long id, String remark) {
        ProfileChangeRequest req = getRequest(id);
        if (req.getStatus() != ProfileChangeRequest.STATUS_PENDING) {
            throw new BusinessException(400, "该申请已处理");
        }
        req.setStatus(ProfileChangeRequest.STATUS_REJECTED);
        req.setReviewRemark(remark);
        req.setReviewedAt(LocalDateTime.now());
        mapper.updateById(req);
        log.info("拒绝资料变更审核: id={}, remark={}", id, remark);
        return toResponse(req, null, null);
    }

    private ProfileChangeRequest getRequest(Long id) {
        ProfileChangeRequest req = mapper.selectById(id);
        if (req == null) {
            throw new BusinessException(404, "审核记录不存在");
        }
        return req;
    }

    private void checkUsernameUnique(String username, Long excludeUserId) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .ne(excludeUserId != null, User::getId, excludeUserId)
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已被占用");
        }
    }

    private ProfileChangeRequestResponse toResponse(ProfileChangeRequest r, String username, String nickname) {
        ProfileChangeRequestResponse resp = new ProfileChangeRequestResponse();
        resp.setId(r.getId());
        resp.setUserId(r.getUserId());
        resp.setUsername(username);
        resp.setNickname(nickname);
        resp.setFieldName(r.getFieldName());
        resp.setOldValue(r.getOldValue());
        resp.setNewValue(r.getNewValue());
        resp.setStatus(r.getStatus());
        resp.setReviewRemark(r.getReviewRemark());
        resp.setReviewedAt(r.getReviewedAt());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
