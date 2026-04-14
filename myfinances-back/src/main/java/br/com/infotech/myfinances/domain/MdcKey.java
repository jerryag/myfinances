package br.com.infotech.myfinances.domain;

public enum MdcKey {
  TRACE_ID("mdc1"),
  USER_LOGIN("mdc2");

  private final String key;

  MdcKey(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }
}
