package com.alibaba.chaosblade.exec.plugin.datasource;

public final class PoolSnapshot {
  private final int maximum;
  private final int active;
  private final int idle;

  public PoolSnapshot(int maximum, int active, int idle) {
    if (maximum <= 0 || active < 0 || idle < 0) {
      throw new IllegalArgumentException("METRICS_UNAVAILABLE: invalid pool metrics");
    }
    this.maximum = maximum;
    this.active = active;
    this.idle = idle;
  }

  public int getMaximum() {
    return maximum;
  }

  public int getActive() {
    return active;
  }

  public int getIdle() {
    return idle;
  }
}
