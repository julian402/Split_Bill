package ue.edu.co.splitbill.session;

/**
 * Donde se guarda el token de acceso entre una apertura de la app y la siguiente.
 *
 * Es una interfaz para que SessionManager no dependa de como se guarda: la implementacion real
 * (KeystoreTokenStore) lo cifra con el Android Keystore, y en una prueba se puede usar una que lo
 * guarde en memoria.
 */
public interface TokenStore {

    void save(String token);

    /** El token guardado, o null si no hay ninguno o no se pudo leer. */
    String read();

    void clear();
}
