package ue.edu.co.splitbill.ui.scan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.ReceiptScan;
import ue.edu.co.splitbill.model.ReceiptScanner;
import ue.edu.co.splitbill.permission.PermissionManager;
import ue.edu.co.splitbill.ui.BaseActivity;

/**
 * Escanear una factura para no digitar el total.
 *
 * Paso 1: la camara (CameraX) o una foto de la galeria. Paso 2: ML Kit lee el texto, ReceiptParser
 * propone el total y el usuario lo confirma o escoge otro de los valores encontrados.
 *
 * La pantalla solo devuelve el valor (EXTRA_AMOUNT_CENTS) a quien la abrio, con setResult: no sabe
 * nada de gastos ni de divisiones. Asi la usan igual la cuenta rapida y el formulario de gastos.
 */
public class ScanReceiptActivity extends BaseActivity {

    /** Resultado: el total escogido, en centavos. */
    public static final String EXTRA_AMOUNT_CENTS = "extraScannedAmountCents";
    /** Resultado: el nombre del comercio, si se reconocio (puede no venir). */
    public static final String EXTRA_MERCHANT = "extraScannedMerchant";

    private static final String TAG = "ScanReceiptActivity";

    private View layoutCamera;
    private View layoutNoCamera;
    private View layoutResult;
    private PreviewView pvCamera;
    private Button btnAllowCamera;
    private Button btnPickImage;
    private Button btnCapture;
    private TextView tvScanMerchant;
    private TextView tvScanTotal;
    private TextView tvOtherAmounts;
    private ChipGroup cgAmounts;
    private Button btnUseAmount;
    private Button btnRescan;

    private PermissionManager permissionManager;
    private ReceiptScanner receiptScanner;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private OnBackPressedCallback backToCamera;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    private ReceiptScan scan;
    private Money selectedAmount;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_scan_receipt;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        requestCamera(null);
    }

    @Override
    protected void initListeners() {
        this.btnAllowCamera.setOnClickListener(this::requestCamera);
        this.btnPickImage.setOnClickListener(this::pickImage);
        this.btnCapture.setOnClickListener(this::takePicture);
        this.btnUseAmount.setOnClickListener(this::returnAmount);
        this.btnRescan.setOnClickListener(this::showCameraStep);
        this.cgAmounts.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                selectAmount((Money) chip.getTag());
            }
        });

        //en el resultado, "atras" vuelve a la camara en vez de cerrar
        this.backToCamera = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                showCameraStep(null);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, this.backToCamera);
    }

    // ------------------------------------------------------------------ paso 1: camara o galeria

    /** La camara se pide al entrar; si la niegan, la galeria sigue funcionando sin permiso. */
    private void requestCamera(View view) {
        this.layoutNoCamera.setVisibility(View.VISIBLE);
        this.btnCapture.setEnabled(false);
        this.permissionManager.request(PermissionManager.CAMERA, R.string.msgCameraRationale, this::startCamera);
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                this.cameraProvider = future.get();
                bindCamera();
            } catch (Exception e) {
                Log.e(TAG, "ERROR AL ABRIR LA CAMARA", e);
                showToast(R.string.msgCameraUnavailable);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Conecta la vista previa y la captura a la camara trasera, atadas al ciclo de vida de la pantalla. */
    private void bindCamera() {
        if (this.cameraProvider == null || !isAlive()) {
            return;
        }
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(this.pvCamera.getSurfaceProvider());
        this.imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();
        try {
            this.cameraProvider.unbindAll();
            this.cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, this.imageCapture);
            this.layoutNoCamera.setVisibility(View.GONE);
            this.btnCapture.setEnabled(true);
        } catch (Exception e) {
            //el celular no tiene camara trasera: queda la galeria
            Log.e(TAG, "ERROR AL CONECTAR LA CAMARA", e);
            showToast(R.string.msgCameraUnavailable);
        }
    }

    private void takePicture(View view) {
        if (this.imageCapture == null) {
            return;
        }
        showLoading();
        this.btnCapture.setEnabled(false);
        this.imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                //la foto se lee en memoria: no se guarda en la galeria
                Bitmap bitmap = image.toBitmap();
                int rotation = image.getImageInfo().getRotationDegrees();
                image.close();
                recognize(InputImage.fromBitmap(bitmap, rotation));
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Log.e(TAG, "ERROR AL TOMAR LA FOTO", e);
                hideLoading();
                btnCapture.setEnabled(true);
                showToast(R.string.msgCameraUnavailable);
            }
        });
    }

    /** El selector de fotos del sistema no necesita permiso: la app solo recibe la foto elegida. */
    private void pickImage(View view) {
        this.pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            showLoading();
            recognize(InputImage.fromFilePath(this, uri));
        } catch (IOException e) {
            Log.e(TAG, "ERROR AL ABRIR LA FOTO", e);
            hideLoading();
            showToast(R.string.msgImageUnreadable);
        }
    }

    private void recognize(InputImage image) {
        this.receiptScanner.scan(image, new UiCallback<ReceiptScan>() {
            @Override
            protected void onData(ReceiptScan data) {
                btnCapture.setEnabled(imageCapture != null);
                if (data.isEmpty()) {
                    showToast(R.string.msgNoAmountFound);
                    return;
                }
                showResultStep(data);
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                btnCapture.setEnabled(imageCapture != null);
            }
        });
    }

    // ------------------------------------------------------------------ paso 2: confirmar el total

    private void showResultStep(ReceiptScan data) {
        this.scan = data;
        this.layoutCamera.setVisibility(View.GONE);
        this.layoutResult.setVisibility(View.VISIBLE);
        this.backToCamera.setEnabled(true);
        //mientras se revisa el resultado la camara no tiene por que seguir encendida
        if (this.cameraProvider != null) {
            this.cameraProvider.unbindAll();
        }

        boolean hasMerchant = data.getMerchant() != null;
        this.tvScanMerchant.setText(hasMerchant ? data.getMerchant() : getString(R.string.tvScanTotalLabel));

        this.cgAmounts.removeAllViews();
        for (Money amount : data.getAmounts()) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(amount.format());
            chip.setTag(amount);
            chip.setCheckable(true);
            this.cgAmounts.addView(chip);
            if (amount.equals(data.getTotal())) {
                chip.setChecked(true);
            }
        }
        boolean hasOthers = data.getAmounts().size() > 1;
        this.tvOtherAmounts.setVisibility(hasOthers ? View.VISIBLE : View.GONE);
        this.cgAmounts.setVisibility(hasOthers ? View.VISIBLE : View.GONE);
        selectAmount(data.getTotal());
    }

    private void selectAmount(Money amount) {
        this.selectedAmount = amount;
        this.tvScanTotal.setText(amount.format());
    }

    private void showCameraStep(View view) {
        this.layoutResult.setVisibility(View.GONE);
        this.layoutCamera.setVisibility(View.VISIBLE);
        this.backToCamera.setEnabled(false);
        bindCamera();
    }

    private void returnAmount(View view) {
        Intent result = new Intent();
        result.putExtra(EXTRA_AMOUNT_CENTS, this.selectedAmount.getCents());
        if (this.scan != null && this.scan.getMerchant() != null) {
            result.putExtra(EXTRA_MERCHANT, this.scan.getMerchant());
        }
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.receiptScanner != null) {
            this.receiptScanner.close();
        }
    }

    @Override
    protected void initObjects() {
        this.layoutCamera = findViewById(R.id.layoutCamera);
        this.layoutNoCamera = findViewById(R.id.layoutNoCamera);
        this.layoutResult = findViewById(R.id.layoutResult);
        this.pvCamera = findViewById(R.id.pvCamera);
        this.btnAllowCamera = findViewById(R.id.btnAllowCamera);
        this.btnPickImage = findViewById(R.id.btnPickImage);
        this.btnCapture = findViewById(R.id.btnCapture);
        this.tvScanMerchant = findViewById(R.id.tvScanMerchant);
        this.tvScanTotal = findViewById(R.id.tvScanTotal);
        this.tvOtherAmounts = findViewById(R.id.tvOtherAmounts);
        this.cgAmounts = findViewById(R.id.cgAmounts);
        this.btnUseAmount = findViewById(R.id.btnUseAmount);
        this.btnRescan = findViewById(R.id.btnRescan);

        this.permissionManager = new PermissionManager(this);
        this.receiptScanner = new ReceiptScanner();
        this.pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                this::onImagePicked);
    }
}
