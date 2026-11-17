package com.mindspace.service;

import com.mindspace.dto.RegisterDTO;

public interface UserService {

    //注册方法
    void register(RegisterDTO dto);
}