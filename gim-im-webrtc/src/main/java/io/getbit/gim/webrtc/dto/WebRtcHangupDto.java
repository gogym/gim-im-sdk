package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC 挂断 DTO
 * 用于 callHangup 信令（signalType=8）
 *
 * @author gogym
 */
@Data
public class WebRtcHangupDto {

    /**
     * 挂断原因
     * normal-正常挂断, timeout-超时, error-错误
     */
    private String reason;
}
