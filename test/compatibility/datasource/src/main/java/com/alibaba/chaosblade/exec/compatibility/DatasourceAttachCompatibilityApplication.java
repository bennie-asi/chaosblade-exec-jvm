package com.alibaba.chaosblade.exec.compatibility;

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DatasourceAttachCompatibilityApplication {
  public static void main(String[] args) {
    SpringApplication.run(DatasourceAttachCompatibilityApplication.class, args);
  }

  @Bean(name = "coreDataSource", destroyMethod = "close")
  DataSource coreDataSource(@Value("${pool.type:hikari}") String poolType) {
    if ("druid".equalsIgnoreCase(poolType)) {
      DruidDataSource dataSource = new DruidDataSource();
      dataSource.setName("coreDataSource");
      dataSource.setUrl("jdbc:h2:mem:druid;DB_CLOSE_DELAY=-1");
      dataSource.setUsername("sa");
      dataSource.setPassword("");
      dataSource.setDriverClassName("org.h2.Driver");
      dataSource.setInitialSize(1);
      dataSource.setMinIdle(1);
      dataSource.setMaxActive(4);
      dataSource.setMaxWait(1000);
      return dataSource;
    }
    HikariConfig config = new HikariConfig();
    config.setPoolName("coreDataSource");
    config.setJdbcUrl("jdbc:h2:mem:hikari;DB_CLOSE_DELAY=-1");
    config.setUsername("sa");
    config.setPassword("");
    config.setDriverClassName("org.h2.Driver");
    config.setMinimumIdle(1);
    config.setMaximumPoolSize(4);
    config.setConnectionTimeout(1000);
    return new HikariDataSource(config);
  }

  @Bean
  JdbcTemplate jdbcTemplate(DataSource coreDataSource) {
    return new JdbcTemplate(coreDataSource);
  }

  @RestController
  static class ProbeController {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    ProbeController(DataSource dataSource, JdbcTemplate jdbcTemplate) {
      this.dataSource = dataSource;
      this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/query")
    ResponseEntity<Map<String, Object>> query() {
      Map<String, Object> body = new LinkedHashMap<String, Object>();
      try {
        body.put("value", jdbcTemplate.queryForObject("select 1", Integer.class));
        body.put("ok", true);
        return ResponseEntity.ok(body);
      } catch (RuntimeException failure) {
        body.put("ok", false);
        body.put("error", failure.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
      }
    }

    @GetMapping("/pool")
    Map<String, Object> pool() {
      Map<String, Object> body = new LinkedHashMap<String, Object>();
      if (dataSource instanceof HikariDataSource) {
        HikariDataSource hikari = (HikariDataSource) dataSource;
        body.put("type", "HIKARI");
        body.put("maximum", hikari.getMaximumPoolSize());
        body.put("active", hikari.getHikariPoolMXBean().getActiveConnections());
        body.put("idle", hikari.getHikariPoolMXBean().getIdleConnections());
      } else {
        DruidDataSource druid = (DruidDataSource) dataSource;
        body.put("type", "DRUID");
        body.put("maximum", druid.getMaxActive());
        body.put("active", druid.getActiveCount());
        body.put("idle", druid.getPoolingCount());
      }
      return body;
    }
  }
}
