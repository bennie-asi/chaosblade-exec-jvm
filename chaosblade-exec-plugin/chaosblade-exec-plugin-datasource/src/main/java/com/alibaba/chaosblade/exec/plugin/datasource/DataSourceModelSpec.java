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
import com.alibaba.chaosblade.exec.common.exception.ExperimentException;
import com.alibaba.chaosblade.exec.common.model.BaseModelSpec;
import com.alibaba.chaosblade.exec.common.model.InjectionResultProvider;
import com.alibaba.chaosblade.exec.common.model.Model;
import com.alibaba.chaosblade.exec.common.model.handler.PreCreateInjectionModelHandler;
import com.alibaba.chaosblade.exec.common.model.handler.PreDestroyInjectionModelHandler;

public final class DataSourceModelSpec extends BaseModelSpec implements PreCreateInjectionModelHandler, PreDestroyInjectionModelHandler, InjectionResultProvider {
  private final DataSourceConnectionPoolFullExecutor executor = new DataSourceConnectionPoolFullExecutor();
  private final DataSourceConnectionPoolFullActionSpec action = new DataSourceConnectionPoolFullActionSpec(executor);

  public DataSourceModelSpec() {
    addActionSpec(action);
  }

  @Override
  public String getTarget() {
    return DataSourceConstant.TARGET;
  }

  @Override
  public String getShortDesc() {
    return "Spring DataSource experiments";
  }

  @Override
  public String getLongDesc() {
    return "Experiments for named HikariCP and Druid DataSource beans";
  }

  @Override
  protected PredicateResult preMatcherPredicate(Model model) {
    return PredicateResult.success();
  }

  @Override
  public void preCreate(String uid, Model model) throws ExperimentException {
    try {
      action.createInjection(uid, model);
    } catch (ExperimentException e) {
      throw e;
    } catch (Exception e) {
      throw new ExperimentException("create datasource injection failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void preDestroy(String uid, Model model) throws ExperimentException {
    try {
      action.destroyInjection(uid, model);
    } catch (ExperimentException e) {
      throw e;
    } catch (Exception e) {
      throw new ExperimentException("destroy datasource injection failed: " + e.getMessage(), e);
    }
  }

  @Override
  public String getInjectionResult(String uid) {
    return executor.resultJson(uid);
  }

  public void unload() {
    executor.unload();
  }
}
