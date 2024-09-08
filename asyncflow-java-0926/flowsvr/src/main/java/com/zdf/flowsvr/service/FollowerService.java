package com.zdf.flowsvr.service;

import java.util.List;
import java.util.Set;

public interface FollowerService {
  void addFollowers(String userId, List<String> followerIds);
  Set<String> getFollowers(String userId);
  void splitSet(String userId, int splitFactor);

  Set<String> getCombinedFollowers(String userId,int splitFactor);
}
