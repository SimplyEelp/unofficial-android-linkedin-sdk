package com.linkedin.android.mobilesdk.errors;

import java.util.HashMap;
import java.util.Map;

public enum LIAppErrorCode {
  NONE("none"),
  INVALID_REQUEST("Invalid request"),
  NETWORK_UNAVAILABLE("Unavailable network connection"),
  USER_CANCELLED("User canceled action"),
  UNKNOWN_ERROR("Unknown or not defined error"),
  SERVER_ERROR("Server side error"),
  LINKEDIN_APP_NOT_FOUND("LinkedIn application not found"),
  NOT_AUTHENTICATED("User is not authenticated in LinkedIn app"),;

  private static final Map<String, LIAppErrorCode> liAuthErrorCodeHashMap = buildMap();

  private static Map<String, LIAppErrorCode> buildMap() {
    HashMap<String, LIAppErrorCode> map = new HashMap<>();
    for (LIAppErrorCode code : LIAppErrorCode.values()) {
      map.put(code.name(), code);
    }
    return map;
  }

  private final String description;

  LIAppErrorCode(String name) {
    this.description = name;
  }

  public String getDescription() {
    return description;
  }

  public static LIAppErrorCode findErrorCode(String errorCode) {
    LIAppErrorCode liAuthErrorCode = liAuthErrorCodeHashMap.get(errorCode);
    return liAuthErrorCode == null ? UNKNOWN_ERROR : liAuthErrorCode;
  }
}
