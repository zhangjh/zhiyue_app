package cn.zhangjh.zhiyue.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.zhangjh.zhiyue.R;
import cn.zhangjh.zhiyue.model.ChatMsg;
import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private final List<ChatMsg> messages;
    private final Context context;
    private final Markwon markwon;

    public ChatAdapter(List<ChatMsg> messages, Context context) {
        this.messages = messages;
        this.context = context;
        this.markwon = Markwon.create(context);
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
            viewType == ChatMsg.TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_ai,
            parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMsg message = messages.get(position);
        if (message.getType() == ChatMsg.TYPE_AI) {
            markwon.setMarkdown(holder.messageText, message.getContent());
        } else {
            holder.messageText.setText(message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }
    }
}