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
