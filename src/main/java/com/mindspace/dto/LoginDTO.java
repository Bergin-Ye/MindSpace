package com.mindspace.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    @Email(message = "邮箱格式错误")
    @NotBlank(message = "邮箱不能为空")
    private  String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
