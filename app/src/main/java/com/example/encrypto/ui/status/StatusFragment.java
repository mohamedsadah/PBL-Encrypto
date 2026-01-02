package com.example.encrypto.ui.status;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.encrypto.R;
import com.example.encrypto.model.Status;
import com.example.encrypto.viewmodel.StatusViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class StatusFragment extends Fragment {

    private StatusViewModel vm;
    private RecyclerView rv;
    private StatusAdapter adapter;
    private FloatingActionButton fab;
    private ProgressBar progress;
    private String currentUserId;

    private View myStatusCard;
    private ImageView myAvatar;
    private TextView myTitle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("auth_user_id", null);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rvStatus);
        fab = view.findViewById(R.id.fabAddStatus);
        progress = view.findViewById(R.id.progress);

        myStatusCard = view.findViewById(R.id.UserStatusHolder);
        myAvatar = myStatusCard.findViewById(R.id.imgAvatar);
        myTitle = myStatusCard.findViewById(R.id.tvTitle);

        adapter = new StatusAdapter();
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(StatusViewModel.class);

        vm.getGroupedStatuses().observe(getViewLifecycleOwner(), list -> {
            if (progress != null) progress.setVisibility(View.GONE);
            if (list != null) adapter.submitList(list);
        });

        vm.getMyStatusList().observe(getViewLifecycleOwner(), myStatuses -> {
            if (myStatuses != null && !myStatuses.isEmpty()) {
                if(myStatuses.size() > 1)
                    myTitle.setText(" My Statuses");
                else
                    myTitle.setText(" My Status");


                Status latestStatus = myStatuses.get(myStatuses.size() - 1);

                Glide.with(this)
                        .load(latestStatus.mediaUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(myAvatar);

                myStatusCard.setOnClickListener(v -> {
                    StatusViewerFragment fragment = StatusViewerFragment.newInstance(
                            myStatuses,
                            "You",
                            latestStatus.mediaUrl
                    );

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

            } else {
                myTitle.setText(" My Status");
                myAvatar.setImageResource(R.drawable.ic_person);

                myStatusCard.setOnClickListener(v -> pickImage());
            }
        });

        vm.getUploadStatus().observe(getViewLifecycleOwner(), url -> {
            if (url == null) return;

            if (!url.isEmpty()) {
                Toast.makeText(getContext(), "Image uploaded, saving...", Toast.LENGTH_SHORT).show();
                vm.postStatus(currentUserId, url, "My Status Update");

                Glide.with(this)
                        .load(url)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(myAvatar);

                vm.resetUploadStatus();
            } else {
                if (progress != null) progress.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                vm.resetUploadStatus();
            }
        });

        vm.getCreateResult().observe(getViewLifecycleOwner(), success -> {
            if (success == null) return;

            if (progress != null) progress.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(getContext(), "Status Posted!", Toast.LENGTH_SHORT).show();
                vm.loadStatuses(currentUserId, requireActivity().getApplicationContext());
            } else {
                Toast.makeText(getContext(), "Failed to save status", Toast.LENGTH_SHORT).show();
            }

            vm.resetCreateResult();
        });

        vm.loadStatuses(currentUserId, requireActivity().getApplicationContext());

        fab.setOnClickListener(v -> pickImage());
    }

    private final ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        if (progress != null) progress.setVisibility(View.VISIBLE);
                        vm.uploadImage(requireContext(), uri);
                    }
                }
            }
    );

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickerLauncher.launch(intent);
    }
}