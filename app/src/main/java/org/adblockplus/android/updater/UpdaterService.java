/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.android.updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import org.adblockplus.android.AdblockPlus;
import org.adblockplus.android.R;
import org.adblockplus.android.Utils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Update downloader.
 */
public class UpdaterService extends Service {
    private static final String TAG = Utils.getTag(UpdaterService.class);

    private File updateDir;

    @Override
    public void onCreate() {
        super.onCreate();
        // Use common Android path for downloads
        updateDir = new File(Environment.getExternalStorageDirectory().getPath(), "downloads");
    }

    @Override
    public void onStart(final Intent intent, final int startId) {
        super.onStart(intent, startId);

        // Stop if media not available
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            stopSelf();
            return;
        }
        updateDir.mkdirs();

        // Start download
        if (intent != null && intent.hasExtra("url"))
            new DownloadTask(this).execute(intent.getStringExtra("url"));
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private final Context context;
        private final NotificationCompat.Builder notificationBuilder;
        //private final Notification notification;
        private final PendingIntent contentIntent;
        private final NotificationManager notificationManager;

        public DownloadTask(final Context context) {
            this.context = context;
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(context);
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        @Override
        protected void onPreExecute() {
            notificationBuilder
                    .setOngoing(true)
                    .setWhen(0)
                    .setSmallIcon(R.drawable.ic_stat_download)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentIntent(contentIntent)
                    .setContentText(String.format(getString(R.string.msg_update_downloading).toString(), 0));
            notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notificationBuilder.build());
        }

        @Override
        protected String doInBackground(final String... sUrl) {
            try {
                // Create connection
                final URL url = new URL(sUrl[0]);
                Log.e(TAG, "D: " + sUrl[0]);
                final URLConnection connection = url.openConnection();
                connection.connect();
                final int fileLength = connection.getContentLength();
                Log.e(TAG, "S: " + fileLength);

                // Check if file already exists
                final File updateFile = new File(updateDir, "AdblockPlus-update.apk");
                if (updateFile.exists()) {
                    // if (updateFile.length() == fileLength)
                    // return updateFile.getAbsolutePath();
                    // else
                    updateFile.delete();
                }

                // Download the file
                final InputStream input = new BufferedInputStream(url.openStream());
                final OutputStream output = new FileOutputStream(updateFile);

                final byte data[] = new byte[1024];
                long total = 0;
                int count;
                int progress = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);

                    final int p = (int) (total * 100 / fileLength);
                    if (p != progress) {
                        publishProgress(p);
                        progress = p;
                    }
                }

                output.flush();
                output.close();
                input.close();
                return updateFile.getAbsolutePath();
            } catch (final Exception e) {
                Log.e(TAG, "Download error", e);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(final Integer... progress) {
            notificationBuilder
                    .setContentText(String.format(getString(R.string.msg_update_downloading).toString(), progress[0]))
                    .setContentIntent(contentIntent);
            notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notificationBuilder.build());
        }

        @Override
        protected void onPostExecute(final String result) {
            notificationManager.cancel(AdblockPlus.UPDATE_NOTIFICATION_ID);
            if (result != null) {
                final Intent intent = new Intent(context, UpdaterActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction("update");
                intent.putExtra("path", result);
                final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                Notification notification = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_download)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setContentIntent(contentIntent)
                        .setContentTitle(context.getText(R.string.app_name))
                        .setContentText(context.getString(R.string.msg_update_ready))
                        .build();

                notificationManager.notify(AdblockPlus.UPDATE_NOTIFICATION_ID, notification);
            }
        }
    }
}
