package com.example.lt_mobile_nhom4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.lt_mobile_nhom4.components.auth.LoginFragment;
import com.example.lt_mobile_nhom4.components.auth.RegisterFragment;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {
    private Button loginButtonWelcome, registerButtonWelcome;
    private FrameLayout fragmentContainer;
    private FirebaseAuth mAuth;
    private SharedPreferences loginPrefs;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String CURRENT_FRAGMENT = "current_fragment";
    private static final String WELCOME_VISIBLE = "welcome_visible";
    private static final int FRAGMENT_NONE = 0;
    private static final int FRAGMENT_LOGIN = 1;
    private static final int FRAGMENT_REGISTER = 2;
    private int currentFragmentType = FRAGMENT_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_screen);

        mAuth = FirebaseAuth.getInstance();
        loginPrefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        if (!loginPrefs.getBoolean("remember", false)) {
            mAuth.signOut();
        }
        
        loginButtonWelcome = findViewById(R.id.loginButtonWelcome);
        registerButtonWelcome = findViewById(R.id.registerButtonWelcome);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        
        loginButtonWelcome.setOnClickListener(v -> showLoginFragment());
        registerButtonWelcome.setOnClickListener(v -> showRegisterFragment());

        if (savedInstanceState != null) {
            currentFragmentType = savedInstanceState.getInt(CURRENT_FRAGMENT, FRAGMENT_NONE);
            boolean welcomeVisible = savedInstanceState.getBoolean(WELCOME_VISIBLE, true);

            toggleWelcomeContentVisibility(welcomeVisible);

            if (currentFragmentType == FRAGMENT_LOGIN) {
                LoginFragment loginFragment = new LoginFragment();
                loginFragment.setAuthActivityCallback(this::onAuthSuccess);
                replaceFragment(loginFragment, "login_fragment");
            } else if (currentFragmentType == FRAGMENT_REGISTER) {
                RegisterFragment registerFragment = new RegisterFragment();
                registerFragment.setAuthActivityCallback(this::onAuthSuccess);
                replaceFragment(registerFragment, "register_fragment");
            }
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_FRAGMENT, currentFragmentType);
        outState.putBoolean(WELCOME_VISIBLE, fragmentContainer.getVisibility() != View.VISIBLE);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.i("INIT", "Current user: " + currentUser);
        if (currentUser != null) {
            startMainActivity();
        }
    }
    
    public void showLoginFragment() {
        toggleWelcomeContentVisibility(false);
        currentFragmentType = FRAGMENT_LOGIN;
        
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.setAuthActivityCallback(this::onAuthSuccess);
        
        replaceFragment(loginFragment, "login_fragment");
    }
    
    public void showRegisterFragment() {
        toggleWelcomeContentVisibility(false);
        currentFragmentType = FRAGMENT_REGISTER;
        
        RegisterFragment registerFragment = new RegisterFragment();
        registerFragment.setAuthActivityCallback(this::onAuthSuccess);
        
        replaceFragment(registerFragment, "register_fragment");
    }
    
    private void replaceFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
    }
    
    private void toggleWelcomeContentVisibility(boolean showWelcome) {
        int welcomeVisibility = showWelcome ? View.VISIBLE : View.GONE;
        int fragmentVisibility = showWelcome ? View.GONE : View.VISIBLE;
        
        View[] welcomeViews = {
            loginButtonWelcome,
            registerButtonWelcome,
            findViewById(R.id.appTitleTextView),
            findViewById(R.id.appLogoImageView),
            findViewById(R.id.appDescriptionTextView)
        };
        
        for (View view : welcomeViews) {
            if (view != null) {
                view.setVisibility(welcomeVisibility);
            }
        }
        
        fragmentContainer.setVisibility(fragmentVisibility);
        
        if (showWelcome) {
            currentFragmentType = FRAGMENT_NONE;
        }
    }

    public void onAuthSuccess() {
        startMainActivity();
    }

    private void showCameraFragment() {
        CameraFragment cameraFragment = new CameraFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, cameraFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            toggleWelcomeContentVisibility(true);
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
