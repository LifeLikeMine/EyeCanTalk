package com.example.eyecantalk.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.example.eyecantalk.GazeTrackerManager;
import com.example.eyecantalk.R;
import com.example.library.AttentionView;
import com.example.library.CalibrationViewer;
import com.example.library.DrowsinessView;
import com.example.library.EyeBlinkView;
import com.example.library.GazePathView;
import com.example.library.PointView;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.ScreenState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA // 시선 추적 input
    };
    private static final int REQ_PERMISSION = 1000;
    private GazeTrackerManager gazeTrackerManager;
    private ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this);
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());

        initView();
        checkPermission();
        initHandler();
    }

    // 앱이 시작 되면
    @Override
    protected void onStart() {
        super.onStart();
        if (preview.isAvailable()) {
          // When if textureView available
          gazeTrackerManager.setCameraPreview(preview);
        }

        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, calibrationCallback, statusCallback);
        Log.i(TAG, "onStart");
    }

    // 주기적으로 확인
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        // 화면 전환후에도 체크하기 위해
        setOffsetOfView();
        gazeTrackerManager.startGazeTracking();
    }

    // 백그라운드로 진입 시
    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
        Log.i(TAG, "onPause");
    }

    // 메인 액티비티가 사라졌을경우
    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCameraPreview(preview);

        gazeTrackerManager.removeCallbacks(gazeCallback, calibrationCallback, statusCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseHandler();
        viewLayoutChecker.releaseChecker();
    }

    // handler

    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseHandler() {
        backgroundThread.quitSafely();
    }

    // handler end

    // permission
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check permission status
            if (!hasPermissions(PERMISSIONS)) {

                requestPermissions(PERMISSIONS, REQ_PERMISSION);
            } else {
                checkPermission(true);
            }
        }else{
            checkPermission(true);
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private boolean hasPermissions(String[] permissions) {
        int result;
        // Check permission status in string array
        for (String perms : permissions) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                // When if unauthorized permission found
                return false;
            }
        }
        // When if all permission allowed
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (isGranted) {
            permissionGranted();
        } else {
            showToast("not granted permissions", true);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraPermissionAccepted) {
                        checkPermission(true);
                    } else {
                        checkPermission(false);
                    }
                }
                break;
        }
    }

    private void permissionGranted() {
        setViewAtGazeTrackerState();
    }
    // permission end

    // view
    private TextureView preview;
    private View layoutProgress;
    private View viewWarningTracking;
    private ImageView imgLogin, imgSetting;
    private Button btnMain, btnTracking, btnGuiDemo;
    private GazePathView pathView;
    private PointView viewPoint;
    private CalibrationViewer viewCalibration;

    // gaze coord filter
    private boolean isUseGazeFilter = true;
    private boolean isStatusBlink = false;
    private boolean isStatusAttention = false;
    private boolean isStatusDrowsiness = false;
    private int activeStatusCount = 0;

    // calibration type
    private CalibrationModeType calibrationType = CalibrationModeType.DEFAULT;
    private AccuracyCriteria criteria = AccuracyCriteria.DEFAULT;

    private void initView() {
        pathView = findViewById(R.id.gazePathView);

        viewPoint = findViewById(R.id.view_point);
        viewCalibration = findViewById(R.id.view_calibration);

        layoutProgress = findViewById(R.id.layout_progress);
        layoutProgress.setOnClickListener(null);

        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        preview = findViewById(R.id.preview);
        preview.setSurfaceTextureListener(surfaceTextureListener);

        btnMain = findViewById(R.id.btn_main);
        btnMain.setOnClickListener(onClickListener);
        btnTracking = findViewById(R.id.btn_traking);
        btnTracking.setOnClickListener(onClickListener);
        btnGuiDemo = findViewById(R.id.btn_demo);
        btnGuiDemo.setOnClickListener(onClickListener);

        imgLogin = findViewById(R.id.img_login);
        imgLogin.setOnClickListener(onClickListener);
        imgSetting = findViewById(R.id.img_setting);
        imgSetting.setOnClickListener(onClickListener);

        hideProgress();
        setOffsetOfView();
        setViewAtGazeTrackerState();
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // When if textureView available
            gazeTrackerManager.setCameraPreview(preview);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };



    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(viewPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                viewPoint.setOffset(x, y);
                viewCalibration.setOffset(x, y);
            }
        });
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private void showTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.INVISIBLE);
            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnMain) {
                goMain();
            } else if (v == btnTracking) {
                initGaze();

            } else if (v == btnGuiDemo) {
                showGuiDemo();
            } else if (v == imgLogin) {
                goLogin();
            } else if (v == imgSetting) {
                goSetting();
            }
        }
    };

    private void initGaze() {
        showProgress();
        gazeTrackerManager.initGazeTracker(initializationCallback);
    }

    private void releaseGaze() {
        gazeTrackerManager.deinitGazeTracker();
        setViewAtGazeTrackerState();
    }

    private void startTracking() {
        gazeTrackerManager.startGazeTracking();
    }

    private void goMain(){
        Intent intent = new Intent(getApplicationContext(), ActActivity.class);
        startActivity(intent);
    }

    private void goLogin(){
        Intent intent = new Intent(getApplicationContext(), DemoActivity.class);
        startActivity(intent);
    }

    private void goSetting(){
        Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
        startActivity(intent);
    }

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showGazePoint(final float x, final float y, final ScreenState type) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPoint.setType(type == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
                viewPoint.setPosition(x, y);
            }
        });
    }

    private void setCalibrationPoint(final float x, final float y) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.VISIBLE);
                viewCalibration.changeDraw(true, null);
                viewCalibration.setPointPosition(x, y);
                viewCalibration.setPointAnimationPower(0);
            }
        });
    }

    private void setCalibrationProgress(final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setPointAnimationPower(progress);
            }
        });
    }

    private void hideCalibrationView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void setViewAtGazeTrackerState() {
        Log.i(TAG, "gaze : " + isTrackerValid() + ", tracking " + isTracking());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                btnTracking.setEnabled(!isTrackerValid());
                if (!isTracking()) {
                    hideCalibrationView();
                }
            }
        });
    }

    // view end

    // gazeTracker
    private boolean isTrackerValid() {
      return gazeTrackerManager.hasGazeTracker();
    }

    private boolean isTracking() {
      return gazeTrackerManager.isTracking();
    }

    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                initSuccess(gazeTracker);
                startTracking();
            } else {
                initFail(error);
            }
        }
    };

    private void initSuccess(GazeTracker gazeTracker) {
        setViewAtGazeTrackerState();
        hideProgress();
    }

    private void initFail(InitializationErrorType error) {
        hideProgress();
    }

    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);
    private final GazeCallback gazeCallback = new GazeCallback() {
      @Override
      public void onGaze(GazeInfo gazeInfo) {
        processOnGaze(gazeInfo);
        Log.i(TAG, "check eyeMovement " + gazeInfo.eyeMovementState);
      }
    };

    private void processOnGaze(GazeInfo gazeInfo) {
      if (gazeInfo.trackingState == TrackingState.SUCCESS) {
        hideTrackingWarning();
        if (!gazeTrackerManager.isCalibrating()) {
          float[] filtered_gaze = filterGaze(gazeInfo);
          showGazePoint(filtered_gaze[0], filtered_gaze[1], gazeInfo.screenState);
        }
      } else {
        showTrackingWarning();
      }
    }

    private float[] filterGaze(GazeInfo gazeInfo) {
      if (isUseGazeFilter) {
        if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
          return oneEuroFilterManager.getFilteredValues();
        }
      }
      return new float[]{gazeInfo.x, gazeInfo.y};
    }

    private CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            setCalibrationProgress(progress);
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            setCalibrationPoint(x, y);
            // Give time to eyes find calibration coordinates, then collect data samples
            backgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCollectSamples();
                }
            }, 1000);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            // When calibration is finished, calibration data is stored to SharedPreference

            hideCalibrationView();
            showToast("calibrationFinished", true);
        }
    };

    private StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            // isTracking true
            // When if camera stream starting
            setViewAtGazeTrackerState();
        }

        @Override
        public void onStopped(StatusErrorType error) {
            // isTracking false
            // When if camera stream stopping
            setViewAtGazeTrackerState();

            if (error != StatusErrorType.ERROR_NONE) {
                switch (error) {
                    case ERROR_CAMERA_START:
                        // When if camera stream can't start
                        showToast("ERROR_CAMERA_START ", false);
                        break;
                    case ERROR_CAMERA_INTERRUPT:
                        // When if camera stream interrupted
                        showToast("ERROR_CAMERA_INTERRUPT ", false);
                        break;
                }
            }
        }
    };


    private boolean startCalibration() {
      boolean isSuccess = gazeTrackerManager.startCalibration(calibrationType, criteria);
      if (!isSuccess) {
        showToast("calibration start fail", false);
      }
      setViewAtGazeTrackerState();
      return isSuccess;
    }

    // Collect the data samples used for calibration
    private boolean startCollectSamples() {
      boolean isSuccess = gazeTrackerManager.startCollectingCalibrationSamples();
      setViewAtGazeTrackerState();
      return isSuccess;
    }

    private void stopCalibration() {
      gazeTrackerManager.stopCalibration();
      hideCalibrationView();
      setViewAtGazeTrackerState();
    }

    private void setCalibration() {
      GazeTrackerManager.LoadCalibrationResult result = gazeTrackerManager.loadCalibrationData();
      switch (result) {
        case SUCCESS:
          showToast("setCalibrationData success", false);
          break;
        case FAIL_DOING_CALIBRATION:
          showToast("calibrating", false);
          break;
        case FAIL_NO_CALIBRATION_DATA:
          showToast("Calibration data is null", true);
          break;
        case FAIL_HAS_NO_TRACKER:
          showToast("No tracker has initialized", true);
          break;
      }
      setViewAtGazeTrackerState();
    }

    private void showGuiDemo() {
      Intent intent = new Intent(getApplicationContext(), DemoActivity.class);
      startActivity(intent);
    }
}
