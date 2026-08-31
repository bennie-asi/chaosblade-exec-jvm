package com.alibaba.chaosblade.exec.plugin.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.alibaba.chaosblade.exec.common.aop.Plugin;
import com.alibaba.chaosblade.exec.common.aop.PluginBean;
import java.util.ServiceLoader;
import org.junit.Test;

public class DataSourcePluginTest {
  @Test
  public void shouldBeDiscoverableWithoutAPointcut() {
    Plugin discovered = null;
    for (Plugin plugin : ServiceLoader.load(Plugin.class)) {
      if ("datasource".equals(plugin.getName())) {
        discovered = plugin;
        break;
      }
    }
    assertFalse("datasource SPI was not discovered", discovered == null);
    PluginBean bean = new PluginBean(discovered);
    assertNull(bean.getPointCut());
    assertEquals("datasource", bean.getModelSpec().getTarget());
    assertEquals("connectionpoolfull", bean.getModelSpec().getActions().get(0).getName());
  }
}
