package com.mindspace.service;

import com.mindspace.dto.LoginDTO;
import com.mindspace.dto.RegisterDTO;
import com.mindspace.dto.UpdatePasswordDTO;
import com.mindspace.dto.UpdateProfileDTO;
import com.mindspace.vo.LoginVO;
import com.mindspace.vo.UserVO;
import jakarta.validation.Valid;

public interface UserService {

    //注册方法
    void register(RegisterDTO dto);
    //登录方法
    LoginVO login(@Valid LoginDTO dto);
    //获取当前用户信息
    UserVO getCurrentUser(Long userId);
    //更新用户信息
    UserVO updateProfile(Long userId, @Valid UpdateProfileDTO updateProfileDTO);
    //更新密码
    void updatePassword(Long userId, @Valid UpdatePasswordDTO updatePasswordDTO);
}