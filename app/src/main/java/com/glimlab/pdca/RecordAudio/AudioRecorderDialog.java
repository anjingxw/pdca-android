package com.glimlab.pdca.RecordAudio;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.glimlab.pdca.R;
import com.shuyu.waveview.AudioPlayer;
import com.shuyu.waveview.FileUtils;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class AudioRecorderDialog extends BasePopupWindow implements  View.OnTouchListener {
    public  interface  AudioRecorderDialogListener{
        public  void success(String string);
        public  void cancel();
    }

    private ImageView imageView;
    private TextView textView;
    private TextView button;
    private TextView playIng;
    private View okAndCancel;
    private TextView ok;
    private TextView cancel;
    AudioRecorderDialogListener listener;

    boolean mIsRecord = false;
    boolean mIsPlay = false;
    int duration;
    int curPosition;

    private boolean  movInButton = false;
    private String filePath="";
    private MP3Recorder mRecorder;
    AudioPlayer audioPlayer;

    public AudioRecorderDialogListener getListener() {
        return listener;
    }

    public void setListener(AudioRecorderDialogListener listener) {
        this.listener = listener;
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case AudioPlayer.HANDLER_CUR_TIME://更新的时间
                    curPosition = (int) msg.obj;
                    textView.setText(toTime(curPosition) + " / " + toTime(duration));
                    break;
                case AudioPlayer.HANDLER_COMPLETE://播放结束
                    textView.setText(toTime(duration));
                    mIsPlay = false;
                    UIFinishPlay();
                    break;
                case AudioPlayer.HANDLER_PREPARED://播放开始
                    duration = (int) msg.obj;
                    textView.setText(toTime(curPosition) + " / " + toTime(duration));
                    break;
                case AudioPlayer.HANDLER_ERROR://播放错误
                    UICancelRecord();
                    break;
            }

        }
    };
    private Long recordMillisecond = 0L;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            recordMillisecond += 200;
            setLevel(mRecorder.getRealVolume());
            if (recordMillisecond%1000==0){
                setTime(recordMillisecond);
            }
            if (recordMillisecond>10*1000){
                stopRecord();
                UIFinishRecord();
                return;
            }
            handler.postDelayed(runnable, 200);
        }
    };

    public AudioRecorderDialog(Context context) {
        super(context);
        View contentView = LayoutInflater.from(context).inflate(R.layout.layout_recoder_dialog, null);
        imageView = (ImageView) contentView.findViewById(R.id.progress);
        textView = (TextView) contentView.findViewById(R.id.text1);
        button = (TextView) contentView.findViewById(R.id.button1);

        okAndCancel = contentView.findViewById(R.id.okAndCancel);
        playIng = (TextView)contentView.findViewById(R.id.playIng);
        ok =  (TextView)contentView.findViewById(R.id.ok);
        cancel = (TextView)contentView.findViewById(R.id.cancel);

        setContentView(contentView);
        button.setOnTouchListener(this);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UICancelRecord();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null){
                    if (filePath.length() >0){
                        listener.success(filePath);
                        listener = null;
                        dismiss();
                        return;
                    }
                    listener.cancel();
                }
            }
        });

        audioPlayer = new AudioPlayer(getContext(), handler);
    }

    public void setLevel(int level) {
        Drawable drawable = imageView.getDrawable();
        drawable.setLevel(3000 + 6000 * level/mRecorder.getMaxVolume());
    }

    public void setTime(long time) {
        textView.setText(toTime(time));
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //Log.i("____", motionEvent.toString());
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                movInButton = true;
                if (!mIsRecord&&!mIsPlay){
                    if (filePath.length()>0){
                        if (mIsPlay){
                            stopPlayer();
                        }else {
                            startPlay();
                        }
                    }else{
                        startRecord();
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mIsRecord){
                    stopRecord();
                    if (isTouchPointInView(button, getCurrentPoints(motionEvent))){
                        //
                        UIFinishRecord();
                    }else {
                        UICancelRecord();
                    }
                }

                return true;
            case MotionEvent.ACTION_MOVE:{
                doResultWithPoints(button, getCurrentPoints(motionEvent));
            }
        }
        return false;
    }

    // 判断一组触摸点是否在 view上；
    public static boolean isTouchPointInView(View view, Point[] points) {
        if (view == null && points == null) {
            throw new NullPointerException();
        }

        int len = points.length;

        boolean result = false;
        for (int i = 0; i < len; i++) {
            if (isTouchPointInView(view, points[i])) {
                result = true;
                break;
            }
        }
        return result;
    }

    private static final int MAX_TOUCH_POINT = 1; // 最多监听1个触点；
    // 获取当前所有触摸点的位置；
    public static Point[] getCurrentPoints(MotionEvent event){
        int pointerCount = event.getPointerCount();
        if (pointerCount > MAX_TOUCH_POINT) {
            pointerCount = MAX_TOUCH_POINT;
        }

        Point[] points = new Point[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            points[i] = new Point((int) event.getRawX(), (int) event.getRawY());
        }

        return points;
    }

    // 判断一个具体的触摸点是否在 view 上；
    public static boolean isTouchPointInView(View view, Point point) {
        if (view == null && point == null) {
            throw new NullPointerException();
        }

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (point.x >= left && point.x <= right && point.y >= top && point.y <= bottom) {
            return true;
        }
        return false;
    }

    // 根据触摸点是否在 view 上进行处理；
    private void doResultWithPoints(View iv, Point[] points) {
        boolean result = isTouchPointInView(iv, points);
        if (movInButton == result)
            return;
        movInButton = result;
        if (movInButton) { // 在范围内：
            //// Log.i("---------", "在范围内");
            UIResumeRecordTips();
        } else { // 不在范围内：
            //Log.i("---------", "不在范围内");
            UIExitRecordTips();
        }
    }

    // 当所有触摸点都松开的时候执行；
    private void whenTouchUp() {
        // TODO
    }


    /**
     * 开始录音
     */
    private void startRecord() {
        filePath = FileUtils.getAppPath();
        File file = new File(filePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(this.getContext(), "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        filePath = FileUtils.getAppPath() + UUID.randomUUID().toString() + ".mp3";
        mRecorder = new MP3Recorder(new File(filePath));
        mRecorder.setErrorHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(getContext(), "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    recordError();
                }
            }
        });

        try {
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "录音出现异常", Toast.LENGTH_SHORT).show();
            recordError();
            return;
        }
        mIsRecord =true;
        UIStartRecord();
    }

    /**
     * 停止录音
     */
    private void stopRecord() {
        mIsRecord = false;
        handler.removeCallbacks(runnable);
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
        }
    }
    /**
     * 录音异常
     */
    private void recordError() {
        UICancelRecord();
        mIsRecord =  false;
        handler.removeCallbacks(runnable);
        FileUtils.deleteFile(filePath);
        filePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
        }
    }


    private void UIStartRecord(){
        handler.postDelayed(runnable, 200);
        button.setBackgroundResource(R.drawable.shape_recoder_btn_recoding);
        button.setText("停止录制");
    }

    private void UIExitRecordTips(){
        if (mIsRecord){
            button.setText("取消录制");
        }
    }
    private void UIResumeRecordTips(){
        if (mIsRecord){
            button.setText("停止录制");
        }
    }
    private void UICancelRecord(){
        if (mIsPlay){
            startPlay();
        }
        handler.removeCallbacks(runnable);
        recordMillisecond = 0L;
        mIsPlay = false;
        setLevel(0);
        button.setBackgroundResource(R.drawable.shape_recoder_btn_normal);
        FileUtils.deleteFile(filePath);
        filePath = "";
        textView.setText("00:00");
        button.setText("开始录制");
        imageView.setVisibility(View.VISIBLE);
        playIng.setVisibility(View.GONE);
        okAndCancel.setVisibility(View.GONE);

    }
    private void UIFinishRecord(){
        setLevel(0);
        handler.removeCallbacks(runnable);
        button.setBackgroundResource(R.drawable.shape_recoder_btn_normal);
        okAndCancel.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        button.setText("开始播放");
    }

    private void UIStartPlay(){
        button.setText("停止播放");
        imageView.setVisibility(View.GONE);
        playIng.setVisibility(View.VISIBLE);
        button.setBackgroundResource(R.drawable.shape_recoder_btn_recoding);
    }
    private void UIFinishPlay(){
        button.setText("开始播放");
        playIng.setVisibility(View.GONE);
        button.setBackgroundResource(R.drawable.shape_recoder_btn_normal);
    }

    private String toTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        String dateString = formatter.format(time);
        return dateString;
    }

    private  void startPlay(){
        if (TextUtils.isEmpty(filePath) || !new File(filePath).exists()) {
            Toast.makeText(getContext(), "文件不存在", Toast.LENGTH_SHORT).show();
            UICancelRecord();
        }

        mIsPlay = true;
        audioPlayer.playUrl(filePath);
        UIStartPlay();
    }

    private  void stopPlayer(){
        mIsPlay = false;
        audioPlayer.pausePlay();
        UIFinishPlay();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (listener != null){
            listener.cancel();
        }
    }
}
