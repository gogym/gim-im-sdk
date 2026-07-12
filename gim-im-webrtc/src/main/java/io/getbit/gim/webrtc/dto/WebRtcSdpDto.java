package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC SDP 数据传输对象
 * 用于交换 Offer 和 Answer SDP 信息
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
