package com.petal.browser.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * PetalDownloadEngine
 * Direct Chromium Native Download Pipeline Wrapper.
 * Replaces old Fetch2/aria2c engine with Chromium's native DownloadManagerService.
 */
public class PetalDownloadEngine {
    private static final String TAG = "PetalDownloadEngine";
    private static PetalDownloadEngine sInstance;
    private final Context context;

    private PetalDownloadEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized PetalDownloadEngine getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PetalDownloadEngine(context);
        }
        return sInstance;
    }

    public long enqueueDownload(String url, String fileName, String userAgent, String mimeType, String cookie, String referrer) {
        Log.i(TAG, "Enqueueing download via native Chromium pipeline: " + fileName + " (" + url + ")");
        try {
            Intent intent = new Intent(context, PetalChromiumDownloadService.class);
            intent.setAction(PetalChromiumDownloadService.ACTION_DOWNLOAD_START);
            intent.putExtra(PetalChromiumDownloadService.EXTRA_DOWNLOAD_URL, url);
            intent.putExtra(PetalChromiumDownloadService.EXTRA_FILE_NAME, fileName);
            intent.putExtra(PetalChromiumDownloadService.EXTRA_MIME_TYPE, mimeType);
            context.startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Chromium download service: " + e.getMessage());
        }
        return System.currentTimeMillis();
    }

    public void cancelDownload(long downloadId) {
        Intent intent = new Intent(context, PetalChromiumDownloadService.class);
        intent.setAction(PetalChromiumDownloadService.ACTION_DOWNLOAD_CANCEL);
        intent.putExtra(PetalChromiumDownloadService.EXTRA_DOWNLOAD_ID, String.valueOf(downloadId));
        context.startService(intent);
    }

    public void pauseDownload(long downloadId) {
        Intent intent = new Intent(context, PetalChromiumDownloadService.class);
        intent.setAction(PetalChromiumDownloadService.ACTION_DOWNLOAD_PAUSE);
        intent.putExtra(PetalChromiumDownloadService.EXTRA_DOWNLOAD_ID, String.valueOf(downloadId));
        context.startService(intent);
    }

    public void resumeDownload(long downloadId) {
        Intent intent = new Intent(context, PetalChromiumDownloadService.class);
        intent.setAction(PetalChromiumDownloadService.ACTION_DOWNLOAD_RESUME);
        intent.putExtra(PetalChromiumDownloadService.EXTRA_DOWNLOAD_ID, String.valueOf(downloadId));
        context.startService(intent);
    }
}
