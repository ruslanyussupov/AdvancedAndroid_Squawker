package android.example.com.squawker.fcm;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.example.com.squawker.MainActivity;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class SquawkFirebaseMessagingService extends FirebaseMessagingService {

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_AUTHOR_KEY = "authorKey";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATE = "date";

    private static final String CHANNEL_ID = "squawk_channel";
    private static final int NOTIFICATION_ID = 111;

    private static final int MESSAGE_MAX_LENGTH = 30;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> messageData = remoteMessage.getData();

        if (messageData != null && !messageData.isEmpty()) {
            insertIntoDb(messageData);
            sendNotification(messageData);
        }

    }

    private void insertIntoDb(final Map<String, String> messageData) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String author = messageData.get(KEY_AUTHOR);
                String authorKey = messageData.get(KEY_AUTHOR_KEY);
                String date = messageData.get(KEY_DATE);
                String message = messageData.get(KEY_MESSAGE);

                ContentValues contentValues = new ContentValues();
                contentValues.put(SquawkContract.COLUMN_AUTHOR, author);
                contentValues.put(SquawkContract.COLUMN_AUTHOR_KEY, authorKey);
                contentValues.put(SquawkContract.COLUMN_DATE, date);
                contentValues.put(SquawkContract.COLUMN_MESSAGE, message);
                getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, contentValues);
            }
        }).start();

    }

    private void sendNotification(Map<String, String> messageData) {

        String author = messageData.get(KEY_AUTHOR);
        String message = messageData.get(KEY_MESSAGE);

        int length = message.length();

        if (length > MESSAGE_MAX_LENGTH) {
            message = message.substring(0, length) + "\u2026";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (notificationManager != null) {

                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        getString(R.string.channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(getString(R.string.channel_description));

                notificationManager.createNotificationChannel(channel);
            }
        }

        Intent openMainActivity = new Intent(this, MainActivity.class);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                openMainActivity, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_duck)
                .setContentTitle(String.format(getString(R.string.notification_message), author))
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());

    }

}
