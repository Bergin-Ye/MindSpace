package com.mindspace.common.result;


import lombok.Getter;

@Getter

/*
* 当前文件是一个错误码枚举
* */

public enum ResultCode {

    // ---------- 通用 ----------
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),

    // ---------- 用户模块 1000+ ----------
    USERNAME_EXISTS(1001, "用户名已存在"),
    EMAIL_EXISTS(1002, "邮箱已被注册"),

    // ---------- 登录失败 ----------
    LOGIN_FAILED(1003, "邮箱或密码错误"),
    USER_NOT_EXIST(1004, "用户不存在"),

    // ---------- 修改密码对应的错误码 ----------
    PASSWORD_ERROR(1005, "原密码错误"),
    PASSWORD_SAME(1006, "新密码不能与原密码相同"),

    // ---------- 知识库 2000+ ----------
    KB_NOT_FOUND(2001, "知识库不存在"),
    KB_LAST_CANNOT_DELETE(2002, "至少保留一个知识库"),

    // ---------- 文件夹 2003+ ----------
    FOLDER_NOT_FOUND(2003, "文件夹不存在"),
    FOLDER_DEFAULT_CANNOT_DELETE(2004, "默认文件夹不能删除"),
    FOLDER_HAS_CHILDREN(2005, "请先删除或移动子文件夹"),
    FOLDER_PARENT_INVALID(2006, "父文件夹不存在或不属于当前知识库"),
    FOLDER_MOVE_INVALID(2007, "不能移动到自身或其子文件夹");

    private final Integer code;
    private final String message;

    //有参数构造方法 传入参数code和msg
    private ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }


}
