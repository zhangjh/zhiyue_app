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
        bindMessage(holder, position);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            // 只更新内容，不重新创建整个视图
            bindMessage(holder, position);
        }
    }
    
    private void bindMessage(ChatViewHolder holder, int position) {
        ChatMsg message = messages.get(position);
        if (message.getType() == ChatMsg.TYPE_AI) {
            String content = message.getContent();

            // 使用预先计算的宽度来设置文本
            holder.messageText.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            
            // 使用Markwon渲染Markdown内容
            markwon.setMarkdown(holder.messageText, content);
            
            // 确保视图立即重新测量和布局
            holder.messageText.requestLayout();
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