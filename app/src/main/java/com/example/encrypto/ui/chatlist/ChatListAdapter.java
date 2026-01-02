package com.example.encrypto.ui.chatlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.example.encrypto.R;
import com.example.encrypto.model.Chat;
import com.example.encrypto.model.User;
import com.example.encrypto.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.VH> {

    public interface OnChatClickListener { void onChatClicked(Chat chat); }

    private final OnChatClickListener listener;
    private final AsyncListDiffer<Chat> differ;

    public ChatListAdapter(OnChatClickListener listener) {
        this.listener = listener;
        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    }

    public void submitList(List<Chat> list) {
        differ.submitList(list);
    }

    public List<Chat> getCurrentList() {
        return new ArrayList<>(differ.getCurrentList());
    }

    // DIFF CALLBACK
    private static final DiffUtil.ItemCallback<Chat> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Chat>() {
                @Override
                public boolean areItemsTheSame(@NonNull Chat a, @NonNull Chat b) {
                    return a.id != null && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Chat a, @NonNull Chat b) {
                    if (!safe(a.title).equals(safe(b.title))) return false;
                    if (!safe(a.lastMessage).equals(safe(b.lastMessage))) return false;
                    if (!safe(a.updatedAt).equals(safe(b.updatedAt))) return false;
                    if (!safe(a.profilePic).equals(safe(b.profilePic))) return false;
                    return true;
                }

                private String safe(String s) { return s == null ? "" : s; }
            };

    // ADAPTER OVERRIDES
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Chat c = differ.getCurrentList().get(pos);

        h.title.setText(c.title != null ? c.title : "Unknown");
        h.last.setText(c.lastMessage != null ? c.lastMessage : "");
        h.time.setText(DateUtils.getRelativeTime(c.updatedAt));

        Glide.with(h.itemView)
                .load(c.profilePic)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(h.avatar);

        h.itemView.setOnClickListener(v -> listener.onChatClicked(c));
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, last, time;
        ImageView avatar;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            last  = v.findViewById(R.id.tvLast);
            time  = v.findViewById(R.id.tvLMtime);
            avatar = v.findViewById(R.id.imgAvatar);
        }
    }

    // CONTACT ADAPTER
    public static class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactVH> {

        public interface OnContactClickListener {
            void onContactClicked(User user);
        }

        private final OnContactClickListener listener;
        private final AsyncListDiffer<User> differ;

        public ContactAdapter(OnContactClickListener listener) {
            this.listener = listener;
            this.differ = new AsyncListDiffer<>(this, USER_DIFF);
        }

        public void submitList(List<User> list) {
            differ.submitList(list);
        }

        @NonNull
        @Override
        public ContactVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_list, parent, false);
            return new ContactVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ContactVH h, int position) {
            User u = differ.getCurrentList().get(position);

            h.title.setText(u.displayName != null ? u.displayName : "Unknown Contact");
            h.last.setText(u.phone != null ? u.phone : "");

            if (h.time != null) {
                h.time.setVisibility(View.GONE);
            }

            Glide.with(h.itemView)
                    .load(u.avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(h.avatar);

            h.itemView.setOnClickListener(v -> listener.onContactClicked(u));
        }

        @Override
        public int getItemCount() {
            return differ.getCurrentList().size();
        }

        // USER DIFF
        private static final DiffUtil.ItemCallback<User> USER_DIFF =
                new DiffUtil.ItemCallback<User>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull User a, @NonNull User b) {
                        return a.id.equals(b.id);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull User a, @NonNull User b) {
                        return safe(a.displayName).equals(safe(b.displayName)) &&
                                safe(a.phone).equals(safe(b.phone)) &&
                                safe(a.avatarUrl).equals(safe(b.avatarUrl));
                    }

                    private String safe(String s) { return s == null ? "" : s; }
                };

        static class ContactVH extends RecyclerView.ViewHolder {
            TextView title, last, time;
            ImageView avatar;

            ContactVH(@NonNull View v) {
                super(v);
                title = v.findViewById(R.id.tvTitle);
                last = v.findViewById(R.id.tvLast);
                time = v.findViewById(R.id.tvTime);
                avatar = v.findViewById(R.id.imgAvatar);
            }
        }
    }
}