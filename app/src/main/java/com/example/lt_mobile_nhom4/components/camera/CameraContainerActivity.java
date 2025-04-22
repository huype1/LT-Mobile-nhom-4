package com.example.lt_mobile_nhom4.components.camera;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lt_mobile_nhom4.R;

public class CameraContainerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_container);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CameraFragment())
                    .commit();
        }

        overridePendingTransition(R.anim.slide_in_top, R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_top);
    }

}
