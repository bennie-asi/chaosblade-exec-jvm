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

public final class ConnectionTarget {
  private final int targetConnections;
  private final int connectionsToHold;

  private ConnectionTarget(int targetConnections, int connectionsToHold) {
    this.targetConnections = targetConnections;
    this.connectionsToHold = connectionsToHold;
  }

  public static ConnectionTarget calculate(int maximum, int active, int percent) {
    if (maximum <= 0 || active < 0 || percent < 1 || percent > 100) {
      throw new IllegalArgumentException("invalid pool metrics or target percent");
    }
    int target = (maximum * percent + 99) / 100;
    return new ConnectionTarget(target, Math.max(0, target - active));
  }

  public int getTargetConnections() {
    return targetConnections;
  }

  public int getConnectionsToHold() {
    return connectionsToHold;
  }
}
