package ue.edu.co.splitbill.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.ui.Avatar;

/**
 * Lista de grupos de la persona. El grupo actual aparece marcado.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    /** Toque (entrar al grupo) y toque largo (renombrarlo). */
    public interface OnGroupListener {
        void onGroupClick(GroupListItem group);

        void onGroupLongClick(GroupListItem group);
    }

    private final List<GroupListItem> groups = new ArrayList<>();
    private final OnGroupListener listener;
    private String currentGroupId;

    public GroupAdapter(OnGroupListener listener) {
        this.listener = listener;
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

    class GroupViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardGroup;
        private final TextView tvGroupInitials;
        private final TextView tvGroupName;
        private final TextView tvGroupMembers;
        private final TextView tvGroupTotal;

        GroupViewHolder(View itemView) {
            super(itemView);
            this.cardGroup = itemView.findViewById(R.id.cardGroup);
            this.tvGroupInitials = itemView.findViewById(R.id.tvGroupInitials);
            this.tvGroupName = itemView.findViewById(R.id.tvGroupName);
            this.tvGroupMembers = itemView.findViewById(R.id.tvGroupMembers);
            this.tvGroupTotal = itemView.findViewById(R.id.tvGroupTotal);
        }

        void bind(final GroupListItem group) {
            boolean isCurrent = group.getGroupId().equals(currentGroupId);
            Avatar.bind(this.tvGroupInitials, group.getName());
            this.tvGroupName.setText(group.getName());
            //los integrantes de un grupo se traen al entrar en el: antes de eso no se sabe cuantos son
            String members = group.getMemberCount() == 0
                    ? itemView.getContext().getString(R.string.tvGroupNotLoaded)
                    : itemView.getResources().getQuantityString(R.plurals.tvGroupMembers,
                    group.getMemberCount(), group.getMemberCount());
            this.tvGroupMembers.setText(isCurrent
                    ? members + " · " + itemView.getContext().getString(R.string.tvCurrentGroup) : members);
            this.tvGroupTotal.setText(group.getTotal().format());
            this.cardGroup.setChecked(isCurrent);
            this.cardGroup.setOnClickListener(view -> listener.onGroupClick(group));
            this.cardGroup.setOnLongClickListener(view -> {
                listener.onGroupLongClick(group);
                return true;
            });
        }
    }
}
