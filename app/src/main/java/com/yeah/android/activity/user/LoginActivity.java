package com.yeah.android.activity.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.TypeReference;
import com.loopj.android.http.RequestParams;
import com.yeah.android.R;
import com.yeah.android.activity.BaseActivity;
import com.yeah.android.activity.MainActivity;
import com.yeah.android.activity.camera.CameraManager;
import com.yeah.android.model.common.ResponseData;
import com.yeah.android.model.user.LoginResult;
import com.yeah.android.model.user.VerifyResponse;
import com.yeah.android.net.http.StickerHttpClient;
import com.yeah.android.net.http.StickerHttpResponseHandler;
import com.yeah.android.utils.Constants;
import com.yeah.android.utils.LogUtil;
import com.yeah.android.utils.StringUtils;
import com.yeah.android.utils.ToastUtil;
import com.yeah.android.utils.UserInfoManager;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by litingchang on 15-10-6.
 */
public class LoginActivity extends BaseActivity {




    @InjectView(R.id.login_input_name)
    EditText loginInputName;
    @InjectView(R.id.login_input_password)
    EditText loginInputPassword;
    @InjectView(R.id.login_btn_login)
    TextView loginBtnLogin;
    @InjectView(R.id.login_forget_password)
    TextView loginForgetPassword;
    @InjectView(R.id.login_register)
    TextView loginRegister;
    @InjectView(R.id.bind_weibo)
    ImageView bindWeibo;
    @InjectView(R.id.bind_weixin)
    ImageView bindWeixin;
    @InjectView(R.id.bind_qq)
    ImageView bindQq;


    private VerifyResponse mVerifyResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.inject(this);

        // TODO
        loginInputName.setText("17091088678");
        loginInputPassword.setText("000321");
    }

    @OnClick(R.id.login_btn_login)
    public void login() {
        String phoneNumber = StringUtils.deleteWhitespace(loginInputName.getText().toString());
        if (StringUtils.isEmpty(phoneNumber)) {
            ToastUtil.shortToast(this, "请输入用户名，不可包含空格");
            return;
        }

        String password = StringUtils.deleteWhitespace(loginInputPassword.getText().toString());
        if (StringUtils.isEmpty(password)) {
            ToastUtil.shortToast(this, "请输入登录密码，不可包含空格");
            return;
        }

        RequestParams requestParams = new RequestParams();
        requestParams.put("appId", Constants.APP_ID);
        requestParams.put("appKey", Constants.APP_KEY);
        requestParams.put("phone", phoneNumber);
        requestParams.put("password", password);
        StickerHttpClient.post("/account/user/login", requestParams,
                new TypeReference<ResponseData<LoginResult>>() {
                }.getType(),
                new StickerHttpResponseHandler<LoginResult>() {

                    @Override
                    public void onStart() {
                        showProgressDialog("登录中...");
                    }

                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        UserInfoManager.savePassword(password);

                        UserInfoManager.login(loginResult);

                        //  登录成功后的处理
                        ToastUtil.shortToast(LoginActivity.this, "登录成功");
                        CameraManager.getInst().openCamera(LoginActivity.this);

                        LoginActivity.this.finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        LogUtil.e("onFailure", message);
                        ToastUtil.shortToast(LoginActivity.this, "登录失败：" + message);
                    }

                    @Override
                    public void onFinish() {
                        dismissProgressDialog();
                    }
                });

    }

    @OnClick(R.id.login_forget_password)
    public void resetPassword() {
        String phoneNumber = StringUtils.deleteWhitespace(loginInputName.getText().toString());
        if (StringUtils.isEmpty(phoneNumber)) {
            ToastUtil.shortToast(this, "请输入用户名，不可包含空格");
            return;
        }

        RequestParams requestParams = new RequestParams();
        requestParams.put("appId", Constants.APP_ID);
        requestParams.put("appKey", Constants.APP_KEY);
        requestParams.put("phone", phoneNumber);
        StickerHttpClient.post("/account/verify/request", requestParams,
                new TypeReference<ResponseData<VerifyResponse>>() {
                }.getType(),
                new StickerHttpResponseHandler<VerifyResponse>() {

                    @Override
                    public void onStart() {
                        showProgressDialog("验证码发送中...");
                    }

                    @Override
                    public void onSuccess(VerifyResponse verifyResponse) {
                        // TODO 验证码
                        mVerifyResponse = verifyResponse;
                        ToastUtil.longToast(LoginActivity.this, "验证码发送成功:" + verifyResponse.getCode());

                        VerifyActivity.launch(LoginActivity.this, phoneNumber, mVerifyResponse);
                    }

                    @Override
                    public void onFailure(String message) {
                        LogUtil.e("onFailure", message);
                        ToastUtil.shortToast(LoginActivity.this, "验证码发送失败：" + message
                                + "\n请稍候重试");
                    }

                    @Override
                    public void onFinish() {
                        dismissProgressDialog();
                    }
                });
    }

    @OnClick(R.id.login_register)
    public void register() {
        RegistActivity.launch(LoginActivity.this);
    }
    @OnClick(R.id.bind_weibo)
    public void bindWeiBo() {

    }
    @OnClick(R.id.bind_weixin)
    public void binWeiXin() {

    }
    @OnClick(R.id.bind_qq)
    public void bindQQ() {

    }

    public static void launch(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }
}