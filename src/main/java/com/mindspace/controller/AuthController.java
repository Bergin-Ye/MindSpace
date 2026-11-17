package com.mindspace.controller;

import com.mindspace.common.result.Result;
import com.mindspace.dto.LoginDTO;
import com.mindspace.dto.RegisterDTO;
import com.mindspace.service.UserService;
import com.mindspace.vo.LoginVO;
import com.mindspace.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    //注册方法 去接受数据 返回类型是Result<Void>
    //先把数据转换成dto 然后去valid验证是否dto是否满足要求 校验不通过不执行
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        //把接收到的数据发送给服务层进行注册
        userService.register(dto);
        //拿到注册结果 返回成功
        return Result.success();
    }

    //登录方法 接收数据通过dto做校验 然后存入loginVO
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    //*
    // 这里是为了方便前端 假如你丢失了用户信息 你就跳转me 凭借token去拿用户信息
    // */
    @GetMapping("/me")
    public Result<UserVO> me(@AuthenticationPrincipal Long userId) {
        return Result.success(userService.getCurrentUser(userId));
    }
}