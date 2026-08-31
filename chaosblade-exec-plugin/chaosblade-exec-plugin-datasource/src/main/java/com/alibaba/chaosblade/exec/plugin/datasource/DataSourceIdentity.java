package com.alibaba.chaosblade.exec.plugin.datasource;

public final class DataSourceIdentity {
  private final int classLoaderId;
  private final int contextId;
  private final String beanName;
  private final String poolType;

  DataSourceIdentity(ClassLoader classLoader, Object context, String beanName, String poolType) {
    this.classLoaderId = System.identityHashCode(classLoader);
    this.contextId = System.identityHashCode(context);
    this.beanName = beanName;
    this.poolType = poolType;
  }

  public String key() {
    return classLoaderId + ":" + contextId + ":" + beanName;
  }

  public String getPoolType() {
    return poolType;
  }
}
