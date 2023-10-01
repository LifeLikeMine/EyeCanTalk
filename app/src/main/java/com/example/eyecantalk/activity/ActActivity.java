package com.example.eyecantalk.activity;

import static com.example.eyecantalk.uuid.UUIDManager.getUUID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
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
import com.example.eyecantalk.adapters.CategoryAdapter;
import com.example.eyecantalk.adapters.ImageAdapter;
import com.example.eyecantalk.adapters.OnCategoryItemClickListener;
import com.example.eyecantalk.adapters.OnImageItemClickListener;
import com.example.eyecantalk.adapters.RecommendImageAdapter;
import com.example.eyecantalk.adapters.SelectedImageAdapter;
import com.example.eyecantalk.imageData.AacResponse;
import com.example.eyecantalk.imageData.ImageData;
import com.example.eyecantalk.imageData.ImageDataList;
import com.example.eyecantalk.retrofit.RetrofitClient;
import com.example.library.GazePathView;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.UserStatusCallback;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.UserStatusOption;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActActivity extends AppCompatActivity {
    private RecyclerView categoryRecyclerView;
    private CategoryAdapter categoryAdapter;
    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;
    private RecyclerView selectedImageRecyclerView;
    private SelectedImageAdapter selectedImageAdapter;
    private RecyclerView recommendRecyclerView;
    private RecommendImageAdapter recommendImageAdapter;
    private List<ImageData> nullList = new ArrayList<>();
    private List<String> nullCList = new ArrayList<>();
    private UUID uuid;

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
    private SharedPreferences sharedPreferences;

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

        sharedPreferences = getSharedPreferences("eyeTracking", Context.MODE_PRIVATE);

        String eyeTrackingUse = sharedPreferences.getString("eyeTrackingUse", "true");
        if(eyeTrackingUse.equals("true")){
            iSwitch.setChecked(true);
        } else {
            iSwitch.setChecked(false);
            if(gazeTrackerManager.isTracking()){
                gazeTrackerManager.stopGazeTracking();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!iSwitch.isChecked()){
            gazeTrackerManager.stopGazeTracking();
        } else {
            gazeTrackerManager.startGazeTracking();
            setOffsetOfView();
        }
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
        gazeTrackerManager.removeCallbacks(gazeCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        releaseHandler();
        super.onDestroy();
    }

    private String suuid;
    private List<String[]> csvAllContent;
    private ArrayList<ImageData> selectedImages  = new ArrayList<>();
    private ArrayList<ImageData> recommendImages = new ArrayList<>();
    private ArrayList<ImageData> imageDataList = new ArrayList<>();
    private ArrayList<String> categoryList;
    private Map<String, List<ImageData>> categoryMap;
    private Button btnClear, btnBack, btnPre, btnNext, btnDelete;
    private Switch iSwitch;
    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);

        btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(onClickListener);
        btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(onClickListener);
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(onClickListener);

        btnPre = findViewById(R.id.btn_pre);
        btnNext = findViewById(R.id.btn_next);

        iSwitch = findViewById(R.id.switch1);

        iSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 스위치가 켜졌을 때
                    gazePathView.setVisibility(View.VISIBLE);
                    if(!gazeTrackerManager.isTracking()){
                        gazeTrackerManager.startGazeTracking();
                        setCalibration();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("eyeTrackingUse", "true"); // 예: "키"는 설정의 이름, "값"은 설정 값
                        editor.apply();
                    }
                } else {
                    // 스위치가 꺼졌을 때
                    if(gazeTrackerManager.isTracking()){
                        gazeTrackerManager.stopGazeTracking();
                    }
                    gazePathView.setVisibility(View.GONE);

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("eyeTrackingUse", "false"); // 예: "키"는 설정의 이름, "값"은 설정 값
                    editor.apply();
                }
            }
        });

        uuid = getUUID(getApplicationContext());
        
        suuid = "cd5f2549-6f14-4d9d-ac0a-76de39b46008";

        // categorylist 만들기
        categoryMap = new HashMap<>();
        Set<String> categorySet = new HashSet<>();

        // 전체 aac 이미지 RecyclerView 에 넣기
        AssetManager assetManager = getAssets();
        try {
            InputStream inputStream = assetManager.open("aac_files.csv");
            CSVReader reader = new CSVReader(new InputStreamReader(inputStream, "UTF-8"));
            csvAllContent = (List<String[]>) reader.readAll();
            for(String content[] : csvAllContent){
                int id = Integer.parseInt(content[0]);
                String category = content[1];
                categorySet.add(category);
                String imageRes = content[2];
                String imageName = content[3];
                InputStream is = assetManager.open(imageRes);
                Drawable drawable = Drawable.createFromStream(is, null);
                ImageData imageData = new ImageData(id, drawable, imageName);

                imageDataList.add(imageData);

                if (!categoryMap.containsKey(category)) {
                    categoryMap.put(category, new ArrayList<>());
                }
                // 해당 카테고리의 리스트에 이미지 데이터를 추가합니다.
                categoryMap.get(category).add(imageData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        categoryList = new ArrayList<>(categorySet);

        // 기존 카테고리 합치기

        // 새로운 카테고리 생성
        String commonExpressions = "자주쓰는 표현";
        categoryMap.put(commonExpressions, new ArrayList<>());

        try {
            InputStream inputStream = assetManager.open("aac_files_rec.csv");
            CSVReader reader = new CSVReader(new InputStreamReader(inputStream, "UTF-8"));
            List<String[]> csvRecContent = (List<String[]>) reader.readAll();
            for (String content[] : csvRecContent) {
                int id = Integer.parseInt(content[0]);
                String imageRes = content[2];
                String imageName = content[3];
                InputStream is = assetManager.open(imageRes);
                Drawable drawable = Drawable.createFromStream(is, null);
                ImageData imageData = new ImageData(id, drawable, imageName);

                categoryMap.get(commonExpressions).add(imageData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        categoryList.add(0, commonExpressions);

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        recommendRecyclerView = findViewById(R.id.recommendRecycleView);
        selectedImageRecyclerView = findViewById(R.id.selectedImageRecyclerView);

        categoryAdapter = new CategoryAdapter(categoryList, new OnCategoryItemClickListener() {
            @Override
            public void onCategoryItemClick(String category) {
                switchCategory(category);
                categoryAdapter.setCategoryList(nullCList);
                btnBack.setVisibility(View.VISIBLE);
                categoryRecyclerView.setVisibility(View.GONE);
                imageRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        imageAdapter = new ImageAdapter(nullList, new OnImageItemClickListener() {
            @Override
            public void onImageItemClick(ImageData imageData) {
                showSelectedImage(imageData);
                selectedImageAdapter.notifyDataSetChanged();
                selectedImageRecyclerView.scrollToPosition(selectedImages.size() - 1);
                sendImageDataToServer();
            }
        });
        selectedImageAdapter = new SelectedImageAdapter(selectedImages);
        recommendImageAdapter = new RecommendImageAdapter(recommendImages, new OnImageItemClickListener() {
            @Override
            public void onImageItemClick(ImageData imageData) {
                showSelectedImage(imageData);
                selectedImageAdapter.notifyDataSetChanged();
                selectedImageRecyclerView.scrollToPosition(selectedImages.size() - 1);
                sendImageDataToServer();
            }
        });


        categoryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        categoryRecyclerView.setAdapter(categoryAdapter);

        imageRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        imageRecyclerView.setAdapter(imageAdapter);

        selectedImageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImageRecyclerView.setAdapter(selectedImageAdapter);

        recommendRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendRecyclerView.setAdapter(recommendImageAdapter);

        btnPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecyclerView recyclerView;
                if(categoryRecyclerView.getVisibility() == View.VISIBLE){
                    recyclerView = categoryRecyclerView;
                } else {
                    recyclerView = imageRecyclerView;
                }
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                int firstVisiblePosition = gridLayoutManager.findFirstVisibleItemPosition();
                if (firstVisiblePosition > 3) {
                    // 이전 아이템이 화면에 보이도록 스크롤합니다.
                    recyclerView.smoothScrollToPosition(firstVisiblePosition - 4);
                } else if (firstVisiblePosition > 0) {
                    // 이전 아이템이 화면에 보이도록 스크롤합니다.
                    recyclerView.smoothScrollToPosition(firstVisiblePosition - 1);
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RecyclerView recyclerView;
                ArrayList dataSet;
                if(categoryRecyclerView.getVisibility() == View.VISIBLE){
                    recyclerView = categoryRecyclerView;
                    dataSet = categoryList;
                } else {
                    recyclerView = imageRecyclerView;
                    dataSet = imageDataList;
                }
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                int lastVisiblePosition = gridLayoutManager.findLastVisibleItemPosition();

                // 마지막 아이템의 위치가 데이터셋의 크기를 초과하지 않으면
                if (lastVisiblePosition < dataSet.size() - 4) {
                    // 다음 아이템이 화면에 보이도록 스크롤합니다.
                    recyclerView.smoothScrollToPosition(lastVisiblePosition + 4);
                } else if (lastVisiblePosition < dataSet.size() - 1) {
                    // 다음 아이템이 화면에 보이도록 스크롤합니다.
                    recyclerView.smoothScrollToPosition(lastVisiblePosition + 1);
                }
            }
        });
    }


    // 카테고리 변경
    private void switchCategory(String category) {
        Log.d("switchCategory", category);
        List<ImageData> imageDataList = categoryMap.get(category);
        if (imageDataList != null) {
            imageAdapter.setImageDataList(imageDataList);
        }
    }

    // 데이터 전송용으로 변환
    public List<Integer> convertSelectedImages(List<ImageData> selectedImages) {
        List<Integer> imageIdList = new ArrayList<>();
        for (ImageData imageData : selectedImages) {
            int imageId = imageData.getId();
            imageIdList.add(imageId);
        }
        return imageIdList;
    }

    // 클릭한 데이터 서버로 보내기
    private void sendImageDataToServer() {
        Log.d("Api", "data send");
        List<Integer> imageIdList =  convertSelectedImages(selectedImages);
        sendImageData(suuid, imageIdList);
    }

    // 클릭한 데이터 레트로핏 처리
    private void sendImageData(String suuid, List<Integer> imageData) {
        RetrofitClient.ApiService apiService = RetrofitClient.getRetrofit().create(RetrofitClient.ApiService.class);
        ImageDataList imageDataListWrapper = new ImageDataList(suuid, imageData);
        Call<AacResponse> call = apiService.uploadImageList(imageDataListWrapper);
        call.enqueue(new Callback<AacResponse>() {
            @Override
            public void onResponse(Call<AacResponse> call, Response<AacResponse> response) {
                if (response.isSuccessful()) {
                    // 성공적으로 요청이 처리됨
                    // 서버 응답 처리
                    try{
                        AacResponse data = response.body();
                        List<String> aacIds = data.getAacId();
                        recommendImages.clear();
                        for(String aacId : aacIds){
                            int id = Integer.parseInt(aacId);
                            for(ImageData imageData : imageDataList){
                                if(id == imageData.getId()){
                                    Log.d("recommedData", ""+imageData.getId());
                                    recommendImageAdapter.addImageData(imageData);
                                }
                            }
                        }
                        recommendImageAdapter.notifyDataSetChanged();
                    } catch (Exception e){
                        Log.d("response", "response fail");
                    }
                } else {
                    Log.d("onResponse", "response fail");
                }
            }
            @Override
            public void onFailure(Call<AacResponse> call, Throwable t) {
                Log.d("onFailure", "network fail");
            }
        });
    }


    // 선택된 AAC 위 화면으로 올리기
    private void showSelectedImage(ImageData imageData) {
        selectedImageAdapter.addImageData(imageData);
    }

    // 클릭 리스너
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnClear) {
                selectedImages.clear();
                recommendImages.clear();
                recommendImageAdapter.notifyDataSetChanged();
                selectedImageAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(),"초기화되었습니다!",Toast.LENGTH_SHORT).show();
            } else if (v == btnBack) {
                imageAdapter.setImageDataList(nullList);
                categoryAdapter.setCategoryList(categoryList);
                imageRecyclerView.setVisibility(View.GONE);
                categoryRecyclerView.setVisibility(View.VISIBLE);
                btnBack.setVisibility(View.GONE);
            } else if (v == btnDelete) {
                if(selectedImages.size() > 1) {
                    selectedImages.remove(selectedImages.size() - 1);
                    selectedImageAdapter.notifyDataSetChanged();
                    sendImageDataToServer();
                } else if(selectedImages.size() == 1){
                    selectedImages.remove(selectedImages.size() - 1);
                    selectedImageAdapter.notifyDataSetChanged();
                    recommendImages.clear();
                    recommendImageAdapter.notifyDataSetChanged();
                }
            }
        }
    };

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
            Log.d("permission","permisson granted");
        } else {
            Log.d("permission","Not permisson");
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

    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                Log.d("gazeInit", "initSuccess!");
                if(iSwitch.isChecked()) {
                    gazeTrackerManager.startGazeTracking();
                    setCalibration();
                }
            } else {
                Log.d("gazeInit", "initFail...");
            }
        }
    };

    private void setCalibration() {
        GazeTrackerManager.LoadCalibrationResult result = gazeTrackerManager.loadCalibrationData();
        switch (result) {
            case SUCCESS:
                Log.d("setCalibration", "setCalibrationData success");
                break;
            case FAIL_DOING_CALIBRATION:
                Log.d("setCalibration", "calibrating");
                break;
            case FAIL_NO_CALIBRATION_DATA:
                Log.d("setCalibration", "Calibration data is null");
                break;
            case FAIL_HAS_NO_TRACKER:
                Log.d("setCalibration", "No tracker has initialized");
                break;
        }
    }



    private final UserStatusCallback userStatusCallback = new UserStatusCallback() {
        @Override
        public void onAttention(long timestampBegin, long timestampEnd, float attentionScore) {
        }
        @Override
        @SuppressLint("ClickableViewAccessibility")
        public void onBlink(long timestamp, boolean isBlinkLeft, boolean isBlinkRight, boolean isBlink, float eyeOpenness) {
            if(isBlink){
                if (isViewContains(btnClear, filtered[0], filtered[1])) {
                    performBtnClick(btnClear);
                }
                if (isViewContains(btnDelete, filtered[0], filtered[1])) {
                    performBtnClick(btnDelete);
                }
                if (isViewContains(btnBack, filtered[0], filtered[1])) {
                    performBtnClick(btnBack);
                }
                if (isViewContains(btnPre, filtered[0], filtered[1])) {
                    performBtnClick(btnPre);
                }
                if (isViewContains(btnNext, filtered[0], filtered[1])) {
                    performBtnClick(btnNext);
                }
                if (isViewContains(imageRecyclerView, filtered[0], filtered[1])) {
                    performRecyclerViewClick(imageRecyclerView, filtered[0], filtered[1]);
                }
                if (isViewContains(categoryRecyclerView, filtered[0], filtered[1])) {
                    performRecyclerViewClick(categoryRecyclerView, filtered[0], filtered[1]);
                }
                if (isViewContains(recommendRecyclerView, filtered[0], filtered[1])) {
                    performRecyclerViewClick(recommendRecyclerView, filtered[0], filtered[1]);
                }
            }
        }
        @Override
        public void onDrowsiness(long timestamp, boolean isDrowsiness) {
        }
    };

    private void performBtnClick(Button btn) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn.performClick();
            }
        });
    }

    private void performRecyclerViewClick(RecyclerView recyclerView, float x, float y){
        View itemView = findViewAtPosition(recyclerView, x, y);
        if (itemView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    itemView.performClick();
                }
            });
        }
    }

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

        int tx = 0;
        int ty = 0;

        int[] location = new int[2];
        recyclerView.getLocationOnScreen(location);
        tx = location[0];
        ty = location[1];

        float rx = x-tx;
        float ry = y-ty;

        View childView = recyclerView.findChildViewUnder(rx, ry);

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
