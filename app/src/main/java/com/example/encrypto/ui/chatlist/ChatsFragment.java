package com.example.encrypto.ui.chatlist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.encrypto.R;
import com.example.encrypto.model.Chat;
import com.example.encrypto.model.Message;
import com.example.encrypto.ui.chat.ChatActivity;
import com.example.encrypto.utils.NotificationHelper;
import com.example.encrypto.viewmodel.ChatListViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ChatsFragment extends Fragment implements ChatListAdapter.OnChatClickListener {

    private ChatListViewModel vm;
    private RecyclerView rv;
    private ProgressBar progress;
    private ChatListAdapter adapter;
    private FloatingActionButton fab;

    private String currentUserId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static ChatsFragment newInstance(String uid) {
        ChatsFragment f = new ChatsFragment();
        Bundle args = new Bundle();
        args.putString("uid", uid);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getString("uid");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rvChats);
        progress = view.findViewById(R.id.progress);
        fab = view.findViewById(R.id.fabNewChat);

        adapter = new ChatListAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(requireActivity()).get(ChatListViewModel.class);

        vm.getChats().observe(getViewLifecycleOwner(), chats -> {
            Log.i("ChatsFragment", "getting messages");
            progress.setVisibility(View.GONE);
            if (chats != null) adapter.submitList(chats);
        });

        vm.getIncomingRealtimeMessage().observe(getViewLifecycleOwner(), msg -> {
            Log.i("ChatsFragment", "============================================");
            Log.i("ChatsFragment", "LiveData observer triggered");
            Log.i("ChatsFragment", "Message is null: " + (msg == null));
            Log.i("ChatsFragment", "============================================");

            if (msg != null) {
                Log.i("ChatsFragment", "Updating UI with message");
                String preview = msg.text != null
                        ? msg.text.substring(0, Math.min(20, msg.text.length()))
                        : "null";
                Log.i("ChatsFragment", "Text Preview: " + preview);

                updateLastMessage(msg);

                if (msg.senderId != null && !msg.senderId.equals(currentUserId)) {
                    String senderName = "New Message";

                    if (getContext() != null) {
                        NotificationHelper.showMessageNotification(requireContext(), msg, senderName);
                    }
                }
            } else {
                Log.e("ChatsFragment", "Received null message in observer");
            }
        });

        vm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
        });

        progress.setVisibility(View.VISIBLE);
        vm.loadChats(currentUserId, requireActivity().getApplicationContext());

        fab.setOnClickListener(v -> {
            if (getActivity() instanceof ChatListActivity) {
                ((ChatListActivity) getActivity()).openNewChatFragment();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            vm.startRealtime(currentUserId);
        }

    }

    public void updateLastMessage(Message msg) {
        List<Chat> currentList = new ArrayList<>(adapter.getCurrentList());
        if (msg.chatId == null) return;

        Chat targetChat = null;
        int index = -1;

        for (int i = 0; i < currentList.size(); i++) {
            Chat c = currentList.get(i);
            if (c.id != null && c.id.equals(msg.chatId)) {
                targetChat = c;
                index = i;
                break;
            }
        }

        if (targetChat != null) {
            targetChat.lastMessage = msg.text;
            targetChat.updatedAt = msg.createdAt;

            currentList.remove(index);
            currentList.add(0, targetChat);

        } else {
            Chat newChat = new Chat();
            newChat.id = msg.chatId;
            newChat.lastMessage = msg.text;
            newChat.updatedAt = msg.createdAt;
            newChat.title = newChat.partnerPhone;
            currentList.add(0, newChat);
        }

        adapter.submitList(currentList);

        if (!currentList.isEmpty()) {
            rv.scrollToPosition(0);
        }
    }

    public void reloadChatList() {
        if (currentUserId != null) {
            handler.postDelayed(() -> {
                vm.loadChats(currentUserId, requireActivity().getApplicationContext());
            }, 500);
        }
    }

    @Override
    public void onChatClicked(Chat chat) {
        Intent i = new Intent(getContext(), ChatActivity.class);
        i.putExtra("chatId", chat.id);
        i.putExtra("chatTitle", chat.title);
        i.putExtra("currentUserId", currentUserId);
        startActivity(i);
    }
}