package ue.edu.co.splitbill.ui.adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.ui.DateText;

/**
 * Burbujas del chat. Las propias van a la derecha, en el color principal; las de los demas a la
 * izquierda, con el nombre de quien escribio (solo en la primera de varias seguidas). El dia aparece
 * cuando cambia, como en cualquier app de mensajes.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-CO"));

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages.clear();
        if (messages != null) {
            this.messages.addAll(messages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message previous = position > 0 ? this.messages.get(position - 1) : null;
        holder.bind(this.messages.get(position), previous);
    }

    @Override
    public int getItemCount() {
        return this.messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout rowMessage;
        private final TextView tvDayHeader;
        private final LinearLayout bubble;
        private final TextView tvSenderName;
        private final TextView tvMessageText;
        private final TextView tvMessageTime;

        MessageViewHolder(View itemView) {
            super(itemView);
            this.rowMessage = itemView.findViewById(R.id.rowMessage);
            this.tvDayHeader = itemView.findViewById(R.id.tvDayHeader);
            this.bubble = itemView.findViewById(R.id.bubble);
            this.tvSenderName = itemView.findViewById(R.id.tvSenderName);
            this.tvMessageText = itemView.findViewById(R.id.tvMessageText);
            this.tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }

        void bind(Message message, Message previous) {
            Context context = itemView.getContext();
            String day = DateText.day(context, message.getSentAt());
            boolean newDay = previous == null || !day.equals(DateText.day(context, previous.getSentAt()));
            this.tvDayHeader.setVisibility(newDay ? View.VISIBLE : View.GONE);
            this.tvDayHeader.setText(day);

            boolean mine = message.getSenderId() != null && message.getSenderId().equals(currentUserId);
            boolean sameSender = !newDay && previous != null && previous.getSenderId() != null
                    && previous.getSenderId().equals(message.getSenderId());
            this.rowMessage.setGravity(mine ? Gravity.END : Gravity.START);
            this.bubble.setBackgroundResource(mine ? R.drawable.bg_bubble_mine : R.drawable.bg_bubble_other);
            this.tvSenderName.setVisibility(mine || sameSender ? View.GONE : View.VISIBLE);
            this.tvSenderName.setText(message.getSenderNames());

            int textColor = ContextCompat.getColor(context, mine ? R.color.colorOnPrimary : R.color.colorTextPrimary);
            this.tvMessageText.setText(message.getText());
            this.tvMessageText.setTextColor(textColor);

            String time = message.getSentAt() == null ? "" : timeFormat.format(message.getSentAt());
            this.tvMessageTime.setText(message.isPending() ? context.getString(R.string.tvMessageSending, time) : time);
            this.tvMessageTime.setTextColor(ContextCompat.getColor(context,
                    mine ? R.color.colorOnPrimary : R.color.colorTextSecondary));
            this.tvMessageTime.setAlpha(mine ? 0.8f : 1f);
        }
    }
}
