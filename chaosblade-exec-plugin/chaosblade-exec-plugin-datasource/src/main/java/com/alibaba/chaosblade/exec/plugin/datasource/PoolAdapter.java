package com.alibaba.chaosblade.exec.plugin.datasource;

import java.sql.Connection;

public interface PoolAdapter {
  String getPoolType();

  PoolSnapshot snapshot() throws Exception;

  Connection borrow() throws Exception;
}
