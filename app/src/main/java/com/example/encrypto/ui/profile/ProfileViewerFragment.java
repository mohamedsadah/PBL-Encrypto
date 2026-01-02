package com.example.encrypto.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.encrypto.R;

public class ProfileViewerFragment extends Fragment {

    private static final String ARG_IMAGE_URL = "image_url";
    private static final String ARG_USER_NAME = "user_name";

    private String imageUrl;
    private String userName;

    public static ProfileViewerFragment newInstance(String imageUrl, String userName) {
        ProfileViewerFragment fragment = new ProfileViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUrl = getArguments().getString(ARG_IMAGE_URL);
            userName = getArguments().getString(ARG_USER_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imgFull = view.findViewById(R.id.imgFullProfile);
        TextView tvName = view.findViewById(R.id.tvUserName);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        tvName.setText(userName != null ? userName : "Profile Picture");

        // Load the image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(imgFull);
        } else {
            imgFull.setImageResource(R.drawable.ic_person);
        }

        // Close listeners
        btnClose.setOnClickListener(v -> closeViewer());


        // view.setOnClickListener(v -> closeViewer());
    }

    private void closeViewer() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }
}