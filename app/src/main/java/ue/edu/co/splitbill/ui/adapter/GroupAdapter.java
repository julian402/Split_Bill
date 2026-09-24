package ue.edu.co.splitbill.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.ui.Avatar;

/**
 * Pinta las tarjetas de los grupos: en el carrusel del inicio (ancho fijo) y en la lista de Grupos.
 *
 * Cada tarjeta muestra el saldo de la persona en ese grupo: "Te deben" en verde, "Debes" en rojo o
 * "Al dia". Los avatares pequenos salen de GroupListItem.getMemberNames(), que llena el repositorio.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    /** Toque (entrar al grupo) y menu de la tarjeta (renombrar, integrantes). */
    public interface OnGroupListener {
        void onGroupClick(GroupListItem group);

        void onGroupMenu(GroupListItem group, View anchor);
    }

    /** Ancho de cada tarjeta en el carrusel del inicio. */
    private static final int CAROUSEL_CARD_WIDTH_DP = 216;
    private static final int MINI_AVATAR_DP = 30;
    private static final int MINI_AVATAR_OVERLAP_DP = 8;

    private final List<GroupListItem> groups = new ArrayList<>();
    private final OnGroupListener listener;
    private final boolean carousel;
    private String currentGroupId;

    /** @param carousel true en el inicio: tarjetas de ancho fijo, una al lado de la otra */
    public GroupAdapter(OnGroupListener listener, boolean carousel) {
        this.listener = listener;
        this.carousel = carousel;
    }

    public void setGroups(List<GroupListItem> groups, String currentGroupId) {
        this.groups.clear();
        this.groups.addAll(groups);
        this.currentGroupId = currentGroupId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        if (this.carousel) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = dp(parent.getContext(), CAROUSEL_CARD_WIDTH_DP);
            view.setLayoutParams(params);
        }
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(this.groups.get(position));
    }

    @Override
    public int getItemCount() {
        return this.groups.size();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvGroupAvatar;
        private final TextView tvGroupName;
        private final TextView tvGroupMembers;
        private final ImageButton btnGroupMenu;
        private final LinearLayout llMemberAvatars;
        private final TextView tvGroupCurrent;
        private final TextView tvGroupStatus;
        private final TextView tvGroupBalance;
        private final TextView tvGroupTotal;

        GroupViewHolder(View itemView) {
            super(itemView);
            this.tvGroupAvatar = itemView.findViewById(R.id.tvGroupAvatar);
            this.tvGroupName = itemView.findViewById(R.id.tvGroupName);
            this.tvGroupMembers = itemView.findViewById(R.id.tvGroupMembers);
            this.btnGroupMenu = itemView.findViewById(R.id.btnGroupMenu);
            this.llMemberAvatars = itemView.findViewById(R.id.llMemberAvatars);
            this.tvGroupCurrent = itemView.findViewById(R.id.tvGroupCurrent);
            this.tvGroupStatus = itemView.findViewById(R.id.tvGroupStatus);
            this.tvGroupBalance = itemView.findViewById(R.id.tvGroupBalance);
            this.tvGroupTotal = itemView.findViewById(R.id.tvGroupTotal);
        }

        void bind(final GroupListItem group) {
            Context context = itemView.getContext();
            Avatar.bind(this.tvGroupAvatar, group.getName());
            this.tvGroupAvatar.setText(Avatar.getInitials(group.getName()).substring(0, 1));
            this.tvGroupName.setText(group.getName());
            this.tvGroupMembers.setText(context.getResources().getQuantityString(
                    R.plurals.tvGroupMembers, group.getMemberCount(), group.getMemberCount()));
            this.tvGroupCurrent.setVisibility(group.getGroupId().equals(currentGroupId) ? View.VISIBLE : View.GONE);
            //en el carrusel del inicio no cabe: alli basta con el saldo propio
            this.tvGroupTotal.setVisibility(carousel ? View.GONE : View.VISIBLE);
            this.tvGroupTotal.setText(context.getString(R.string.tvGroupTotalLine, group.getTotal().format()));
            bindMemberAvatars(group);
            bindBalance(group.getBalance());

            itemView.setOnClickListener(view -> listener.onGroupClick(group));
            this.btnGroupMenu.setOnClickListener(view -> listener.onGroupMenu(group, view));
        }

        /** Positivo: le deben a la persona; negativo: ella debe; cero: esta al dia. */
        private void bindBalance(Money balance) {
            int text;
            int color;
            int container;
            if (balance.isPositive()) {
                text = R.string.tvYouAreOwed;
                color = R.color.colorCreditor;
                container = R.color.colorCreditorContainer;
            } else if (balance.isNegative()) {
                text = R.string.tvYouOwe;
                color = R.color.colorDebtor;
                container = R.color.colorDebtorContainer;
            } else {
                text = R.string.tvAllSettledShort;
                color = R.color.colorTextPrimary;
                container = R.color.colorSettledContainer;
            }
            Context context = itemView.getContext();
            this.tvGroupStatus.setText(text);
            this.tvGroupStatus.setTextColor(ContextCompat.getColor(context, balance.isZero() ? R.color.colorCreditor : color));
            this.tvGroupStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                    balance.isZero() ? R.color.colorCreditorContainer : container)));
            this.tvGroupBalance.setText(balance.abs().format());
            this.tvGroupBalance.setTextColor(ContextCompat.getColor(context, color));
        }

        /** Hasta tres circulos con iniciales, montados un poco uno sobre otro, y "+N" con el resto. */
        private void bindMemberAvatars(GroupListItem group) {
            Context context = itemView.getContext();
            this.llMemberAvatars.removeAllViews();
            List<String> names = group.getMemberNames();
            int size = dp(context, MINI_AVATAR_DP);
            for (int i = 0; i < names.size(); i++) {
                TextView avatar = miniAvatar(context, size, i);
                Avatar.bindOutlined(avatar, names.get(i));
                this.llMemberAvatars.addView(avatar);
            }
            int others = group.getMemberCount() - names.size();
            if (others > 0) {
                TextView more = miniAvatar(context, size, names.size());
                more.setText(context.getString(R.string.tvMoreMembers, others));
                more.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                Avatar.outline(more, R.color.colorIconCircle);
                this.llMemberAvatars.addView(more);
            }
        }

        private TextView miniAvatar(Context context, int size, int index) {
            TextView avatar = new TextView(context, null, 0, R.style.Widget_SplitBill_Avatar);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginStart(index == 0 ? 0 : -dp(context, MINI_AVATAR_OVERLAP_DP));
            avatar.setLayoutParams(params);
            avatar.setTextSize(12);
            return avatar;
        }
    }
}
