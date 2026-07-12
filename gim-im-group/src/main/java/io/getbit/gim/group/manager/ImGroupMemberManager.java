package io.getbit.gim.group.manager;

import io.getbit.gim.group.entity.ImGroupMember;
import io.getbit.gim.group.repository.ImGroupMemberRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class ImGroupMemberManager {
    private final ImGroupMemberRepository repo;
    public ImGroupMemberManager(ImGroupMemberRepository repo) { this.repo = repo; }
    public List<ImGroupMember> findActiveMembers(String groupId) { return repo.findActiveMembers(groupId); }
    public List<Long> findActiveMemberUserIds(String groupId) { return repo.findActiveMemberUserIds(groupId); }
    public ImGroupMember findMember(String groupId, Long userId) { return repo.findMember(groupId, userId); }
    public List<String> findGroupIdsByUserId(Long userId) { return repo.findGroupIdsByUserId(userId); }
    public long countActiveMembers(String groupId) { return repo.countActiveMembers(groupId); }
    public boolean isMember(String groupId, Long userId) { return repo.findMember(groupId, userId) != null; }
    public boolean isAdminOrOwner(String groupId, Long userId) { ImGroupMember m = repo.findMember(groupId, userId); return m != null && m.getRole() != null && m.getRole() >= 1; }
    public List<ImGroupMember> findAdminMembers(String groupId) { return repo.findAdminMembers(groupId); }
    public boolean insert(ImGroupMember member) { return repo.insert(member) > 0; }
    public boolean updateById(ImGroupMember member) { return repo.updateById(member) > 0; }
    public boolean removeMember(String groupId, Long userId) { return repo.removeMember(groupId, userId); }
    public boolean setMemberRole(String groupId, Long userId, int role) { return repo.setMemberRole(groupId, userId, role); }
    public boolean setMemberMuted(String groupId, Long userId, boolean muted) { return repo.setMemberMuted(groupId, userId, muted); }
}
