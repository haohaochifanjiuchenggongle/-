package com.zdf.flowsvr.service.impl;

import com.zdf.flowsvr.service.FollowerService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

@Service
public class FollowerServiceImpl implements FollowerService {

  private static final String REDIS_FOLLOWER_KEY = "user:fans:";

  @Autowired
  private Jedis jedis;

  @Override
  public void addFollowers(String userId, List<String> followerIds) {
    String key = REDIS_FOLLOWER_KEY + userId;
    for (String followerId : followerIds) {
      jedis.sadd(key, followerId);
    }
  }
  @Override
  public Set<String> getFollowers(String userId) {
    userId = userId.trim(); // 去除首尾的空白字符
    String key = REDIS_FOLLOWER_KEY + userId;
    Set<String> followers = jedis.smembers(key);
    System.out.println("Fetched followers: " + followers);
    return followers;
  }

  @Override
  public void splitSet(String userId, int splitFactor) {
    String key = REDIS_FOLLOWER_KEY + userId;
    Long size = jedis.scard(key);
    if (size != null && size > splitFactor) {
      for (int i = 0; i < splitFactor; i++) {
        // 之前错误地将 List 转换为 Set
        // Set<String> subset = (Set<String>) jedis.srandmember(key, (int) (size / splitFactor));

        // 正确的处理方式
        List<String> subsetList = jedis.srandmember(key, (int) (size / splitFactor));
        Set<String> subset = new HashSet<>(subsetList);
        String newKey = key + ":" + i;
        for (String follower : subset) {
          jedis.sadd(newKey, follower);
        }
      }
      jedis.del(key);
    }
  }
  public Set<String> getCombinedFollowers(String userId, int splitFactor) {
    Set<String> combinedFollowers = new HashSet<>();

    // 遍历所有拆分后的集合
    for (int i = 0; i < splitFactor; i++) {
      String splitKey = REDIS_FOLLOWER_KEY + userId + ":" + i;
      Set<String> splitSet = jedis.smembers(splitKey);
      combinedFollowers.addAll(splitSet);
    }

    return combinedFollowers;
  }

}

