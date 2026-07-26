package com.prompttraining.module.auth.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名格式不正确，需为4-20位字母、数字或下划线")
    private String username;

    /** 昵称，可选，默认与用户名相同 */
    private String nickname;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "密码格式不正确，需为6-20位且包含字母和数字")
    private String password;
}
