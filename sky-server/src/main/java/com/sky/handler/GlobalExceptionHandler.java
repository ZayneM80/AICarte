package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(Exception ex) {
        log.error("未知异常：{}", ex.getMessage(), ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    @ExceptionHandler
    public Result exceptionHandler(DuplicateKeyException ex) {
        log.error("数据库唯一键冲突：{}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("username")) {
                return Result.error(MessageConstant.USERNAME_ALREADY_EXISTS);
            } else if (message.contains("phone")) {
                return Result.error(MessageConstant.PHONE_ALREADY_EXISTS);
            } else if (message.contains("id_number")) {
                return Result.error(MessageConstant.ID_NUMBER_ALREADY_EXISTS);
            }
        }
        
        return Result.error(MessageConstant.DATA_ALREADY_EXISTS);
    }

}
