package io.getbit.gim.friend.manager;

import io.getbit.gim.friend.entity.ImFriendRequestLog;
import io.getbit.gim.friend.repository.ImFriendRequestLogRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImFriendRequestLogManager {

    private final ImFriendRequestLogRepository repo;

    public ImFriendRequestLogManager(ImFriendRequestLogRepository repo) {
        this.repo = repo;
    }

    public List<ImFriendRequestLog> findByRequestId(Long requestId) {
        return repo.findByRequestId(requestId);
    }

    public boolean insert(ImFriendRequestLog log) {
        return repo.insert(log) > 0;
    }
}
