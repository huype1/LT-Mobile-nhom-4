package com.example.lt_mobile_nhom4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UpdateUserInfoFragment extends Fragment {

    private EditText etUserName, etUserEmail, etUserPhone, etUserPassword;
    private Button btnSubmit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_user_info, container, false);

        etUserName = view.findViewById(R.id.et_user_name);
        etUserEmail = view.findViewById(R.id.et_user_email);
        etUserPhone = view.findViewById(R.id.et_user_phone);
        etUserPassword = view.findViewById(R.id.et_user_password);
        btnSubmit = view.findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            String email = etUserEmail.getText().toString().trim();
            String phone = etUserPhone.getText().toString().trim();
            String password = etUserPassword.getText().toString().trim();

            if (name.isEmpty() && email.isEmpty() && phone.isEmpty() && password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill out at least one field", Toast.LENGTH_SHORT).show();
            } else {
                // Save user information logic (if needed)
                Toast.makeText(getContext(), "User info updated successfully", Toast.LENGTH_SHORT).show();

                // Navigate back to profile_fragment after 3 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }, 3000);
            }
        });

        return view;
    }
}
