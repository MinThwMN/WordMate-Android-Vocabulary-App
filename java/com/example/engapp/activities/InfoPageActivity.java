package com.example.engapp.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.engapp.R;
import com.example.engapp.utils.LocaleHelper;

public class InfoPageActivity extends AppCompatActivity {

    public static final String EXTRA_PAGE_TYPE = "page_type";
    public static final String TYPE_TERMS = "terms";
    public static final String TYPE_PRIVACY = "privacy";

    private TextView txtInfoTitle;
    private TextView txtInfoContent;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_page);

        applyInfoPageInsets();

        txtInfoTitle = findViewById(R.id.txtInfoTitle);
        txtInfoContent = findViewById(R.id.txtInfoContent);

        loadContent();
    }

    private void applyInfoPageInsets() {
        View content = findViewById(R.id.layoutInfoContent);

        if (content == null) {
            return;
        }

        int defaultLeft = content.getPaddingLeft();
        int defaultTop = content.getPaddingTop();
        int defaultRight = content.getPaddingRight();
        int defaultBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            view.setPadding(
                    defaultLeft,
                    statusBarHeight + defaultTop,
                    defaultRight,
                    navigationBarHeight + defaultBottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    // Tải nội dung phù hợp theo loại trang: chính sách bảo mật hoặc điều khoản sử dụng
    private void loadContent() {
        String pageType = getIntent().getStringExtra(EXTRA_PAGE_TYPE);

        if (TYPE_PRIVACY.equals(pageType)) {
            txtInfoTitle.setText(getString(R.string.privacy_policy));
            txtInfoContent.setText(getString(R.string.privacy_policy_content));
        } else {
            txtInfoTitle.setText(getString(R.string.terms_of_service));
            txtInfoContent.setText(getString(R.string.terms_of_service_content));
        }
    }
}
