package com.linkedin.android.mobilesdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.linkedin.android.mobilesdk.errors.LIAppErrorCode;
import com.linkedin.android.mobilesdk.errors.LIAuthError;
import com.linkedin.android.mobilesdk.internals.AppStore;
import com.linkedin.android.mobilesdk.internals.LIAppVersion;
import com.linkedin.android.mobilesdk.listeners.AuthListener;
import com.linkedin.android.mobilesdk.utils.Scope;

import java.util.List;

/**
 * LISessionManager manages the authorizations needed for an application
 * to access LinkedIn data and to view LinkedIn profiles.
 * <p>
 * A typical usage flow is:
 * <p>
 * LISessionManager.init(activity, Scope.build(LIPermission.R_BASICPROFILE), callback, true);
 * <p>
 * When callback.onAuthSuccess() is called, calls to {@link APIHelper}
 * to retrieve LinkedIn data or calls to {@link DeepLinkHelper} can be
 * made to view LinkedIn profiles.
 * {@link LISession#isValid()} should be used to validate the session before
 * making api calls or deeplink calls
 */
public class LISessionManager {

  private static final String TAG = LISessionManager.class.getSimpleName();
  private static final int LI_SDK_AUTH_REQUEST_CODE = 3672;
  private static final String AUTH_TOKEN = "token";
  private static final String LI_APP_PACKAGE_NAME = "com.linkedin.android";
  private static final String LI_APP_AUTH_CLASS_NAME = "com.linkedin.android.liauthlib.thirdparty.LiThirdPartyAuthorizeActivity";
  private static final String SCOPE_DATA = "com.linkedin.thirdpartysdk.SCOPE_DATA";
  private static final String LI_APP_ACTION_AUTHORIZE_APP = "com.linkedin.android.auth.AUTHORIZE_APP";
  private static final String LI_APP_CATEGORY = "com.linkedin.android.auth.thirdparty.authorize";
  private static final String LI_ERROR_INFO = "com.linkedin.thirdparty.authorize.RESULT_ACTION_ERROR_INFO";
  private static final String LI_ERROR_DESCRIPTION = "com.linkedin.thirdparty.authorize.RESULT_ACTION_ERROR_DESCRIPTION";

  private static LISessionManager sessionManager;

  private Context ctx;
  private final LISessionImpl session;
  private AuthListener authListener;

  public static LISessionManager getInstance(@NonNull Context context) {
    if (sessionManager == null) {
      sessionManager = new LISessionManager();
    }
    if (sessionManager.ctx == null) {
      sessionManager.ctx = context.getApplicationContext();
    }
    return sessionManager;
  }

  private LISessionManager() {
    this.session = new LISessionImpl();
  }

  /**
   * Initializes LISession using previously obtained AccessToken
   * The passed in access token should be one that was obtained from the LinkedIn Mobile SDK.
   *
   * @param accessToken access token
   */
  public void init(AccessToken accessToken) {
    session.setAccessToken(accessToken);
  }

  private Intent prepare(Activity activity, Scope scope, AuthListener callback, boolean
      showGoToAppStoreDialog) {
    Intent intent = null;
    // Check if Linkedin app is installed
    if (LIAppVersion.isLIAppCurrent(ctx)) {
      Log.d(TAG, "Linkedin app installed");
      authListener = callback;
      intent = new Intent();
      intent.setClassName(LI_APP_PACKAGE_NAME, LI_APP_AUTH_CLASS_NAME);
      intent.putExtra(SCOPE_DATA, scope.createScope());
      intent.setAction(LI_APP_ACTION_AUTHORIZE_APP);
      intent.addCategory(LI_APP_CATEGORY);
    } else {
      Log.d(TAG, "Linkedin app not installed");
      AppStore.goAppStore(activity, showGoToAppStoreDialog);
    }

    return intent;
  }

  /**
   * Brings the user to an authorization screen which allows the user to authorize
   * the application to access their LinkedIn data.  When the user authorizes the application
   * {@link AuthListener#onAuthSuccess()} is called.
   * If the user has previously authorized the application, onAuthSuccess will be called without
   * the authorization screen being shown.
   * <p>
   * If there is no user logged into the LinkedIn application, the user will be prompted to login
   * to LinkedIn, after which the authorization screen will be shown.
   * <p>
   * Either this method or {@link LISessionManager#init(AccessToken)} must be
   * called before the application can make API calls or DeepLink calls.
   *
   * @param activity               activity to return to after initialization
   * @param scope                  The type of LinkedIn data that for which access is requested.
   *                               see {@link Scope}
   * @param callback               listener to execute on completion
   * @param showGoToAppStoreDialog determines behaviour when the LinkedIn app is not installed
   *                               if true, a dialog is shown which prompts the user to install
   *                               the LinkedIn app via the app store.  If false, the user is
   *                               taken directly to the app store.
   */
  public void init(Activity activity, Scope scope, AuthListener callback, boolean showGoToAppStoreDialog) {
    Intent intent = prepare(activity, scope, callback, showGoToAppStoreDialog);
    if (intent != null) {
      try {
        activity.startActivityForResult(intent, LI_SDK_AUTH_REQUEST_CODE);
      } catch (ActivityNotFoundException exception) {
        Log.d(TAG, exception.getMessage(), exception);
      }
    }
  }

  /**
   * Brings the user to an authorization screen which allows the user to authorize
   * the application to access their LinkedIn data.  When the user authorizes the application
   * {@link AuthListener#onAuthSuccess()} is called.
   * If the user has previously authorized the application, onAuthSuccess will be called without
   * the authorization screen being shown.
   * <p>
   * If there is no user logged into the LinkedIn application, the user will be prompted to login
   * to LinkedIn, after which the authorization screen will be shown.
   * <p>
   * Either this method or {@link LISessionManager#init(AccessToken)} must be
   * called before the application can make API calls or DeepLink calls.
   *
   * @param fragment               fragment to return to after initialization
   * @param scope                  The type of LinkedIn data that for which access is requested.
   *                               see {@link Scope}
   * @param callback               listener to execute on completion
   * @param showGoToAppStoreDialog determines behaviour when the LinkedIn app is not installed
   *                               if true, a dialog is shown which prompts the user to install
   *                               the LinkedIn app via the app store.  If false, the user is
   *                               taken directly to the app store.
   */
  public void init(Fragment fragment, Scope scope, AuthListener callback, boolean showGoToAppStoreDialog) {
    Intent intent = prepare(fragment.getActivity(), scope, callback, showGoToAppStoreDialog);
    if (intent != null) {
      try {
        fragment.startActivityForResult(intent, LI_SDK_AUTH_REQUEST_CODE);
      } catch (ActivityNotFoundException exception) {
        Log.d(TAG, exception.getMessage(), exception);
      }
    }
  }

  /**
   * This method must be called in the calling Activity's onActivityResult in order to
   * process the response to
   * {@link LISessionManager#init(Activity, Scope, AuthListener, boolean)}
   *
   * @param requestCode
   * @param resultCode
   * @param data
   */
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    // set access token
    if (authListener != null && requestCode == LI_SDK_AUTH_REQUEST_CODE) {
      // got result
      if (resultCode == Activity.RESULT_OK) {
        String token = data.getStringExtra(AUTH_TOKEN);
        long expiresOn = data.getLongExtra("expiresOn", 0L);
        AccessToken accessToken = new AccessToken(token, expiresOn);
        init(accessToken);
        // call the callback with the
        authListener.onAuthSuccess();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        authListener.onAuthError(new LIAuthError(LIAppErrorCode.USER_CANCELLED, "user canceled"));
      } else {
        String errorInfo = data.getStringExtra(LI_ERROR_INFO);
        String errorDesc = data.getStringExtra(LI_ERROR_DESCRIPTION);
        authListener.onAuthError(new LIAuthError(errorInfo, errorDesc));
      }
      authListener = null;
    }
  }

  /**
   * @return the LISession
   */
  public LISession getSession() {
    return session;
  }

  /**
   * Clears the session.  Calls to retrieve LinkedIn data or to view profiles will no longer
   * work.
   */
  public void clearSession() {
    session.setAccessToken(null);
  }

  /**
   * Builds scope based on List of permissions.
   *
   * @param perms list of permissions
   * @return a new scope
   */
  private static String createScope(List<String> perms) {
    if (perms == null || perms.isEmpty()) {
      return "";
    }
    return TextUtils.join(" ", perms);
  }

  /**
   * private implementation of LISession
   * takes are of saving and restoring session to/from shared preferences
   */
  private static class LISessionImpl implements LISession {

    private static final String LI_SDK_SHARED_PREF_STORE = "li_shared_pref_store";
    private static final String SHARED_PREFERENCES_ACCESS_TOKEN = "li_sdk_access_token";
    private AccessToken accessToken = null;

    public LISessionImpl() {
    }

    @Override
    public AccessToken getAccessToken() {
      if (accessToken == null) {
        recover();
      }
      return accessToken;
    }

    void setAccessToken(@Nullable AccessToken accessToken) {
      this.accessToken = accessToken;
      save();
    }

    /**
     * @return true if a valid accessToken is present.  Note that if the member revokes
     * access to this application, this will still return true
     */
    @Override
    public boolean isValid() {
      AccessToken at = getAccessToken();
      return at != null && !at.isExpired();
    }

    /**
     * clears session. (Kills it)
     */
    public void clear() {
      setAccessToken(null);
    }

    /**
     * Storage
     */
    private SharedPreferences getSharedPref() {
      return LISessionManager.sessionManager.ctx.getSharedPreferences(LI_SDK_SHARED_PREF_STORE, Context.MODE_PRIVATE);
    }

    private void save() {
      SharedPreferences.Editor edit = getSharedPref().edit();
      edit.putString(SHARED_PREFERENCES_ACCESS_TOKEN, accessToken == null ? null : accessToken.toString());
      edit.apply();
    }

    private void recover() {
      SharedPreferences sharedPref = getSharedPref();
      String accessTokenStr = sharedPref.getString(SHARED_PREFERENCES_ACCESS_TOKEN, null);
      accessToken = accessTokenStr == null ? null : AccessToken.buildAccessToken(accessTokenStr);
    }
  }
}
