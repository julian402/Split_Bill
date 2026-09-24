package ue.edu.co.splitbill.ui.adapter;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.ui.Avatar;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;

/**
 * Lista de participantes de un gasto: una casilla por integrante y, cuando la division lo requiere,
 * un campo para digitar su monto o su porcentaje.
 *
 * El adaptador guarda que esta marcado y que se digito en sus propias estructuras, indexadas por el
 * identificador del integrante y no por la posicion en pantalla. Asi el estado sobrevive al reciclaje
 * de las filas, que es el error tipico cuando se pone un EditText dentro de un RecyclerView.
 *
 * El campo de valor aparece o desaparece segun el tipo de division: el adaptador no decide nada, solo
 * refleja el SplitType que le pasa la pantalla.
 */
public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    /** Aviso de que cambio lo marcado o lo digitado: la pantalla recalcula la division estimada. */
    public interface OnParticipantsChangedListener {
        void onParticipantsChanged();
    }

    private static final String KEY_SELECTED = "participantSelected";
    private static final String KEY_VALUE_IDS = "participantValueIds";
    private static final String KEY_VALUE_TEXTS = "participantValueTexts";

    private final List<User> participants = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final Map<String, String> typedValues = new LinkedHashMap<>();

    private SplitType splitType = SplitType.EQUAL;
    private boolean loaded;
    private OnParticipantsChangedListener changedListener;

    public void setOnParticipantsChangedListener(OnParticipantsChangedListener changedListener) {
        this.changedListener = changedListener;
    }

    private void notifyChanged() {
        if (this.changedListener != null) {
            this.changedListener.onParticipantsChanged();
        }
    }

    /** "Seleccionar todos": marca a todos; si ya estaban todos, los desmarca. */
    public void toggleAll() {
        if (this.selectedIds.size() == this.participants.size()) {
            this.selectedIds.clear();
            this.typedValues.clear();
        } else {
            for (User user : this.participants) {
                this.selectedIds.add(user.getId());
            }
        }
        notifyDataSetChanged();
        notifyChanged();
    }

    public SplitType getSplitType() {
        return this.splitType;
    }

    /** Suma de lo digitado por los marcados (montos o porcentajes); lo que no es numero cuenta cero. */
    public BigDecimal sumTypedValues() {
        BigDecimal sum = BigDecimal.ZERO;
        for (String userId : getSelectedUserIds()) {
            String typed = this.typedValues.get(userId);
            if (typed == null || typed.trim().isEmpty()) {
                continue;
            }
            try {
                sum = sum.add(new BigDecimal(typed.trim()));
            } catch (NumberFormatException e) {
                //todavia lo esta escribiendo: no suma
            }
        }
        return sum;
    }

    /**
     * Carga o recarga la lista de integrantes.
     *
     * La pantalla vuelve a llamar a este metodo cada vez que se reanuda, por ejemplo despues de
     * girar el celular. Por eso lo marcado y lo digitado no se borra: solo se descarta lo que
     * corresponda a integrantes que ya no estan, y los integrantes nuevos entran marcados.
     */
    public void setParticipants(List<User> participants) {
        List<String> incomingIds = new ArrayList<>();
        if (participants != null) {
            for (User user : participants) {
                incomingIds.add(user.getId());
            }
        }

        if (!this.loaded) {
            //Primera carga: por defecto participan todos, que es lo mas comun al registrar un gasto
            this.selectedIds.addAll(incomingIds);
            this.loaded = true;
        } else {
            Set<String> previousIds = new LinkedHashSet<>();
            for (User user : this.participants) {
                previousIds.add(user.getId());
            }
            this.selectedIds.retainAll(incomingIds);
            this.typedValues.keySet().retainAll(incomingIds);
            for (String userId : incomingIds) {
                if (!previousIds.contains(userId)) {
                    this.selectedIds.add(userId);
                }
            }
        }

        this.participants.clear();
        if (participants != null) {
            this.participants.addAll(participants);
        }
        notifyDataSetChanged();
        notifyChanged();
    }

    /**
     * Marca a los participantes y llena sus valores, para editar un gasto que ya existe. Se llama
     * despues de setParticipants, cuando la lista de integrantes ya esta cargada.
     */
    public void preload(Set<String> ids, Map<String, String> values) {
        this.selectedIds.clear();
        this.selectedIds.addAll(ids);
        this.typedValues.clear();
        this.typedValues.putAll(values);
        this.loaded = true;
        notifyDataSetChanged();
        notifyChanged();
    }

    /**
     * Resumen de lo marcado y lo digitado. La pantalla lo compara con el de cuando abrio el formulario
     * para saber si hay cambios sin guardar.
     */
    public String snapshot() {
        return getSelectedUserIds().toString() + this.typedValues.toString();
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
        notifyDataSetChanged();
        notifyChanged();
    }

    /**
     * Guarda en el Bundle lo marcado y lo digitado.
     *
     * Al girar el celular, Android destruye la Activity y la vuelve a crear, con lo que el adaptador
     * tambien nace de nuevo. Sin esto, el usuario perderia los porcentajes que acababa de escribir.
     */
    public void saveState(Bundle outState) {
        outState.putStringArrayList(KEY_SELECTED, new ArrayList<>(this.selectedIds));
        ArrayList<String> valueIds = new ArrayList<>(this.typedValues.keySet());
        ArrayList<String> valueTexts = new ArrayList<>(valueIds.size());
        for (String userId : valueIds) {
            valueTexts.add(this.typedValues.get(userId));
        }
        outState.putStringArrayList(KEY_VALUE_IDS, valueIds);
        outState.putStringArrayList(KEY_VALUE_TEXTS, valueTexts);
    }

    /** Restaura lo marcado y lo digitado. Se llama despues de cargar la lista de integrantes. */
    public void restoreState(Bundle state) {
        if (state == null) {
            return;
        }
        ArrayList<String> selected = state.getStringArrayList(KEY_SELECTED);
        if (selected == null) {
            return;
        }
        this.selectedIds.clear();
        this.selectedIds.addAll(selected);

        this.typedValues.clear();
        ArrayList<String> valueIds = state.getStringArrayList(KEY_VALUE_IDS);
        ArrayList<String> valueTexts = state.getStringArrayList(KEY_VALUE_TEXTS);
        if (valueIds != null && valueTexts != null && valueIds.size() == valueTexts.size()) {
            for (int i = 0; i < valueIds.size(); i++) {
                this.typedValues.put(valueIds.get(i), valueTexts.get(i));
            }
        }
        this.loaded = true;
        notifyDataSetChanged();
        notifyChanged();
    }

    /** Integrantes marcados, en el orden en que aparecen en pantalla. */
    public List<String> getSelectedUserIds() {
        List<String> ids = new ArrayList<>();
        for (User user : this.participants) {
            if (this.selectedIds.contains(user.getId())) {
                ids.add(user.getId());
            }
        }
        return ids;
    }

    /**
     * Valores digitados para los integrantes marcados.
     * Devuelve un mapa vacio cuando la division es en partes iguales, porque alli no se digita nada.
     *
     * @throws IllegalArgumentException si falta un valor o no es un numero
     */
    public Map<String, BigDecimal> getTypedValues() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (this.splitType == SplitType.EQUAL) {
            return values;
        }
        for (User user : this.participants) {
            if (!this.selectedIds.contains(user.getId())) {
                continue;
            }
            String typed = this.typedValues.get(user.getId());
            if (typed == null || typed.trim().isEmpty()) {
                throw new IllegalArgumentException("Falta el valor de " + user.getNames());
            }
            try {
                values.put(user.getId(), new BigDecimal(typed.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El valor de " + user.getNames() + " no es un número");
            }
        }
        return values;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        holder.bind(this.participants.get(position));
    }

    @Override
    public int getItemCount() {
        return this.participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardParticipant;
        private final CheckBox cbParticipant;
        private final TextView tvParticipantAvatar;
        private final TextView tvParticipantName;
        private final EditText etParticipantValue;
        private SimpleTextWatcher valueWatcher;

        ParticipantViewHolder(View itemView) {
            super(itemView);
            this.cardParticipant = itemView.findViewById(R.id.cardParticipant);
            this.cbParticipant = itemView.findViewById(R.id.cbParticipant);
            this.tvParticipantAvatar = itemView.findViewById(R.id.tvParticipantAvatar);
            this.tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            this.etParticipantValue = itemView.findViewById(R.id.etParticipantValue);
        }

        /** La tarjeta marcada lleva borde lila; la desmarcada, gris. */
        private void showChecked(boolean checked) {
            this.cbParticipant.setChecked(checked);
            this.cardParticipant.setStrokeColor(ContextCompat.getColor(itemView.getContext(),
                    checked ? R.color.colorHighlightStroke : R.color.colorStroke));
            this.etParticipantValue.setEnabled(checked);
        }

        void bind(final User user) {
            //Se sueltan los listeners antes de cambiar el contenido para que no se disparen solos
            this.cardParticipant.setOnClickListener(null);
            if (this.valueWatcher != null) {
                this.etParticipantValue.removeTextChangedListener(this.valueWatcher);
            }

            Avatar.bind(this.tvParticipantAvatar, user.getNames());
            this.tvParticipantAvatar.setText(Avatar.getInitials(user.getNames()).substring(0, 1));
            this.tvParticipantName.setText(user.getNames());

            boolean needsValue = splitType != SplitType.EQUAL;
            this.etParticipantValue.setVisibility(needsValue ? View.VISIBLE : View.GONE);
            this.etParticipantValue.setHint(splitType == SplitType.PERCENTAGE
                    ? R.string.hintValuePercentage
                    : R.string.hintValueExact);
            String typed = typedValues.get(user.getId());
            this.etParticipantValue.setText(typed == null ? "" : typed);
            showChecked(selectedIds.contains(user.getId()));

            //toda la tarjeta marca o desmarca al participante
            this.cardParticipant.setOnClickListener(view -> {
                boolean isChecked = !selectedIds.contains(user.getId());
                if (isChecked) {
                    selectedIds.add(user.getId());
                } else {
                    selectedIds.remove(user.getId());
                    typedValues.remove(user.getId());
                    etParticipantValue.setText("");
                }
                showChecked(isChecked);
                notifyChanged();
            });

            this.valueWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    typedValues.put(user.getId(), s.toString());
                    notifyChanged();
                }
            };
            this.etParticipantValue.addTextChangedListener(this.valueWatcher);
        }
    }
}
