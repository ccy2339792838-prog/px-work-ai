package com.pxwork.api.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.pxwork.common.utils.Result;
import com.pxwork.common.utils.StpUserUtil;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Object> handleNotLoginException(NotLoginException exception) {
        String message = StpUserUtil.TYPE.equals(exception.getLoginType()) ? "学员未登录或登录已过期" : "管理员未登录或登录已过期";
        return Result.fail(401, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.fail(exception.getMessage());
    }
}
