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

import com.alibaba.chaosblade.exec.common.aop.EnhancerModel;
import com.alibaba.chaosblade.exec.common.util.ConfigUtil;
import com.alibaba.chaosblade.exec.plugin.jvm.JvmConstant;
import com.alibaba.chaosblade.exec.plugin.jvm.StoppableActionExecutor;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author shizhi.zhu@qunar.com */
public class FullGCExecutor implements StoppableActionExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FullGCExecutor.class);
  private static final int DEFAULT_INTERVAL_MILLIS = 1000;
  private static final int DEFAULT_TIMEOUT_SECONDS = 60;

  private volatile ScheduledExecutorService scheduledExecutorService;
  private final AtomicInteger fullGCCounter = new AtomicInteger();
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final FullGCInvoker fullGCInvoker;

  private final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

  public FullGCExecutor() {
    this(null);
  }

  FullGCExecutor(FullGCInvoker fullGCInvoker) {
    this.fullGCInvoker = fullGCInvoker;
  }

  @Override
  public synchronized void run(EnhancerModel enhancerModel) throws Exception {
    if (started.compareAndSet(false, true)) {
      final int interval =
          positiveFlag(enhancerModel, JvmConstant.FLAG_FULL_GC_INTERVAL, DEFAULT_INTERVAL_MILLIS);
      final int totalCount =
          ConfigUtil.getActionFlag(enhancerModel, JvmConstant.FLAG_FULL_GC_TOTAL_COUNT, 0);
      final int timeoutSeconds =
          positiveFlag(enhancerModel, JvmConstant.FLAG_FULL_GC_TIMEOUT, DEFAULT_TIMEOUT_SECONDS);
      if (totalCount < 0) {
        started.set(false);
        throw new IllegalArgumentException("effect-count must be greater than or equal to 0");
      }
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
      fullGCCounter.set(0);
      scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor(
              new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                  Thread thread = new Thread(r, "chaosblade-fgc-thread");
                  thread.setDaemon(true);
                  return thread;
                }
              });
      scheduledExecutorService.scheduleAtFixedRate(
          new Runnable() {
            @Override
            public void run() {
              try {
                if (System.nanoTime() >= deadline
                    || (totalCount > 0 && fullGCCounter.get() >= totalCount)) {
                  doStop();
                  return;
                }
                boolean triggered = fullGCInvoker == null ? doGc() : fullGCInvoker.invoke();
                if (triggered) {
                  int count = fullGCCounter.incrementAndGet();
                  LOGGER.debug("jvm full gc triggered, count:{}", count);
                  if (totalCount > 0 && count >= totalCount) {
                    doStop();
                  }
                }
              } catch (Exception e) {
                LOGGER.error("trigger full gc error", e);
              }
            }
          },
          0,
          interval,
          TimeUnit.MILLISECONDS);
    } else {
      LOGGER.warn("another executor is running now");
    }
  }

  @Override
  public synchronized void stop(EnhancerModel enhancerModel) throws Exception {
    doStop();
  }

  private void doStop() {
    if (started.compareAndSet(true, false)) {
      if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
        try {
          scheduledExecutorService.shutdownNow();
        } catch (Exception e) {
          LOGGER.error("shutdown executor error", e);
        }
        scheduledExecutorService = null;
        LOGGER.info("jvm full gc stopped");
      }
    }
  }

  private int positiveFlag(EnhancerModel enhancerModel, String name, int defaultValue) {
    int value = ConfigUtil.getActionFlag(enhancerModel, name, defaultValue);
    if (value <= 0) {
      started.set(false);
      throw new IllegalArgumentException(name + " must be greater than 0");
    }
    return value;
  }

  boolean isRunning() {
    return started.get();
  }

  int getFullGCCount() {
    return fullGCCounter.get();
  }

  private boolean doGc()
      throws MalformedObjectNameException, IntrospectionException, ReflectionException,
          InstanceNotFoundException, MBeanException {
    if (jvmBefore8()) {
      return doGCHistogramInSeparateProcess();
    }
    return doGcHistogramInMbean();
  }

  private boolean doGcHistogramInMbean()
      throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException,
          ReflectionException, MBeanException {
    Set<ObjectName> objectNames =
        mbeanServer.queryNames(new ObjectName("com.sun.management:type=DiagnosticCommand"), null);
    if (objectNames == null || objectNames.isEmpty()) {
      LOGGER.warn("no mBean found, exit");
      return false;
    }
    for (ObjectName name : objectNames) {
      MBeanInfo mBeanInfo = mbeanServer.getMBeanInfo(name);
      MBeanOperationInfo[] operations = mBeanInfo.getOperations();
      for (MBeanOperationInfo op : operations) {
        if (op.getName().equals("gcClassHistogram")) {
          String[] emptyStringArgs = {};
          Object[] dcmdArgs = {emptyStringArgs};
          String[] signature = {String[].class.getName()};
          Object invoke = mbeanServer.invoke(name, op.getName(), dcmdArgs, signature);
          LOGGER.debug("gc class histogram result: {}", invoke);
          return true;
        }
      }
    }
    LOGGER.warn("gcClassHistogram operation was not found");
    return false;
  }

  private boolean doGCHistogramInSeparateProcess() {
    int pid = getPid();
    String cmd = null;
    try {
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome == null || javaHome.trim().isEmpty()) {
        javaHome = System.getProperty("java.home");
      }
      if (javaHome.endsWith("/")) {
        javaHome = javaHome.substring(0, javaHome.length() - 1);
      }
      cmd = javaHome + "/bin/jmap";
      ProcessBuilder pb = new ProcessBuilder(cmd, "-histo:live", String.valueOf(pid));
      pb.redirectErrorStream(true);
      pb.redirectOutput(new File("/dev/null"));
      Process start = pb.start();
      int exitCode = start.waitFor();
      LOGGER.debug("gc class histogram process exited with {}, cmd:{}", exitCode, cmd);
      return exitCode == 0;
    } catch (Exception e) {
      LOGGER.error("exec jmap error, cmd:{}", cmd, e);
      return false;
    }
  }

  private static int getPid() {
    int pid = 0;
    try {
      String name = ManagementFactory.getRuntimeMXBean().getName();
      return Integer.parseInt(name.substring(0, name.indexOf('@')));
    } catch (Throwable e) {
      return pid;
    }
  }

  private boolean jvmBefore8() {
    String javaVersion = System.getProperty("java.version");
    String[] versions = javaVersion.split("\\.");
    if (Integer.parseInt(versions[1]) < 8 && Integer.parseInt(versions[0]) == 1) {
      return true;
    }
    return false;
  }

  interface FullGCInvoker {
    boolean invoke() throws Exception;
  }
}
