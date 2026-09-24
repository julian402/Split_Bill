package ue.edu.co.splitbill.ui;

import ue.edu.co.splitbill.session.TokenStore;

/** TokenStore de prueba: guarda el token en memoria en vez de cifrarlo con el Android Keystore. */
public class MemoryTokenStore implements TokenStore {

    private String token;

    @Override
    public synchronized void save(String token) {
        this.token = token;
    }

    @Override
    public synchronized String read() {
        return this.token;
    }

    @Override
    public synchronized void clear() {
        this.token = null;
    }
}
