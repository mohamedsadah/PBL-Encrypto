package com.example.encrypto.data;

import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.example.encrypto.Constants;
import com.example.encrypto.api.SupabaseApi;
import com.example.encrypto.model.Chat;
import com.example.encrypto.model.Message;
import com.example.encrypto.model.Status;
import com.example.encrypto.model.User;
import com.example.encrypto.ui.login.LoginActivity;
import com.example.encrypto.utils.TokenManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseRepository {
    private static SupabaseRepository instance;
    private final SupabaseApi api;
    private final String apiKey = Constants.SUPABASE_ANON_KEY;

    private SupabaseRepository() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request.Builder builder = originalRequest.newBuilder()
                            .header("apikey", Constants.SUPABASE_ANON_KEY);

                    String path = originalRequest.url().encodedPath();

                    if (path.contains("/auth/v1/signup") || path.contains("/auth/v1/token")) {
                        return chain.proceed(builder.build());
                    }

                    String token = null;
                    try {
                        token = TokenManager.getAccessToken();
                    } catch (IllegalStateException ignored) {}

                    if (token != null && !token.isEmpty()) {
                        builder.header("Authorization", "Bearer " + token);
                    }

                    return chain.proceed(builder.build());
                })
                .addInterceptor(logging)
                .build();


        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(SupabaseApi.class);
    }



    public static SupabaseRepository getInstance() {
        if (instance == null) instance = new SupabaseRepository();
        return instance;
    }

    public void getChats(String userId, final MutableLiveData<List<Chat>> result) {
        Map<String, Object> body = new HashMap<>();
        body.put("p_user_id", userId);

        Call<List<Chat>> call = api.getChats(body);
        call.enqueue(new Callback<List<Chat>>() {
            @Override public void onResponse(Call<List<Chat>> call, Response<List<Chat>> response) {
                result.postValue(response.body());
            }
            @Override public void onFailure(Call<List<Chat>> call, Throwable t) {
                Log.e("SupabaseRepo", "getChats failure", t);
                result.postValue(null);
            }
        });
    }

    public void getMessages(String chatId, final MutableLiveData<List<Message>> result) {
        Call<List<Message>> call = api.getMessages("eq." + chatId);
        call.enqueue(new Callback<List<Message>>() {
            @Override public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                result.postValue(response.body());
            }
            @Override public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("SupabaseRepo", "getMessages failure", t);
                result.postValue(null);
            }
        });
    }

    public void sendMessage(Message message, final MutableLiveData<Message> result) {
        Call<Message> call = api.sendMessage(message);
        call.enqueue(new Callback<Message>() {
            @Override public void onResponse(Call<Message> call, Response<Message> response) {
                result.postValue(response.body());
            }
            @Override public void onFailure(Call<Message> call, Throwable t) {
                Log.e("SupabaseRepo", "sendMessage failure", t);
                result.postValue(null); }
        });
    }

    public void signIn(String email, String password, final MutableLiveData<Map<String, Object>> result) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        Call<Map<String, Object>> call = api.signIn(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                result.postValue(response.body());
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("SupabaseRepo", "signIn failure", t);
                result.postValue(null); }
        });
    }

    public void signInWithPhone(String phone, String password, MutableLiveData<Map<String, Object>> result) {
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", password);

        Call<Map<String, Object>> call = api.signIn(body);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(response.body());
                } else {
                    Map<String, Object> errorMap = new HashMap<>();
                    String errorMessage = "An error occurred";

                    if (response.code() == 400 || response.code() == 401) {
                        errorMessage = "Invalid phone number or password";
                    } else if (response.code() >= 500) {
                        errorMessage = "Server error. Please try again later";
                    } else if (response.errorBody() != null) {
                        try {
                            errorMessage = response.errorBody().string();
                        } catch (Exception e) {
                            errorMessage = "Unknown error";
                        }
                    }

                    errorMap.put("error", errorMessage);
                    result.postValue(errorMap);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> errorMap = new HashMap<>();
                String errorMessage;

                if (t instanceof java.io.IOException) {
                    errorMessage = "Network error. Please check your internet connection";
                } else {
                    errorMessage = "Unexpected error: " + t.getMessage();
                }

                errorMap.put("error", errorMessage);
                result.postValue(errorMap);
            }
        });
    }

    public void signUpWithPhone(String phone, String fullname, String password, final MutableLiveData<ApiResult> result) {

        Map<String, String> data = new HashMap<>();
        data.put("full_name", fullname);

        Map<String, Object> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", password);
        body.put("data", data);

        Call<Map<String, Object>> call = api.signUp(body);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(new ApiResult(true, response.body(), null));
                } else {
                    String rawError = null;
                    try {
                        ResponseBody eb = response.errorBody();
                        rawError = (eb != null) ? eb.string() : "empty error body";
                    } catch (IOException e) {
                        rawError = "error reading errorBody: " + e.getMessage();
                    }

                    String friendly = rawError;
                    try {
                        JSONObject o = new JSONObject(rawError);
                        if (o.has("msg")) {
                            friendly = o.optString("msg");
                        } else if (o.has("error_description")) {
                            friendly = o.optString("error_description");
                        } else if (o.has("message")) {
                            friendly = o.optString("message");
                        }
                    } catch (Exception ignored) {}

                    android.util.Log.i("SupabaseRepo", "<-- " + response.code() + " " + call.request().url() + " : " + rawError);

                    result.postValue(new ApiResult(false, null, friendly));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                String err = t == null ? "unknown" : t.getMessage();
                Log.e("SupabaseRepo", "signup failure", t);
                result.postValue(new ApiResult(false, null, err));
            }
        });
    }

    public static class ApiResult<T> {
        public final boolean success;
        public final T body;
        public final String error;

        public ApiResult(boolean success, T body, String error) {
            this.success = success;
            this.body = body;
            this.error = error;
        }
    }

    public void rpcFindUsersByPhone(
            List<String> phoneNumbers,
            String currentUserId,
            MutableLiveData<ApiResult> result
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("phone_numbers", phoneNumbers);
        body.put("caller_id", currentUserId);

        Call<List<User>> call = api.rpcFindUsersByPhone(body);

        call.enqueue(new Callback<List<User>>() {
            @Override public void onResponse(
                    Call<List<User>> call,
                    Response<List<User>> response
            ) {
                if (response.isSuccessful()) {
                    result.postValue(new ApiResult(true, response.body(), null));
                } else {
                    result.postValue(new ApiResult(false, null, readError(response)));
                }
            }

            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("SupabaseRepo", "rpcFindUsersByPhone failure", t);
                result.postValue(new ApiResult(false, null, t.getMessage()));
            }
        });
    }

    public void rpcSendMessage(
            String chatId,
            String senderId,
            String content,
            MutableLiveData<ApiResult> result
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("sender_id", senderId);
        body.put("content", content);

        Call<Map<String, Object>> call = api.rpcSendMessage(body);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(new ApiResult(true, response.body(), null));
                } else {
                    String err = readError(response);
                    result.postValue(new ApiResult(false, null, err));
                }
            }

            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                result.postValue(new ApiResult(false, null, t.getMessage()));
                Log.e("SupabaseRepo", "rpcSendMessage failure", t);
            }
        });
    }


    public void rpcGetOrCreateChat(
            String user1,
            String user2,
            MutableLiveData<ApiResult<Map<String, Object>>> result // FIX: Using MutableLiveData
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("user1", user1);
        body.put("user2", user2);

        Call<Map<String, Object>> call = api.rpcGetOrCreateChat(body);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                ApiResult<Map<String, Object>> resultObj;
                if (response.isSuccessful() && response.body() != null) {
                    resultObj = new ApiResult<>(true, response.body(), null);
                } else {
                    String err = readError(response);
                    resultObj = new ApiResult<>(false, null, err);
                }
                result.postValue(resultObj);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                ApiResult<Map<String, Object>> resultObj = new ApiResult<>(false, null, t == null ? "unknown" : t.getMessage());
                result.postValue(resultObj);
                Log.e("SupabaseRepo", "rpcGetOrCreateChat failure", t);
            }
        });
    }


    public void rpcUpdateStatus(
            String userId,
            String statusText,
            MutableLiveData<ApiResult> result
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("status_text", statusText);

        Call<Map<String, Object>> call = api.rpcUpdateStatus(body);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override public void onResponse(
                    Call<Map<String, Object>> call,
                    Response<Map<String, Object>> response
            ) {
                if (response.isSuccessful() && response.body() != null) {
                    result.postValue(new ApiResult(true, response.body(), null));
                } else {
                    result.postValue(new ApiResult(false, null, readError(response)));
                }
            }

            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("SupabaseRepo", "rpcUpdateStatus failure", t);
                result.postValue(new ApiResult(false, null, t.getMessage()));
            }
        });
    }



    private String readError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (Exception ignored) {}
        return "Unknown error (" + response.code() + ")";
    }


    public void getCurrentUser(String userId, MutableLiveData<User> result) {
        Call<List<User>> call = api.getUser("eq." + userId, "*");

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    result.postValue(response.body().get(0));
                } else {
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                result.postValue(null);
            }
        });
    }

    public void uploadStatusImage(byte[] imageBytes, String fileExtension, MutableLiveData<String> urlResult) {
        String fileName = System.currentTimeMillis() + "." + fileExtension;
        String url = Constants.SUPABASE_URL + "/storage/v1/object/status/" + fileName;

        // Create RequestBody from bytes
        RequestBody requestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + TokenManager.getAccessToken())
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .post(requestBody)
                .build();

        new Thread(() -> {
            try (okhttp3.Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/status/" + fileName;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            urlResult.postValue(publicUrl)
                    );
                } else {
                    Log.e("SupabaseRepo", "Upload failed: " + response.body().string());
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            urlResult.postValue(null)
                    );
                }
            } catch (Exception e) {
                Log.e("SupabaseRepo", "Upload error", e);
                urlResult.postValue(null);
            }
        }).start();
    }

    public void createStatus(Status status, MutableLiveData<Boolean> result) {
        Call<Void> call = api.postStatus(status);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    result.postValue(true);
                } else {
                    Log.e("SupabaseRepo", "Post status failed: " + response.code());
                    result.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SupabaseRepo", "Post status error", t);
                result.postValue(false);
            }
        });
    }

    public void getStatuses(MutableLiveData<List<Status>> result) {
        Call<List<Status>> call = api.getStatus("*,user:users(*)", "created_at.desc");

        call.enqueue(new Callback<List<Status>>() {
            @Override public void onResponse(Call<List<Status>> call, Response<List<Status>> response) {
                result.postValue(response.body());
            }
            @Override public void onFailure(Call<List<Status>> call, Throwable t) {
                result.postValue(null);
            }
        });
    }

    public void uploadAvatar(byte[] imageBytes, MutableLiveData<String> result) {
        String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
        String url = Constants.SUPABASE_URL + "/storage/v1/object/profile_pics/" + fileName;

        RequestBody requestBody = RequestBody.create(imageBytes, okhttp3.MediaType.parse("image/jpeg"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + TokenManager.getAccessToken())
                .addHeader("apikey", Constants.SUPABASE_ANON_KEY)
                .post(requestBody)
                .build();

        new Thread(() -> {
            try (okhttp3.Response response = new OkHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String publicUrl = Constants.SUPABASE_URL + "/storage/v1/object/public/profile_pics/" + fileName;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            result.postValue(publicUrl)
                    );
                } else {
                    result.postValue(null);
                }
            } catch (Exception e) {
                result.postValue(null);
            }
        }).start();
    }

    public void updateUserProfile(String userId, String photoUrl, MutableLiveData<Boolean> result) {
        Map<String, Object> body = new HashMap<>();
        body.put("profile_pic", photoUrl);

        Call<Void> call = api.updateUserProfile("eq." + userId, body);
        call.enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                result.postValue(response.isSuccessful());
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                result.postValue(false);
            }
        });
    }

    public void changePassword(String newPassword, MutableLiveData<String> result) {
        Map<String, Object> body = new HashMap<>();
        body.put("password", newPassword);

        String token = "Bearer " + TokenManager.getAccessToken();

        Call<Void> call = api.updateAuthUser(token, body);
        call.enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) result.postValue("Success");
                else result.postValue("Failed: " + response.message());
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                result.postValue("Error: " + t.getMessage());
            }
        });
    }
}