package com.alibaba.chaosblade.exec.plugin.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class ExperimentRegistryTest {
  @Test
  public void shouldSerializeByDatasourceAndReleaseExactlyOnce() {
    ExperimentRegistry registry = new ExperimentRegistry((holder, reason) -> { });
    ConnectionPoolResult firstResult = new ConnectionPoolResult();
    ConnectionHolder first = registry.register(
        "uid-1", "context:coreDataSource", System.currentTimeMillis() + 5000, firstResult);
    first.add(connection());

    try {
      registry.register("uid-2", "context:coreDataSource",
          System.currentTimeMillis() + 5000, new ConnectionPoolResult());
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().startsWith("RESOURCE_BUSY"));
    }

    ConnectionPoolResult released = registry.release("uid-1", "DESTROY");
    assertEquals("RELEASED", released.state);
    assertEquals(1, released.closedCount);
    assertEquals(0, released.closeFailedCount);
    assertEquals(1, registry.release("uid-1", "DESTROY").closedCount);

    ConnectionHolder second = registry.register(
        "uid-2", "context:coreDataSource", System.currentTimeMillis() + 5000,
        new ConnectionPoolResult());
    assertEquals("uid-2", second.uid);
    registry.releaseAll("TEST_CLEANUP");
  }

  @Test
  public void shouldReleaseAtHardTTL() throws Exception {
    CountDownLatch released = new CountDownLatch(1);
    ExperimentRegistry registry = new ExperimentRegistry((holder, reason) -> {
      if ("TTL".equals(reason)) {
        released.countDown();
      }
    });
    ConnectionPoolResult result = new ConnectionPoolResult();
    ConnectionHolder holder = registry.register(
        "ttl-uid", "context:ttl", System.currentTimeMillis() + 30, result);
    holder.add(connection());

    assertTrue("TTL release was not observed", released.await(2, TimeUnit.SECONDS));
    assertEquals("RELEASED", registry.result("ttl-uid").state);
    assertEquals("TTL", registry.result("ttl-uid").releaseReason);
    assertEquals(1, registry.result("ttl-uid").closedCount);
  }

  private Connection connection() {
    AtomicBoolean closed = new AtomicBoolean();
    return (Connection) Proxy.newProxyInstance(
        getClass().getClassLoader(),
        new Class<?>[] {Connection.class},
        (proxy, method, args) -> {
          if ("close".equals(method.getName())) {
            closed.set(true);
            return null;
          }
          if ("isClosed".equals(method.getName())) {
            return closed.get();
          }
          Class<?> type = method.getReturnType();
          if (type == boolean.class) {
            return false;
          }
          if (type == int.class) {
            return 0;
          }
          if (type == long.class) {
            return 0L;
          }
          return null;
        });
  }
}
