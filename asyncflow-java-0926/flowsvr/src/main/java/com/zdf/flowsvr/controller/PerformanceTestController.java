package com.zdf.flowsvr.controller;


import com.zdf.flowsvr.service.FollowerService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/performance")
public class PerformanceTestController {

  @Autowired
  private FollowerService followerService;

  @PostMapping("/addFollowers")
  public ResponseEntity<String> addFollowers(@RequestParam String userId, @RequestParam int count) {
    List<String> followerIds = IntStream.range(0, count)
        .mapToObj(i -> "follower" + i)
        .collect(Collectors.toList());
    long start = System.currentTimeMillis();
    followerService.addFollowers(userId, followerIds);
    long end = System.currentTimeMillis();
    return ResponseEntity.ok("添加 " + count + " 个粉丝耗时: " + (end - start) + "ms");
  }

  @GetMapping("/getFollowers")
  public ResponseEntity<String> getFollowers(@RequestParam String userId) {
    long start = System.currentTimeMillis();
    Set<String> followers = followerService.getFollowers(userId); // 直接获取粉丝集合
    System.out.println(followers.size());
    long end = System.currentTimeMillis();
    return ResponseEntity.ok("获取 " + followers.size() + " 个粉丝耗时: " + (end - start) + "ms");
  }


  @PostMapping("/splitSet")
  public ResponseEntity<String> splitSet(@RequestParam String userId, @RequestParam int splitFactor) {
    long start = System.currentTimeMillis();
    followerService.splitSet(userId, splitFactor);
    long end = System.currentTimeMillis();
    return ResponseEntity.ok("拆分粉丝列表耗时: " + (end - start) + "ms");
  }
  @GetMapping("/getCombinedFollowers")
  public ResponseEntity<String> getCombinedFollowers(@RequestParam String userId, @RequestParam int splitFactor) {
    long start = System.currentTimeMillis();
    Set<String> combinedFollowers = followerService.getCombinedFollowers(userId, splitFactor);
    long end = System.currentTimeMillis();
    return ResponseEntity.ok("拆分粉丝列表耗时: " + (end - start) + "ms");
  }

}
