package com.example.lt_mobile_nhom4;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ProfileActivity extends AppCompatActivity {
    LinearLayout deleteAccountButton;
    private static final int REQUEST_PICK_WIDGET = 1001;
    private AppWidgetHost appWidgetHost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_fragment);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);
        LinearLayout linearAddUtilities = findViewById(R.id.add_widget);
        appWidgetHost = new AppWidgetHost(this, 1);

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.delete_account)
                    .setMessage(R.string.delete_account_confirm)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            deleteFirestoreData(user.getUid());
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        linearAddUtilities.setOnClickListener(v -> {
            int appWidgetId = appWidgetHost.allocateAppWidgetId(); // Dùng AppWidgetHost, không phải AppWidgetManager
            ComponentName componentName = new ComponentName(this, HomeWidget.class);
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // Có thể thêm extras nếu cần, ví dụ để chọn widget nào khác
            startActivityForResult(intent, REQUEST_PICK_WIDGET);
        });

    }

    private void deleteFirestoreData(String uid) {
        FirebaseFirestore db =  MyApplication.getFirestore();
        DocumentReference userRef = db.collection("users").document(uid);
        userRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("ProfileActivity", "Firestore data deleted successfully.");
                deleteFirebaseAuthAccount();
            } else {
                Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("ProfileActivity", "Error deleting Firestore document", e);
            Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteFirebaseAuthAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.account_deleted), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, AuthActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.account_delete_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_WIDGET && resultCode == RESULT_OK) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_home);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        }
    }
}
