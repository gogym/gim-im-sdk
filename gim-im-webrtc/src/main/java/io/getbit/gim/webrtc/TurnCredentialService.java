package io.getbit.gim.webrtc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TurnCredentialService {
    @Value("${gim.webrtc.turn.stun-url:stun:127.0.0.1:3478}") private String stunUrl;
    @Value("${gim.webrtc.turn.turn-url:turn:127.0.0.1:3478}") private String turnUrl;
    @Value("${gim.webrtc.turn.shared-secret:secret}") private String sharedSecret;
    @Value("${gim.webrtc.turn.credential-ttl:3600}") private int credentialTtl;

    public Map<String, Object> generateTurnInfo() {
        long timestamp = System.currentTimeMillis() / 1000 + credentialTtl;
        String username = String.valueOf(timestamp);
        String password;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            password = Base64.getEncoder().encodeToString(mac.doFinal(username.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { log.error("TURN凭证生成失败", e); return null; }
        Map<String, Object> turnInfo = new HashMap<>();
        turnInfo.put("stunUrl", stunUrl); turnInfo.put("turnUrl", turnUrl);
        turnInfo.put("username", username); turnInfo.put("credential", password);
        return turnInfo;
    }
}
