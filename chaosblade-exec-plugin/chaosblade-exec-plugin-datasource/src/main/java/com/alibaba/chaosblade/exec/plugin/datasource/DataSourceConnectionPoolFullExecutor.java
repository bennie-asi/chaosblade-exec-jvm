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

import com.alibaba.chaosblade.exec.common.exception.ExperimentException;
import com.alibaba.chaosblade.exec.common.model.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;

public final class DataSourceConnectionPoolFullExecutor {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final SpringContextResolver resolver;
  private final ExperimentRegistry registry;

  public DataSourceConnectionPoolFullExecutor() {
    this(new SpringContextResolver());
  }

  DataSourceConnectionPoolFullExecutor(SpringContextResolver resolver) {
    this.resolver = resolver;
    this.registry =
        new ExperimentRegistry(
            new ExperimentRegistry.ReleaseObserver() {
              @Override
              public void afterRelease(ConnectionHolder holder, String reason) {
                refreshReleaseMetrics(holder);
              }
            });
  }

  public void create(String uid, Model model) throws ExperimentException {
    ConnectionPoolRequest request;
    try {
      request = ConnectionPoolRequest.from(model);
    } catch (RuntimeException e) {
      throw new ExperimentException(e.getMessage(), e);
    }
    if (registry.result(uid) != null) {
      return;
    }
    long startedAt = System.currentTimeMillis();
    long expiresAt = startedAt + request.getTimeoutSeconds() * 1000L;
    ResolvedDataSource resolved;
    PoolAdapter adapter;
    try {
      resolved = resolver.resolve(request.getDataSourceName());
      adapter = PoolAdapterFactory.create(resolved.getDataSource());
      probe(adapter);
    } catch (Exception e) {
      throw new ExperimentException(message(e), e);
    }

    ConnectionHolder holder = null;
    try {
      PoolSnapshot before = adapter.snapshot();
      ConnectionTarget target =
          ConnectionTarget.calculate(
              before.getMaximum(), before.getActive(), request.getTargetPercent());
      ConnectionPoolResult result =
          initialResult(uid, request, adapter, before, target, startedAt, expiresAt);
      if (target.getConnectionsToHold() == 0) {
        result.state = "ALREADY_AT_TARGET";
        returnWithoutHolder(uid, result);
        return;
      }
      holder =
          registry.register(uid, resolved.identity(adapter.getPoolType()).key(), expiresAt, result);
      for (int i = 0; i < target.getConnectionsToHold(); i++) {
        if (System.currentTimeMillis() >= expiresAt) {
          throw new IllegalStateException("EXPERIMENT_EXPIRED: hard TTL reached while acquiring");
        }
        Connection connection = adapter.borrow();
        holder.add(connection);
        result.actualHold++;
      }
      PoolSnapshot after = adapter.snapshot();
      result.activeAfter = after.getActive();
      result.idleAfter = after.getIdle();
      if (after.getActive() < target.getTargetConnections()) {
        result.failedHold = target.getTargetConnections() - after.getActive();
        result.state = "TARGET_NOT_REACHED";
        registry.release(uid, "CREATE_ROLLBACK");
        throw new IllegalStateException("TARGET_NOT_REACHED: active connections below target");
      }
      result.state = "ACTIVE";
    } catch (Exception e) {
      if (holder != null) {
        registry.release(uid, "CREATE_ROLLBACK");
      }
      throw new ExperimentException(message(e), e);
    }
  }

  private void returnWithoutHolder(String uid, ConnectionPoolResult result) {
    long shortExpiry = System.currentTimeMillis() + 1L;
    ConnectionHolder holder = registry.register(uid, "terminal:" + uid, shortExpiry, result);
    registry.release(holder.uid, "NO_EFFECT");
    result.state = "ALREADY_AT_TARGET";
  }

  private void probe(PoolAdapter adapter) throws Exception {
    Connection connection = adapter.borrow();
    try {
      if (connection == null) {
        throw new IllegalStateException("PROBE_FAILED: datasource returned null connection");
      }
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  public void destroy(String uid) throws ExperimentException {
    ConnectionPoolResult result = registry.release(uid, "DESTROY");
    if (result == null) {
      throw new ExperimentException("NOT_FOUND: datasource experiment " + uid);
    }
    if ("RELEASE_INCOMPLETE".equals(result.state)) {
      throw new ExperimentException(
          "RELEASE_INCOMPLETE: some owned connections could not be closed");
    }
  }

  public String resultJson(String uid) {
    ConnectionPoolResult result = registry.result(uid);
    if (result == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(result);
    } catch (Exception e) {
      return "{\"uid\":\"" + uid + "\",\"state\":\"RESULT_SERIALIZATION_FAILED\"}";
    }
  }

  public void unload() {
    registry.releaseAll("AGENT_UNLOAD");
  }

  private ConnectionPoolResult initialResult(
      String uid,
      ConnectionPoolRequest request,
      PoolAdapter adapter,
      PoolSnapshot before,
      ConnectionTarget target,
      long startedAt,
      long expiresAt) {
    ConnectionPoolResult result = new ConnectionPoolResult();
    result.uid = uid;
    result.state = "ACQUIRING";
    result.dataSourceName = request.getDataSourceName();
    result.poolType = adapter.getPoolType();
    result.maximumBefore = before.getMaximum();
    result.activeBefore = before.getActive();
    result.idleBefore = before.getIdle();
    result.targetPercent = request.getTargetPercent();
    result.targetConnections = target.getTargetConnections();
    result.plannedHold = target.getConnectionsToHold();
    result.startedAt = startedAt;
    result.expiresAt = expiresAt;
    return result;
  }

  private void refreshReleaseMetrics(ConnectionHolder holder) {
    // Pool metrics are best-effort after release; ownership cleanup is the hard success criterion.
    holder.result.activeAfterRelease = -1;
    holder.result.idleAfterRelease = -1;
  }

  private String message(Throwable throwable) {
    String value = throwable.getMessage();
    return value == null || value.length() == 0 ? throwable.getClass().getSimpleName() : value;
  }
}
