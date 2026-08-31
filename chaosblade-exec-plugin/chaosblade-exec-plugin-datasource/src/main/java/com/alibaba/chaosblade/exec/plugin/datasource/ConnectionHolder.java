package com.alibaba.chaosblade.exec.plugin.datasource;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

final class ConnectionHolder {
  final String uid;
  final String identityKey;
  final long expiresAt;
  final List<Connection> connections = new ArrayList<Connection>();
  volatile ConnectionPoolResult result;
  private boolean releasing;

  ConnectionHolder(String uid, String identityKey, long expiresAt, ConnectionPoolResult result) {
    this.uid = uid;
    this.identityKey = identityKey;
    this.expiresAt = expiresAt;
    this.result = result;
  }

  synchronized void add(Connection connection) {
    if (releasing || System.currentTimeMillis() >= expiresAt) {
      closeQuietly(connection);
      throw new IllegalStateException("EXPERIMENT_EXPIRED: connection returned after hard TTL");
    }
    connections.add(connection);
  }

  synchronized int release() {
    releasing = true;
    int closed = 0;
    List<Connection> failed = new ArrayList<Connection>();
    for (Connection connection : connections) {
      try {
        connection.close();
        if (!connection.isClosed()) {
          failed.add(connection);
        } else {
          closed++;
        }
      } catch (Exception e) {
        failed.add(connection);
      }
    }
    connections.clear();
    connections.addAll(failed);
    releasing = !failed.isEmpty();
    return closed;
  }

  synchronized int size() {
    return connections.size();
  }

  private void closeQuietly(Connection connection) {
    try {
      connection.close();
    } catch (Exception ignored) {
      // Best effort for a connection that arrived after expiration.
    }
  }
}
