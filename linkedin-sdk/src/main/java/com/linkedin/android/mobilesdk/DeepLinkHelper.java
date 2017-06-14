package com.linkedin.android.mobilesdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.linkedin.android.mobilesdk.errors.LIAppErrorCode;
import com.linkedin.android.mobilesdk.errors.LiCommonError;
import com.linkedin.android.mobilesdk.internals.AppStore;
import com.linkedin.android.mobilesdk.internals.LIAppVersion;
import com.linkedin.android.mobilesdk.listeners.DeepLinkListener;

/**
 * DeepLinkHelper enables linking to pages within the LinkedIn application
 */
public class DeepLinkHelper {

  public static final int LI_SDK_CROSSLINK_REQUEST_CODE = 13287;
  private static final String CURRENTLY_LOGGED_IN_MEMBER = "you";
  private static final String DEEPLINK_ERROR_CODE_EXTRA_NAME = "com.linkedin.thirdparty.deeplink.EXTRA_ERROR_CODE";
  private static final String DEEPLINK_ERROR_MESSAGE_EXTRA_NAME = "com.linkedin.thirdparty.deeplink.EXTRA_ERROR_MESSAGE";
  private static DeepLinkHelper deepLinkHelper;
  private DeepLinkListener deepLinkListener;

  public static DeepLinkHelper getInstance() {
    if (deepLinkHelper == null) {
      deepLinkHelper = new DeepLinkHelper();
    }
    return deepLinkHelper;
  }

  /**
   * opens up a view which shows the profile of the user that is currently logged in to the
   * LinkedIn app.
   *
   * @param activity
   * @param callback
   */
  public void openCurrentProfile(@NonNull Activity activity, DeepLinkListener callback) {
    openOtherProfile(activity, CURRENTLY_LOGGED_IN_MEMBER, callback);
  }

  /**
   * opens a view which shows the profile of the given member
   *
   * @param activity
   * @param memberId obtained through an api call
   * @param callback
   */
  public void openOtherProfile(@NonNull Activity activity, String memberId, DeepLinkListener callback) {
    this.deepLinkListener = callback;

    LISession session = LISessionManager.getInstance(activity.getApplicationContext()).getSession();
    if (!session.isValid()) {
      callback.onDeepLinkError(new LiCommonError(LIAppErrorCode.NOT_AUTHENTICATED,
          "there is no access token"));
      return;
    }
    try {
      if (!LIAppVersion.isLIAppCurrent(activity)) {
        AppStore.goAppStore(activity, true);
        return;
      }
      deepLinkToProfile(activity, memberId, session.getAccessToken());
    } catch (ActivityNotFoundException e) {
      callback.onDeepLinkError(new LiCommonError(LIAppErrorCode.LINKEDIN_APP_NOT_FOUND,
          "LinkedIn app needs to be either installed or updated"));
      deepLinkListener = null;
    }
  }

  private void deepLinkToProfile(@NonNull Activity activity, String memberId, @NonNull AccessToken accessToken) {
    Intent i = new Intent("android.intent.action.VIEW");
    Uri.Builder uriBuilder = new Uri.Builder();
    uriBuilder.scheme("linkedin");
    if (CURRENTLY_LOGGED_IN_MEMBER.equals(memberId)) {
      uriBuilder.authority(CURRENTLY_LOGGED_IN_MEMBER);
    } else {
      uriBuilder.authority("profile").appendPath(memberId);
    }
    uriBuilder.appendQueryParameter("accessToken", accessToken.getValue());
    uriBuilder.appendQueryParameter("src", "sdk");
    i.setData(uriBuilder.build());
    activity.startActivityForResult(i, LI_SDK_CROSSLINK_REQUEST_CODE);
  }

  /**
   * Call this method in your activity's onActivityResult method.
   * Handles any response code from LinkedIn and calls the DeepLinkListener callback
   *
   * @param requestCode
   * @param resultCode
   * @param data
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == LI_SDK_CROSSLINK_REQUEST_CODE && deepLinkListener != null) {
      if (resultCode == Activity.RESULT_OK) {
        deepLinkListener.onDeepLinkSuccess();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        if (data == null || data.getExtras() == null) {
          deepLinkListener.onDeepLinkError(new LiCommonError(LIAppErrorCode.USER_CANCELLED, ""));
        } else {
          String errorMessage = data.getExtras().getString(DEEPLINK_ERROR_MESSAGE_EXTRA_NAME);
          String errorCode = data.getExtras().getString(DEEPLINK_ERROR_CODE_EXTRA_NAME);
          deepLinkListener.onDeepLinkError(new LiCommonError(errorCode, errorMessage));
        }
      }
    }
  }
}
