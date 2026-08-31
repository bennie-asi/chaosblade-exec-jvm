package com.alibaba.chaosblade.exec.plugin.datasource;

import com.alibaba.chaosblade.exec.common.aop.Enhancer;
import com.alibaba.chaosblade.exec.common.aop.Plugin;
import com.alibaba.chaosblade.exec.common.aop.PointCut;
import com.alibaba.chaosblade.exec.common.model.ModelSpec;

public final class DataSourcePlugin implements Plugin {
  @Override
  public String getName() {
    return "datasource";
  }

  @Override
  public ModelSpec getModelSpec() {
    return new DataSourceModelSpec();
  }

  @Override
  public PointCut getPointCut() {
    return null;
  }

  @Override
  public Enhancer getEnhancer() {
    return null;
  }
}
