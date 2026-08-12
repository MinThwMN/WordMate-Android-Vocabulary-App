package com.example.engapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtRegisterName;
    private EditText edtRegisterEmail;
    private EditText edtRegisterPassword;
    private EditText edtRegisterConfirmPassword;

    private ImageView imgToggleRegisterPassword;
    private ImageView imgToggleRegisterConfirmPassword;

    private TextView btnRegister;
    private TextView btnGoToLogin;

    private FirebaseAuth firebaseAuth;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        initView();
        handleTogglePassword();
        handleToggleConfirmPassword();
        handleRegister();
        handleGoToLogin();
    }

    private void initView() {
        edtRegisterName = findViewById(R.id.edtRegisterName);
        edtRegisterEmail = findViewById(R.id.edtRegisterEmail);
        edtRegisterPassword = findViewById(R.id.edtRegisterPassword);
        edtRegisterConfirmPassword = findViewById(R.id.edtRegisterConfirmPassword);

        imgToggleRegisterPassword = findViewById(R.id.imgToggleRegisterPassword);
        imgToggleRegisterConfirmPassword = findViewById(R.id.imgToggleRegisterConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
    }

    private void handleTogglePassword() {
        imgToggleRegisterPassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtRegisterPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                imgToggleRegisterPassword.setImageResource(R.drawable.ic_visibility_off);
                isPasswordVisible = false;
            } else {
                edtRegisterPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                imgToggleRegisterPassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = true;
            }

            edtRegisterPassword.setSelection(edtRegisterPassword.getText().length());
        });
    }

    private void handleToggleConfirmPassword() {
        imgToggleRegisterConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                edtRegisterConfirmPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                imgToggleRegisterConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
                isConfirmPasswordVisible = false;
            } else {
                edtRegisterConfirmPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                imgToggleRegisterConfirmPassword.setImageResource(R.drawable.ic_visibility);
                isConfirmPasswordVisible = true;
            }

            edtRegisterConfirmPassword.setSelection(edtRegisterConfirmPassword.getText().length());
        });
    }

    private void handleRegister() {
        btnRegister.setOnClickListener(v -> {
            String name = edtRegisterName.getText().toString().trim();
            String email = edtRegisterEmail.getText().toString().trim();
            String password = edtRegisterPassword.getText().toString();
            String confirmPassword = edtRegisterConfirmPassword.getText().toString();

            if (!validateInput(name, email, password, confirmPassword)) {
                return;
            }

            registerWithEmail(name, email, password);
        });
    }

    private boolean validateInput(
            String name,
            String email,
            String password,
            String confirmPassword
    ) {
        if (TextUtils.isEmpty(name)) {
            edtRegisterName.setError(getString(R.string.error_enter_name));
            edtRegisterName.requestFocus();
            return false;
        }

        if (name.length() < 2) {
            edtRegisterName.setError(getString(R.string.error_name_min_2));
            edtRegisterName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            edtRegisterEmail.setError(getString(R.string.error_enter_email));
            edtRegisterEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtRegisterEmail.setError(getString(R.string.error_invalid_email));
            edtRegisterEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtRegisterPassword.setError(getString(R.string.error_enter_password));
            edtRegisterPassword.requestFocus();
            return false;
        }

        if (hasWhiteSpace(password)) {
            edtRegisterPassword.setError(getString(R.string.error_password_no_space));
            edtRegisterPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            edtRegisterPassword.setError(getString(R.string.error_password_min_6));
            edtRegisterPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtRegisterConfirmPassword.setError(getString(R.string.error_enter_confirm_password));
            edtRegisterConfirmPassword.requestFocus();
            return false;
        }

        if (hasWhiteSpace(confirmPassword)) {
            edtRegisterConfirmPassword.setError(getString(R.string.error_confirm_password_no_space));
            edtRegisterConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            edtRegisterConfirmPassword.setError(getString(R.string.error_password_not_match));
            edtRegisterConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean hasWhiteSpace(String text) {
        return text != null && text.matches(".*\\s.*");
    }

    private void registerWithEmail(String name, String email, String password) {
        setRegisterLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        setRegisterLoading(false);

                        Toast.makeText(
                                this,
                                getString(R.string.register_success_go_login),
                                Toast.LENGTH_SHORT
                        ).show();

                        firebaseAuth.signOut();
                        goToLoginActivity();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                setRegisterLoading(false);

                                Toast.makeText(
                                        this,
                                        getString(R.string.register_success_go_login),
                                        Toast.LENGTH_SHORT
                                ).show();

                                firebaseAuth.signOut();
                                goToLoginActivity();
                            });
                })
                .addOnFailureListener(e -> {
                    setRegisterLoading(false);

                    Toast.makeText(
                            this,
                            getString(R.string.register_failed_format, e.getMessage()),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void setRegisterLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);

        if (isLoading) {
            btnRegister.setText(getString(R.string.logging_register));
        } else {
            btnRegister.setText(getString(R.string.register));
        }
    }

    private void handleGoToLogin() {
        btnGoToLogin.setOnClickListener(v -> finish());
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}