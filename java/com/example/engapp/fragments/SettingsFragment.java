package com.example.engapp.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.engapp.R;
import com.example.engapp.activities.InfoPageActivity;
import com.example.engapp.activities.LoginActivity;
import com.example.engapp.repositories.FirebaseUserDataRepository;
import com.example.engapp.repositories.SettingsRepository;
import com.example.engapp.utils.ReminderScheduler;
import com.facebook.login.LoginManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private LinearLayout layoutProfileCard;
    private LinearLayout layoutDarkMode;
    private LinearLayout layoutSound;
    private LinearLayout layoutLanguage;
    private LinearLayout layoutReminder;
    private LinearLayout layoutAccountInfo;
    private LinearLayout layoutLogout;
    private LinearLayout layoutDeleteAccount;
    private LinearLayout layoutTerms;
    private LinearLayout layoutPrivacy;

    private ShapeableImageView imgUserAvatar;

    private TextView btnLogin;
    private TextView txtProfileTitle;
    private TextView txtProfileSubtitle;
    private TextView txtSelectedLanguage;
    private TextView txtReminderTime;

    private Switch switchDarkMode;
    private Switch switchSound;
    private Switch switchReminder;

    private SettingsRepository settingsRepository;
    private FirebaseAuth firebaseAuth;
    private SharedPreferences userPrefs;
    private FirebaseUserDataRepository firebaseUserDataRepository;

    private ActivityResultLauncher<Intent> pickAvatarLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private static final String PREF_USER = "user_profile";
    private static final String KEY_AVATAR_URI = "avatar_uri";

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();

                        if (imageUri != null) {
                            saveAvatar(imageUri);

                            Glide.with(this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(imgUserAvatar);

                            imgUserAvatar.setPadding(0, 0, 0, 0);

                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.avatar_changed),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        settingsRepository.setReminderEnabled(true);
                        ReminderScheduler.scheduleDailyReminder(requireContext());

                        if (switchReminder != null) {
                            switchReminder.setChecked(true);
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.reminder_enabled_message),
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        settingsRepository.setReminderEnabled(false);
                        ReminderScheduler.cancelReminder(requireContext());

                        if (switchReminder != null) {
                            switchReminder.setChecked(false);
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.notification_permission_required),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initViews(view);
        applySettingsInsets(view);

        initRepository();
        loadSettings();
        loadUserProfile();
        setupEvents();

        return view;
    }

    private void initViews(View view) {
        layoutProfileCard = view.findViewById(R.id.layoutProfileCard);

        layoutDarkMode = view.findViewById(R.id.layoutDarkMode);
        layoutSound = view.findViewById(R.id.layoutSound);
        layoutLanguage = view.findViewById(R.id.layoutLanguage);
        layoutReminder = view.findViewById(R.id.layoutReminder);

        layoutAccountInfo = view.findViewById(R.id.layoutAccountInfo);
        layoutLogout = view.findViewById(R.id.layoutLogout);
        layoutDeleteAccount = view.findViewById(R.id.layoutDeleteAccount);

        layoutTerms = view.findViewById(R.id.layoutTerms);
        layoutPrivacy = view.findViewById(R.id.layoutPrivacy);

        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);

        btnLogin = view.findViewById(R.id.btnLogin);
        txtProfileTitle = view.findViewById(R.id.txtProfileTitle);
        txtProfileSubtitle = view.findViewById(R.id.txtProfileSubtitle);

        txtSelectedLanguage = view.findViewById(R.id.txtSelectedLanguage);
        txtReminderTime = view.findViewById(R.id.txtReminderTime);

        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchSound = view.findViewById(R.id.switchSound);
        switchReminder = view.findViewById(R.id.switchReminder);
    }

    private void applySettingsInsets(View rootView) {
        View content = rootView.findViewById(R.id.layoutSettingsContent);

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

    private void initRepository() {
        settingsRepository = new SettingsRepository(requireContext());
        firebaseAuth = FirebaseAuth.getInstance();
        userPrefs = requireContext().getSharedPreferences(PREF_USER, Context.MODE_PRIVATE);
        firebaseUserDataRepository = new FirebaseUserDataRepository(requireContext());
    }

    private void loadSettings() {
        switchDarkMode.setChecked(settingsRepository.isDarkMode());
        switchSound.setChecked(settingsRepository.isSoundEnabled());
        switchReminder.setChecked(settingsRepository.isReminderEnabled());

        txtSelectedLanguage.setText(
                getLanguageDisplayName(settingsRepository.getLanguageCode())
        );

        txtReminderTime.setText(settingsRepository.getReminderTime());
    }

    private String getLanguageDisplayName(String languageCode) {
        if (SettingsRepository.LANGUAGE_EN.equals(languageCode)) {
            return getString(R.string.language_english);
        }

        if (SettingsRepository.LANGUAGE_JA.equals(languageCode)) {
            return getString(R.string.language_japanese);
        }

        if (SettingsRepository.LANGUAGE_KO.equals(languageCode)) {
            return getString(R.string.language_korean);
        }

        return getString(R.string.language_vietnamese);
    }

    private void loadUserProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null) {
            btnLogin.setVisibility(View.GONE);
            layoutLogout.setVisibility(View.VISIBLE);
            layoutDeleteAccount.setVisibility(View.VISIBLE);

            String name = user.getDisplayName();
            String email = user.getEmail();

            if (name != null && !name.trim().isEmpty()) {
                txtProfileTitle.setText(name);
            } else {
                txtProfileTitle.setText(getString(R.string.default_user_name));
            }

            if (email != null && !email.trim().isEmpty()) {
                txtProfileSubtitle.setText(email);
            } else {
                txtProfileSubtitle.setText(getString(R.string.logged_in));
            }

            loadAvatar();

        } else {
            btnLogin.setVisibility(View.VISIBLE);
            layoutLogout.setVisibility(View.GONE);
            layoutDeleteAccount.setVisibility(View.GONE);

            txtProfileTitle.setText(getString(R.string.start_your_journey));
            txtProfileSubtitle.setText(getString(R.string.login_to_sync_progress));

            imgUserAvatar.setImageResource(R.drawable.ic_user);
            imgUserAvatar.setPadding(18, 18, 18, 18);
        }
    }

    private void loadAvatar() {
        String savedAvatarUri = userPrefs.getString(KEY_AVATAR_URI, null);

        if (savedAvatarUri != null && !savedAvatarUri.isEmpty()) {
            Glide.with(this)
                    .load(Uri.parse(savedAvatarUri))
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .circleCrop()
                    .into(imgUserAvatar);

            imgUserAvatar.setPadding(0, 0, 0, 0);
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .circleCrop()
                    .into(imgUserAvatar);

            imgUserAvatar.setPadding(0, 0, 0, 0);
            return;
        }

        imgUserAvatar.setImageResource(R.drawable.ic_user);
        imgUserAvatar.setPadding(18, 18, 18, 18);
    }

    private void setupEvents() {
        layoutDarkMode.setOnClickListener(v -> {
            boolean newValue = !switchDarkMode.isChecked();
            switchDarkMode.setChecked(newValue);
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDarkMode(isChecked);
        });

        layoutSound.setOnClickListener(v -> {
            boolean newValue = !switchSound.isChecked();
            switchSound.setChecked(newValue);
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setSoundEnabled(isChecked);
        });

        layoutLanguage.setOnClickListener(v -> showLanguageDialog());

        layoutReminder.setOnClickListener(v -> showReminderTimePicker());

        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepository.setReminderEnabled(isChecked);

            if (isChecked) {
                requestNotificationPermissionAndSchedule();
            } else {
                ReminderScheduler.cancelReminder(requireContext());

                Toast.makeText(
                        requireContext(),
                        getString(R.string.reminder_disabled_message),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
        });

        layoutProfileCard.setOnClickListener(v -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user == null) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.login_required_change_avatar),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            openImagePicker();
        });

        layoutAccountInfo.setOnClickListener(v -> showAccountInfoDialog());

        layoutLogout.setOnClickListener(v -> showLogoutDialog());
        layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        layoutTerms.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), InfoPageActivity.class);
            intent.putExtra(InfoPageActivity.EXTRA_PAGE_TYPE, InfoPageActivity.TYPE_TERMS);
            startActivity(intent);
        });

        layoutPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), InfoPageActivity.class);
            intent.putExtra(InfoPageActivity.EXTRA_PAGE_TYPE, InfoPageActivity.TYPE_PRIVACY);
            startActivity(intent);
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        pickAvatarLauncher.launch(intent);
    }

    private void saveAvatar(Uri imageUri) {
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        userPrefs.edit()
                .putString(KEY_AVATAR_URI, imageUri.toString())
                .apply();
    }

    private void saveDarkMode(boolean enabled) {
        settingsRepository.setDarkMode(enabled);

        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showLanguageDialog() {
        String[] languageNames = {
                getString(R.string.language_vietnamese),
                getString(R.string.language_english),
                getString(R.string.language_japanese),
                getString(R.string.language_korean)
        };

        String[] languageCodes = {
                SettingsRepository.LANGUAGE_VI,
                SettingsRepository.LANGUAGE_EN,
                SettingsRepository.LANGUAGE_JA,
                SettingsRepository.LANGUAGE_KO
        };

        String currentCode = settingsRepository.getLanguageCode();
        int checkedIndex = 0;

        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentCode)) {
                checkedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setSingleChoiceItems(languageNames, checkedIndex, (dialog, which) -> {
                    String selectedCode = languageCodes[which];

                    settingsRepository.setLanguageCode(selectedCode);

                    Toast.makeText(
                            requireContext(),
                            getString(R.string.language_changed),
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showReminderTimePicker() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_reminder_time, null);

        TimePicker timePicker = dialogView.findViewById(R.id.timePickerReminder);
        TextView btnCancel = dialogView.findViewById(R.id.btnReminderCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnReminderSave);

        timePicker.setIs24HourView(true);

        String currentTime = settingsRepository.getReminderTime();

        int hour = 20;
        int minute = 0;

        try {
            String[] parts = currentTime.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } else {
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int selectedHour;
            int selectedMinute;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                selectedHour = timePicker.getHour();
                selectedMinute = timePicker.getMinute();
            } else {
                selectedHour = timePicker.getCurrentHour();
                selectedMinute = timePicker.getCurrentMinute();
            }

            String time = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    selectedHour,
                    selectedMinute
            );

            settingsRepository.setReminderTime(time);
            settingsRepository.setReminderEnabled(true);
            txtReminderTime.setText(time);

            if (switchReminder != null) {
                switchReminder.setChecked(true);
            }

            requestNotificationPermissionAndSchedule();

            Toast.makeText(
                    requireContext(),
                    getString(R.string.reminder_time_set_format, time),
                    Toast.LENGTH_SHORT
            ).show();

            dialog.dismiss();
        });

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        dialog.show();
    }

    private void requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            settingsRepository.setReminderEnabled(true);
            ReminderScheduler.scheduleDailyReminder(requireContext());

            if (switchReminder != null) {
                switchReminder.setChecked(true);
            }

            Toast.makeText(
                    requireContext(),
                    getString(R.string.notification_permission_not_required),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {

            settingsRepository.setReminderEnabled(true);
            ReminderScheduler.scheduleDailyReminder(requireContext());

            if (switchReminder != null) {
                switchReminder.setChecked(true);
            }

            Toast.makeText(
                    requireContext(),
                    getString(R.string.reminder_enabled_message),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.allow_notification))
                .setMessage(getString(R.string.notification_permission_message))
                .setPositiveButton(getString(R.string.grant_permission), (dialog, which) -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton(getString(R.string.later), (dialog, which) -> {
                    settingsRepository.setReminderEnabled(false);
                    ReminderScheduler.cancelReminder(requireContext());

                    if (switchReminder != null) {
                        switchReminder.setChecked(false);
                    }
                })
                .show();
    }

    private void showAccountInfoDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.account_info))
                    .setMessage(getString(R.string.account_not_logged_in))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_account_info, null);

        TextView txtEmail = dialogView.findViewById(R.id.txtDialogEmail);
        EditText edtName = dialogView.findViewById(R.id.edtDialogName);
        TextView btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnDialogSave);
        TextView btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);

        String currentName = user.getDisplayName();
        String email = user.getEmail();

        if (currentName == null || currentName.trim().isEmpty()) {
            currentName = getString(R.string.default_user_name);
        }

        if (email == null || email.trim().isEmpty()) {
            email = getString(R.string.no_email);
        }

        txtEmail.setText(email);
        edtName.setText(currentName);
        edtName.setSelection(edtName.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnSave.setOnClickListener(v -> {
            String newName = edtName.getText().toString().trim();

            if (newName.isEmpty()) {
                edtName.setError(getString(R.string.error_name_empty));
                edtName.requestFocus();
                return;
            }

            if (newName.length() < 2) {
                edtName.setError(getString(R.string.error_name_min_2));
                edtName.requestFocus();
                return;
            }

            updateDisplayName(newName, dialog);
        });

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        dialog.show();
    }

    private boolean hasPasswordProvider(FirebaseUser user) {
        if (user == null || user.getProviderData() == null) {
            return false;
        }

        for (UserInfo provider : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(provider.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.not_logged_in),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.no_email),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        TextView txtCurrentPasswordLabel = dialogView.findViewById(R.id.txtCurrentPasswordLabel);
        EditText edtCurrentPassword = dialogView.findViewById(R.id.edtCurrentPassword);
        EditText edtNewPassword = dialogView.findViewById(R.id.edtNewPassword);
        EditText edtConfirmNewPassword = dialogView.findViewById(R.id.edtConfirmNewPassword);

        TextView btnCancel = dialogView.findViewById(R.id.btnChangePasswordCancel);
        TextView btnSave = dialogView.findViewById(R.id.btnChangePasswordSave);

        boolean hasPassword = hasPasswordProvider(user);

        if (!hasPassword) {
            txtCurrentPasswordLabel.setVisibility(View.GONE);
            edtCurrentPassword.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String currentPassword = edtCurrentPassword.getText().toString();
            String newPassword = edtNewPassword.getText().toString();
            String confirmNewPassword = edtConfirmNewPassword.getText().toString();

            if (!validateChangePasswordInput(
                    edtCurrentPassword,
                    edtNewPassword,
                    edtConfirmNewPassword,
                    currentPassword,
                    newPassword,
                    confirmNewPassword,
                    hasPassword
            )) {
                return;
            }

            if (hasPassword) {
                changePasswordForEmailAccount(user, currentPassword, newPassword, dialog);
            } else {
                addPasswordForGoogleFacebookAccount(user, newPassword, dialog);
            }
        });

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        dialog.show();
    }

    private boolean validateChangePasswordInput(
            EditText edtCurrentPassword,
            EditText edtNewPassword,
            EditText edtConfirmNewPassword,
            String currentPassword,
            String newPassword,
            String confirmNewPassword,
            boolean hasPassword
    ) {
        if (hasPassword && TextUtils.isEmpty(currentPassword)) {
            edtCurrentPassword.setError(getString(R.string.error_enter_current_password));
            edtCurrentPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError(getString(R.string.error_enter_new_password));
            edtNewPassword.requestFocus();
            return false;
        }

        if (newPassword.contains(" ")) {
            edtNewPassword.setError(getString(R.string.error_password_no_space));
            edtNewPassword.requestFocus();
            return false;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError(getString(R.string.error_password_min_6));
            edtNewPassword.requestFocus();
            return false;
        }

        if (hasPassword && newPassword.equals(currentPassword)) {
            edtNewPassword.setError(getString(R.string.error_new_password_same));
            edtNewPassword.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmNewPassword)) {
            edtConfirmNewPassword.setError(getString(R.string.error_enter_confirm_new_password));
            edtConfirmNewPassword.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            edtConfirmNewPassword.setError(getString(R.string.error_password_not_match));
            edtConfirmNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void changePasswordForEmailAccount(
            FirebaseUser user,
            String currentPassword,
            String newPassword,
            AlertDialog dialog
    ) {
        String email = user.getEmail();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.no_email),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.password_change_success),
                                        Toast.LENGTH_SHORT
                                ).show();

                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.password_change_failed_format, e.getMessage()),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.current_password_wrong),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addPasswordForGoogleFacebookAccount(
            FirebaseUser user,
            String newPassword,
            AlertDialog dialog
    ) {
        String email = user.getEmail();

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.no_email),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, newPassword);

        user.linkWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.password_change_success),
                            Toast.LENGTH_SHORT
                    ).show();

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.password_change_failed_format, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateDisplayName(String newName, AlertDialog dialog) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        UserProfileChangeRequest profileUpdates =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();

        user.updateProfile(profileUpdates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.account_update_success),
                            Toast.LENGTH_SHORT
                    ).show();

                    loadUserProfile();

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.account_update_failed_format, e.getMessage()),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirm_message))
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> logoutCurrentUser())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void logoutCurrentUser() {
        firebaseAuth.signOut();

        try {
            LoginManager.getInstance().logOut();
        } catch (Exception ignored) {
        }

        if (firebaseUserDataRepository != null) {
            firebaseUserDataRepository.clearLocalUserData();
        }

        ReminderScheduler.cancelReminder(requireContext());

        Toast.makeText(
                requireContext(),
                getString(R.string.logged_out),
                Toast.LENGTH_SHORT
        ).show();

        goToLoginActivity();
    }

    private void showDeleteAccountDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirm_message))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deleteCurrentAccount())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteCurrentAccount() {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(requireContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseUserDataRepository == null) {
            firebaseUserDataRepository = new FirebaseUserDataRepository(requireContext());
        }

        firebaseUserDataRepository.clearCloudUserData(new FirebaseUserDataRepository.SyncCallback() {
            @Override
            public void onSuccess(boolean hasCloudData) {
                user.delete()
                        .addOnSuccessListener(unused -> {
                            try {
                                LoginManager.getInstance().logOut();
                            } catch (Exception ignored) {
                            }

                            firebaseUserDataRepository.clearLocalUserData();
                            ReminderScheduler.cancelReminder(requireContext());

                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.delete_account_success),
                                    Toast.LENGTH_SHORT
                            ).show();

                            goToLoginActivity();
                        })
                        .addOnFailureListener(SettingsFragment.this::handleDeleteAccountFailure);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.delete_account_data_failed_format, e.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void handleDeleteAccountFailure(Exception e) {
        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            if (firebaseUserDataRepository != null) {
                firebaseUserDataRepository.syncLocalToCloud(null);
            }

            Toast.makeText(
                    requireContext(),
                    getString(R.string.re_login_required_delete_account),
                    Toast.LENGTH_LONG
            ).show();

            logoutCurrentUser();
            return;
        }

        if (firebaseUserDataRepository != null) {
            firebaseUserDataRepository.syncLocalToCloud(null);
        }

        Toast.makeText(
                requireContext(),
                getString(R.string.delete_account_failed_format, e.getMessage()),
                Toast.LENGTH_LONG
        ).show();
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (firebaseAuth != null) {
            loadUserProfile();
        }
    }
}