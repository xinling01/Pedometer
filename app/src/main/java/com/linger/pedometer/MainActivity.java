package com.linger.pedometer;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import utils.SaveKeyValues;
import view.CircularRingPercentageView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;      //传感器管理
    CircularRingPercentageView progressCircle;  //自定义圆形进度条控件
    TextView stepNumber;                    //显示步数的文本框控件
    EditText change_step;                     //目标步数控件
    private int custom_steps;               //用户的步数
    private int tarGet;                      //目标步数
    private boolean isClearHint;            //记录第一次设置目标步数清除默认步数

    public static int CURRENT_SETP;      //步数
    public static float SENSITIVITY = 8; // SENSITIVITY灵敏度

    private float mLastValues[] = new float[3 * 2];
    private float mScale[] = new float[2];
    private float mYOffset;//位移
    private static long mEnd = 0;//运动相隔时间
    private static long mStart = 0;//运动开始时间
    /**
     * 最后加速度方向
     */
    private float mLastDirections[] = new float[3 * 2];//最后的方向
    private float mLastExtremes[][] = {new float[3 * 2], new float[3 * 2]};
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取传感器服务
        sensorManager= (SensorManager) this.getSystemService(SENSOR_SERVICE);
        //用于判断是否计步值
        int h=480;
        mYOffset=h*0.5f;
        mScale[0]=-(h*0.5f*(1.0f/(SensorManager.STANDARD_GRAVITY*2)));//重力加速度
        mScale[1]=-(h*0.5f*(1.0f/(SensorManager.MAGNETIC_FIELD_EARTH_MAX)));//地球最大磁场
        progressCircle= (CircularRingPercentageView) findViewById(R.id.progress);//获取自定义进度条控件
        stepNumber = (TextView) findViewById(R.id.stepNumber);                     //获取显示步数的控件
        change_step = (EditText) findViewById(R.id.change_step);                   //获取设置目标步数的控件
        if(isClearHint==false){
            //目标步数获取焦点后清空默认的步数
            change_step.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    change_step.setHint(null);                //清空默认步数
                }
            });
            isClearHint=true;
        }
        change_step.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    //获取设置目标的步数
                    Float f = Float.parseFloat(change_step.getText().toString());
                    progressCircle.setTarGet(f);        //设置目标步数
                } catch (NumberFormatException e) {
                }
            }
        });
        SaveKeyValues.createSharePreferences(this);
        //获取保存的步数
        if(CURRENT_SETP!=0){
            custom_steps=SaveKeyValues.getIntValues("sport_steps",CURRENT_SETP);
            stepNumber.setText(custom_steps+"");//显示保存的步数
            progressCircle.setProgress(custom_steps);//设置步数进度
        }


    }

    @Override
    protected void onResume() {
        //注册传感器
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        //取消传感器监听
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        //获取默认与重新设置的目标步数
        tarGet = (int) progressCircle.getTarGet();
        synchronized (this) {
            // 判断传感器的类型是否为重力传感器(加速度传感器)
            int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
            if (j == 1) {
                float vSum = 0;
                // 获取x轴、y轴、z轴的加速度
                for (int i = 0; i < 3; i++) {
                    final float v = mYOffset + event.values[i] * mScale[j];
                    vSum += v;
                }
                int k = 0;
                float v = vSum / 3;//获取三轴加速度的平均值
                // 判断人是否处于行走中，主要从以下几个方面判断：
                // 人如果走起来了，一般会连续多走几步。因此，如果没有连续4-5个波动，那么就极大可能是干扰。
                // 人走动的波动，比坐车产生的波动要大，因此可以看波峰波谷的高度，只检测高于某个高度的波峰波谷。
                // 人的反射神经决定了人快速动的极限，怎么都不可能两步之间小于0.2秒，因此间隔小于0.2秒的波峰波谷直接跳过通过重力加速计感应，
                // 重力变化的方向，大小。与正常走路或跑步时的重力变化比对，达到一定相似度时认为是在走路或跑步。实现起来很简单，只要手机有重力感应器就能实现。
                // 软件记步数的精准度跟用户的补偿以及体重有关，也跟用户设置的传感器的灵敏度有关系，在设置页面可以对相应的参数进行调节。一旦调节结束，可以重新开始。
                float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                if (direction == -mLastDirections[k]) {
                    int extType = (direction > 0 ? 0 : 1);
                    mLastExtremes[extType][k] = mLastValues[k];
                    float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                    if (diff > SENSITIVITY) {
                        boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                        boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                        boolean isNotContra = (mLastMatch != 1 - extType);
                        if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                            mEnd = System.currentTimeMillis();
                            // 通过判断两次运动间隔判断是否走了一步
                            if (mEnd - mStart > 500) {
                                CURRENT_SETP++;             //加一步
                                mLastMatch = extType;
                                mStart = mEnd;              //结束时间为开始时间
                                if (CURRENT_SETP <= tarGet) {
                                    progressCircle.setProgress(CURRENT_SETP);   //设置步数的进度与进度条结合
                                    progressCircle.postInvalidate();            //刷新
                                }
                                //存储当前的步数

                                SaveKeyValues.putIntValues("sport_steps", CURRENT_SETP);
                                stepNumber.setText(CURRENT_SETP + "");          //显示当前步数
                            }
                        } else {
                            mLastMatch = -1;
                        }
                    }
                    mLastDiff[k] = diff;
                }
                mLastDirections[k] = direction;
                mLastValues[k] = v;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
