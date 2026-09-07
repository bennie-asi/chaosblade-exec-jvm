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

public final class ConnectionPoolResult {
  public String uid;
  public String state;
  public String reason;
  public String dataSourceName;
  public String poolType;
  public int maximumBefore;
  public int activeBefore;
  public int idleBefore;
  public int targetPercent;
  public int targetConnections;
  public int plannedHold;
  public int actualHold;
  public int failedHold;
  public int activeAfter;
  public int idleAfter;
  public int closedCount;
  public int closeFailedCount;
  public int activeAfterRelease;
  public int idleAfterRelease;
  public long startedAt;
  public long expiresAt;
  public String releaseReason;
}
