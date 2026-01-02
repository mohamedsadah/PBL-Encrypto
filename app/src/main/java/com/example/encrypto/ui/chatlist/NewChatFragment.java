package com.example.encrypto.ui.chatlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.encrypto.R;
import com.example.encrypto.model.User;
import com.example.encrypto.viewmodel.NewChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class NewChatFragment extends Fragment {

    public interface NewChatFragmentListener {
        void onContactSelected(User user);
        void onFragmentClosed();
    }

    private NewChatViewModel vm;
    private RecyclerView rv;
    private ChatListAdapter.ContactAdapter adapter;
    private ProgressBar progress;
    private NewChatFragmentListener listener;
    private String currentUserId;

    private MaterialToolbar toolbar;
    private CardView noContacts;

    // Map to store local contact names (Key: Phone, Value: Name)
    private final Map<String, String> localContactNames = new HashMap<>();

    public static NewChatFragment newInstance(String currentUserId) {
        NewChatFragment fragment = new NewChatFragment();
        Bundle args = new Bundle();
        args.putString("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NewChatFragmentListener) {
            listener = (NewChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NewChatFragmentListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getString("currentUserId");
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadContacts();
                } else {
                    Toast.makeText(getContext(), "Permission denied.", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onFragmentClosed();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_chat, container, false);
        rv = view.findViewById(R.id.rvContacts);
        progress = view.findViewById(R.id.progressContacts);
        toolbar = view.findViewById(R.id.Toolbar);
        noContacts = view.findViewById(R.id.NoContacts);

        adapter = new ChatListAdapter.ContactAdapter(user -> {
            if (listener != null) listener.onContactSelected(user);
        });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(NewChatViewModel.class);

        vm.getRegisteredUsers().observe(getViewLifecycleOwner(), users -> {
            progress.setVisibility(View.GONE);
            if (users == null || users.isEmpty()) {
                if (noContacts != null) noContacts.setVisibility(View.VISIBLE);
            } else {
                if (noContacts != null) noContacts.setVisibility(View.GONE);

                // Match Supabase users with Local Contact Names
                for (User u : users) {
                    String normalizedDBPhone = normalizePhone(u.phone);
                    if (localContactNames.containsKey(normalizedDBPhone)) {
                        u.displayName = localContactNames.get(normalizedDBPhone);
                    }
                }

                adapter.submitList(users);
            }
        });

        vm.getError().observe(getViewLifecycleOwner(), error -> {
            progress.setVisibility(View.GONE);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        checkPermissionAndLoad();

        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_back);
            toolbar.setNavigationOnClickListener(v -> {
                if (listener != null) listener.onFragmentClosed();
            });
        }
    }

    private void checkPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    @SuppressLint("Range")
    private void loadContacts() {
        progress.setVisibility(View.VISIBLE);
        localContactNames.clear(); // Clear old data
        HashSet<String> phoneNumbersToSend = new HashSet<>();

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        // Request NAME and NUMBER
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    if (number != null) {
                        String normalized = normalizePhone(number);
                        if (normalized.length() > 8) {
                            phoneNumbersToSend.add(normalized);
                            // Store the mapping: Phone -> Name
                            localContactNames.put(normalized, name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (phoneNumbersToSend.isEmpty() || currentUserId == null) {
            progress.setVisibility(View.GONE);
            if (phoneNumbersToSend.isEmpty()){
                if (noContacts != null) noContacts.setVisibility(View.VISIBLE);
            }
            return;
        }

        vm.findRegisteredUsers(new ArrayList<>(phoneNumbersToSend), currentUserId);
    }

    // Helper to strip everything except digits
    private String normalizePhone(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^\\d]", ""); // Matches DB logic
    }
}