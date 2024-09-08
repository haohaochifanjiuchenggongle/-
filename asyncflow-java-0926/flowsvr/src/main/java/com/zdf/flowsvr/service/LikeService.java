package com.zdf.flowsvr.service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LikeService {

  private static final int SEGMENT_COUNT = 10;
  private final JedisPool jedisPool;
  private final ReentrantLock[] locks = new ReentrantLock[SEGMENT_COUNT];

  public LikeService(JedisPool jedisPool) {
    this.jedisPool = jedisPool;
    for (int i = 0; i < SEGMENT_COUNT; i++) {
      locks[i] = new ReentrantLock();
    }
  }

  // 不使用锁的点赞操作
  public void likeWithoutLock(String momentId, String userId) {
    try (Jedis jedis = jedisPool.getResource()) {
      String key = "like:moment:" + momentId;
      jedis.sadd(key, userId);
    }
  }

  // 使用分段锁的点赞操作
  public void likeWithSegmentedLock(String momentId, String userId) {
    int segmentIndex = Math.abs((momentId + userId).hashCode()) % SEGMENT_COUNT;
    ReentrantLock lock = locks[segmentIndex];
    lock.lock();
    try (Jedis jedis = jedisPool.getResource()) {
      String key = "like:moment:" + momentId + ":" + segmentIndex;
      jedis.sadd(key, userId);
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    // 配置Jedis连接池
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(500);  // 增加最大连接数
    poolConfig.setMaxIdle(100);    // 增加最大空闲连接数
    poolConfig.setMinIdle(50);    // 设置最小空闲连接数
    poolConfig.setTestOnBorrow(false); // 关闭借用时的连接验证
    poolConfig.setTestOnReturn(false); // 关闭归还时的连接验证
    poolConfig.setTestWhileIdle(true); // 保留空闲连接验证，确保空闲连接的健康
    poolConfig.setBlockWhenExhausted(true);
    poolConfig.setMaxWaitMillis(10000); // 等待时间设置为10秒

    JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379, 10000);

    LikeService likeService = new LikeService(jedisPool);

    String momentId = "moment123";
    int threadCount = 100;  // 增加并发线程数

    // 测试不使用锁的点赞操作
    ExecutorService executorWithoutLock = Executors.newFixedThreadPool(threadCount);
    long startWithoutLock = System.currentTimeMillis();
    for (int i = 0; i < threadCount; i++) {
      String userId = "user" + i;
      executorWithoutLock.execute(() -> likeService.likeWithoutLock(momentId, userId));
    }
    executorWithoutLock.shutdown();
    executorWithoutLock.awaitTermination(1, TimeUnit.MINUTES);
    long endWithoutLock = System.currentTimeMillis();
    System.out.println("不使用锁的点赞操作总耗时: " + (endWithoutLock - startWithoutLock) + "ms");

    // 清理之前的点赞数据
    try (Jedis jedis = jedisPool.getResource()) {
      for (int i = 0; i < SEGMENT_COUNT; i++) {
        jedis.del("like:moment:" + momentId + ":" + i);
      }
      jedis.del("like:moment:" + momentId);
    }

    // 测试使用分段锁的点赞操作
    ExecutorService executorWithLock = Executors.newFixedThreadPool(threadCount);
    long startWithLock = System.currentTimeMillis();
    for (int i = 0; i < threadCount; i++) {
      String userId = "user" + i;
      executorWithLock.execute(() -> likeService.likeWithSegmentedLock(momentId, userId));
    }
    executorWithLock.shutdown();
    executorWithLock.awaitTermination(1, TimeUnit.MINUTES);
    long endWithLock = System.currentTimeMillis();
    System.out.println("使用分段锁的点赞操作总耗时: " + (endWithLock - startWithLock) + "ms");

    // 关闭连接池
    jedisPool.close();
  }
}
