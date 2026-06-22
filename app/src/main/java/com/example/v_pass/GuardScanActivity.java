package com.example.v_pass;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.HashMap;

// Retrofit imports for our new connection
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuardScanActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private DatabaseReference visitorRef, logRef;
    private MaterialButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard_scan);

        barcodeView = findViewById(R.id.barcodeScanner);
        btnBack = findViewById(R.id.btnBack);

        CameraSettings settings = new CameraSettings();
        settings.setRequestedCameraId(0);
        settings.setAutoFocusEnabled(true);
        barcodeView.setCameraSettings(settings);

        // Keep direct Firebase references ONLY for checking-in/writing logs for now
        String dbUrl = "https://v-pass-d85c7-default-rtdb.firebaseio.com/";
        visitorRef = FirebaseDatabase.getInstance(dbUrl).getReference("visitors");
        logRef = FirebaseDatabase.getInstance(dbUrl).getReference("entry_logs");

        btnBack.setOnClickListener(v -> {
            barcodeView.pause();
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                barcodeView.pause();
                finish();
            }
        });

        barcodeView.decodeContinuous(callback);
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            barcodeView.pause(); // Pause scanner during processing
            verifyQR(result.getText());
        }
    };

    // =========================================================================
    // UPDATED: Now verifies via your custom Node.js Backend using Retrofit!
    // =========================================================================
    private void verifyQR(String qrData) {
        // CHANGE THIS TO:
        // true  -> if testing via the Android Studio Emulator
        // false -> if testing on a physical Android phone plugged into USB
        boolean useEmulator = true;

        RetrofitClient.getApiService(useEmulator).verifyVisitor(qrData).enqueue(new Callback<VisitorResponse>() {
            @Override
            public void onResponse(Call<VisitorResponse> call, Response<VisitorResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Node.js approved access!
                    VisitorResponse visitor = response.body();
                    String name = visitor.getName();
                    String plateNumber = visitor.getPlateNumber();

                    showSuccessDialog(qrData, name, plateNumber);
                } else {
                    // Node.js denied access! Handle status codes set up in index.js
                    if (response.code() == 404) {
                        showFailDialog("Invalid QR: Record not found.");
                    } else if (response.code() == 403) {
                        showFailDialog("Access Denied: Banned by management.");
                    } else {
                        showFailDialog("Access Denied: Server error (" + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(Call<VisitorResponse> call, Throwable t) {
                // If the server is offline or connection fails
                Toast.makeText(GuardScanActivity.this, "Backend Connection Failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                barcodeView.resume(); // Restart scanner so guard can try again
            }
        });
    }

    private void showSuccessDialog(String qrId, String name, String plateNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ACCESS GRANTED");
        builder.setMessage(
                "Visitor: " + name +
                        "\nVehicle Plate: " + (plateNumber != null ? plateNumber : "No Registered Plate") +
                        "\n\nProceed with Check-In?"
        );

        builder.setPositiveButton("CHECK-IN", (dialog, which) -> {
            checkInVisitor(qrId, name, plateNumber);
        });

        builder.setNegativeButton("CANCEL", (dialog, which) -> {
            dialog.dismiss();
            barcodeView.resume();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void showFailDialog(String reason) {
        NotificationHelper.showNotification(this, "⚠️ SECURITY ALERT", reason);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ACCESS DENIED");
        builder.setMessage(reason);
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            barcodeView.resume();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void checkInVisitor(String qrId, String name, String plateNumber) {
        visitorRef.child(qrId).child("status").setValue("CHECKED_IN");

        HashMap<String, Object> log = new HashMap<>();
        log.put("name", name);
        log.put("vehicle", plateNumber);
        log.put("timestamp", System.currentTimeMillis());

        logRef.push().setValue(log)
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.showNotification(this, "✅ CHECK-IN SUCCESS", "Visitor: " + name + " has entered.");
                    Toast.makeText(this, "Check-in Successful", Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Log Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}