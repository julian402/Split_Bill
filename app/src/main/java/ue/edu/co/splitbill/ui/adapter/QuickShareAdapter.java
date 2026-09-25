package ue.edu.co.splitbill.ui.adapter;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;

/**
 * Filas de la cuenta rapida: una por persona, con su nombre, su valor digitado y lo que le toca pagar.
 *
 * A diferencia de ParticipantAdapter, aqui las personas no salen de la base de datos: son posiciones
 * de la cuenta. Se les asigna un identificador sintetico (p1, p2, p3...) para poder reutilizar las
 * mismas SplitStrategy del dominio sin cambiarles una sola linea, porque a la estrategia le da igual
 * si el identificador viene de un integrante registrado o de una persona de paso.
 *
 * El nombre es editable: arranca como "Persona 1" y el usuario puede escribir el real si quiere.
 */
public class QuickShareAdapter extends RecyclerView.Adapter<QuickShareAdapter.QuickShareViewHolder> {

    private static final String KEY_NAME_IDS = "quickNameIds";
    private static final String KEY_NAME_TEXTS = "quickNameTexts";
    private static final String KEY_VALUE_IDS = "quickValueIds";
    private static final String KEY_VALUE_TEXTS = "quickValueTexts";
    private static final String KEY_PEOPLE_COUNT = "quickPeopleCount";

    private final Map<String, String> names = new LinkedHashMap<>();
    private final Map<String, String> typedValues = new LinkedHashMap<>();
    private final Map<String, Money> results = new LinkedHashMap<>();

    private int peopleCount;
    private SplitType splitType = SplitType.EQUAL;

    /** Identificador sintetico de la persona que ocupa una posicion de la cuenta. */
    public static String participantId(int position) {
        return "p" + (position + 1);
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
        //Al cambiar el numero de personas el resultado anterior deja de ser valido
        this.results.clear();
        notifyDataSetChanged();
    }

    public int getPeopleCount() {
        return this.peopleCount;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
        this.results.clear();
        notifyDataSetChanged();
    }

    public List<String> getParticipantIds() {
        List<String> ids = new ArrayList<>(this.peopleCount);
        for (int i = 0; i < this.peopleCount; i++) {
            ids.add(participantId(i));
        }
        return ids;
    }

    /**
     * Valores digitados. Vacio cuando la division es en partes iguales.
     *
     * @throws IllegalArgumentException si falta un valor o no es un numero
     */
    public Map<String, BigDecimal> getTypedValues() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (this.splitType == SplitType.EQUAL) {
            return values;
        }
        for (int i = 0; i < this.peopleCount; i++) {
            String id = participantId(i);
            String typed = this.typedValues.get(id);
            if (typed == null || typed.trim().isEmpty()) {
                throw new IllegalArgumentException("Falta el valor de " + nameOrDefault(id, i));
            }
            try {
                values.put(id, new BigDecimal(typed.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "El valor de " + nameOrDefault(id, i) + " no es un número");
            }
        }
        return values;
    }

    /** Guarda el resultado del reparto para mostrarlo en cada fila. */
    public void setResults(List<Share> shares) {
        this.results.clear();
        for (Share share : shares) {
            this.results.put(share.getUserId(), share.getAmount());
        }
        notifyDataSetChanged();
    }

    public boolean hasResults() {
        return !this.results.isEmpty();
    }

    public void clearResults() {
        this.results.clear();
        notifyDataSetChanged();
    }

    /** El nombre de cada persona, en orden; "Persona N" si no se escribio. Es el que se guarda. */
    public List<String> getDisplayNames() {
        List<String> displayNames = new ArrayList<>(this.peopleCount);
        for (int i = 0; i < this.peopleCount; i++) {
            String name = this.names.get(participantId(i));
            displayNames.add(name == null || name.trim().isEmpty() ? "Persona " + (i + 1) : name.trim());
        }
        return displayNames;
    }

    /** Nombre digitado para una persona, o uno generico si el usuario no lo cambio. */
    private String nameOrDefault(String participantId, int position) {
        String name = this.names.get(participantId);
        return name == null || name.trim().isEmpty() ? "la persona " + (position + 1) : name.trim();
    }

    public void saveState(Bundle outState) {
        outState.putInt(KEY_PEOPLE_COUNT, this.peopleCount);
        putMap(outState, this.names, KEY_NAME_IDS, KEY_NAME_TEXTS);
        putMap(outState, this.typedValues, KEY_VALUE_IDS, KEY_VALUE_TEXTS);
    }

    public void restoreState(Bundle state) {
        if (state == null) {
            return;
        }
        this.peopleCount = state.getInt(KEY_PEOPLE_COUNT, this.peopleCount);
        readMap(state, this.names, KEY_NAME_IDS, KEY_NAME_TEXTS);
        readMap(state, this.typedValues, KEY_VALUE_IDS, KEY_VALUE_TEXTS);
        //El resultado no se guarda: se vuelve a calcular con el boton
        this.results.clear();
        notifyDataSetChanged();
    }

    private void putMap(Bundle outState, Map<String, String> map, String keyIds, String keyTexts) {
        ArrayList<String> ids = new ArrayList<>(map.keySet());
        ArrayList<String> texts = new ArrayList<>(ids.size());
        for (String id : ids) {
            texts.add(map.get(id));
        }
        outState.putStringArrayList(keyIds, ids);
        outState.putStringArrayList(keyTexts, texts);
    }

    private void readMap(Bundle state, Map<String, String> map, String keyIds, String keyTexts) {
        ArrayList<String> ids = state.getStringArrayList(keyIds);
        ArrayList<String> texts = state.getStringArrayList(keyTexts);
        if (ids == null || texts == null || ids.size() != texts.size()) {
            return;
        }
        map.clear();
        for (int i = 0; i < ids.size(); i++) {
            map.put(ids.get(i), texts.get(i));
        }
    }

    @NonNull
    @Override
    public QuickShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_share, parent, false);
        return new QuickShareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuickShareViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return this.peopleCount;
    }

    class QuickShareViewHolder extends RecyclerView.ViewHolder {

        private final EditText etPersonName;
        private final EditText etPersonValue;
        private final TextView tvPersonAmount;
        private SimpleTextWatcher nameWatcher;
        private SimpleTextWatcher valueWatcher;

        QuickShareViewHolder(View itemView) {
            super(itemView);
            this.etPersonName = itemView.findViewById(R.id.etPersonName);
            this.etPersonValue = itemView.findViewById(R.id.etPersonValue);
            this.tvPersonAmount = itemView.findViewById(R.id.tvPersonAmount);
        }

        void bind(int position) {
            final String participantId = participantId(position);

            //Se sueltan los observadores antes de cambiar el texto para que no se disparen solos
            if (this.nameWatcher != null) {
                this.etPersonName.removeTextChangedListener(this.nameWatcher);
            }
            if (this.valueWatcher != null) {
                this.etPersonValue.removeTextChangedListener(this.valueWatcher);
            }

            String defaultName = itemView.getContext().getString(R.string.hintPersonName, position + 1);
            String typedName = names.get(participantId);
            this.etPersonName.setText(typedName == null ? defaultName : typedName);
            this.etPersonName.setHint(defaultName);

            boolean needsValue = splitType != SplitType.EQUAL;
            this.etPersonValue.setVisibility(needsValue ? View.VISIBLE : View.GONE);
            this.etPersonValue.setHint(splitType == SplitType.PERCENTAGE
                    ? R.string.hintValuePercentage
                    : R.string.hintValueExact);
            String typedValue = typedValues.get(participantId);
            this.etPersonValue.setText(typedValue == null ? "" : typedValue);

            Money amount = results.get(participantId);
            this.tvPersonAmount.setText(amount == null ? "" : amount.format());

            this.nameWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    names.put(participantId, s.toString());
                }
            };
            this.etPersonName.addTextChangedListener(this.nameWatcher);

            this.valueWatcher = new SimpleTextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    typedValues.put(participantId, s.toString());
                }
            };
            this.etPersonValue.addTextChangedListener(this.valueWatcher);
        }
    }
}
