package com.linkedin.android.mobilesdk.errors;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class LIAuthError {

  private static final String TAG = LIAuthError.class.getName();

  private final LIAppErrorCode errorCode;
  private final String errorMsg;

  public LIAuthError(String errorInfo, String errorMsg) {
    this.errorCode = LIAppErrorCode.findErrorCode(errorInfo);
    this.errorMsg = errorMsg;
  }

  public LIAuthError(LIAppErrorCode errorCode, String errorMsg) {
    this.errorCode = errorCode;
    this.errorMsg = errorMsg;
  }

  @Override
  public String toString() {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("errorCode", errorCode.name());
      jsonObject.put("errorMessage", errorMsg);
      return jsonObject.toString(2);
    } catch (JSONException exception) {
      Log.d(TAG, exception.getMessage());
    }
    return null;
  }
}
