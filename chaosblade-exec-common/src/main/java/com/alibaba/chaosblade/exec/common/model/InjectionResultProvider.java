package com.alibaba.chaosblade.exec.common.model;

/** Optional model contract for experiments that expose a structured JSON result. */
public interface InjectionResultProvider {

  /** Returns a JSON object string for the experiment, or {@code null} when unavailable. */
  String getInjectionResult(String uid);
}
