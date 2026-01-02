package com.example.encrypto.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.encrypto.R;
import com.example.encrypto.model.Message;
import com.example.encrypto.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final String currentUserId;
    private final AsyncListDiffer<Message> differ;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;

        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Message> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Message>() {
                @Override
                public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                    if (oldItem.id != null && newItem.id != null) {
                        return oldItem.id.equals(newItem.id);
                    }
                    return (oldItem.chatId + ":" + oldItem.senderId + ":" + oldItem.text)
                            .equals(newItem.chatId + ":" + newItem.senderId + ":" + newItem.text);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                    return safe(oldItem.text).equals(safe(newItem.text))
                            && safe(oldItem.senderId).equals(safe(newItem.senderId))
                            && safe(oldItem.createdAt).equals(safe(newItem.createdAt));
                }

                private String safe(String s) { return s == null ? "" : s; }
            };

    @Override
    public int getItemViewType(int position) {
        Message m = differ.getCurrentList().get(position);
        return currentUserId != null && currentUserId.equals(m.senderId)
                ? VIEW_TYPE_SENT
                : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_SENT)
                ? R.layout.item_message_sent
                : R.layout.item_message_received;

        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(differ.getCurrentList().get(position));
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<Message> list) {
        List<Message> sorted = new ArrayList<>(list);
        sortByTimestamp(sorted);
        differ.submitList(sorted);
    }

    public void autoAppend(Message message) {
        List<Message> current = new ArrayList<>(differ.getCurrentList());

        for (Message m : current) {
            if (m.id != null && m.id.equals(message.id)) {
                return;
            }
        }

        current.add(message);
        sortByTimestamp(current);
        differ.submitList(current);
    }

    public void replaceOptimistic(String optimisticId, Message realMessage) {
        List<Message> current = new ArrayList<>(differ.getCurrentList());
        for (int i = 0; i < current.size(); i++) {
            Message m = current.get(i);
            if (m.id != null && m.id.equals(optimisticId)) {
                current.set(i, realMessage);
                sortByTimestamp(current);
                differ.submitList(current);
                return;
            }
        }
    }

    public void removeOptimistic(String optimisticId) {
        List<Message> current = new ArrayList<>(differ.getCurrentList());
        for (int i = 0; i < current.size(); i++) {
            Message m = current.get(i);
            if (m.id != null && m.id.equals(optimisticId)) {
                current.remove(i);
                differ.submitList(current);
                return;
            }
        }
    }

    private void sortByTimestamp(List<Message> messages) {
        Collections.sort(messages, (a, b) -> {
            if (a.createdAt == null) return -1;
            if (b.createdAt == null) return 1;
            return a.createdAt.compareTo(b.createdAt);
        });
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;

        private static final SimpleDateFormat INPUT_FORMAT =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);

        private static final SimpleDateFormat OUTPUT_FORMAT =
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        VH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        private String sanitizeTimestamp(String fullTime) {
            int dotIndex = fullTime.indexOf('.');
            int offsetIndex = fullTime.indexOf('+');

            if (dotIndex != -1 && offsetIndex != -1 && offsetIndex > dotIndex) {
                return fullTime.substring(0, dotIndex + 4) + fullTime.substring(offsetIndex);
            }
            return fullTime;
        }

        void bind(Message m) {
            tvMessage.setText(m.text != null ? m.text : "");

            if (tvTime != null && m.createdAt != null && !m.createdAt.isEmpty()) {
                try {
                    tvTime.setText(DateUtils.getRelativeTime(m.createdAt));
                } catch (Exception e) {
                    tvTime.setText("");
                }
            }
        }
    }
}