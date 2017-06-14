package com.linkedin.android.mobilesdk.internals;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.linkedin.android.mobilesdk.R;

public class AppStore {

  private static final String TAG = AppStore.class.getName();

  public static void goAppStore(final Activity activity, boolean showDialog) {
    if (showDialog) {
      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setMessage(R.string.update_linkedin_app_message)
          .setTitle(R.string.update_linkedin_app_title);
      builder.setPositiveButton(R.string.update_linkedin_app_download, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          goToAppStore(activity);
          dialogInterface.dismiss();
        }
      });
      builder.setNegativeButton(R.string.update_linkedin_app_cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          dialogInterface.dismiss();
        }
      });
      builder.create().show();
    } else {
      goToAppStore(activity);
    }
  }

  private static void goToAppStore(final Activity activity) {
    SupportedAppStore appStore = SupportedAppStore.fromDeviceManufacturer();
    String appStoreUri = appStore.getAppStoreUri();
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(appStoreUri));
    try {
      activity.startActivity(intent);
    } catch (ActivityNotFoundException exception) {
      // Should not happen
      Log.e(TAG, exception.getMessage(), exception);
    }
  }

  private enum SupportedAppStore {
    amazonAppstore("amazon", "amzn://apps/android?p=com.linkedin.android"),
    googlePlay("google", "market://details?id=com.linkedin.android"),
    samsungApps("samsung", "samsungapps://ProductDetail/com.linkedin.android");

    private final String deviceManufacturer;
    private final String appStoreUri;

    SupportedAppStore(String deviceManufacturer, String appStoreUri) {
      this.deviceManufacturer = deviceManufacturer;
      this.appStoreUri = appStoreUri;
    }

    public String getDeviceManufacturer() {
      return deviceManufacturer;
    }

    public String getAppStoreUri() {
      return appStoreUri;
    }

    public static SupportedAppStore fromDeviceManufacturer() {
      for (SupportedAppStore appStore : values()) {
        if (appStore.getDeviceManufacturer().equalsIgnoreCase(Build.MANUFACTURER)) {
          return appStore;
        }
      }
      // Google play by default
      return googlePlay;
    }
  }
}
