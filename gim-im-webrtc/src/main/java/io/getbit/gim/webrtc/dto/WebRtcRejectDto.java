package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC 拒绝 DTO
 * 用于 callReject 信令（signalType=6）
 *
 * @author gogym
 */
@Data
public class WebRtcRejectDto {

    /**
     * 拒绝原因（如 reject、busy）
     */
    private String reason;
}
