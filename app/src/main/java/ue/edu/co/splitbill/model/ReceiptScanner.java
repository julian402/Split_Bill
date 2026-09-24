package ue.edu.co.splitbill.model;

import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.domain.ReceiptLine;
import ue.edu.co.splitbill.domain.ReceiptParser;
import ue.edu.co.splitbill.domain.ReceiptScan;

/**
 * Lee el texto de la foto de una factura con ML Kit y le pide a ReceiptParser que encuentre el total.
 *
 * El reconocimiento se hace en el celular con el modelo que viene dentro del APK: funciona sin
 * internet y la foto no sale del telefono.
 *
 * ML Kit ya trabaja en su propio hilo y responde en el hilo principal, por eso esta clase no usa
 * AppExecutors como los repositorios.
 */
public class ReceiptScanner {

    private static final String TAG = "ReceiptScanner";

    private final TextRecognizer recognizer;

    public ReceiptScanner() {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void scan(InputImage image, final DataCallback<ReceiptScan> callback) {
        this.recognizer.process(image)
                .addOnSuccessListener(text -> callback.onSuccess(ReceiptParser.parse(toLines(text))))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "ERROR AL RECONOCER EL TEXTO DE LA FACTURA", e);
                    callback.onError("No se pudo leer la foto. Intenta de nuevo con más luz");
                });
    }

    /** Libera el modelo cuando la pantalla se cierra. */
    public void close() {
        this.recognizer.close();
    }

    /** Pasa el resultado de ML Kit (bloques > lineas) a lineas con posicion que entiende el dominio. */
    private static List<ReceiptLine> toLines(Text text) {
        List<ReceiptLine> lines = new ArrayList<>();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect box = line.getBoundingBox();
                if (box == null) {
                    lines.add(new ReceiptLine(line.getText(), lines.size()));
                } else {
                    lines.add(new ReceiptLine(line.getText(), box.top, box.bottom, box.left));
                }
            }
        }
        return lines;
    }
}
