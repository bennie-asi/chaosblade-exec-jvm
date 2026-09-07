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

package com.alibaba.chaosblade.exec.plugin.jvm.gc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.alibaba.chaosblade.exec.common.aop.EnhancerModel;
import com.alibaba.chaosblade.exec.common.model.Model;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class FullGCExecutorTest {

  @Test
  public void shouldStopAfterEffectCount() throws Exception {
    final AtomicInteger invocations = new AtomicInteger();
    FullGCExecutor executor =
        new FullGCExecutor(
            new FullGCExecutor.FullGCInvoker() {
              @Override
              public boolean invoke() {
                invocations.incrementAndGet();
                return true;
              }
            });
    try {
      executor.run(modelWithFlags("10", "3", "2"));
      waitUntilStopped(executor, 2);
      assertEquals(3, invocations.get());
      assertEquals(3, executor.getFullGCCount());
    } finally {
      executor.stop(null);
    }
  }

  @Test
  public void shouldStopAtHardTimeout() throws Exception {
    FullGCExecutor executor =
        new FullGCExecutor(
            new FullGCExecutor.FullGCInvoker() {
              @Override
              public boolean invoke() {
                return true;
              }
            });
    try {
      executor.run(modelWithFlags("20", "0", "1"));
      waitUntilStopped(executor, 2);
      int countAtStop = executor.getFullGCCount();
      TimeUnit.MILLISECONDS.sleep(100);
      assertTrue(countAtStop > 0);
      assertEquals(countAtStop, executor.getFullGCCount());
    } finally {
      executor.stop(null);
    }
  }

  @Test
  public void shouldRejectNonPositiveInterval() throws Exception {
    FullGCExecutor executor = new FullGCExecutor();
    try {
      executor.run(modelWithFlags("0", "1", "1"));
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("interval"));
      assertFalse(executor.isRunning());
      return;
    }
    throw new AssertionError("expected invalid interval to be rejected");
  }

  private EnhancerModel modelWithFlags(String interval, String count, String timeout) {
    Model model = new Model("jvm", "full-gc");
    model.getAction().addFlag("interval", interval);
    model.getAction().addFlag("effect-count", count);
    model.getAction().addFlag("timeout", timeout);
    EnhancerModel enhancerModel =
        new EnhancerModel(getClass().getClassLoader(), model.getMatcher());
    enhancerModel.merge(model);
    return enhancerModel;
  }

  private void waitUntilStopped(FullGCExecutor executor, int timeoutSeconds) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
    while (executor.isRunning() && System.nanoTime() < deadline) {
      TimeUnit.MILLISECONDS.sleep(10);
    }
    assertFalse("executor did not stop", executor.isRunning());
  }
}
