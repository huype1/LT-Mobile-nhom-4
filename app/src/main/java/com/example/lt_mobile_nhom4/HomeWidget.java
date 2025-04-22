package com.example.lt_mobile_nhom4;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;

public class HomeWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_home);

            // Lấy userId từ cấu hình hoặc SharedPreferences
            String userId = prefs.getString("current_user_id_" + appWidgetId, null);
            if (userId != null) {
                String imagePath = prefs.getString("widget_image_path_" + userId, null);
                if (imagePath != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_image_view, bitmap);
                    } else {
                        Log.w("HomeWidget", "Bitmap decoding failed for path: " + imagePath);
                        views.setImageViewResource(R.id.widget_image_view, R.drawable.ic_widget_preview_static); // fallback
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("com.example.lt_mobile_nhom4.UPDATE_WIDGET".equals(intent.getAction())) {
            String friendId = intent.getStringExtra("friendId");
            String senderName = intent.getStringExtra("senderName");
            String description = intent.getStringExtra("description");
            String imagePath = intent.getStringExtra("imagePath");

            SharedPreferences prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE);

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, HomeWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            for (int appWidgetId : appWidgetIds) {
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_home);

                // Cập nhật imageView với ảnh
                if (imagePath != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_image_view, bitmap);
                    }
                }

                // Cập nhật senderName và description
                views.setTextViewText(R.id.widget_sender_name, senderName);

                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }


}
