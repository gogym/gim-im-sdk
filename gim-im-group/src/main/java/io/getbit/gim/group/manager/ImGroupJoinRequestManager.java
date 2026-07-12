package io.getbit.gim.group.manager;

import io.getbit.gim.group.entity.ImGroupJoinRequest;
import io.getbit.gim.group.repository.ImGroupJoinRequestRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class ImGroupJoinRequestManager {
    private final ImGroupJoinRequestRepository repo;
    public ImGroupJoinRequestManager(ImGroupJoinRequestRepository repo) { this.repo = repo; }
    public List<ImGroupJoinRequest> findPendingRequests(String groupId) { return repo.findPendingRequests(groupId); }
    public ImGroupJoinRequest findPendingRequest(String groupId, Long userId) { return repo.findPendingRequest(groupId, userId); }
    public boolean insert(ImGroupJoinRequest request) { return repo.insert(request) > 0; }
    public boolean updateById(ImGroupJoinRequest request) { return repo.updateById(request) > 0; }
}
