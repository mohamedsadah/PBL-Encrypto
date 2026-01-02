package com.example.encrypto.ui.status;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.encrypto.R;
import com.example.encrypto.model.UserStatus;
import com.example.encrypto.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.VH> {
    private List<UserStatus> list = new ArrayList<>();

    private static final SimpleDateFormat INPUT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private static final SimpleDateFormat OUTPUT_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public void submitList(List<UserStatus> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_status, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        UserStatus us = list.get(position);

        holder.title.setText(us.localName != null ? us.localName : "Unknown");

        String imageUrl = us.getLatestMediaUrl();

        if (imageUrl == null && us.user != null) {
            Object pic = us.user.get("profile_pic");
            if (pic != null) imageUrl = pic.toString();
        }

        Glide.with(holder.itemView)
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .into(holder.img);

        holder.time.setText(DateUtils.getRelativeTime(us.getLatestTimestamp()));

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) v.getContext();

                String userName = (us.localName != null) ? us.localName : "Unknown";
                String userPic = null;

                if (us.user != null && us.user.get("profile_pic") != null) {
                    userPic = us.user.get("profile_pic").toString();
                }

                StatusViewerFragment fragment = StatusViewerFragment.newInstance(
                        us.statuses,
                        userName,
                        userPic
                );

                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override public int getItemCount() { return list.size(); }

    private String formatTime(String raw) {
        if (raw == null) return "";
        try {
            int dot = raw.indexOf('.');
            int plus = raw.indexOf('+');
            if (dot > 0 && plus > 0) raw = raw.substring(0, dot+4) + raw.substring(plus);
            Date d = INPUT_FORMAT.parse(raw);
            return OUTPUT_FORMAT.format(d);
        } catch (Exception e) { return ""; }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title, time;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.imgAvatar);
            title = v.findViewById(R.id.tvTitle);
            time = v.findViewById(R.id.tvLMtime);
        }
    }
}