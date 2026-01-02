package com.example.encrypto.api;

import com.example.encrypto.model.Chat;
import com.example.encrypto.model.Message;
import com.example.encrypto.model.User;
import com.example.encrypto.model.Status;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface SupabaseApi {

    @POST("/auth/v1/token?grant_type=password")
    Call<Map<String, Object>> signIn(@Body Map<String, String> body);

    @POST("/auth/v1/signup")
    Call<Map<String, Object>> signUp(@Body Map<String, Object> body);

    @GET("/rest/v1/messages?select=*&order=created_at.asc")
    Call<List<Message>> getMessages(@Query("chat_id") String chatId);

    @POST("/rest/v1/messages")
    Call<Message> sendMessage(@Body Message message);

    @GET("/rest/v1/status")
    Call<List<Status>> getStatus(@Query("select") String select, @Query("order") String order);
    @POST("/rest/v1/status")
    Call<Void> postStatus(@Body Status status);

    @POST("/rest/v1/rpc/get_chats")
    Call<List<Chat>> getChats(@Body Map<String, Object> body);

    @POST("/rest/v1/rpc/find_users_by_phone")
    Call<List<User>> rpcFindUsersByPhone(@Body Map<String, Object> body);

    @POST("/rest/v1/rpc/get_or_create_chat")
    Call<Map<String, Object>> rpcGetOrCreateChat(@Body Map<String, Object> body);

    @POST("/rest/v1/rpc/send_message")
    Call<Map<String, Object>> rpcSendMessage(@Body Map<String, Object> body);

    @POST("/rest/v1/rpc/update_status")
    Call<Map<String, Object>> rpcUpdateStatus(@Body Map<String, Object> body);


    @GET("/rest/v1/users")
    Call<List<User>> getUser(
            @Query("id") String idFilter,
            @Query("select") String select
    );

    @retrofit2.http.PATCH("/rest/v1/users")
    Call<Void> updateUserProfile(
            @Query("id") String idFilter,
            @Body Map<String, Object> body
    );

    @PUT("/auth/v1/user")
    Call<Void> updateAuthUser(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );
}