package ue.edu.co.splitbill.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import java.util.Locale;

import ue.edu.co.splitbill.R;

/**
 * Circulo con las iniciales de una persona, como el que muestran las listas de integrantes y saldos.
 *
 * El color no es aleatorio: sale del nombre, asi que la misma persona tiene siempre el mismo color en
 * todas las pantallas y el usuario la reconoce de un vistazo.
 */
public final class Avatar {

    private static final int[] BACKGROUND_COLORS = {
            R.color.colorAvatar1,
            R.color.colorAvatar2,
            R.color.colorAvatar3,
            R.color.colorAvatar4,
            R.color.colorAvatar5
    };

    /** Color de la letra de cada fondo, en el mismo orden: el mismo tono, mas oscuro. */
    private static final int[] TEXT_COLORS = {
            R.color.colorOnAvatar1,
            R.color.colorOnAvatar2,
            R.color.colorOnAvatar3,
            R.color.colorOnAvatar4,
            R.color.colorOnAvatar5
    };

    private Avatar() {
        //Clase de utilidades: no se instancia
    }

    /** Pinta en el TextView las iniciales del nombre sobre su color. */
    public static void bind(TextView tvAvatar, String names) {
        tvAvatar.setText(getInitials(names));
        int index = colorIndex(names);
        int color = ContextCompat.getColor(tvAvatar.getContext(), BACKGROUND_COLORS[index]);
        ViewCompat.setBackgroundTintList(tvAvatar, ColorStateList.valueOf(color));
        tvAvatar.setTextColor(ContextCompat.getColor(tvAvatar.getContext(), TEXT_COLORS[index]));
    }

    /**
     * Avatar pequeno con un aro del color de la tarjeta, para montarlos uno sobre otro. Se usa un
     * GradientDrawable porque un tinte pintaria tambien el aro.
     */
    public static void bindOutlined(TextView tvAvatar, String names) {
        int index = colorIndex(names);
        tvAvatar.setText(getInitials(names).substring(0, 1));
        tvAvatar.setTextColor(ContextCompat.getColor(tvAvatar.getContext(), TEXT_COLORS[index]));
        outline(tvAvatar, BACKGROUND_COLORS[index]);
    }

    /** Circulo del color indicado con aro del color de la tarjeta. */
    public static void outline(TextView tvAvatar, int fillColorResourceId) {
        Context context = tvAvatar.getContext();
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(ContextCompat.getColor(context, fillColorResourceId));
        circle.setStroke(Math.round(2 * context.getResources().getDisplayMetrics().density),
                ContextCompat.getColor(context, R.color.colorCard));
        ViewCompat.setBackgroundTintList(tvAvatar, null);
        tvAvatar.setBackground(circle);
    }

    /** "Julián Corredor" devuelve "JC"; "Sofía" devuelve "S". */
    public static String getInitials(String names) {
        if (names == null || names.trim().isEmpty()) {
            return "?";
        }
        String[] words = names.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        initials.append(words[0].charAt(0));
        if (words.length > 1) {
            initials.append(words[words.length - 1].charAt(0));
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }

    private static int colorIndex(String names) {
        if (names == null) {
            return 0;
        }
        //Math.abs de Integer.MIN_VALUE sigue siendo negativo; floorMod evita ese caso
        return Math.floorMod(names.trim().hashCode(), BACKGROUND_COLORS.length);
    }
}
