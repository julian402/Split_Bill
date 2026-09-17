package ue.edu.co.splitbill;

import android.app.Application;

import ue.edu.co.splitbill.di.ServiceLocator;

/**
 * Punto de arranque de la aplicacion.
 *
 * Se crea una sola vez, antes que cualquier Activity, y vive mientras viva el proceso. Por eso es el
 * lugar natural para guardar el ServiceLocator: cualquier pantalla puede pedirle sus dependencias
 * sabiendo que siempre son las mismas instancias.
 *
 * Queda registrada en el AndroidManifest con android:name=".SplitBillApplication".
 */
public class SplitBillApplication extends Application {

    private ServiceLocator serviceLocator;

    @Override
    public void onCreate() {
        super.onCreate();
        this.serviceLocator = new ServiceLocator(this);
    }

    public ServiceLocator getServiceLocator() {
        return this.serviceLocator;
    }
}
