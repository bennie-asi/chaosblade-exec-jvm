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

public final class DataSourceConstant {
  public static final String TARGET = "datasource";
  public static final String ACTION = "connectionpoolfull";
  public static final String DATA_SOURCE_NAME = "data-source-name";
  public static final String TARGET_PERCENT = "target-percent";
  public static final String TIMEOUT = "timeout";

  private DataSourceConstant() {}
}
