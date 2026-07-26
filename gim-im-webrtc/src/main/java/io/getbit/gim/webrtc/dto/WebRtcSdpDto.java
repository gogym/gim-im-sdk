package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC SDP DTO
 * 用于 offer/answer 信令（signalType=1,2）
 *
 * @author gogym
 */
@Data
public class WebRtcSdpDto {

    /**
     * SDP 内容
     */
    private String sdp;
}
