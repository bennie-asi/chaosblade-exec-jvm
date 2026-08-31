package com.alibaba.chaosblade.exec.plugin.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alibaba.chaosblade.exec.common.model.Model;
import org.junit.Test;

public class ConnectionPoolRequestTest {
  @Test
  public void shouldApplyDefaults() {
    ConnectionPoolRequest request = ConnectionPoolRequest.from(new Model("datasource", "connectionpoolfull"));
    assertEquals("coreDataSource", request.getDataSourceName());
    assertEquals(100, request.getTargetPercent());
    assertEquals(60L, request.getTimeoutSeconds());
  }

  @Test
  public void shouldRejectInvalidFlags() {
    Model model = new Model("datasource", "connectionpoolfull");
    model.getAction().addFlag("target-percent", "101");
    try {
      ConnectionPoolRequest.from(model);
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().startsWith("INVALID_TARGET_PERCENT"));
      return;
    }
    throw new AssertionError("expected invalid target percent");
  }
}
