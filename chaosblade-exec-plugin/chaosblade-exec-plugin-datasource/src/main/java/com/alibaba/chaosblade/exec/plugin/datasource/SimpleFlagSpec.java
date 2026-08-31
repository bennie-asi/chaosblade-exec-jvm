package com.alibaba.chaosblade.exec.plugin.datasource;

import com.alibaba.chaosblade.exec.common.model.FlagSpec;

final class SimpleFlagSpec implements FlagSpec {
  private final String name;
  private final String desc;

  SimpleFlagSpec(String name, String desc) {
    this.name = name;
    this.desc = desc;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDesc() {
    return desc;
  }

  @Override
  public boolean noArgs() {
    return false;
  }

  @Override
  public boolean required() {
    return false;
  }
}
