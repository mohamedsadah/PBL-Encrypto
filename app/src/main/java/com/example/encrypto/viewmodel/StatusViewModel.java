package com.example.encrypto.viewmodel;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.encrypto.data.SupabaseRepository;
import com.example.encrypto.model.Status;
import com.example.encrypto.model.UserStatus;
import com.example.encrypto.utils.ImageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusViewModel extends ViewModel {
    private final SupabaseRepository repo = SupabaseRepository.getInstance();

    private final MutableLiveData<List<UserStatus>> groupedStatuses = new MutableLiveData<>();
    private final MutableLiveData<Status> myLatestStatus = new MutableLiveData<>();
    private final MutableLiveData<String> uploadStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();

    private final Map<String, String> localContactMap = new HashMap<>();

    public LiveData<List<UserStatus>> getGroupedStatuses() { return groupedStatuses; }
    public LiveData<Status> getMyLatestStatus() { return myLatestStatus; }
    public LiveData<String> getUploadStatus() { return uploadStatus; }
    public LiveData<Boolean> getCreateResult() { return createResult; }

    public void loadStatuses(String currentUserId, Context context) {
        loadLocalContactNames(context);

        repo.getStatuses(new MutableLiveData<List<Status>>() {
            @Override
            public void postValue(List<Status> rawList) {
                if (rawList != null) {
                    processStatuses(rawList, currentUserId);
                }
            }
        });
    }

    private final MutableLiveData<List<Status>> myStatusList = new MutableLiveData<>();

    public LiveData<List<Status>> getMyStatusList() { return myStatusList; }

    private void processStatuses(List<Status> rawList, String currentUserId) {
        Map<String, UserStatus> groups = new HashMap<>();
        List<Status> myOwnStatuses = new ArrayList<>();

        for (Status s : rawList) {
            if (s.userId.equals(currentUserId)) {
                myOwnStatuses.add(s);
                continue;
            }

            UserStatus us = groups.get(s.userId);
            if (us == null) {
                us = new UserStatus();
                us.userId = s.userId;
                us.user = s.user;

                String phone = (s.user != null) ? (String) s.user.get("phone") : null;

                if (phone != null) {
                    String norm = normalize(phone);
                    us.localName = localContactMap.get(norm);
                }

                if (us.localName == null && s.user != null) {
                    Object fn = s.user.get("full_name");
                    us.localName = fn != null ? fn.toString() : "Unknown";
                }

                groups.put(s.userId, us);
            }
            us.statuses.add(s);
        }

        List<UserStatus> finalList = new ArrayList<>(groups.values());

        Collections.sort(finalList, (a, b) ->
                b.getLatestTimestamp().compareTo(a.getLatestTimestamp())
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Collections.sort(myOwnStatuses, Comparator.comparing(a -> a.createdAt));
        }

        groupedStatuses.postValue(finalList);
        myStatusList.postValue(myOwnStatuses);
    }

    public void resetUploadStatus() {
        uploadStatus.postValue(null);
    }

    public void resetCreateResult() {
        createResult.postValue(null);
    }

    public void uploadImage(Context context, Uri uri) {
        new Thread(() -> {
            byte[] data = ImageUtils.getBytesFromUri(context, uri);
            if (data != null) {
                repo.uploadStatusImage(data, "jpg", uploadStatus);
            } else {
                uploadStatus.postValue("");
            }
        }).start();
    }

    public void postStatus(String userId, String url, String caption) {
        Status s = new Status();
        s.userId = userId;
        s.mediaUrl = url;
        s.caption = caption;
        repo.createStatus(s, createResult);
    }

    private void loadLocalContactNames(Context context) {
        localContactMap.clear();
        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null)) {

            if (cursor != null) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (cursor.moveToNext()) {
                    String num = cursor.getString(numIdx);
                    if (num != null) localContactMap.put(normalize(num), cursor.getString(nameIdx));
                }
            }
        } catch (Exception e) {
            Log.e("StatusVM", "Contact read error", e);
        }
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }
}