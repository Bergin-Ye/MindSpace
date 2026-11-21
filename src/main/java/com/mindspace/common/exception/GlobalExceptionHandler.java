package com.mindspace.common.exception;

import com.mindspace.common.result.Result;
import com.mindspace.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.stream.Collectors;
/*
* 这里是全局异常处理
* 他会接受来自BusinessException的异常
* 然后把code 和 message拆分封装好 然后通过Result返回给前端
* */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：Service 主动抛出的
     */
    //BusinessException只接受业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常：@Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    /*
    * 没加这个异常处理 你超过最大文件限制他会走兜底异常
    * 加了这个他就走这个异常
    * */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制：{}", e.getMessage());
        return Result.error(ResultCode.FILE_SIZE_EXCEEDED);
    }

    /**
     * 兜底：所有没被上面接住的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }


    /**
     * 查询参数绑定失败：日期格式错误、page 不是数字等
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "参数格式错误";
        }
        log.warn("参数绑定失败：{}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }
}