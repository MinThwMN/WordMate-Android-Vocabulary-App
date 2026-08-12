package com.example.engapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.engapp.R;
import com.example.engapp.utils.LocaleHelper;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private ImageView imgTogglePassword; // Icon ẩn/hiện mật khẩu
    private TextView btnLoginEmail, txtForgotPassword, btnRegisterEmail;
    private FrameLayout btnLoginGoogle, btnLoginFacebookCustom;
    private LoginButton btnLoginFacebook;

    // FirebaseAuth dùng để xử lý đăng nhập bằng email, Google và Facebook
    private FirebaseAuth firebaseAuth;

    // Client dùng để đăng nhập Google
    private GoogleSignInClient googleSignInClient;
    // CallbackManager dùng để nhận kết quả đăng nhập Facebook
    private CallbackManager callbackManager;

    private boolean isPasswordVisible = false;

    // Launcher dùng để mở màn hình đăng nhập Google và nhận kết quả trả về
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        initGoogleLauncher();

        initView();
        setupGoogleSignIn();
        setupFacebookLogin();

        handleTogglePassword();
        handleLoginEmail();
        handleForgotPassword();
        handleRegister();
        handleGoogleLogin();
        handleFacebookLogin();
    }

    private void initView() {
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        imgTogglePassword = findViewById(R.id.imgTogglePassword);

        btnLoginEmail = findViewById(R.id.btnLoginEmail);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        btnRegisterEmail = findViewById(R.id.btnRegisterEmail);

        btnLoginGoogle = findViewById(R.id.btnLoginGoogle);
        btnLoginFacebookCustom = findViewById(R.id.btnLoginFacebookCustom);
        btnLoginFacebook = findViewById(R.id.btnLoginFacebook);
    }

    // Khởi tạo launcher để nhận kết quả đăng nhập Google
    private void initGoogleLauncher() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);

                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Toast.makeText(this, "Không lấy được tài khoản Google", Toast.LENGTH_SHORT).show();
                        }

                    } catch (ApiException e) {
                        Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Cấu hình Google Sign-In để lấy email và idToken
    private void setupGoogleSignIn() {
        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    // Cấu hình đăng nhập Facebook và xử lý kết quả trả về
    private void setupFacebookLogin() {
        btnLoginFacebook.setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);
        btnLoginFacebook.setPermissions(Arrays.asList("email", "public_profile"));

        btnLoginFacebook.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                firebaseAuthWithFacebook(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Đã hủy đăng nhập Facebook", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                android.util.Log.e("FB_LOGIN_ERROR", "Facebook login error", error);

                Toast.makeText(
                        LoginActivity.this,
                        "Lỗi Facebook: " + error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Xử lý ẩn hoặc hiện mật khẩu khi bấm icon con mắt
    private void handleTogglePassword() {
        imgTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                imgTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                isPasswordVisible = false;
            } else {
                edtPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                imgTogglePassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = true;
            }

            edtPassword.setSelection(edtPassword.getText().length());
        });
    }

    // Xử lý đăng nhập bằng email và mật khẩu
    private void handleLoginEmail() {
        btnLoginEmail.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString();

            if (!validateLoginInput(email, password)) {
                return;
            }

            loginWithEmail(email, password);
        });
    }

    // Kiểm tra dữ liệu email và mật khẩu trước khi đăng nhập
    private boolean validateLoginInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return false;
        }

        if (hasWhiteSpace(password)) {
            edtPassword.setError("Mật khẩu không được chứa khoảng trắng");
            edtPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải từ 6 ký tự");
            edtPassword.requestFocus();
            return false;
        }

        return true;
    }

    // Kiểm tra chuỗi có chứa khoảng trắng hay không
    private boolean hasWhiteSpace(String text) {
        return text != null && text.matches(".*\\s.*");
    }

    // Gọi Firebase để đăng nhập bằng email và mật khẩu
    private void loginWithEmail(String email, String password) {
        btnLoginEmail.setEnabled(false);
        btnLoginEmail.setText("Đang đăng nhập...");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    btnLoginEmail.setEnabled(true);
                    btnLoginEmail.setText("Đăng nhập");

                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    btnLoginEmail.setEnabled(true);
                    btnLoginEmail.setText("Đăng nhập");

                    Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Chuyển sang màn hình quên mật khẩu
    private void handleForgotPassword() {
        txtForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void sendResetPasswordEmail(String email) {
        txtForgotPassword.setEnabled(false);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    txtForgotPassword.setEnabled(true);

                    Toast.makeText(
                            this,
                            getString(R.string.reset_password_email_sent),
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnFailureListener(e -> {
                    txtForgotPassword.setEnabled(true);

                    Toast.makeText(
                            this,
                            getString(R.string.reset_password_failed_format, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Chuyển sang màn hình đăng ký tài khoản
    private void handleRegister() {
        btnRegisterEmail.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Mở màn hình đăng nhập Google
    private void handleGoogleLogin() {
        btnLoginGoogle.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        });
    }

    // Xác thực tài khoản Google với Firebase bằng idToken
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Đăng nhập Google thành công", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firebase Google lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void handleFacebookLogin() {
        btnLoginFacebookCustom.setOnClickListener(v -> {
            btnLoginFacebook.performClick();
        });
    }

    private void firebaseAuthWithFacebook(String token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Đăng nhập Facebook thành công", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firebase Facebook lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Nhận kết quả đăng nhập Facebook từ Facebook SDK
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}