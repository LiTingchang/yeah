package com.yeah.android.activity.camera.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.yeah.android.R;
import com.yeah.android.YeahApp;
import com.yeah.android.activity.MainActivity;
import com.yeah.android.activity.camera.CameraBaseActivity;
import com.yeah.android.activity.camera.EffectService;
import com.yeah.android.activity.camera.adapter.FilterAdapter;
import com.yeah.android.activity.camera.adapter.StickerToolAdapter;
import com.yeah.android.activity.camera.effect.FilterEffect;
import com.yeah.android.activity.camera.util.EffectUtil;
import com.yeah.android.activity.camera.util.GPUImageFilterTools;
import com.yeah.android.model.Addon;
import com.yeah.android.model.FeedItem;
import com.yeah.android.model.TagItem;
import com.yeah.android.utils.Constants;
import com.yeah.android.utils.DataUtils;
import com.yeah.android.utils.FileUtils;
import com.yeah.android.utils.ImageUtils;
import com.yeah.android.utils.StringUtils;
import com.yeah.android.utils.TimeUtils;
import com.yeah.android.view.LabelView;
import com.yeah.android.view.MyHighlightView;
import com.yeah.android.view.MyImageViewDrawableOverlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import it.sephiroth.android.library.widget.HListView;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 *图片贴图
 */
public class PhotoStickerActivity extends CameraBaseActivity {

    //滤镜图片
    @InjectView(R.id.gpuimage)
    GPUImageView mGPUImageView;
    //绘图区域
    @InjectView(R.id.drawing_view_container)
    ViewGroup drawArea;
    //底部按钮
    @InjectView(R.id.sticker_next)
    TextView stickerNext;
    //工具区
    @InjectView(R.id.list_tools)
    HListView bottomToolBar;
    @InjectView(R.id.toolbar_area)
    ViewGroup toolArea;
    private MyImageViewDrawableOverlay mImageView;
//    private LabelSelector labelSelector;

    //当前图片
    private Bitmap currentBitmap;
    //用于预览的小图片
    private Bitmap smallImageBackgroud;
    //小白点标签
//    private LabelView emptyLabelView;

    private List<LabelView> labels = new ArrayList<LabelView>();

    //标签区域
//    private View commonLabelArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_sticker);
        ButterKnife.inject(this);
        EffectUtil.clear();
        initView();
        initEvent();
        initStickerToolBar();

        ImageUtils.asyncLoadImage(this, getIntent().getData(), new ImageUtils.LoadImageCallback() {
            @Override
            public void callback(Bitmap result) {
                currentBitmap = result;
                mGPUImageView.setImage(currentBitmap);
            }
        });

        ImageUtils.asyncLoadSmallImage(this, getIntent().getData(), new ImageUtils.LoadImageCallback() {
            @Override
            public void callback(Bitmap result) {
                smallImageBackgroud = result;
            }
        });

    }
    private void initView() {
        //添加贴纸水印的画布
        View overlay = LayoutInflater.from(PhotoStickerActivity.this).inflate(
                R.layout.view_drawable_overlay, null);
        mImageView = (MyImageViewDrawableOverlay) overlay.findViewById(R.id.drawable_overlay);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(YeahApp.getApp().getScreenWidth(),
                YeahApp.getApp().getScreenWidth());
        mImageView.setLayoutParams(params);
        overlay.setLayoutParams(params);
        drawArea.addView(overlay);


        //添加标签选择器
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(YeahApp.getApp().getScreenWidth(), YeahApp.getApp().getScreenWidth());
//        labelSelector = new LabelSelector(this);
//        labelSelector.setLayoutParams(rparams);
//        drawArea.addView(labelSelector);
//        labelSelector.hide();

        //初始化滤镜图片
        mGPUImageView.setLayoutParams(rparams);


        //初始化空白标签
//        emptyLabelView = new LabelView(this);
//        emptyLabelView.setEmpty();
//        EffectUtil.addLabelEditable(mImageView, drawArea, emptyLabelView,
//                mImageView.getWidth() / 2, mImageView.getWidth() / 2);
//        emptyLabelView.setVisibility(View.INVISIBLE);

        //初始化推荐标签栏
//        commonLabelArea = LayoutInflater.from(PhotoStickerActivity.this).inflate(
//                R.layout.view_label_bottom,null);
//        commonLabelArea.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//        toolArea.addView(commonLabelArea);
//        commonLabelArea.setVisibility(View.GONE);
    }

    private void initEvent() {
        bottomToolBar.setVisibility(View.VISIBLE);
//            labelSelector.hide();
//        emptyLabelView.setVisibility(View.GONE);
        initStickerToolBar();

//        mImageView.setOnDrawableEventListener(wpEditListener);
//        mImageView.setSingleTapListener(()->{
//                emptyLabelView.updateLocation((int) mImageView.getmLastMotionScrollX(),
//                        (int) mImageView.getmLastMotionScrollY());
//                emptyLabelView.setVisibility(View.VISIBLE);
//
////                labelSelector.showToTop();
//                drawArea.postInvalidate();
//        });
//        labelSelector.setOnClickListener(v -> {
//            labelSelector.hide();
//            emptyLabelView.updateLocation((int) labelSelector.getmLastTouchX(),
//                    (int) labelSelector.getmLastTouchY());
//            emptyLabelView.setVisibility(View.VISIBLE);
//        });
    }

    @OnClick(R.id.sticker_next)
    public void stickerNext() {
        savePicture();
    }

    //保存图片
    private void savePicture(){
        //加滤镜
        final Bitmap newBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newBitmap);
        RectF dst = new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight());
        try {
            cv.drawBitmap(mGPUImageView.capture(), null, dst, null);
        } catch (InterruptedException e) {
            e.printStackTrace();
            cv.drawBitmap(currentBitmap, null, dst, null);
        }
        //加贴纸水印
        EffectUtil.applyOnSave(cv, mImageView);

        new SavePicToFileTask().execute(newBitmap);
    }

    private class SavePicToFileTask extends AsyncTask<Bitmap,Void,String>{
        Bitmap bitmap;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("图片处理中...");
        }

        @Override
        protected String doInBackground(Bitmap... params) {
            String fileName = null;
            try {
                bitmap = params[0];

                String picName = TimeUtils.dtFormat(new Date(), "yyyyMMddHHmmss");
                 fileName = ImageUtils.saveToFile(FileUtils.getInst().getPhotoSavedPath() + "/"+ picName, false, bitmap);

            } catch (Exception e) {
                e.printStackTrace();
                toast("图片处理错误，请退出相机并重试", Toast.LENGTH_LONG);
            }
            return fileName;
        }

        @Override
        protected void onPostExecute(String fileName) {
            super.onPostExecute(fileName);
            dismissProgressDialog();
            if (StringUtils.isEmpty(fileName)) {
                return;
            }

            //将照片信息保存至sharedPreference
            //保存标签信息
            List<TagItem> tagInfoList = new ArrayList<TagItem>();
            for (LabelView label : labels) {
                tagInfoList.add(label.getTagInfo());
            }

            //将图片信息通过EventBus发送到MainActivity
            FeedItem feedItem = new FeedItem(tagInfoList,fileName);
//            EventBus.getDefault().post(feedItem);


            List<FeedItem> feedList = new ArrayList<>();
            String str = DataUtils.getStringPreferences(YeahApp.getApp(), Constants.FEED_INFO);
            if (StringUtils.isNotEmpty(str)) {
                feedList = JSON.parseArray(str, FeedItem.class);
            }

            feedList.add(0, feedItem);

            DataUtils.setStringPreferences(YeahApp.getApp(), Constants.FEED_INFO, JSON.toJSONString(feedList));

            MainActivity.launch(PhotoStickerActivity.this);
        }
    }


    private MyImageViewDrawableOverlay.OnDrawableEventListener wpEditListener   = new MyImageViewDrawableOverlay.OnDrawableEventListener() {
        @Override
        public void onMove(MyHighlightView view) {
        }

        @Override
        public void onFocusChange(MyHighlightView newFocus, MyHighlightView oldFocus) {
        }

        @Override
        public void onDown(MyHighlightView view) {

        }

        @Override
        public void onClick(MyHighlightView view) {
        }

        @Override
        public void onClick(final LabelView label) {
        }
    };

    //初始化贴图
    private void initStickerToolBar(){

        bottomToolBar.setAdapter(new StickerToolAdapter(PhotoStickerActivity.this, EffectUtil.addonList));
        bottomToolBar.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> arg0,
                                    View arg1, int arg2, long arg3) {
//                labelSelector.hide();
                Addon sticker = EffectUtil.addonList.get(arg2);
                EffectUtil.addStickerImage(mImageView, PhotoStickerActivity.this, sticker,
                        new EffectUtil.StickerCallback() {
                            @Override
                            public void onRemoveSticker(Addon sticker) {
//                                labelSelector.hide();
                            }
                        });
            }
        });
    }
}
