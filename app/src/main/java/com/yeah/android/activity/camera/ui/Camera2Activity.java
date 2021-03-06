package com.yeah.android.activity.camera.ui;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.umeng.update.UmengUpdateAgent;
import com.yeah.android.R;
import com.yeah.android.YeahApp;
import com.yeah.android.activity.camera.CameraBaseActivity;
import com.yeah.android.activity.camera.CameraManager;
import com.yeah.android.activity.camera.adapter.CameraFilterAdapter;
import com.yeah.android.activity.camera.util.CameraHelper;
import com.yeah.android.activity.camera.util.CameraHelper.CameraInfo2;
import com.yeah.android.activity.camera.util.StateCameraGridHander;
import com.yeah.android.activity.camera.util.StateCameraScaleHander;
import com.yeah.android.activity.camera.util.StateCameraTakePhotoHander;
import com.yeah.android.activity.camera.util.StateCameraTimerHander;
import com.yeah.android.activity.user.MessageListActivity;
import com.yeah.android.activity.user.SettingActivity;
import com.yeah.android.activity.user.UserHomeActivity;
import com.yeah.android.impl.ICameraLightBack;
import com.yeah.android.impl.IFilterChange;
import com.yeah.android.model.PhotoItem;
import com.yeah.android.model.user.UserInfo;
import com.yeah.android.utils.CameraUtils;
import com.yeah.android.utils.Constants;
import com.yeah.android.utils.DistanceUtil;
import com.yeah.android.utils.FileUtils;
import com.yeah.android.utils.IOUtil;
import com.yeah.android.utils.ImageUtils;
import com.yeah.android.utils.StringUtils;
import com.yeah.android.utils.UserInfoManager;
import com.yeah.android.view.CameraGrid;
import com.yeah.android.view.CameraTopBarMenu;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import me.xiaopan.psts.PagerSlidingTabStrip;

/**
 * 相机界面
 * on 15/7/6.
 */
public class Camera2Activity extends CameraBaseActivity implements View.OnClickListener, IFilterChange, CameraTopBarMenu.OnMenuClickListener, StateCameraScaleHander.OnScaleChangeListener{



    private CameraHelper mCameraHelper;
    private Camera.Parameters parameters = null;
//    private Camera cameraInst = null;
    private Bundle bundle = null;
    private int photoWidth = DistanceUtil.getCameraPhotoWidth();
    private int photoNumber = 4;
    private int photoMargin = YeahApp.getApp().dp2px(1);
    private float pointX, pointY;
    static final int FOCUS = 1;            // 聚焦
    static final int ZOOM = 2;            // 缩放
    private int mode;                      //0是聚焦 1是放大
    private float dist;
    private int PHOTO_SIZE = 2000;
//    private int mCurrentCameraId = 0;  //1是前置 0是后置
    private Handler handler = new Handler();

    private GPUImage mGPUImage;
    private CameraLoader mCamera;
    private GPUImageFilter mFilter;

    @InjectView(R.id.masking)
    CameraGrid cameraGrid;
    @InjectView(R.id.panel_take_photo)
    View takePhotoPanel;
    @InjectView(R.id.takepicture)
    View takePicture;
    @InjectView(R.id.change)
    ImageView changeBtn;
    @InjectView(R.id.next)
    ImageView galaryBtn;
    @InjectView(R.id.focus_index)
    View focusIndex;
    @InjectView(R.id.surfaceView)
    GLSurfaceView surfaceView;
    @InjectView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @InjectView(R.id.drawerBtn)
    ImageView drawerBtn;

    //
    @InjectView(R.id.drawer_camera)
    LinearLayout drawerCamera;
    @InjectView(R.id.drawer_user_home)
    LinearLayout drawerUserHome;
    @InjectView(R.id.drawer_message)
    LinearLayout drawerMessage;
    @InjectView(R.id.drawer_setting)
    LinearLayout drawerSetting;
    @InjectView(R.id.userNickName)
    TextView userNickName;
    @InjectView(R.id.userAvatar)
    SimpleDraweeView userAvatar;


    @InjectView(R.id.change_ratio)
    ImageView mChangeRatioBtn;
    @InjectView(R.id.camera_filter_choose)
    PagerSlidingTabStrip mPagerSlidingTabStrip;
    @InjectView(R.id.camera_viewpager)
    ViewPager mViewPager;
    @InjectView(R.id.tab_layout)
    LinearLayout mTabLayout;
    @InjectView(R.id.camera_menu)
    ImageView mCameraMenu;
    @InjectView(R.id.camera_top)
    RelativeLayout mCameraTopLayout;

    private CameraFilterAdapter mCameraFilterAdapter;
    private CameraTopBarMenu mCameraTopBarMenu;
    private StateCameraScaleHander mStateCameraScaleHander;
    private CameraHandler mCameraHandler;

    private static final int HANDLER_MSG_TAKEPIC = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        mCameraHelper = new CameraHelper(this);
        ButterKnife.inject(this);
        initView();
        initEvent();
        initData();

        YeahApp.getApp().setMainActivityLaunched(true);

        UmengUpdateAgent.update(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        YeahApp.getApp().setMainActivityLaunched(false);
    }

    private void initView() {
        mGPUImage = new GPUImage(this);
        mGPUImage.setGLSurfaceView(surfaceView);
        mCamera = new CameraLoader();

        mCameraHandler = new CameraHandler();

        mCameraFilterAdapter = new CameraFilterAdapter(Camera2Activity.this, mTabLayout, this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraTopBarMenu.isShowing()){
                    mCameraTopBarMenu.dismiss();
                }
                if(mCameraTopBarMenu.getCameraTakeState() == StateCameraTakePhotoHander.STATE_TOUCH_SCREEN){
                    takePictureDelay();
                }
            }
        });
        mViewPager.setAdapter(mCameraFilterAdapter);
        mViewPager.addOnPageChangeListener(mCameraFilterAdapter);
        mChangeRatioBtn.setOnClickListener(this);
        mPagerSlidingTabStrip.setViewPager(mViewPager);
//        mPagerSlidingTabStrip.setTextColorResource(R.color.camera_bottom_filter_tab_color);
//        mPagerSlidingTabStrip.setIndicatorColor(R.color.camera_bottom_filter_bg);
        mCameraTopBarMenu = new CameraTopBarMenu(LayoutInflater.from(this).inflate(
                R.layout.popupwindow_camera_topbar_menu, null), this);
        mStateCameraScaleHander = new StateCameraScaleHander(mChangeRatioBtn, this);
        mCameraMenu.setOnClickListener(this);
//        mViewPager.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.onResume();
    }

    @Override
    protected void onPause() {
        mCamera.onPause();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.change_ratio:
                //TODO 切换比例
                break;
            case R.id.camera_menu:
                if(mCameraTopBarMenu.isShowing()){
                    mCameraTopBarMenu.dismiss();
                }else{
                    mCameraTopBarMenu.showAsDropDown(v);
                }
                break;
        }
    }

    class CameraHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_MSG_TAKEPIC:
                    takePicture();
                    break;

            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onScaleChange(int scale) {
        if(scale == StateCameraScaleHander.STATE_SCALE_43){
            mCameraTopLayout.setBackgroundColor(getResources().getColor(R.color.transparent));
        }else{
            mCameraTopLayout.setBackgroundColor(getResources().getColor(R.color.camera_top_bar_bg_color));
        }
    }

    public void onFlashBtnClick(ImageView v){

        CameraUtils.turnLight(mCamera.mCameraInstance, new ICameraLightBack() {
            @Override
            public void onFlashOn() {
                v.setImageResource(R.drawable.ic_camera_topbar_menu_flash_on);
            }

            @Override
            public void onFlashOff() {
                v.setImageResource(R.drawable.ic_camera_topbar_menu_flash_off);
            }

            @Override
            public void onFlashAuto() {
                v.setImageResource(R.drawable.ic_camera_topbar_menu_flash_auto);
            }
        });

    }
    public void onGridBtnClick(int state){
        if(state == StateCameraGridHander.STATE_GRID_OFF){
            cameraGrid.setVisibility(View.INVISIBLE);
        }else{
            cameraGrid.setVisibility(View.VISIBLE);
        }
    }

    public void onFilterChange(GPUImageFilter gpuImageFilter){
        switchFilterTo(gpuImageFilter);
    }

    private void initEvent() {

        drawerBtn.setOnClickListener(v -> {
            drawerLayout.openDrawer(Gravity.LEFT);
            displayUserInfo();
        });

        //拍照
        takePicture.setOnClickListener(v -> {

            takePictureDelay();

        });

        //闪光灯
//        flashBtn.setOnClickListener(v -> );
        //前后置摄像头切换
        boolean canSwitch = false;
        try {
            canSwitch = mCameraHelper.hasFrontCamera() && mCameraHelper.hasBackCamera();
        } catch (Exception e) {
            //获取相机信息失败
        }
        if (!canSwitch) {
            changeBtn.setVisibility(View.GONE);
        } else {
            changeBtn.setOnClickListener(v -> switchCamera());
        }
        //跳转相册
        galaryBtn.setOnClickListener(v -> startActivity(new Intent(Camera2Activity.this, AlbumActivity.class)));
        surfaceView.setOnTouchListener((v, event) -> {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                // 主点按下
                case MotionEvent.ACTION_DOWN:
                    pointX = event.getX();
                    pointY = event.getY();
                    mode = FOCUS;
                    break;
                // 副点按下
                case MotionEvent.ACTION_POINTER_DOWN:
                    dist = spacing(event);
                    // 如果连续两点距离大于10，则判定为多点模式
                    if (spacing(event) > 10f) {
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = FOCUS;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == FOCUS) {
                        //pointFocus((int) event.getRawX(), (int) event.getRawY());
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            float tScale = (newDist - dist) / dist;
                            if (tScale < 0) {
                                tScale = tScale * 10;
                            }
                            addZoomIn((int) tScale);
                        }
                    }
                    break;
            }
            return false;
        });


        surfaceView.setOnClickListener(v -> {
            try {
                pointFocus((int) pointX, (int) pointY);
            } catch (Exception e) {
                e.printStackTrace();
            }
            RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(focusIndex.getLayoutParams());
            layout.setMargins((int) pointX - 60, (int) pointY - 60, 0, 0);
            focusIndex.setLayoutParams(layout);
            focusIndex.setVisibility(View.VISIBLE);
            ScaleAnimation sa = new ScaleAnimation(3f, 1f, 3f, 1f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            sa.setDuration(800);
            focusIndex.startAnimation(sa);
            handler.postDelayed(() -> focusIndex.setVisibility(View.INVISIBLE), 800);
        });

        takePhotoPanel.setOnClickListener(v -> {
            //doNothing 防止聚焦框出现在拍照区域
        });
    }

    private void initData() {
        displayUserInfo();
    }

    private void displayUserInfo() {
        if(UserInfoManager.isLogin()) {

            UserInfo userInfo = UserInfoManager.getUserInfo();

            if(userInfo != null) {
                userNickName.setText(userInfo.getNickname());
                userAvatar.setImageURI(Uri.parse(userInfo.getAvatar()));
            }
        }
    }


    @OnClick(R.id.drawer_camera)
    public void drawerCamera() {
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    @OnClick(R.id.drawer_user_home)
    public void drawerUserHome() {
        UserHomeActivity.launch(Camera2Activity.this);
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    @OnClick(R.id.drawer_message)
    public void drawerMessage() {
        MessageListActivity.launch(Camera2Activity.this);
        drawerLayout.closeDrawer(Gravity.LEFT);
    }

    @OnClick(R.id.drawer_setting)
    public void drawerSeting() {
        drawerLayout.closeDrawer(Gravity.LEFT);

        SettingActivity.launch(Camera2Activity.this);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent result) {
        if (requestCode == Constants.REQUEST_PICK && resultCode == RESULT_OK) {
            CameraManager.getInst().filterPhotoItem(
                    Camera2Activity.this,
                    new PhotoItem(result.getData().getPath(), System
                            .currentTimeMillis()));
        } else if (requestCode == Constants.REQUEST_CROP && resultCode == RESULT_OK) {
            Intent newIntent = new Intent(this, PhotoFilterActivity.class);
            newIntent.setData(result.getData());
            startActivity(newIntent);


        }
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    //放大缩小
    int curZoomValue = 0;

    private void addZoomIn(int delta) {

        try {
            Camera.Parameters params = mCamera.mCameraInstance.getParameters();
            Log.d("Camera", "Is support Zoom " + params.isZoomSupported());
            if (!params.isZoomSupported()) {
                return;
            }
            curZoomValue += delta;
            if (curZoomValue < 0) {
                curZoomValue = 0;
            } else if (curZoomValue > params.getMaxZoom()) {
                curZoomValue = params.getMaxZoom();
            }

            if (!params.isSmoothZoomSupported()) {
                params.setZoom(curZoomValue);
                mCamera.mCameraInstance.setParameters(params);
                return;
            } else {
                mCamera.mCameraInstance.startSmoothZoom(curZoomValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //定点对焦的代码
    private void pointFocus(int x, int y) {
        mCamera.mCameraInstance.cancelAutoFocus();
//        cameraInst.cancelAutoFocus();
        parameters = mCamera.mCameraInstance.getParameters();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            showPoint(x, y);
        }
        mCamera.mCameraInstance.setParameters(parameters);
        autoFocus();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showPoint(int x, int y) {
        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> areas = new ArrayList<Camera.Area>();
            //xy变换了
            int rectY = -x * 2000 / YeahApp.getApp().getScreenWidth() + 1000;
            int rectX = y * 2000 / YeahApp.getApp().getScreenHeight() - 1000;

            int left = rectX < -900 ? -1000 : rectX - 100;
            int top = rectY < -900 ? -1000 : rectY - 100;
            int right = rectX > 900 ? 1000 : rectX + 100;
            int bottom = rectY > 900 ? 1000 : rectY + 100;
            Rect area1 = new Rect(left, top, right, bottom);
            areas.add(new Camera.Area(area1, 800));
            parameters.setMeteringAreas(areas);
        }

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bundle = new Bundle();
            bundle.putByteArray("bytes", data); //将图片字节数据保存在bundle当中，实现数据交换
            new SavePicTask(data).execute();
            camera.startPreview(); // 拍完照后，重新开始预览
        }
    }

    private class SavePicTask extends AsyncTask<Void, Void, String> {
        private byte[] data;

        protected void onPreExecute() {
            showProgressDialog("处理中");
        }

        SavePicTask(byte[] data) {
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return saveToSDCard(data);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (StringUtils.isNotEmpty(result)) {
                dismissProgressDialog();
                CameraManager.getInst().processPhotoItem(Camera2Activity.this,
                        new PhotoItem(result, System.currentTimeMillis()));
            } else {
                toast("拍照失败，请稍后重试！", Toast.LENGTH_LONG);
            }
        }
    }


    //实现自动对焦
    private void autoFocus() {
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mCamera.mCameraInstance == null) {
                    return;
                }
                mCamera.mCameraInstance.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (success) {
                            initCamera();//实现相机的参数初始化
                        }
                    }
                });
            }
        };
    }

    private Camera.Size adapterSize = null;
    private Camera.Size previewSize = null;

    private void initCamera() {
        parameters = mCamera.mCameraInstance.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        //if (adapterSize == null) {
        setUpPicSize(parameters);
        setUpPreviewSize(parameters);
        //}
        if (adapterSize != null) {
            parameters.setPictureSize(adapterSize.width, adapterSize.height);
        }
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        setDispaly(parameters, mCamera.mCameraInstance);
        try {
            mCamera.mCameraInstance.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.mCameraInstance.startPreview();
        mCamera.mCameraInstance.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    private void setUpPicSize(Camera.Parameters parameters) {

        if (adapterSize != null) {
            return;
        } else {
            adapterSize = findBestPictureResolution();
            return;
        }
    }

    private void setUpPreviewSize(Camera.Parameters parameters) {

        if (previewSize != null) {
            return;
        } else {
            previewSize = findBestPreviewResolution();
        }
    }

    /**
     * 最小预览界面的分辨率
     */
    private static final int MIN_PREVIEW_PIXELS = 640 * 480;//480 * 320;
    /**
     * 最大宽高比差
     */
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final String TAG = "Camera";

    /**
     * 找出最适合的预览界面分辨率
     *
     * @return
     */
    private Camera.Size findBestPreviewResolution() {
        Camera.Parameters cameraParameters = mCamera.mCameraInstance.getParameters();
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return defaultPreviewResolution;
        }

        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder previewResolutionSb = new StringBuilder();
        for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
            previewResolutionSb.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height)
                    .append(' ');
        }
        Log.v(TAG, "Supported preview resolutions: " + previewResolutionSb);


        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) YeahApp.getApp().getScreenWidth()
                / (double) YeahApp.getApp().getScreenHeight();
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 移除低于下限的分辨率，尽可能取高分辨率
            if (width * height < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然preview宽高比后在比较
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            // 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
            if (maybeFlippedWidth == YeahApp.getApp().getScreenWidth()
                    && maybeFlippedHeight == YeahApp.getApp().getScreenHeight()) {
                return supportedPreviewResolution;
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，则设置其中最大比例的，对于配置比较低的机器不太合适
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            return largestPreview;
        }

        // 没有找到合适的，就返回默认的

        return defaultPreviewResolution;
    }

    private Camera.Size findBestPictureResolution() {
        Camera.Parameters cameraParameters = mCamera.mCameraInstance.getParameters();
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值

        StringBuilder picResolutionSb = new StringBuilder();
        for (Camera.Size supportedPicResolution : supportedPicResolutions) {
            picResolutionSb.append(supportedPicResolution.width).append('x')
                    .append(supportedPicResolution.height).append(" ");
        }
        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);

        Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
        Log.d(TAG, "default picture resolution " + defaultPictureResolution.width + "x"
                + defaultPictureResolution.height);

        // 排序
        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(
                supportedPicResolutions);
        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) YeahApp.getApp().getScreenWidth()
                / (double) YeahApp.getApp().getScreenHeight();
        Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然后在比较宽高比
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，对于照片，则取其中最大比例的，而不是选择与屏幕分辨率相同的
        if (!sortedSupportedPicResolutions.isEmpty()) {
            return sortedSupportedPicResolutions.get(0);
        }

        // 没有找到合适的，就返回默认的
        return defaultPictureResolution;
    }


    //控制图像的正确显示方向
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Build.VERSION.SDK_INT >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
                    new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }


    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws java.io.IOException
     */
    public String saveToSDCard(byte[] data) throws IOException {
        Bitmap croppedImage;

        //获得图片大小
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        PHOTO_SIZE = options.outHeight > options.outWidth ? options.outWidth : options.outHeight;
        int height = options.outHeight > options.outWidth ? options.outHeight : options.outWidth;
        options.inJustDecodeBounds = false;
        Rect r;
        if (mCamera.mCurrentCameraId == 1) {
            r = new Rect(height - PHOTO_SIZE, 0, height, PHOTO_SIZE);
        } else {
            r = new Rect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        }
        try {
            croppedImage = decodeRegionCrop(data, r);
        } catch (Exception e) {
            return null;
        }
        String imagePath = ImageUtils.saveToFile(FileUtils.getInst().getSystemPhotoPath(), true,
                croppedImage);
        croppedImage.recycle();
        return imagePath;
    }

    private Bitmap decodeRegionCrop(byte[] data, Rect rect) {

        InputStream is = null;
        System.gc();
        Bitmap croppedImage = null;
        try {
            is = new ByteArrayInputStream(data);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());
            } catch (IllegalArgumentException e) {
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeStream(is);
        }
        Matrix m = new Matrix();
        m.setRotate(90, PHOTO_SIZE / 2, PHOTO_SIZE / 2);
        if (mCamera.mCurrentCameraId == 1) {
            m.postScale(1, -1);
        }
        Bitmap rotatedImage = Bitmap.createBitmap(croppedImage, 0, 0, PHOTO_SIZE, PHOTO_SIZE, m, true);
        if (rotatedImage != croppedImage)
            croppedImage.recycle();
        return rotatedImage;
    }


    private class CameraLoader {

        private int mCurrentCameraId = 0;
        private Camera mCameraInstance;

        public void onResume() {
            setUpCamera(mCurrentCameraId);
        }

        public void onPause() {
            releaseCamera();
        }

        public void switchCamera() {
            releaseCamera();
            mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
            setUpCamera(mCurrentCameraId);
        }

        private void setUpCamera(final int id) {
            mCameraInstance = getCameraInstance(id);
            Parameters parameters = mCameraInstance.getParameters();
            // TODO adjust by getting supportedPreviewSizes and then choosing
            // the best one for screen size (best fill screen)
            if (parameters.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCameraInstance.setParameters(parameters);

            int orientation = mCameraHelper.getCameraDisplayOrientation(
                    Camera2Activity.this, mCurrentCameraId);
            CameraInfo2 cameraInfo = new CameraInfo2();
            mCameraHelper.getCameraInfo(mCurrentCameraId, cameraInfo);
            boolean flipHorizontal = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
            mGPUImage.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
        }

        /** A safe way to get an instance of the Camera object. */
        private Camera getCameraInstance(final int id) {
            Camera c = null;
            try {
                c = mCameraHelper.openCamera(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return c;
        }

        private void releaseCamera() {
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.release();
            mCameraInstance = null;
        }
    }


    //切换前后置摄像头
    private void switchCamera() {
        mCamera.switchCamera();
    }


    private void switchFilterTo(final GPUImageFilter filter) {
        mFilter = filter;
        mGPUImage.setFilter(mFilter);
    }

    private void takePictureDelay(){
        if(mCameraTopBarMenu.getCameraTimerState() == StateCameraTimerHander.STATE_TIMER_OFF){
            takePicture();
        }else{
            mCameraHandler.sendEmptyMessageDelayed(HANDLER_MSG_TAKEPIC, 3000);
        }
    }

    private void takePicture() {


        // TODO get a size that is about the size of the screen
        Camera.Parameters params = mCamera.mCameraInstance.getParameters();
        params.setRotation(90);
        mCamera.mCameraInstance.setParameters(params);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            Log.i("ASDF", "Supported: " + size.width + "x" + size.height);
        }
        mCamera.mCameraInstance.takePicture(null, null,
                new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {

                        final File pictureFile = FileUtils.getOutputMediaFile();
                        if (pictureFile == null) {
                            Log.d("ASDF",
                                    "Error creating media file, check storage permissions");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.d("ASDF", "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d("ASDF", "Error accessing file: " + e.getMessage());
                        }

                        data = null;
                        Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
                        // mGPUImage.setImage(bitmap);
                        final GLSurfaceView view = (GLSurfaceView) findViewById(R.id.surfaceView);
                        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

                        mGPUImage.saveToPictures(bitmap, "GPUImage",
                                System.currentTimeMillis() + ".jpg",
                                new OnPictureSavedListener() {

                                    @Override
                                    public void onPictureSaved(final Uri
                                                                       uri) {

                                        if (StringUtils.isNotEmpty(getRealFilePath(Camera2Activity.this,uri))) {
                                            dismissProgressDialog();
                                            CameraManager.getInst().processPhotoItem(Camera2Activity.this,
                                                    new PhotoItem(getRealFilePath(Camera2Activity.this,uri), System.currentTimeMillis()));
                                        } else {
                                            toast("拍照失败，请稍后重试！", Toast.LENGTH_LONG);
                                        }

//                                        pictureFile.delete();
//                                        camera.startPreview();
//                                        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                                    }
                                });
                    }
                });
    }

    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

}
