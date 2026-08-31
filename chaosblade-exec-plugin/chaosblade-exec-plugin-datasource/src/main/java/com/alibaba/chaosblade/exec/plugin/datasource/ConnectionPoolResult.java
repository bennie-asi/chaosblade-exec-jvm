package com.alibaba.chaosblade.exec.plugin.datasource;

public final class ConnectionPoolResult {
  public String uid;
  public String state;
  public String reason;
  public String dataSourceName;
  public String poolType;
  public int maximumBefore;
  public int activeBefore;
  public int idleBefore;
  public int targetPercent;
  public int targetConnections;
  public int plannedHold;
  public int actualHold;
  public int failedHold;
  public int activeAfter;
  public int idleAfter;
  public int closedCount;
  public int closeFailedCount;
  public int activeAfterRelease;
  public int idleAfterRelease;
  public long startedAt;
  public long expiresAt;
  public String releaseReason;
}
