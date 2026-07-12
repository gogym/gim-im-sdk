package io.getbit.gim.core.api;

import lombok.Data;

/**
 * 统一 API 响应包装
 *
 * @author gogym
 */
@Data
public class ApiResult<T> {

    private String code;
    private String msg;
    private T data;

    private ApiResult() {}

    public static <T> ApiResult<T> ok() {
        ApiResult<T> r = new ApiResult<>();
        r.code = "0";
        r.msg = "success";
        return r;
    }

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> r = ok();
        r.data = data;
        return r;
    }

    public static <T> ApiResult<T> fail(String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = "-1";
        r.msg = msg;
        return r;
    }

    public static <T> ApiResult<T> fail(String code, String msg) {
        ApiResult<T> r = new ApiResult<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
