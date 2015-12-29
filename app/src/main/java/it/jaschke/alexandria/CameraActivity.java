package it.jaschke.alexandria;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import it.jaschke.alexandria.CameraPreview.CameraPreview;
import it.jaschke.alexandria.CameraPreview.GraphicOverlay;
import it.jaschke.alexandria.utils.BarcodeGraphic;
import it.jaschke.alexandria.utils.BarcodeTrackerFactory;

public class CameraActivity extends AppCompatActivity {

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    private CameraPreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    private CameraSource mCameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BarcodeGraphic graphic = mGraphicOverlay.getFirstGraphic();
                Barcode barcode = null;
                if (graphic != null) {
                    barcode = graphic.getBarcode();
                    if (barcode != null) {
                        TextView textView = (TextView) findViewById(R.id.bar_code);
                        textView.setText(barcode.rawValue);
                        Intent data = new Intent();
                        data.putExtra("Barcode", barcode.rawValue);
                        setResult(CommonStatusCodes.SUCCESS, data);
                        finish();
                    }
                }

            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPreview = (CameraPreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(R.id.graphicOverlay);
        mGraphicOverlay.setTrigger(fab);

        safeCameraOpen();

    }

    @Override
    public void onResume() {
        super.onResume();
        // Create our Preview view and set it as the content of our activity.
        startCameraSource();
    }


    private void safeCameraOpen() {

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay);
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());


        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder.setAutoFocusEnabled(true);
        }


        mCameraSource = builder.build();
    }

    //starting the preview
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }
}
