package com.example.eyecantalk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eyecantalk.GazeTrackerManager;
import com.example.eyecantalk.R;
import com.example.library.GazePathView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

public class ActActivity extends AppCompatActivity {
    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;
    private RecyclerView selectedImageRecyclerView;
    private SelectedImageAdapter selectedImageAdapter;

    // 아이 트래킹
    private static final String TAG = ActActivity.class.getSimpleName();
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA // 시선 추적 input
    };
    private static final int REQ_PERMISSION = 1000;
    private HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;
    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act);

        checkPermission();
        initHandler();
        initView();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this);
        initGaze();
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, userStatusCallback);
        gazeTrackerManager = GazeTrackerManager.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        gazeTrackerManager.startGazeTracking();
        setOffsetOfView();
        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, userStatusCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        releaseHandler();
        super.onDestroy();
    }

    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);

        List<ImageData> imageDataList = new ArrayList<>();
        imageDataList.add(new ImageData(1, R.drawable.chair, "의자"));
        imageDataList.add(new ImageData(2, R.drawable.like, "좋아요"));
        imageDataList.add(new ImageData(3, R.drawable.hate, "싫어요"));
        imageDataList.add(new ImageData(4, R.drawable.hello, "인사"));

        List<ImageData> selectedImages = new ArrayList<>();

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        selectedImageRecyclerView = findViewById(R.id.selectedImageRecyclerView);

        imageAdapter = new ImageAdapter(imageDataList, new OnImageItemClickListener() {
            @Override
            public void onImageItemClick(ImageData imageData) {
                showSelectedImage(imageData);
            }
        });
        selectedImageAdapter = new SelectedImageAdapter(selectedImages);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        imageRecyclerView.setLayoutManager(gridLayoutManager);
        imageRecyclerView.setAdapter(imageAdapter);

        selectedImageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImageRecyclerView.setAdapter(selectedImageAdapter);
    }

    // 클릭 리스너
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == null) {
                // 넣어야 함
            }
        }
    };
    private void showSelectedImage(ImageData imageData) {
        selectedImageAdapter.addImageData(imageData);
    }

    // 권한
    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    private void releaseHandler() {
        backgroundThread.quitSafely();
    }
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
            Toast.makeText(this,"Not permisson",Toast.LENGTH_SHORT);
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
        return;
    }
    // 권한

    // 아이 트래킹
    float[] filtered = oneEuroFilterManager.getFilteredValues();

    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(gazePathView, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                gazePathView.setOffset(x, y);
            }
        });
    }

    private final GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                filtered = oneEuroFilterManager.getFilteredValues();
                gazePathView.onGaze(filtered[0], filtered[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
            }
        }
    };

    private void initGaze() {
        UserStatusOption userStatusOption = new UserStatusOption();

        userStatusOption.useBlink();

        gazeTrackerManager.initGazeTracker(initializationCallback, userStatusOption);
    }

    private void startTracking() {
        gazeTrackerManager.startGazeTracking();
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
//        Toast.makeText(getApplicationContext(),"initSuccess!",Toast.LENGTH_SHORT).show();
    }

    private void initFail(InitializationErrorType error) {
        Toast.makeText(getApplicationContext(), "initFail...", Toast.LENGTH_SHORT).show();
    }


    private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
        @Override
        public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
        }
        @Override
        @SuppressLint("ClickableViewAccessibility")
        public void onBlink(long timestamp, boolean isBlinkLeft, boolean isBlinkRight, boolean isBlink, float eyeOpenness) {
            if(isBlink){
                if (isViewContains(imageRecyclerView, filtered[0], filtered[1])) {
                    View itemView = findViewAtPosition(imageRecyclerView, filtered[0], filtered[1]);
                    if (itemView != null) {
                        itemView.performClick();
                    }
                }
            }
        }
        @Override
        public void onDrowsiness(long timestamp, boolean isDrowsiness) {
        }
    };

    private boolean isViewContains(View view, float x, float y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();

        return x >= viewX && x <= viewX + viewWidth && y >= viewY && y <= viewY + viewHeight;
    }

    private View findViewAtPosition(RecyclerView recyclerView, float x, float y) {
        View childView = recyclerView.findChildViewUnder(x, y);
        if (childView != null) {
            RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(childView);
            int position = viewHolder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                return childView;
            }
        }
        return null;
    }
}
