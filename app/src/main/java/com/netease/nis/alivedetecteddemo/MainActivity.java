package com.netease.nis.alivedetecteddemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.netease.nis.alivedetected.ActionType;
import com.netease.nis.alivedetected.AliveDetector;
import com.netease.nis.alivedetected.DetectedListener;
import com.netease.nis.alivedetected.NISCameraPreview;
import com.sfyc.ctpv.CountTimeProgressView;

import org.opencv.android.CameraBridgeViewBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifImageView;


public class MainActivity extends Activity {
    private final String TAG = "AliveDetector";
    private ViewStub vsStep2, vsStep3, vsStep4;
    private TextView tvStateTip, tvStep1, tvStep2, tvStep3, tvStep4;
    private ImageButton imgBtnBack;
    private ImageView ivVoice;
    private GifImageView givAction;
    private CountTimeProgressView mCountTimeView;
    private NISCameraPreview mCameraPreview;
    private AliveDetector mAliveDetector;
    private boolean DEBUG = false;
    private boolean isUsedCustomStateTip = true; // 是否使用自定义活体状态文案
    private static String BUSINESS_ID;
    private Map<String, String> stateTipMap = new HashMap();
    private static final String KEY_STRAIGHT_AHEAD = "straight_ahead";
    private static final String KEY_OPEN_MOUTH = "open_mouth";
    private static final String KEY_TURN_HEAD_TO_LEFT = "turn_head_to_left";
    private static final String KEY_TURN_HEAD_TO_RIGHT = "turn_head_to_right";
    private static final String KEY_BLINK_EYES = "blink_eyes";
    private static final String TIP_STRAIGHT_AHEAD = "请正对手机屏幕\n" +
            "将面部移入框内";//"请将面部移入框内并保持不动";
    private static final String TIP_OPEN_MOUTH = "张张嘴";
    private static final String TIP_TURN_HEAD_TO_LEFT = "慢慢左转头";
    private static final String TIP_TURN_HEAD_TO_RIGHT = "慢慢右转头";
    private static final String TIP_BLINK_EYES = "眨眨眼";
    private int mCurrentCheckStepIndex = 0;
    private ActionType mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD;
    private ActionType[] mActions;
    private boolean isOpenVoice = true;
    private MediaPlayer mPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAliveDetector != null) {
            mAliveDetector.stopDetect();
        }
    }

    private void initView() {
        mCameraPreview = (NISCameraPreview) findViewById(R.id.surface_view);
        mCameraPreview.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mCameraPreview.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        tvStateTip = findViewById(R.id.tv_tip);
        tvStep1 = findViewById(R.id.tv_step_1);
        vsStep2 = findViewById(R.id.vs_step_2);
        vsStep3 = findViewById(R.id.vs_step_3);
        vsStep4 = findViewById(R.id.vs_step_4);
        givAction = findViewById(R.id.gif_action);
        imgBtnBack = findViewById(R.id.img_btn_back);
        imgBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAliveDetector != null) {
                    mAliveDetector.stopDetect();
                }
                finish();
            }
        });
        ivVoice = findViewById(R.id.iv_voice);
        ivVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOpenVoice = !isOpenVoice;
                if (isOpenVoice) {
                    ivVoice.setImageResource(R.mipmap.ico_voice_open_2x);
                } else {
                    ivVoice.setImageResource(R.mipmap.ico_voice_close_2x);
                }
            }
        });
        mCountTimeView = findViewById(R.id.pv_count_time);
        initData();
    }

    private void initData() {
        stateTipMap.put(KEY_STRAIGHT_AHEAD, TIP_STRAIGHT_AHEAD);
        stateTipMap.put(KEY_TURN_HEAD_TO_LEFT, TIP_TURN_HEAD_TO_LEFT);
        stateTipMap.put(KEY_TURN_HEAD_TO_RIGHT, TIP_TURN_HEAD_TO_RIGHT);
        stateTipMap.put(KEY_OPEN_MOUTH, TIP_OPEN_MOUTH);
        stateTipMap.put(KEY_BLINK_EYES, TIP_BLINK_EYES);
        if (DEBUG) {
            BUSINESS_ID = "34b479cb4b97406497f7b8d1de85a1ab";
        } else {
            BUSINESS_ID = "6a1a399443a54d31b91896a4208bf6e0";
        }
        mAliveDetector = AliveDetector.getInstance();
        mAliveDetector.setDebugMode(true);
        mAliveDetector.init(this, mCameraPreview, BUSINESS_ID);
        mAliveDetector.setDetectedListener(new DetectedListener() {
            @Override
            public void onReady(boolean isInitSuccess) {
                if (isInitSuccess) {
                    Log.d(TAG, "活体检测引擎初始化完成");
                } else {
                    //  mAliveDetector.startDetect();
                    Log.e(TAG, "活体检测引擎初始化失败");
                }
            }

            /**
             * 此次活体检测下发的待检测动作指令序列
             *
             * @param actionTypes
             */
            @Override
            public void onActionCommands(ActionType[] actionTypes) {
                mActions = actionTypes;
                String commands = buildActionCommand(actionTypes);
                showIndicatorOnUiThread(commands.length() - 1);
//                showToast("活体检测动作序列为:" + commands);
                Log.e(TAG, "活体检测动作序列为:" + commands);
            }

            @Override
            public void onStateTipChanged(ActionType actionType, String stateTip) {
                Log.d(TAG, "actionType:" + actionType.getActionTip() + " stateTip:" + actionType + " CurrentCheckStepIndex:" + mCurrentCheckStepIndex);
                if (actionType != ActionType.ACTION_ERROR && actionType != mCurrentActionType) {
                    mCurrentCheckStepIndex++;
                    if (mCurrentCheckStepIndex < mActions.length) {
                        updateIndicatorOnUiThread(mCurrentCheckStepIndex);
                        if (isOpenVoice) {
                            playSounds(mCurrentCheckStepIndex);
                        }
                        mCurrentActionType = mActions[mCurrentCheckStepIndex];
                    }
                }
                if (isUsedCustomStateTip) {
                    switch (actionType) {
                        case ACTION_STRAIGHT_AHEAD:
                            setTipText(stateTipMap.get(KEY_STRAIGHT_AHEAD));
                            break;
                        case ACTION_OPEN_MOUTH:
                            setTipText(stateTipMap.get(KEY_OPEN_MOUTH));
                            break;
                        case ACTION_TURN_HEAD_TO_LEFT:
                            setTipText(stateTipMap.get(KEY_TURN_HEAD_TO_LEFT));
                            break;
                        case ACTION_TURN_HEAD_TO_RIGHT:
                            setTipText(stateTipMap.get(KEY_TURN_HEAD_TO_RIGHT));
                            break;
                        case ACTION_BLINK_EYES:
                            setTipText(stateTipMap.get(KEY_BLINK_EYES));
                            break;
                        case ACTION_ERROR:
                            setTipText(TIP_STRAIGHT_AHEAD);
                            break;
//                        case ACTION_PASSED:
//                            setTipText(stateTip);
//                            break;
                    }
                } else {
                    setTipText(stateTip);
                }
            }

            @Override
            public void onPassed(boolean isPassed, String token) {
                if (isPassed) {
                    Log.d(TAG, "活体检测通过,token is:" + token);
                    finish();
                    Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
                    startActivity(intent);
                } else {
                    Log.e(TAG, "活体检测不通过,token is:" + token);
                    jump2FailureActivity(token);
                }
            }

            @Override
            public void onError(int code, String msg, String token) {
                Log.e(TAG, "listener [onError] 活体检测出错,原因:" + msg + " token:" + token);
                jump2FailureActivity(token);
            }

            @Override
            public void onOverTime() {
                Util.showDialog(MainActivity.this, "检测超时", "请在规定时间内完成动作",
                        "重试", "返回首页", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAliveDetector.stopDetect();
                                resetIndicator();
                                mCountTimeView.startCountTimeAnimation();
                                mAliveDetector.startDetect();
                            }
                        }, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                                startActivity(intent);
                            }
                        });
            }
        });
        mAliveDetector.setTimeOut(1000 * 30);
        mAliveDetector.startDetect();
        initCountTimeView();
    }

    private void initCountTimeView() {
        mCountTimeView.setStartAngle(0);
        mCountTimeView.startCountTimeAnimation();
    }

    private void setTipText(final String tip) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvStateTip.setText(tip);
            }
        });
    }


    public static String buildActionCommand(ActionType[] actionCommands) {
        StringBuilder commands = new StringBuilder();
        for (ActionType actionType : actionCommands) {
            commands.append(actionType.getActionID());
        }
        return commands == null ? "" : commands.toString();
    }

    private void showIndicatorOnUiThread(final int commandLength) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showViewStub(commandLength);
                showIndicator(commandLength);
            }
        });
    }

    private void showIndicator(int commandLength) {
        switch (commandLength) {
            case 2:
                vsStep2.setVisibility(View.VISIBLE);
                tvStep2.setVisibility(View.VISIBLE);
                break;
            case 3:
                tvStep2.setVisibility(View.VISIBLE);
                tvStep3.setVisibility(View.VISIBLE);
                break;
            case 4:
                tvStep2.setVisibility(View.VISIBLE);
                tvStep3.setVisibility(View.VISIBLE);
                tvStep4.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showViewStub(int commandLength) {
        switch (commandLength) {
            case 2:
                vsStep2.setVisibility(View.VISIBLE);
                tvStep2 = findViewById(R.id.tv_step_2);
                break;
            case 3:
                vsStep2.setVisibility(View.VISIBLE);
                tvStep2 = findViewById(R.id.tv_step_2);
                vsStep3.setVisibility(View.VISIBLE);
                tvStep3 = findViewById(R.id.tv_step_3);
                break;
            case 4:
                vsStep2.setVisibility(View.VISIBLE);
                tvStep2 = findViewById(R.id.tv_step_2);
                vsStep3.setVisibility(View.VISIBLE);
                tvStep3 = findViewById(R.id.tv_step_3);
                vsStep4.setVisibility(View.VISIBLE);
                tvStep4 = findViewById(R.id.tv_step_4);
                break;
        }
    }

    private void updateIndicatorOnUiThread(final int currentActionIndex) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateIndicator(currentActionIndex);
                updateGif(currentActionIndex);
            }
        });
    }

    private void resetIndicator() {
        mCurrentCheckStepIndex = 0;
        mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD;
        tvStep1.setText("1");
        tvStep2.setText("");
        tvStep3.setText("");
        tvStep4.setText("");
        setTextViewUnFocus(tvStep2);
        setTextViewUnFocus(tvStep3);
        setTextViewUnFocus(tvStep4);
    }

    private void updateIndicator(int currentActionPassedCount) {
        switch (currentActionPassedCount) {
            case 2:
                tvStep1.setText("");
                tvStep2.setText("2");
                setTextViewFocus(tvStep2);
                break;
            case 3:
                tvStep1.setText("");
                tvStep2.setText("");
                setTextViewFocus(tvStep2);
                tvStep3.setText("3");
                setTextViewFocus(tvStep3);
                break;
            case 4:
                tvStep1.setText("");
                tvStep2.setText("");
                setTextViewFocus(tvStep2);
                tvStep3.setText("");
                setTextViewFocus(tvStep3);
                tvStep4.setText("4");
                setTextViewFocus(tvStep4);
                break;
        }
    }

    private void updateGif(int currentActionIndex) {
        switch (mActions[currentActionIndex]) {
            case ACTION_TURN_HEAD_TO_LEFT:
                givAction.setImageResource(R.drawable.turn_left);
                break;
            case ACTION_TURN_HEAD_TO_RIGHT:
                givAction.setImageResource(R.drawable.turn_right);
                break;
            case ACTION_OPEN_MOUTH:
                givAction.setImageResource(R.drawable.open_mouth);
                break;
            case ACTION_BLINK_EYES:
                givAction.setImageResource(R.drawable.open_eyes);
                break;
        }
    }

    private void playSounds(int currentActionIndex) {
        switch (mActions[currentActionIndex]) {
            case ACTION_TURN_HEAD_TO_LEFT:
                playSound(getAssetFileDescriptor("turn_head_to_left.wav"));
                break;
            case ACTION_TURN_HEAD_TO_RIGHT:
                playSound(getAssetFileDescriptor("turn_head_to_right.wav"));
                break;
            case ACTION_OPEN_MOUTH:
                playSound(getAssetFileDescriptor("open_mouth.wav"));
                break;
            case ACTION_BLINK_EYES:
                playSound(getAssetFileDescriptor("blink_eyes.wav"));
                break;
        }
    }

    private void setTextViewFocus(TextView tv) {
        tv.setBackgroundDrawable(ContextCompat.getDrawable(getApplication(), R.drawable.circle_tv_focus));
    }

    private void setTextViewUnFocus(TextView tv) {
        tv.setBackgroundDrawable(ContextCompat.getDrawable(getApplication(), R.drawable.circle_tv_un_focus));
    }

    private AssetFileDescriptor getAssetFileDescriptor(String assetName) {
        try {
            AssetFileDescriptor fileDescriptor = getApplication().getAssets().openFd(assetName);
            return fileDescriptor;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getAssetFileDescriptor error" + e.toString());
        }
        return null;
    }

    private void playSound(AssetFileDescriptor fileDescriptor) {
        try {
            mPlayer.reset();
            mPlayer.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getLength());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "playSound error" + e.toString());
        }
    }

    private void jump2FailureActivity(String token) {
        finish();
        Intent intent = new Intent(MainActivity.this, FailureActivity.class);
        intent.putExtra("token", token);
        startActivity(intent);
    }
}
