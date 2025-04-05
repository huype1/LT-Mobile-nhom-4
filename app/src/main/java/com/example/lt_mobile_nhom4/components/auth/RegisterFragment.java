package com.example.lt_mobile_nhom4.components.auth;

import static com.example.lt_mobile_nhom4.utils.Helper.isValidEmail;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lt_mobile_nhom4.AuthActivity;
import com.example.lt_mobile_nhom4.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private EditText emailEditText, passwordEditText, confirmPasswordEditText, phoneNumberEditText, usernameEditText, fullNameEditText;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private LoginFragment.AuthActivityCallback callback;
    private LinearLayout root;

    public void setAuthActivityCallback(LoginFragment.AuthActivityCallback callback) {
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
        View view = inflater.inflate(R.layout.register_fragment, container, false);

        this.root = (LinearLayout) view;
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText);
        phoneNumberEditText = view.findViewById(R.id.phoneNumberEditText);
        usernameEditText = view.findViewById(R.id.usernameEditText);
        fullNameEditText = view.findViewById(R.id.fullNameEditText);
        registerButton = view.findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerUser());
        TextView loginTextView = view.findViewById(R.id.signUpTextView);
        loginTextView.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                ((AuthActivity) getActivity()).showLoginFragment();
            }
        });
        return view;
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String phoneNumber = phoneNumberEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String fullName = fullNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
            emailEditText.setError(getString(R.string.error_email_required));
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordEditText.setError(getString(R.string.error_password_required));
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_confirm_password_required));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError(getString(R.string.error_passwords_not_match));
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            phoneNumberEditText.setError(getString(R.string.error_phone_required));
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError(getString(R.string.error_username_required));
            return;
        }
        
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError(getString(R.string.error_fullname_required));
            return;
        }

        registerButton.setEnabled(false);
        Toast.makeText(getContext(), getString(R.string.checking_username), Toast.LENGTH_SHORT).show();
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Username exists
                            usernameEditText.setError(getString(R.string.username_existed));
                            Toast.makeText(getContext(), getString(R.string.username_existed), Toast.LENGTH_SHORT).show();
                            registerButton.setEnabled(true);
                        } else {
                            createFirebaseUser(email, password);
                        }
                    } else {
                        registerButton.setEnabled(true);
                        Toast.makeText(getContext(), getString(R.string.register_fail), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void createFirebaseUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    registerButton.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(usernameEditText.getText().toString().trim())
                                .setPhotoUri(Uri.parse("https://t4.ftcdn.net/jpg/02/15/84/43/360_F_215844325_ttX9YiIIyeaR7Ne6EaLLjMAmy4GvPC69.jpg"))
                                .build();
                        Log.d("INIT", "User created in firebase auth");

                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                .addOnCompleteListener(profileTask -> {
                                    if (profileTask.isSuccessful()) {
                                        initData(user);

                                        Toast.makeText(getContext(), getString(R.string.register_success), Toast.LENGTH_SHORT).show();

                                        if (callback != null) {
                                            callback.onAuthSuccess();
                                        }
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), getString(R.string.register_fail), Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void initData(FirebaseUser user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getUid());
        data.put("username", usernameEditText.getText().toString().trim());
        data.put("full-name", fullNameEditText.getText().toString().trim());
        data.put("phone-number", phoneNumberEditText.getText().toString().trim());
        data.put("email", user.getEmail());
        data.put("image-url", user.getPhotoUrl().toString());
        data.put("created-at", timestamp);

        Map<String, String> friendsMap = new HashMap<>();
        data.put("friends", friendsMap);

        firestore.collection("users")
                .document(user.getUid())
                .set(data);
    }
}
