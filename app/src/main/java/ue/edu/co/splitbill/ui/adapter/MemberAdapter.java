package ue.edu.co.splitbill.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.ui.Avatar;

/**
 * Pinta los integrantes de un grupo: avatar, nombre, telefono y su papel. Quien creo el grupo es
 * "Administrador"; los demas, "Integrante". Nadie se puede borrar a si mismo desde aqui.
 */
public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    /** Aviso de que el usuario quiere dar de baja a un integrante. */
    public interface OnMemberDeleteListener {
        void onMemberDelete(User user);
    }

    /** Aviso de que el usuario quiere vincular a un integrante sin cuenta con la cuenta de su email. */
    public interface OnMemberLinkListener {
        void onMemberLink(User user);
    }

    private final List<User> members = new ArrayList<>();
    private final OnMemberDeleteListener deleteListener;
    /** Null mientras el grupo no exista en el servidor (modo crear): ahi no se puede vincular. */
    private OnMemberLinkListener linkListener;
    private final String currentUserId;
    private String ownerId;

    public MemberAdapter(OnMemberDeleteListener deleteListener, String currentUserId) {
        this.deleteListener = deleteListener;
        this.currentUserId = currentUserId;
    }

    public void setOnMemberLinkListener(OnMemberLinkListener linkListener) {
        this.linkListener = linkListener;
        notifyDataSetChanged();
    }

    /** @param ownerId el dueno del grupo; null si aun no se sabe (se toma a la persona de la sesion) */
    public void setMembers(List<User> members, String ownerId) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
        this.ownerId = ownerId == null ? this.currentUserId : ownerId;
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

        private final TextView tvMemberInitials;
        private final TextView tvMemberNames;
        private final TextView tvMemberPhone;
        private final TextView tvMemberRole;
        private final TextView tvLinkAccount;
        private final ImageButton btnDeleteMember;

        MemberViewHolder(View itemView) {
            super(itemView);
            this.tvMemberInitials = itemView.findViewById(R.id.tvMemberInitials);
            this.tvMemberNames = itemView.findViewById(R.id.tvMemberNames);
            this.tvMemberPhone = itemView.findViewById(R.id.tvMemberPhone);
            this.tvMemberRole = itemView.findViewById(R.id.tvMemberRole);
            this.tvLinkAccount = itemView.findViewById(R.id.tvLinkAccount);
            this.btnDeleteMember = itemView.findViewById(R.id.btnDeleteMember);
        }

        void bind(final User user) {
            Context context = itemView.getContext();
            Avatar.bind(this.tvMemberInitials, user.getNames());
            this.tvMemberInitials.setText(Avatar.getInitials(user.getNames()).substring(0, 1));
            boolean isMe = user.getId().equals(currentUserId);
            //quien inicio sesion aparece marcado como "Tu" y no se puede borrar a si mismo desde aqui
            this.tvMemberNames.setText(isMe
                    ? user.getNames() + " (" + context.getString(R.string.tvYou) + ")"
                    : user.getNames());
            //con cuenta se muestra su email; sin cuenta, el telefono (si lo tiene)
            boolean hasAccount = user.getEmail() != null && !user.getEmail().trim().isEmpty();
            boolean hasPhone = user.getPhone() != null && !user.getPhone().trim().isEmpty();
            if (hasAccount) {
                this.tvMemberPhone.setText(context.getString(R.string.tvHasAccount, user.getEmail()));
            } else {
                this.tvMemberPhone.setText(hasPhone ? user.getPhone() : context.getString(R.string.tvNoPhone));
            }
            boolean canLink = linkListener != null && !hasAccount && !isMe;
            this.tvLinkAccount.setVisibility(canLink ? View.VISIBLE : View.GONE);
            this.tvLinkAccount.setOnClickListener(canLink ? view -> linkListener.onMemberLink(user) : null);

            boolean isOwner = user.getId().equals(ownerId);
            this.tvMemberRole.setText(isOwner ? R.string.tvRoleAdmin : R.string.tvRoleMember);
            this.tvMemberRole.setTextColor(ContextCompat.getColor(context,
                    isOwner ? R.color.colorPrimary : R.color.colorTextPrimary));
            this.tvMemberRole.setBackgroundTintList(ContextCompat.getColorStateList(context,
                    isOwner ? R.color.colorIconCircle : R.color.colorSettledContainer));

            this.btnDeleteMember.setVisibility(isMe ? View.INVISIBLE : View.VISIBLE);
            this.btnDeleteMember.setOnClickListener(view -> deleteListener.onMemberDelete(user));
        }
    }
}
