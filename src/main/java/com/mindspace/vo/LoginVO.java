package com.mindspace.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
* loginVO 是登录接口的出参结构，
* 四个字段分别是 JWT、token 类型（Bearer）、
* 过期秒数、嵌套的 UserVO；它不是方法，是数据结构，
* Service 里 new LoginVO() 后 set 四个字段返回，
* 其中 user 字段直接复用 UserVO.from(user)，
* 前端一次拿到 token 和用户信息，不用再发第二次请求。
* */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserVO user;
}
