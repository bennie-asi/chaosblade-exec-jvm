package com.alibaba.chaosblade.exec.plugin.datasource;

public final class ResolvedDataSource {
  private final Object dataSource;
  private final ClassLoader classLoader;
  private final Object context;
  private final String beanName;

  ResolvedDataSource(Object dataSource, ClassLoader classLoader, Object context, String beanName) {
    this.dataSource = dataSource;
    this.classLoader = classLoader;
    this.context = context;
    this.beanName = beanName;
  }

  public Object getDataSource() {
    return dataSource;
  }

  public DataSourceIdentity identity(String poolType) {
    return new DataSourceIdentity(classLoader, context, beanName, poolType);
  }
}
