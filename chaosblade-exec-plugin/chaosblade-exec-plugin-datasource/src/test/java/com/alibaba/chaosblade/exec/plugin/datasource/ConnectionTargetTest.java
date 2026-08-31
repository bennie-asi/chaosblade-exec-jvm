package com.alibaba.chaosblade.exec.plugin.datasource;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConnectionTargetTest {
  @Test
  public void shouldRoundUpAndSubtractActive() {
    ConnectionTarget target = ConnectionTarget.calculate(5, 2, 81);
    assertEquals(5, target.getTargetConnections());
    assertEquals(3, target.getConnectionsToHold());
  }
}
