package com.mindspace.common.exception;

import com.mindspace.common.result.ResultCode;
import lombok.Getter;

@Getter
/*
* 当前为业务异常类
* 任何业务错误就调用当前类
* 这里主要是去ResultCode枚举找对应的code和message
* 然后冒泡自动转发给全局处理
* */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码
     */
    private final Integer code;

    /**
     * 只传错误码枚举，提示信息用枚举里的默认提示
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 传错误码枚举 + 自定义提示，code 仍用枚举里的
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    /**
     * 完全自定义 code 和提示
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}