package com.example.encrypto.ui.chatlist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.encrypto.R;
import com.example.encrypto.data.SupabaseRealtimeClient;
import com.example.encrypto.model.Chat;
import com.example.encrypto.model.User;
import com.example.encrypto.ui.chat.ChatActivity;
import com.example.encrypto.ui.profile.ProfileSettingsFragment;
import com.example.encrypto.ui.status.StatusFragment;
import com.example.encrypto.viewmodel.ChatListViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ChatListActivity extends AppCompatActivity implements NewChatFragment.NewChatFragmentListener {

    private BottomNavigationView bottomNav;
    private ChatListViewModel vm;
    private String currentUserId;
    private View overlayContainer;

    private String pendingChatTitle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        currentUserId = prefs.getString("auth_user_id", null);

        if (currentUserId == null) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        overlayContainer = findViewById(R.id.overlay_container);

        vm = new ViewModelProvider(this).get(ChatListViewModel.class);

        vm.getNewChat().observe(this, chat -> {
            Log.i("ChatListActivity", "New chat received");
            if (chat != null) {
                if (pendingChatTitle != null) {
                    chat.title = pendingChatTitle;
                    pendingChatTitle = null;
                }
                onChatClicked(chat);

                vm.clearNewChat();
            }

        });

        loadFragment(ChatsFragment.newInstance(currentUserId));

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;

            if (id == R.id.nav_chats) {
                selectedFragment = ChatsFragment.newInstance(currentUserId);
            } else if (id == R.id.nav_status) {
                selectedFragment = new StatusFragment();

            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileSettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentUserId != null) {
            vm.startRealtime(currentUserId);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        SupabaseRealtimeClient client = SupabaseRealtimeClient.getInstance();
        if (client.isConnected()) {
            String currentSub = client.getCurrentSubscribedChatId();
            Log.d("ChatListActivity", "Resuming, current sub: " + currentSub);


            if (currentSub != null) {
                Log.d("ChatListActivity", "Restoring 'all messages' subscription");
                client.subscribeToChat(null);
                vm.loadChats(currentUserId, ChatListActivity.this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (vm != null) vm.stopRealtime();
    }

    // NAVIGATION AND HELPERS

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void openNewChatFragment() {
        overlayContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.overlay_container, NewChatFragment.newInstance(currentUserId))
                .addToBackStack("NEW_CHAT_TAG")
                .commit();
    }

    @Override
    public void onFragmentClosed() {
        getSupportFragmentManager().popBackStack();
        overlayContainer.setVisibility(View.GONE);
    }

    @Override
    public void onContactSelected(User user) {
        String name = user.displayName != null ? user.displayName : "User";
        pendingChatTitle = name;

        vm.getOrCreateChat(currentUserId, user.id);

        onFragmentClosed();
    }

    public void onChatClicked(Chat chat) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("chatId", chat.id);
        i.putExtra("chatTitle", chat.title);
        i.putExtra("currentUserId", currentUserId);
        startActivity(i);
    }
}