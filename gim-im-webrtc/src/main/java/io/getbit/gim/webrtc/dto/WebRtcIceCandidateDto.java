package io.getbit.gim.webrtc.dto;

import lombok.Data;

/**
 * WebRTC ICE 候选者数据传输对象
 * 用于交换 ICE Candidate 信息，帮助建立 P2P 连接
 *
 * @author gogym
 */
@Data
public class WebRtcIceCandidateDto {

    /**
     * ICE Candidate 的 SDP Mid
     */
    private String sdpMid;

    /**
     * ICE Candidate 的 SDP MLine Index
     */
    private Integer sdpMLineIndex;

    /**
     * ICE Candidate 内容
     */
    private String candidate;
}
