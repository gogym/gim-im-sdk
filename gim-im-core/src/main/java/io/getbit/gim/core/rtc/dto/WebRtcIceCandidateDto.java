package io.getbit.gim.core.rtc.dto;

import lombok.Data;

/**
 * WebRTC ICE 候选者 DTO
 * 用于 iceCandidate 信令（signalType=3）
 *
 * @author gogym
 */
@Data
public class WebRtcIceCandidateDto {

    /**
     * ICE Candidate 内容
     */
    private String candidate;

    /**
     * ICE Candidate 的 SDP Mid
     */
    private String sdpMid;

    /**
     * ICE Candidate 的 SDP MLine Index
     */
    private Integer sdpMLineIndex;
}
