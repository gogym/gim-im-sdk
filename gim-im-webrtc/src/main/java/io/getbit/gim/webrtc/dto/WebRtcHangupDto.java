package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC 挂断请求/响应数据传输对象
 *
 * @author gogym
 */
@Data
public class WebRtcHangupDto {

    /**
     * 挂断原因
     * normal-正常挂断, timeout-超时, reject-拒绝, cancel-取消, busy-忙线, error-错误
     */
    private String reason;
}
