package com.alibaba.chaosblade.exec.plugin.datasource;

import java.lang.reflect.Method;
import java.sql.Connection;
import javax.sql.DataSource;

public final class PoolAdapterFactory {
  private PoolAdapterFactory() {}

  public static PoolAdapter create(Object candidate) {
    if (!(candidate instanceof DataSource)) {
      throw new IllegalArgumentException("DATASOURCE_NOT_FOUND: bean is not javax.sql.DataSource");
    }
    Class<?> type = candidate.getClass();
    if (hasMethod(type, "getHikariPoolMXBean") && hasMethod(type, "getMaximumPoolSize")) {
      return new HikariAdapter((DataSource) candidate);
    }
    if (hasMethod(type, "getMaxActive") && hasMethod(type, "getActiveCount") && hasMethod(type, "getPoolingCount")) {
      return new DruidAdapter((DataSource) candidate);
    }
    throw new IllegalArgumentException("UNSUPPORTED_POOL: " + type.getName());
  }

  private static boolean hasMethod(Class<?> type, String name) {
    try {
      type.getMethod(name);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  private abstract static class ReflectiveAdapter implements PoolAdapter {
    final DataSource dataSource;

    ReflectiveAdapter(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    Object invoke(Object target, String method) throws Exception {
      Method value = target.getClass().getMethod(method);
      if (!value.isAccessible()) {
        value.setAccessible(true);
      }
      return value.invoke(target);
    }

    int integer(Object target, String method) throws Exception {
      Object value = invoke(target, method);
      if (!(value instanceof Number)) {
        throw new IllegalStateException("METRICS_UNAVAILABLE: " + method);
      }
      return ((Number) value).intValue();
    }

    @Override
    public Connection borrow() throws Exception {
      return dataSource.getConnection();
    }
  }

  private static final class HikariAdapter extends ReflectiveAdapter {
    HikariAdapter(DataSource dataSource) {
      super(dataSource);
    }

    @Override
    public String getPoolType() {
      return "HIKARI";
    }

    @Override
    public PoolSnapshot snapshot() throws Exception {
      int maximum = integer(dataSource, "getMaximumPoolSize");
      Object mxBean = invoke(dataSource, "getHikariPoolMXBean");
      if (mxBean == null) {
        throw new IllegalStateException("METRICS_UNAVAILABLE: Hikari pool is not started");
      }
      return new PoolSnapshot(maximum, integer(mxBean, "getActiveConnections"), integer(mxBean, "getIdleConnections"));
    }
  }

  private static final class DruidAdapter extends ReflectiveAdapter {
    DruidAdapter(DataSource dataSource) {
      super(dataSource);
    }

    @Override
    public String getPoolType() {
      return "DRUID";
    }

    @Override
    public PoolSnapshot snapshot() throws Exception {
      return new PoolSnapshot(integer(dataSource, "getMaxActive"), integer(dataSource, "getActiveCount"), integer(dataSource, "getPoolingCount"));
    }
  }
}
