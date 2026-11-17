package com.mindspace.service;

import com.mindspace.dto.LoginDTO;
import com.mindspace.dto.RegisterDTO;
import com.mindspace.vo.LoginVO;
import com.mindspace.vo.UserVO;
import jakarta.validation.Valid;

public interface UserService {

    //注册方法
    void register(RegisterDTO dto);

    LoginVO login(@Valid LoginDTO dto);

    UserVO getCurrentUser(Long userId);
}