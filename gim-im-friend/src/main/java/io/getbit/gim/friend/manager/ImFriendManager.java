package io.getbit.gim.friend.manager;

import io.getbit.gim.friend.entity.ImFriend;
import io.getbit.gim.friend.entity.ImFriendGroup;
import io.getbit.gim.friend.repository.ImFriendGroupRepository;
import io.getbit.gim.friend.repository.ImFriendRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImFriendManager {

    private final ImFriendRepository friendRepo;
    private final ImFriendGroupRepository groupRepo;

    public ImFriendManager(ImFriendRepository friendRepo, ImFriendGroupRepository groupRepo) {
        this.friendRepo = friendRepo;
        this.groupRepo = groupRepo;
    }

    public List<ImFriend> findFriendsByUserId(Long userId) {
        return friendRepo.findFriendsByUserId(userId);
    }

    public List<ImFriend> findFriendsByGroupId(Long userId, Long groupId) {
        return friendRepo.findFriendsByGroupId(userId, groupId);
    }

    public boolean isFriend(Long userId, Long friendId) {
        List<ImFriend> friends = findFriendsByUserId(userId);
        return friends != null && friends.stream()
                .anyMatch(f -> f.getFriendId().equals(friendId)
                        && f.getStatus() != null && f.getStatus() == 1);
    }

    public ImFriend findFriend(Long userId, Long friendId) {
        return friendRepo.selectByUserIdAndFriendId(userId, friendId);
    }

    public boolean insertFriend(ImFriend friend) {
        return friendRepo.insert(friend) > 0;
    }

    public boolean updateFriend(ImFriend friend) {
        return friendRepo.updateById(friend) > 0;
    }

    public int deleteFriend(Long userId, Long friendId) {
        return friendRepo.deleteByUserIdAndFriendId(userId, friendId);
    }

    public List<ImFriendGroup> findFriendGroupsByUserId(Long userId) {
        return groupRepo.findByUserId(userId);
    }

    public ImFriendGroup findFriendGroupById(Long groupId) {
        return groupRepo.selectById(groupId);
    }

    public boolean insertFriendGroup(ImFriendGroup group) {
        return groupRepo.insert(group) > 0;
    }

    public boolean updateFriendGroup(ImFriendGroup group) {
        return groupRepo.updateById(group) > 0;
    }

    public int deleteFriendGroup(Long groupId) {
        return groupRepo.deleteById(groupId);
    }
}
