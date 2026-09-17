package ue.edu.co.splitbill.ui;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * TextWatcher con los dos metodos que casi nunca se usan ya implementados.
 *
 * La interfaz TextWatcher obliga a escribir tres metodos aunque solo interese uno. Esta clase
 * abstracta los resuelve una vez para que quien la extienda solo escriba lo que le importa.
 */
public abstract class SimpleTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //no se usa
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //no se usa
    }

    @Override
    public abstract void afterTextChanged(Editable s);
}
