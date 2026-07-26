package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC 呼叫请求 DTO
 * 用于 callRequest 信令（signalType=4）
 *
 * @author gogym
 */
@Data
public class WebRtcCallDto {

    /**
     * 通话类型：audio-音频通话, video-视频通话
     */
    private String callType;
}
