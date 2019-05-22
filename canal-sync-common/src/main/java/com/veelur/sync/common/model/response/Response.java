package com.veelur.sync.common.model.response;


import com.veelur.sync.common.util.JsonUtils;

/**
 * @author <a href="mailto:wangchao.star@gmail.com">wangchao</a>
 * @version 1.0
 * @since 2017-08-31 17:58:00
 */
public class Response<T> {
    private int code = 0;
    private String message = "SUCCESS";
    private T data;

    public Response(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Response(T data) {
        this.data = data;
    }

    public static <T> Response<T> success(T result) {
        return new Response<>(result);
    }

    public static <T> Response<T> fail(int code, String message) {
        return new Response<>(code, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
