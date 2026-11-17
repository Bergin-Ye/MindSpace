package com.mindspace.controller;

import com.mindspace.common.result.Result;
import com.mindspace.dto.RegisterDTO;
import com.mindspace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}