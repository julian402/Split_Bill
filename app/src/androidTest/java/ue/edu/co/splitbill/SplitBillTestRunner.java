package ue.edu.co.splitbill;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.test.runner.AndroidJUnitRunner;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

/**
 * Arranca las pruebas instrumentadas con TestSplitBillApplication en vez de la aplicacion normal.
 *
 * Esta registrado en app/build.gradle.kts (testInstrumentationRunner). Las pruebas que arman su
 * propia base de datos (SyncManagerTest, MigrationTest...) no se ven afectadas; las de interfaz
 * (paquete ui) usan la base en memoria y el servidor falso de TestSplitBillApplication.
 *
 * Ademas enciende la pantalla para cada pantalla de la app que abre una prueba: si el celular se
 * apago mientras corrian las pruebas sin interfaz, la ventana no recibe el foco y Espresso no
 * puede tocar nada.
 */
public class SplitBillTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader classLoader, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(classLoader, TestSplitBillApplication.class.getName(), context);
    }

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback((Activity activity, Stage stage) -> {
            if (stage == Stage.PRE_ON_CREATE) {
                activity.setShowWhenLocked(true);
                activity.setTurnScreenOn(true);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }
}
