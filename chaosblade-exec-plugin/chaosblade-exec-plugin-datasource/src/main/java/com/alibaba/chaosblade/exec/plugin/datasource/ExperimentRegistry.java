package com.alibaba.chaosblade.exec.plugin.datasource;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class ExperimentRegistry {
  interface ReleaseObserver {
    void afterRelease(ConnectionHolder holder, String reason);
  }

  private final Map<String, ConnectionHolder> holders = new ConcurrentHashMap<String, ConnectionHolder>();
  private final Map<String, String> identities = new ConcurrentHashMap<String, String>();
  private final Map<String, ConnectionPoolResult> completed = new ConcurrentHashMap<String, ConnectionPoolResult>();
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "chaosblade-datasource-ttl");
      thread.setDaemon(true);
      return thread;
    }
  });
  private final ReleaseObserver observer;

  ExperimentRegistry(ReleaseObserver observer) {
    this.observer = observer;
  }

  synchronized ConnectionHolder register(String uid, String identityKey, long expiresAt, ConnectionPoolResult result) {
    ConnectionHolder existing = holders.get(uid);
    if (existing != null) {
      return existing;
    }
    String owner = identities.get(identityKey);
    if (owner != null && !owner.equals(uid)) {
      throw new IllegalStateException("RESOURCE_BUSY: datasource already owned by " + owner);
    }
    ConnectionHolder holder = new ConnectionHolder(uid, identityKey, expiresAt, result);
    holders.put(uid, holder);
    identities.put(identityKey, uid);
    long delay = Math.max(1L, expiresAt - System.currentTimeMillis());
    scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        release(uid, "TTL");
      }
    }, delay, TimeUnit.MILLISECONDS);
    return holder;
  }

  synchronized ConnectionPoolResult release(String uid, String reason) {
    ConnectionHolder holder = holders.get(uid);
    if (holder == null) {
      return completed.get(uid);
    }
    int closed = holder.release();
    holder.result.closedCount += closed;
    holder.result.closeFailedCount = holder.size();
    holder.result.releaseReason = reason;
    observer.afterRelease(holder, reason);
    if (holder.size() == 0) {
      holder.result.state = "RELEASED";
      holders.remove(uid);
      identities.remove(holder.identityKey, uid);
      completed.put(uid, holder.result);
      scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          completed.remove(uid);
        }
      }, 120, TimeUnit.SECONDS);
    } else {
      holder.result.state = "RELEASE_INCOMPLETE";
    }
    return holder.result;
  }

  ConnectionPoolResult result(String uid) {
    ConnectionHolder holder = holders.get(uid);
    return holder == null ? completed.get(uid) : holder.result;
  }

  void releaseAll(String reason) {
    for (String uid : new ArrayList<String>(holders.keySet())) {
      release(uid, reason);
    }
  }
}
