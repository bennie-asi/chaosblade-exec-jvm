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

import com.alibaba.chaosblade.exec.common.aop.PredicateResult;
import com.alibaba.chaosblade.exec.common.constant.CategoryConstants;
import com.alibaba.chaosblade.exec.common.model.FlagSpec;
import com.alibaba.chaosblade.exec.common.model.Model;
import com.alibaba.chaosblade.exec.common.model.action.ActionModel;
import com.alibaba.chaosblade.exec.common.model.action.BaseActionSpec;
import com.alibaba.chaosblade.exec.common.model.action.DirectlyInjectionAction;
import java.util.Arrays;
import java.util.List;

public final class DataSourceConnectionPoolFullActionSpec extends BaseActionSpec implements DirectlyInjectionAction {
  private final DataSourceConnectionPoolFullExecutor executor;

  DataSourceConnectionPoolFullActionSpec(DataSourceConnectionPoolFullExecutor executor) {
    super(null);
    this.executor = executor;
    setExample("blade create datasource connectionpoolfull --data-source-name coreDataSource --target-percent 100 --timeout 60");
  }

  @Override
  public String getName() {
    return DataSourceConstant.ACTION;
  }

  @Override
  public String[] getAliases() {
    return new String[0];
  }

  @Override
  public String getShortDesc() {
    return "Exhaust a Spring DataSource connection pool";
  }

  @Override
  public String getLongDesc() {
    return "Borrow and hold connections from a named Spring DataSource until destroy or hard timeout";
  }

  @Override
  public List<FlagSpec> getActionFlags() {
    return Arrays.<FlagSpec>asList(
        new SimpleFlagSpec(DataSourceConstant.DATA_SOURCE_NAME, "Spring DataSource bean name, default coreDataSource"),
        new SimpleFlagSpec(DataSourceConstant.TARGET_PERCENT, "Target pool usage percent, default 100"),
        new SimpleFlagSpec(DataSourceConstant.TIMEOUT, "Non-renewable hard TTL in seconds, default 60"));
  }

  @Override
  public PredicateResult predicate(ActionModel actionModel) {
    Model model = new Model(DataSourceConstant.TARGET, DataSourceConstant.ACTION);
    model.setAction(actionModel);
    try {
      ConnectionPoolRequest.from(model);
      return PredicateResult.success();
    } catch (IllegalArgumentException e) {
      return PredicateResult.fail(e.getMessage());
    }
  }

  @Override
  public String[] getCategories() {
    return new String[] {CategoryConstants.JAVA_RESOURCE};
  }

  @Override
  public void createInjection(String uid, Model model) throws Exception {
    executor.create(uid, model);
  }

  @Override
  public void destroyInjection(String uid, Model model) throws Exception {
    executor.destroy(uid);
  }
}
