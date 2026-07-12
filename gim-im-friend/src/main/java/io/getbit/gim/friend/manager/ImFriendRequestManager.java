package io.getbit.gim.friend.manager;

import io.getbit.gim.friend.entity.ImFriendRequest;
import io.getbit.gim.friend.repository.ImFriendRequestRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImFriendRequestManager {

    private final ImFriendRequestRepository repo;

    public ImFriendRequestManager(ImFriendRequestRepository repo) {
        this.repo = repo;
    }

    public ImFriendRequest findLatestRequest(Long fromUserId, Long toUserId) {
        return repo.findLatestRequest(fromUserId, toUserId);
    }

    public List<ImFriendRequest> findAllByToUserId(Long toUserId) {
        return repo.findAllByToUserId(toUserId);
    }

    public List<ImFriendRequest> findSentByUserId(Long fromUserId) {
        return repo.findByFromUserId(fromUserId);
    }

    public ImFriendRequest findLatestBetween(Long userId1, Long userId2) {
        return repo.findLatestBetween(userId1, userId2);
    }

    public ImFriendRequest selectById(Long id) {
        return repo.selectById(id);
    }

    public boolean insert(ImFriendRequest request) {
        return repo.insert(request) > 0;
    }

    public boolean updateById(ImFriendRequest request) {
        return repo.updateById(request) > 0;
    }
}
