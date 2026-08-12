package com.example.engapp.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FirebaseUserDataRepository {

    public interface SyncCallback {
        void onSuccess(boolean hasCloudData);
        void onFailure(Exception e);
    }

    private static final String NODE_USERS = "users";
    private static final String NODE_LOCAL_DATA = "localData";
    private static final String NODE_UPDATED_AT = "updatedAt";

    private static final String TYPE_STRING = "string";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_INT = "int";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_STRING_SET = "string_set";

    private final Context appContext;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference usersRef;

    private boolean isRestoring = false;

    /*
     * Các pref này được sync lên Firebase.
     * settings_pref vẫn được sync để nếu user đăng nhập lại có thể khôi phục cài đặt.
     */
    private final String[] prefNames = {
            "settings_pref",
            "goal_pref",
            "study_session_pref",
            "study_time_pref",
            "saved_api_words_pref",
            "progress_pref",
            "favorite_lists_pref",
            "user_profile"
    };

    /*
     * Các pref này sẽ bị xóa local khi logout.
     * Không xóa settings_pref để giữ ngôn ngữ/theme/sound của máy.
     */
    private final String[] userDataPrefNames = {
            "goal_pref",
            "study_session_pref",
            "study_time_pref",
            "saved_api_words_pref",
            "progress_pref",
            "favorite_lists_pref",
            "user_profile"
    };

    private final Map<String, SharedPreferences.OnSharedPreferenceChangeListener> listeners = new HashMap<>();

    public FirebaseUserDataRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.usersRef = FirebaseDatabase.getInstance().getReference(NODE_USERS);
    }

    public boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public void restoreCloudThenStartAutoSync(@Nullable SyncCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }

        restoreCloudToLocal(new SyncCallback() {
            @Override
            public void onSuccess(boolean hasCloudData) {
                startAutoSync();

                /*
                 * Nếu tài khoản này chưa có dữ liệu cloud,
                 * backup dữ liệu local hiện tại lên Firebase.
                 */
                if (!hasCloudData) {
                    syncLocalToCloud(new SyncCallback() {
                        @Override
                        public void onSuccess(boolean ignored) {
                            if (callback != null) {
                                callback.onSuccess(false);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (callback != null) {
                                callback.onFailure(e);
                            }
                        }
                    });

                    return;
                }

                if (callback != null) {
                    callback.onSuccess(true);
                }
            }

            @Override
            public void onFailure(Exception e) {
                startAutoSync();

                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void startAutoSync() {
        stopAutoSync();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        for (String prefName : prefNames) {
            SharedPreferences preferences = getPreferences(prefName);

            SharedPreferences.OnSharedPreferenceChangeListener listener =
                    (sharedPreferences, key) -> {
                        if (isRestoring) {
                            return;
                        }

                        if (firebaseAuth.getCurrentUser() == null) {
                            return;
                        }

                        syncLocalToCloud(null);
                    };

            preferences.registerOnSharedPreferenceChangeListener(listener);
            listeners.put(prefName, listener);
        }
    }

    public void stopAutoSync() {
        for (Map.Entry<String, SharedPreferences.OnSharedPreferenceChangeListener> entry : listeners.entrySet()) {
            SharedPreferences preferences = getPreferences(entry.getKey());
            preferences.unregisterOnSharedPreferenceChangeListener(entry.getValue());
        }

        listeners.clear();
    }

    public void syncLocalToCloud(@Nullable SyncCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }

        Map<String, Object> localData = new HashMap<>();

        for (String prefName : prefNames) {
            SharedPreferences preferences = getPreferences(prefName);
            localData.put(prefName, serializeSharedPreferences(preferences));
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put(NODE_LOCAL_DATA, localData);
        updateData.put(NODE_UPDATED_AT, System.currentTimeMillis());

        usersRef.child(user.getUid())
                .updateChildren(updateData)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public void restoreCloudToLocal(@Nullable SyncCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }

        usersRef.child(user.getUid())
                .child(NODE_LOCAL_DATA)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (callback != null) {
                            callback.onSuccess(false);
                        }
                        return;
                    }

                    isRestoring = true;

                    try {
                        for (String prefName : prefNames) {
                            DataSnapshot prefSnapshot = snapshot.child(prefName);

                            if (!prefSnapshot.exists()) {
                                continue;
                            }

                            SharedPreferences preferences = getPreferences(prefName);
                            restoreSharedPreferences(preferences, prefSnapshot);
                        }

                        if (callback != null) {
                            callback.onSuccess(true);
                        }

                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onFailure(e);
                        }

                    } finally {
                        isRestoring = false;
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /*
     * Dùng khi logout.
     * Xóa dữ liệu học/favorite/avatar local để tài khoản mới không bị dính dữ liệu cũ.
     * Không xóa settings_pref.
     */
    public void clearLocalUserData() {
        stopAutoSync();

        for (String prefName : userDataPrefNames) {
            getPreferences(prefName)
                    .edit()
                    .clear()
                    .apply();
        }
    }

    /*
     * Dùng sau này khi xóa tài khoản thật.
     */
    public void clearCloudUserData(@Nullable SyncCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            if (callback != null) {
                callback.onSuccess(false);
            }
            return;
        }

        usersRef.child(user.getUid())
                .removeValue()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private SharedPreferences getPreferences(String prefName) {
        return appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE);
    }

    private Map<String, Object> serializeSharedPreferences(SharedPreferences preferences) {
        Map<String, Object> result = new HashMap<>();
        Map<String, ?> allValues = preferences.getAll();

        for (Map.Entry<String, ?> entry : allValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            if (shouldSkipKey(key)) {
                continue;
            }

            Map<String, Object> valueMap = new HashMap<>();

            if (value instanceof String) {
                valueMap.put("type", TYPE_STRING);
                valueMap.put("value", value);

            } else if (value instanceof Boolean) {
                valueMap.put("type", TYPE_BOOLEAN);
                valueMap.put("value", value);

            } else if (value instanceof Integer) {
                valueMap.put("type", TYPE_INT);
                valueMap.put("value", value);

            } else if (value instanceof Long) {
                valueMap.put("type", TYPE_LONG);
                valueMap.put("value", value);

            } else if (value instanceof Float) {
                valueMap.put("type", TYPE_FLOAT);
                valueMap.put("value", value);

            } else if (value instanceof Set<?>) {
                valueMap.put("type", TYPE_STRING_SET);

                ArrayList<String> list = new ArrayList<>();

                for (Object item : (Set<?>) value) {
                    if (item != null) {
                        list.add(String.valueOf(item));
                    }
                }

                valueMap.put("value", list);
            }

            if (!valueMap.isEmpty()) {
                result.put(key, valueMap);
            }
        }

        return result;
    }

    private void restoreSharedPreferences(SharedPreferences preferences, DataSnapshot prefSnapshot) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        for (DataSnapshot keySnapshot : prefSnapshot.getChildren()) {
            String key = keySnapshot.getKey();

            if (key == null) {
                continue;
            }

            if (shouldSkipKey(key)) {
                continue;
            }

            String type = keySnapshot.child("type").getValue(String.class);
            Object value = keySnapshot.child("value").getValue();

            if (type == null || value == null) {
                continue;
            }

            switch (type) {
                case TYPE_STRING:
                    editor.putString(key, String.valueOf(value));
                    break;

                case TYPE_BOOLEAN:
                    if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                    } else {
                        editor.putBoolean(key, Boolean.parseBoolean(String.valueOf(value)));
                    }
                    break;

                case TYPE_INT:
                    if (value instanceof Number) {
                        editor.putInt(key, ((Number) value).intValue());
                    } else {
                        try {
                            editor.putInt(key, Integer.parseInt(String.valueOf(value)));
                        } catch (Exception ignored) {
                        }
                    }
                    break;

                case TYPE_LONG:
                    if (value instanceof Number) {
                        editor.putLong(key, ((Number) value).longValue());
                    } else {
                        try {
                            editor.putLong(key, Long.parseLong(String.valueOf(value)));
                        } catch (Exception ignored) {
                        }
                    }
                    break;

                case TYPE_FLOAT:
                    if (value instanceof Number) {
                        editor.putFloat(key, ((Number) value).floatValue());
                    } else {
                        try {
                            editor.putFloat(key, Float.parseFloat(String.valueOf(value)));
                        } catch (Exception ignored) {
                        }
                    }
                    break;

                case TYPE_STRING_SET:
                    Set<String> set = new HashSet<>();

                    if (value instanceof ArrayList<?>) {
                        for (Object item : (ArrayList<?>) value) {
                            if (item != null) {
                                set.add(String.valueOf(item));
                            }
                        }
                    }

                    editor.putStringSet(key, set);
                    break;
            }
        }

        editor.apply();
    }

    private boolean shouldSkipKey(String key) {
        return "learned_word_ids".equals(key)
                || "language".equals(key)
                || "language_code".equals(key);
    }
}