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

import com.alibaba.chaosblade.exec.common.model.Model;

public final class ConnectionPoolRequest {
  private final String dataSourceName;
  private final int targetPercent;
  private final long timeoutSeconds;

  private ConnectionPoolRequest(String dataSourceName, int targetPercent, long timeoutSeconds) {
    this.dataSourceName = dataSourceName;
    this.targetPercent = targetPercent;
    this.timeoutSeconds = timeoutSeconds;
  }

  public static ConnectionPoolRequest from(Model model) {
    String name = valueOrDefault(model, DataSourceConstant.DATA_SOURCE_NAME, "coreDataSource").trim();
    if (name.length() == 0) {
      throw new IllegalArgumentException("INVALID_DATASOURCE_NAME: data-source-name must not be empty");
    }
    int percent = parseInt(model, DataSourceConstant.TARGET_PERCENT, 100);
    if (percent < 1 || percent > 100) {
      throw new IllegalArgumentException("INVALID_TARGET_PERCENT: target-percent must be between 1 and 100");
    }
    long timeout = parseLong(model, DataSourceConstant.TIMEOUT, 60L);
    if (timeout <= 0) {
      throw new IllegalArgumentException("INVALID_TIMEOUT: timeout must be positive seconds");
    }
    return new ConnectionPoolRequest(name, percent, timeout);
  }

  private static String valueOrDefault(Model model, String key, String defaultValue) {
    String value = model.getAction().getFlag(key);
    return value == null || value.trim().length() == 0 ? defaultValue : value;
  }

  private static int parseInt(Model model, String key, int defaultValue) {
    String value = model.getAction().getFlag(key);
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("INVALID_" + key.toUpperCase().replace('-', '_') + ": must be an integer");
    }
  }

  private static long parseLong(Model model, String key, long defaultValue) {
    String value = model.getAction().getFlag(key);
    if (value == null || value.trim().length() == 0) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("INVALID_TIMEOUT: timeout must be integer seconds");
    }
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

  public int getTargetPercent() {
    return targetPercent;
  }

  public long getTimeoutSeconds() {
    return timeoutSeconds;
  }
}
