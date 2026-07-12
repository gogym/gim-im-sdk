package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC 呼叫请求/响应数据传输对象
 *
 * @author gogym
 */
@Data
public class WebRtcCallDto {

    /**
     * 通话类型：audio-音频通话, video-视频通话
     */
    private String callType;

    /**
     * 拒绝原因（如 busy）
     */
    private String reason;
}
