package com.example.engapp.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtResetEmail;
    private TextView btnSendResetEmail;

    // Đối tượng FirebaseAuth dùng để gửi email reset password
    private FirebaseAuth firebaseAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseAuth = FirebaseAuth.getInstance();

        initViews();
        setupEvents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtResetEmail = findViewById(R.id.edtResetEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
    }

    // Gắn sự kiện cho nút quay lại và nút gửi email khôi phục mật khẩu
    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        btnSendResetEmail.setOnClickListener(v -> {
            String email = edtResetEmail.getText().toString().trim();

            if (!validateEmail(email)) {
                return;
            }

            sendResetPasswordEmail(email);
        });
    }

    // Kiểm tra email có rỗng hoặc sai định dạng hay không
    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            edtResetEmail.setError(getString(R.string.error_enter_email_for_reset));
            edtResetEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtResetEmail.setError(getString(R.string.error_invalid_email));
            edtResetEmail.requestFocus();
            return false;
        }

        return true;
    }

    // Gửi email đặt lại mật khẩu thông qua Firebase Authentication
    private void sendResetPasswordEmail(String email) {
        setLoading(true);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);

                    Toast.makeText(
                            ForgotPasswordActivity.this,
                            getString(R.string.reset_password_email_sent),
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    Toast.makeText(
                            ForgotPasswordActivity.this,
                            getString(R.string.reset_password_failed_format, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Bật hoặc tắt trạng thái loading cho nút gửi email
    private void setLoading(boolean isLoading) {
        btnSendResetEmail.setEnabled(!isLoading);

        if (isLoading) {
            btnSendResetEmail.setText(getString(R.string.sending));
        } else {
            btnSendResetEmail.setText(getString(R.string.send_reset_email));
        }
    }
}