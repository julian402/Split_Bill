package ue.edu.co.splitbill.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.model.Contact;
import ue.edu.co.splitbill.ui.Avatar;

/**
 * Lista de contactos de la agenda con casillas para elegir varios a la vez.
 *
 * La seleccion se guarda aparte de lo que se ve: al filtrar con el buscador los elegidos no se
 * pierden aunque dejen de aparecer.
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    /** Aviso de que cambio la cantidad de elegidos, para actualizar el boton "Agregar (n)". */
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selected);
    }

    private final List<Contact> allContacts = new ArrayList<>();
    private final List<Contact> visibleContacts = new ArrayList<>();
    private final Set<Contact> selected = new LinkedHashSet<>();
    private final OnSelectionChangedListener listener;

    public ContactAdapter(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<Contact> contacts) {
        this.allContacts.clear();
        this.allContacts.addAll(contacts);
        this.selected.clear();
        filter("");
        this.listener.onSelectionChanged(0);
    }

    /** Muestra los contactos cuyo nombre o telefono contiene el texto, sin importar tildes. */
    public void filter(String query) {
        String clean = simplify(query);
        this.visibleContacts.clear();
        for (Contact contact : this.allContacts) {
            if (clean.isEmpty() || simplify(contact.getNames()).contains(clean)
                    || (contact.getPhone() != null && contact.getPhone().contains(clean))) {
                this.visibleContacts.add(contact);
            }
        }
        notifyDataSetChanged();
    }

    public List<Contact> getSelected() {
        return new ArrayList<>(this.selected);
    }

    public int getVisibleCount() {
        return this.visibleContacts.size();
    }

    private static String simplify(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.bind(this.visibleContacts.get(position));
    }

    @Override
    public int getItemCount() {
        return this.visibleContacts.size();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvContactInitials;
        private final TextView tvContactNames;
        private final TextView tvContactPhone;
        private final CheckBox cbContact;

        ContactViewHolder(View itemView) {
            super(itemView);
            this.tvContactInitials = itemView.findViewById(R.id.tvContactInitials);
            this.tvContactNames = itemView.findViewById(R.id.tvContactNames);
            this.tvContactPhone = itemView.findViewById(R.id.tvContactPhone);
            this.cbContact = itemView.findViewById(R.id.cbContact);
        }

        void bind(final Contact contact) {
            Avatar.bind(this.tvContactInitials, contact.getNames());
            this.tvContactNames.setText(contact.getNames());
            //los que ya estan en el grupo se ven marcados y no se pueden tocar
            this.tvContactPhone.setText(contact.isMember()
                    ? itemView.getContext().getString(R.string.tvAlreadyMember, contact.getPhone())
                    : contact.getPhone());
            this.cbContact.setOnCheckedChangeListener(null);
            this.cbContact.setChecked(contact.isMember() || selected.contains(contact));
            this.cbContact.setEnabled(!contact.isMember());
            itemView.setEnabled(!contact.isMember());
            itemView.setAlpha(contact.isMember() ? 0.5f : 1f);
            itemView.setOnClickListener(view -> cbContact.toggle());
            this.cbContact.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    selected.add(contact);
                } else {
                    selected.remove(contact);
                }
                listener.onSelectionChanged(selected.size());
            });
        }
    }
}
