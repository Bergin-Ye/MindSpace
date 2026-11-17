package com.mindspace.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;

/*
* 当前文件是用于返回给前端的结果类
* 包含code message data三部分
* */

@Data
@NoArgsConstructor
//创建一个用于返回结果的类 叫做Result 然后需要传值是一个泛型（意思就是还没定的类型啥都可以传）
public class Result<T> {
    private Integer code;
    private String message;
    //返回的业务数据
    private T data;

    //创建一个叫作success的静态方法 同时创建有参数和无参数的版本 并且需要传递的值T是一个泛型
    public static <T> Result<T> success(){
        //声明一个变量叫 result，类型是 Result<T>；用 new 创建一个 Result 对象，让 result 指向它。
        Result<T> result = new Result<>();
        //成功就去调用枚举中的SUCCESS然后赋值给code和message
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        return result;
    }

    //创建success的有参数版本 传递业务数据data
    public static <T> Result<T> success(T data){
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    // 创建一个叫作error的静态方法 用枚举里面的code和message创建一个Result对象
    public static <T> Result<T> error(ResultCode resultCode, String message) {
        Result<T> result = new Result<>();
        result.setCode(resultCode.getCode());
        result.setMessage(message);
        return result;
    }

    // 和上边一样但是传递的是自定义的错误码和错误信息
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return error(resultCode.getCode(), resultCode.getMessage());
    }
}
