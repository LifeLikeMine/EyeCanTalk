package com.example.eyecantalk.activity;

import static com.example.eyecantalk.uuid.UUIDManager.getUUID;

import android.Manifest;
import android.annotation.SuppressLint;
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
    private RecyclerView recommendRecycleView;
    private RecommendImageAdapter recommendImageAdapter;
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

    String suuid;
    List<String[]> csvAllContent;
    ArrayList<ImageData> selectedImages  = new ArrayList<>();
    ArrayList<ImageData> recommendImages = new ArrayList<>();
    ArrayList<ImageData> imageDataList = new ArrayList<>();
    Map<String, List<ImageData>> categoryMap;
    private Button btnClear;
    private Button btnBack;
    private Switch iSwitch;
    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);

        btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(onClickListener);
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(onClickListener);

        iSwitch = findViewById(R.id.switch1);

        iSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 스위치가 켜졌을 때
                    gazePathView.setVisibility(View.GONE);
                    gazeTrackerManager.stopGazeTracking();
                } else {
                    // 스위치가 꺼졌을 때
                    gazePathView.setVisibility(View.VISIBLE);
                    gazeTrackerManager.startGazeTracking();
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
        List<String> categoryList = new ArrayList<>(categorySet);

        // 기존 카테고리 합치기
        List<String> categoriesToCombine = Arrays.asList("대화하기", "감정표현"); // 합칠 카테고리 목록

        // 새로운 카테고리 생성
        String commonExpressions = "자주쓰는 표현";

        // 새로운 카테고리를 위한 리스트 생성
        categoryMap.put(commonExpressions, new ArrayList<>());

        // 기존 카테고리 합치기
        for (String category : categoriesToCombine) {
            if (categoryMap.containsKey(category)) {
                // 기존 카테고리의 이미지 데이터를 새로운 카테고리에 추가
                categoryMap.get(commonExpressions).addAll(categoryMap.get(category));
            }
        }

        // 필요한 경우, 새로운 카테고리를 전체 카테고리 리스트에 추가
        categoryList.add(0, commonExpressions);

        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        recommendRecycleView = findViewById(R.id.recommendRecycleView);
        selectedImageRecyclerView = findViewById(R.id.selectedImageRecyclerView);

        categoryAdapter = new CategoryAdapter(categoryList, new OnCategoryItemClickListener() {
            @Override
            public void onCategoryItemClick(String category) {
                switchCategory(category);
                btnBack.setVisibility(View.VISIBLE);
                categoryRecyclerView.setVisibility(View.GONE);
                imageRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        imageAdapter = new ImageAdapter(imageDataList, new OnImageItemClickListener() {
            @Override
            public void onImageItemClick(ImageData imageData) {
                showSelectedImage(imageData);
                selectedImageAdapter.notifyDataSetChanged();
                sendImageDataToServer();
            }
        });
        selectedImageAdapter = new SelectedImageAdapter(selectedImages);
        recommendImageAdapter = new RecommendImageAdapter(recommendImages);

        categoryRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        categoryRecyclerView.setAdapter(categoryAdapter);

        imageRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        imageRecyclerView.setAdapter(imageAdapter);

        selectedImageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImageRecyclerView.setAdapter(selectedImageAdapter);

        recommendRecycleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recommendRecycleView.setAdapter(recommendImageAdapter);
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
                    // 요청 실패
                    // 오류 처리
                }
            }
            @Override
            public void onFailure(Call<AacResponse> call, Throwable t) {
                // 네트워크 오류 등으로 인한 요청 실패
                // 오류 처리
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
                selectedImageAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(),"초기화되었습니다!",Toast.LENGTH_SHORT).show();
            } else if (v == btnBack) {
                imageRecyclerView.setVisibility(View.GONE);
                categoryRecyclerView.setVisibility(View.VISIBLE);
                btnBack.setVisibility(View.GONE);
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
                if (isViewContains(btnClear, filtered[0], filtered[1])) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnClear.performClick();
                        }
                    });
                }
                if (isViewContains(imageRecyclerView, filtered[0], filtered[1])) {
                    View itemView = findViewAtPosition(imageRecyclerView, filtered[0], filtered[1]);
                    if (itemView != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                itemView.performClick();
                            }
                        });
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
