package ue.edu.co.splitbill.ui;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ue.edu.co.splitbill.R;

/**
 * Fechas como las dice una persona: "Hoy, 14:30", "Ayer, 21:15" o "12 de mar., 10:20".
 */
public final class DateText {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");

    private DateText() {
        //Clase de utilidades: no se instancia
    }

    /** Fecha y hora, para las filas de gastos. */
    public static String format(Context context, Date date) {
        if (date == null) {
            return "";
        }
        String time = new SimpleDateFormat("HH:mm", SPANISH).format(date);
        return context.getString(R.string.tvDateTime, day(context, date), time);
    }

    /** Solo el dia, para el campo Fecha del formulario y los encabezados de la actividad. */
    public static String day(Context context, Date date) {
        if (date == null) {
            return "";
        }
        int daysAgo = daysBetween(date, new Date());
        if (daysAgo == 0) {
            return context.getString(R.string.tvToday);
        }
        if (daysAgo == 1) {
            return context.getString(R.string.tvYesterday);
        }
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.setTime(date);
        String pattern = calendar.get(Calendar.YEAR) == currentYear ? "d 'de' MMM" : "d 'de' MMM 'de' yyyy";
        return new SimpleDateFormat(pattern, SPANISH).format(date);
    }

    /** "Hoy, 24 de sep. de 2026": el campo Fecha del formulario. */
    public static String longDay(Context context, Date date) {
        String full = new SimpleDateFormat("d 'de' MMM 'de' yyyy", SPANISH).format(date);
        int daysAgo = daysBetween(date, new Date());
        if (daysAgo == 0) {
            return context.getString(R.string.tvDateTime, context.getString(R.string.tvToday), full);
        }
        if (daysAgo == 1) {
            return context.getString(R.string.tvDateTime, context.getString(R.string.tvYesterday), full);
        }
        return full;
    }

    /** Dias calendario entre dos fechas, en la hora del celular (no bloques de 24 horas). */
    public static int daysBetween(Date from, Date to) {
        return (int) ((startOfDay(to) - startOfDay(from)) / (24L * 60 * 60 * 1000));
    }

    private static long startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
