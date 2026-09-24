package ue.edu.co.splitbill.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;

/**
 * Lee la agenda del celular para agregar integrantes sin escribir sus datos.
 *
 * La agenda no es una tabla propia: la administra Android y se consulta con un ContentResolver,
 * que funciona como un cursor de SQLite sobre los datos de otra aplicacion. Por eso necesita el
 * permiso READ_CONTACTS, que la pantalla pide con PermissionManager antes de llamar aqui.
 */
public class ContactRepository extends BaseRepository {

    private static final String TAG = "ContactRepository";

    private static final String[] PROJECTION = {
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
    };

    private final ContentResolver contentResolver;

    public ContactRepository(SplitBillDatabase database, AppExecutors executors, ContentResolver contentResolver) {
        super(database, executors);
        this.contentResolver = contentResolver;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /**
     * Contactos con telefono, ordenados por nombre. Si una persona tiene varios numeros se toma el
     * primero. Los que ya son integrantes del grupo (mismo telefono o mismo nombre) llegan marcados,
     * para no agregar a la misma persona dos veces.
     *
     * @param members integrantes actuales del grupo
     */
    public void getContacts(final List<User> members, DataCallback<List<Contact>> callback) {
        runAsync(new Callable<List<Contact>>() {
            @Override
            public List<Contact> call() {
                Set<String> memberPhones = new HashSet<>();
                Set<String> memberNames = new HashSet<>();
                for (User member : members) {
                    memberNames.add(Contact.normalizeName(member.getNames()));
                    String phone = Contact.normalizePhone(member.getPhone());
                    if (!phone.isEmpty()) {
                        memberPhones.add(phone);
                    }
                }

                Map<String, Contact> byContactId = new LinkedHashMap<>();
                try (Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        PROJECTION, null, null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
                    if (cursor == null) {
                        return new ArrayList<>();
                    }
                    while (cursor.moveToNext()) {
                        String contactId = cursor.getString(0);
                        String names = cursor.getString(1);
                        String phone = cursor.getString(2);
                        if (names == null || names.trim().isEmpty() || byContactId.containsKey(contactId)) {
                            continue;
                        }
                        Contact contact = new Contact(names.trim(), phone == null ? null : phone.trim());
                        contact.setMember(memberPhones.contains(Contact.normalizePhone(phone))
                                || memberNames.contains(Contact.normalizeName(names)));
                        byContactId.put(contactId, contact);
                    }
                }
                return new ArrayList<>(byContactId.values());
            }
        }, callback);
    }
}
