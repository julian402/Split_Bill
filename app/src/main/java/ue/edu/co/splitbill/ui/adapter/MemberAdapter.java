package ue.edu.co.splitbill.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;

/**
 * Pinta la lista de integrantes del grupo.
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    /** Aviso de que el usuario quiere dar de baja a un integrante. */
    public interface OnMemberDeleteListener {
        void onMemberDelete(User user);
    }

    private final List<User> members = new ArrayList<>();
    private final OnMemberDeleteListener deleteListener;

    public MemberAdapter(OnMemberDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setMembers(List<User> members) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        holder.bind(this.members.get(position));
    }

    @Override
    public int getItemCount() {
        return this.members.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvMemberNames;
        private final TextView tvMemberPhone;
        private final Button btnDeleteMember;

        MemberViewHolder(View itemView) {
            super(itemView);
            this.tvMemberNames = itemView.findViewById(R.id.tvMemberNames);
            this.tvMemberPhone = itemView.findViewById(R.id.tvMemberPhone);
            this.btnDeleteMember = itemView.findViewById(R.id.btnDeleteMember);
        }

        void bind(final User user) {
            this.tvMemberNames.setText(user.getNames());
            boolean hasPhone = user.getPhone() != null && !user.getPhone().trim().isEmpty();
            this.tvMemberPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
            this.tvMemberPhone.setText(user.getPhone());
            this.btnDeleteMember.setOnClickListener(view -> deleteListener.onMemberDelete(user));
        }
    }
}
