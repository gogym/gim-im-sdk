package io.getbit.gim.group.manager;

import io.getbit.gim.group.entity.ImGroup;
import io.getbit.gim.group.repository.ImGroupRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImGroupManager {
    private final ImGroupRepository repo;
    public ImGroupManager(ImGroupRepository repo) { this.repo = repo; }
    public ImGroup findByGroupId(String groupId) { return repo.findByGroupId(groupId); }
    public boolean insert(ImGroup group) { return repo.insert(group) > 0; }
    public boolean updateGroupInfo(String groupId, String name, String avatar, String announcement) { return repo.updateGroupInfo(groupId, name, avatar, announcement); }
    public boolean dissolveGroup(String groupId) { return repo.dissolveGroup(groupId); }
    public boolean setMuteAll(String groupId, boolean muteAll) { return repo.setMuteAll(groupId, muteAll); }
    public boolean transferOwner(String groupId, Long newOwnerId) { return repo.transferOwner(groupId, newOwnerId); }
    public boolean setJoinVerify(String groupId, boolean joinVerify) { return repo.setJoinVerify(groupId, joinVerify); }
}
