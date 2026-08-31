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
