package com.example.encrypto.ui.status;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.encrypto.R;
import com.example.encrypto.model.Status;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatusViewerFragment extends Fragment {

    private static final String ARG_STATUS_LIST = "status_list";
    private static final String ARG_USER_NAME = "user_name";
    private static final String ARG_USER_PIC = "user_pic";

    private List<Status> statusList;
    private String userName;
    private String userPic;
    private int currentIndex = 0;

    private ImageView statusImage, userAvatar;
    private TextView tvName, tvTime, tvCaption;
    private LinearLayout progressContainer;
    private View touchLeft, touchRight;

    private CountDownTimer timer;
    private long timeRemaining = 5000;
    private static final long TOTAL_TIME = 5000;
    private boolean isPaused = false;

    public static StatusViewerFragment newInstance(List<Status> statuses, String userName, String userPic) {
        StatusViewerFragment fragment = new StatusViewerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATUS_LIST, (Serializable) statuses);
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_PIC, userPic);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            statusList = (List<Status>) getArguments().getSerializable(ARG_STATUS_LIST);
            userName = getArguments().getString(ARG_USER_NAME);
            userPic = getArguments().getString(ARG_USER_PIC);
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status_viewer, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusImage = view.findViewById(R.id.statusImage);
        userAvatar = view.findViewById(R.id.imgUserAvatar);
        tvName = view.findViewById(R.id.tvUserName);
        tvTime = view.findViewById(R.id.tvTime);
        tvCaption = view.findViewById(R.id.tvCaption);
        progressContainer = view.findViewById(R.id.llProgressContainer);
        touchLeft = view.findViewById(R.id.viewLeft);
        touchRight = view.findViewById(R.id.viewRight);

        setupUI();
        setupProgressBars();
        loadStatus(0);

        View.OnTouchListener touchListener = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pauseTimer();
                    return true;
                case MotionEvent.ACTION_UP:
                    resumeTimer();
                    if (v.getId() == R.id.viewLeft) previousStatus();
                    else nextStatus();
                    return true;
            }
            return false;
        };

        touchLeft.setOnTouchListener(touchListener);
        touchRight.setOnTouchListener(touchListener);
    }

    private void setupUI() {
        tvName.setText(userName != null ? userName : "Unknown");
        Glide.with(this)
                .load(userPic)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(userAvatar);
    }

    private void setupProgressBars() {
        progressContainer.removeAllViews();
        for (int i = 0; i < statusList.size(); i++) {
            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 5, 0.5f);
            params.setMargins(4, 0, 4, 0);
            pb.setLayoutParams(params);
            pb.setMax(100);
            pb.setProgress(0);
            progressContainer.addView(pb);
        }
    }

    private void loadStatus(int index) {
        if (index < 0 || index >= statusList.size()) return;
        currentIndex = index;
        Status s = statusList.get(index);

        for (int i = 0; i < progressContainer.getChildCount(); i++) {
            ProgressBar pb = (ProgressBar) progressContainer.getChildAt(i);
            if (i < index) pb.setProgress(100);
            else pb.setProgress(0);
        }

        // Load Image
        Glide.with(this).load(s.mediaUrl).into(statusImage);

        // Set Caption & Time
        if (s.caption != null && !s.caption.isEmpty()) {
            tvCaption.setText(s.caption);
            tvCaption.setVisibility(View.VISIBLE);
        } else {
            tvCaption.setVisibility(View.GONE);
        }
        tvTime.setText(formatTime(s.createdAt));

        startTimer();
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timeRemaining = TOTAL_TIME;

        timer = new CountDownTimer(TOTAL_TIME, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isPaused) return;
                timeRemaining = millisUntilFinished;
                int progress = (int) (((TOTAL_TIME - millisUntilFinished) * 100) / TOTAL_TIME);
                updateCurrentProgressBar(progress);
            }

            @Override
            public void onFinish() {
                updateCurrentProgressBar(100);
                nextStatus();
            }
        }.start();
    }

    private void updateCurrentProgressBar(int progress) {
        if (currentIndex < progressContainer.getChildCount()) {
            ((ProgressBar) progressContainer.getChildAt(currentIndex)).setProgress(progress);
        }
    }

    private void pauseTimer() {
        isPaused = true;
        if (timer != null) timer.cancel();
    }

    private void resumeTimer() {
        isPaused = false;
        // Create a new timer with remaining time

        timer = new CountDownTimer(timeRemaining, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isPaused) return;
                timeRemaining = millisUntilFinished;
                long totalPassed = TOTAL_TIME - millisUntilFinished;
                int progress = (int) ((totalPassed * 100) / TOTAL_TIME);
                updateCurrentProgressBar(progress);
            }

            @Override
            public void onFinish() {
                nextStatus();
            }
        }.start();
    }

    private void nextStatus() {
        if (currentIndex < statusList.size() - 1) {
            loadStatus(currentIndex + 1);
        } else {
            closeViewer();
        }
    }

    private void previousStatus() {
        if (currentIndex > 0) {
            loadStatus(currentIndex - 1);
        } else {
            loadStatus(0); // Restart first one
        }
    }

    private void closeViewer() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        if (timer != null) timer.cancel();
        super.onDestroyView();
    }

    private String formatTime(String raw) {
        // Reuse your date formatter logic here
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return out.format(in.parse(raw));
        } catch (Exception e) { return ""; }
    }
}