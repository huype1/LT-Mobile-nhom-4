package com.example.lt_mobile_nhom4.components.auth;

import static com.example.lt_mobile_nhom4.utils.Helper.isValidEmail;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lt_mobile_nhom4.AuthActivity;
import com.example.lt_mobile_nhom4.R;
import com.example.lt_mobile_nhom4.components.camera.CameraFragment;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private EditText    emailEditText, passwordEditText;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private AuthActivityCallback callback;

    public interface AuthActivityCallback {
        void onAuthSuccess();
    }

    public void setAuthActivityCallback(AuthActivityCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_fragment, container, false);

        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> loginUser());
        loginButton.setEnabled(true); // Enable the login button
        TextView signUpTextView = view.findViewById(R.id.signUpTextView);
        TextView forgotPasswordTextView = view.findViewById(R.id.forgotPassword);
        signUpTextView.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showRegisterFragment();
            }
        });
        forgotPasswordTextView.setOnClickListener(v -> {
            handleForgotPassword();
        });

        return view;
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
            emailEditText.setError("Please enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onAuthSuccess();
                        }
                    } else {
                        Toast.makeText(getContext(), getString(R.string.login_fail),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Please enter your email address");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send password reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
