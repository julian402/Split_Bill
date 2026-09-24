package ue.edu.co.splitbill.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Contacto de la agenda del celular que se puede agregar como integrante.
 *
 * No es una tabla: se lee de la agenda cada vez que se abre el selector y solo se guarda en la base
 * de datos cuando el usuario lo elige, convertido en User.
 */
public class Contact {

    private String names;
    private String phone;
    /** Ya esta en el grupo (mismo telefono o mismo nombre): se muestra marcado y no se puede volver a agregar. */
    private boolean member;

    public Contact() {
    }

    public Contact(String names, String phone) {
        this.names = names;
        this.phone = phone;
    }

    /**
     * Deja solo los ultimos 10 digitos del telefono, para que "+57 300 123 4567" y "3001234567" se
     * reconozcan como el mismo numero.
     */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    /** "Sofía  Reyes" -> "sofia reyes", para reconocer el mismo nombre escrito distinto. */
    public static String normalizeName(String names) {
        if (names == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(names, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public String getNames() {
        return this.names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isMember() {
        return this.member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Contact{");
        sb.append("names=").append(names);
        sb.append(", phone=").append(phone);
        sb.append(", member=").append(member);
        sb.append('}');
        return sb.toString();
    }
}
