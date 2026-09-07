/*
 * Copyright 2025 The ChaosBlade Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.chaosblade.exec.plugin.datasource;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.Test;

public class PoolAdapterFactoryTest {
  @Test
  public void shouldSelectSupportedPoolByCapabilities() throws Exception {
    PoolAdapter adapter = PoolAdapterFactory.create(new FakeDruidDataSource());
    assertEquals("DRUID", adapter.getPoolType());
    assertEquals(4, adapter.snapshot().getMaximum());
  }

  public static final class FakeDruidDataSource implements DataSource {
    public int getMaxActive() {
      return 4;
    }

    public int getActiveCount() {
      return 1;
    }

    public int getPoolingCount() {
      return 3;
    }

    public Connection getConnection() {
      return null;
    }

    public Connection getConnection(String username, String password) {
      return null;
    }

    public PrintWriter getLogWriter() {
      return null;
    }

    public void setLogWriter(PrintWriter out) {}

    public void setLoginTimeout(int seconds) {}

    public int getLoginTimeout() {
      return 0;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return Logger.getGlobal();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException();
    }

    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
