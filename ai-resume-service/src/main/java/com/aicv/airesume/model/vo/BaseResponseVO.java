package com.aicv.airesume.model.vo;

/**
 * 基础响应VO
 */
public class BaseResponseVO {
    
    private Integer code;
    private String message;
    private Object data;
    
    public BaseResponseVO() {
        this.code = 200;
        this.message = "success";
    }
    
    public BaseResponseVO(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public BaseResponseVO(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public static BaseResponseVO success() {
        return new BaseResponseVO();
    }
    
    public static BaseResponseVO success(Object data) {
        return new BaseResponseVO(200, "success", data);
    }
    
    public static BaseResponseVO error(String message) {
        return new BaseResponseVO(500, message);
    }
    
    public static BaseResponseVO error(Integer code, String message) {
        return new BaseResponseVO(code, message);
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
}