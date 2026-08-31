package com.alibaba.chaosblade.exec.plugin.datasource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves live Spring Boot application contexts without linking Spring classes into the agent. */
public final class SpringContextResolver {

  public ResolvedDataSource resolve(String beanName) {
    List<ResolvedDataSource> matches = new ArrayList<ResolvedDataSource>();
    Set<Object> visitedContexts = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    Throwable lastFailure = null;
    for (ClassLoader loader : candidateClassLoaders()) {
      try {
        for (Object context : contexts(loader)) {
          if (!visitedContexts.add(context) || !isActive(context)) {
            continue;
          }
          if (!booleanMethod(context, "containsBean", beanName)) {
            continue;
          }
          Object bean = method(context.getClass(), "getBean", String.class).invoke(context, beanName);
          if (bean instanceof javax.sql.DataSource) {
            matches.add(new ResolvedDataSource(bean, loader, context, beanName));
          }
        }
      } catch (Throwable failure) {
        lastFailure = failure;
      }
    }
    if (matches.size() == 1) {
      return matches.get(0);
    }
    if (matches.size() > 1) {
      throw new IllegalStateException("DATASOURCE_AMBIGUOUS: multiple active contexts contain bean " + beanName);
    }
    String suffix = lastFailure == null ? "" : ": " + lastFailure.getClass().getSimpleName();
    throw new IllegalStateException("DATASOURCE_CONTEXT_UNAVAILABLE: no unique active bean " + beanName + suffix);
  }

  private Set<ClassLoader> candidateClassLoaders() {
    Set<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
    loaders.add(Thread.currentThread().getContextClassLoader());
    loaders.add(ClassLoader.getSystemClassLoader());
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      loaders.add(thread.getContextClassLoader());
    }
    loaders.remove(null);
    return loaders;
  }

  private Collection<?> contexts(ClassLoader loader) throws Exception {
    Class<?> springApplication = Class.forName("org.springframework.boot.SpringApplication", false, loader);
    Field hookField = field(springApplication, "shutdownHook");
    Object hook = hookField.get(null);
    Field contextsField = findContextsField(hook.getClass());
    Object value = contextsField.get(hook);
    if (!(value instanceof Collection)) {
      throw new IllegalStateException("Spring Boot context registry is not a collection");
    }
    Collection<?> contexts = (Collection<?>) value;
    synchronized (contexts) {
      return new ArrayList<Object>(contexts);
    }
  }

  private Field findContextsField(Class<?> type) throws Exception {
    for (String name : new String[] {"contexts", "applicationContexts"}) {
      try {
        return field(type, name);
      } catch (NoSuchFieldException ignored) {
        // Try the next known Boot layout.
      }
    }
    throw new NoSuchFieldException("SpringApplicationShutdownHook contexts");
  }

  private boolean isActive(Object context) throws Exception {
    try {
      return Boolean.TRUE.equals(method(context.getClass(), "isActive").invoke(context));
    } catch (NoSuchMethodException ignored) {
      return true;
    }
  }

  private boolean booleanMethod(Object target, String name, String value) throws Exception {
    return Boolean.TRUE.equals(method(target.getClass(), name, String.class).invoke(target, value));
  }

  private static Field field(Class<?> type, String name) throws Exception {
    Class<?> current = type;
    while (current != null) {
      try {
        Field field = current.getDeclaredField(name);
        field.setAccessible(true);
        return field;
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(type.getName() + "." + name);
  }

  private static Method method(Class<?> type, String name, Class<?>... parameterTypes) throws Exception {
    Method method = type.getMethod(name, parameterTypes);
    method.setAccessible(true);
    return method;
  }
}
