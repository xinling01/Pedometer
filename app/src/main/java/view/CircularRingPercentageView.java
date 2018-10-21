package view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.linger.pedometer.R;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static android.os.Build.VERSION_CODES.M;

/**
 * Created by linger on 2018/10/21.
 */

public class CircularRingPercentageView extends View {
    private Paint paint;                  //定义画笔
    private int circleWith;              //圆形进度条的直径
    private int roundBackgroundColor;   //渐变背景色
    private int textColor;                //刻度字体的颜色
    private float textSize;               //刻度文字大小
    private float roundWidth;             //进度条的宽度
    private float progress = 0;              //进度
    private int[] colors = {0xffff4639, 0xffCDD513, 0xff3CDF5F};//颜色数组
    private int radius;                     //圆环的半径
    private RectF oval;                      //进度条与中心的位置
    private Paint mPaintText;                //绘制刻度的画笔
    private int maxColorNumber = 100;         //进度条分割数量
    private float singlPoint;               //两个间隔块之间的距离
    private float lingWidth = 0.3f;            //间隔块的宽度
    private int circleCenter;                //圆环的半径
    private SweepGradient sweepGradient;      //渐变色的位置
    private boolean isLing;                   //是否绘制间隔块标记


    private float tarGet = 6000;               //设置默认目标频数

    public CircularRingPercentageView(Context context) {
        this(context, null);
    }

    public CircularRingPercentageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularRingPercentageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //设置自定义属性样式
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircularRing);
        //分割数量
        maxColorNumber = mTypedArray.getInt(R.styleable.CircularRing_circleNumber, 100);
        //圆环进度条的直径
        circleWith = mTypedArray.getDimensionPixelOffset(R.styleable.CircularRing_circleWidth, getDpValue(280));
        //渐变色的背景
        roundBackgroundColor = mTypedArray.getColor(R.styleable.CircularRing_roundColor, 0xffdddddd);
        //刻度字体的颜色
        textColor = mTypedArray.getColor(R.styleable.CircularRing_circleTextColor, 0xff999999);
        //进度条的宽度
        roundWidth = mTypedArray.getDimension(R.styleable.CircularRing_circleWidth, 40);
        //刻度文字的大小
        textSize = mTypedArray.getDimension(R.styleable.CircularRing_circleTextSize, getDpValue(8));
        //渐变数组
        colors[0] = mTypedArray.getColor(R.styleable.CircularRing_circleColor1, 0xffff4639);
        colors[1] = mTypedArray.getColor(R.styleable.CircularRing_circleColor2, 0xffCDD513);
        colors[2] = mTypedArray.getColor(R.styleable.CircularRing_circleColor3, 0xff3CDF5F);
        initView();//初始化控件
        mTypedArray.recycle();//回收资源
    }

    //像素转换dp
    private int getDpValue(int w) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, w, getContext().getResources().getDisplayMetrics());
    }
public synchronized void setProgress(float p){
    progress=p;
    postInvalidate();
}
    //空白处原色背景
    public void setRoundBackgroundColor(int roundBackgroundColor) {
        this.roundBackgroundColor = roundBackgroundColor;
        paint.setColor(roundBackgroundColor);
        invalidate();
    }

    //设置刻度字体颜色
    public void setTextColor(int textColor) {
        this.textColor = textColor;
        mPaintText.setColor(textColor);
        invalidate();
    }

    //设置刻度字体大小
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        mPaintText.setTextSize(textSize);
        invalidate();

    }

    //设置渐变色
    public void setColors(int[] colors) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("Colors length<2");
        }
        this.colors = colors;
        sweepGradientInit();
    }

    //渐变色初始化
    public void sweepGradientInit() {
        //渐变色位置
        sweepGradient = new SweepGradient(this.circleWith / 2, this.circleWith / 2, colors, null);
        //旋转渐变
        Matrix matrix = new Matrix();
        matrix.setRotate(-90, this.circleWith / 2, this.circleWith / 2);
        sweepGradient.setLocalMatrix(matrix);
    }

    //设置间隔块宽度
    public void setLingWidth(float lingWidth) {
        this.lingWidth = lingWidth;
        invalidate();
    }

    //是否绘制间隔块
    public void setLing(boolean line) {
        isLing = line;
        invalidate();
    }

    //获取圆形进度条直径
    public int getCircleWith() {
        return circleWith;
    }

    //设置圆环宽度
    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
        if (roundWidth > circleCenter) {
            this.roundWidth = circleCenter;
        }
        radius = (int) (circleCenter - this.roundWidth / 2);//计算圆环半径值
        oval.left = circleCenter - radius;      //圆环的左侧
        oval.right = circleCenter + radius;     //圆环的右侧
        oval.bottom = circleCenter + radius;
        oval.top = circleCenter - radius;
        invalidate();
    }

    //设置圆环的直径
    public void setCircleWith(int circleWith) {
        this.circleWith = circleWith;
        circleCenter = circleWith / 2;
        if (roundWidth > circleCenter) {
            roundWidth = circleCenter;
        }
        setRoundWidth(roundWidth);//设置进度条宽度
        sweepGradient = new SweepGradient(this.circleWith / 2, this.circleWith / 2, colors, null);
        Matrix matrix = new Matrix();
        matrix.setRotate(-90, this.circleWith / 2, this.circleWith / 2);//设置矩阵旋转
        sweepGradient.setLocalMatrix(matrix);   //设置矩阵
    }

    //设置目标步数
    public void setTarGet(Float tarGet) {
        this.tarGet = tarGet;
        invalidate();
    }

    //返回默认与修改后的目标步数
    public float getTarGet() {
        return tarGet;
    }
    //初始化控件
    public void initView(){
        circleCenter=circleWith/2;//圆环中心点
        singlPoint=360/maxColorNumber;//两个间隔块之间的距离
        radius= (int) (circleCenter-roundWidth/2);//圆环半径
        sweepGradientInit();          //渐变色初始化
        mPaintText=new Paint();             //刻度文字画笔
        mPaintText.setColor(textColor);    //画笔颜色
        mPaintText.setTextAlign(Paint.Align.CENTER);//排列位置
        mPaintText.setTextSize(textSize);  //文字大小
        mPaintText.setAntiAlias(true);    //开启抗锯齿

        paint=new Paint();
        paint.setColor(roundBackgroundColor);//背景色
        paint.setStyle(Paint.Style.STROKE);//画笔样式
        paint.setStrokeWidth(roundWidth); //画笔宽度
        paint.setAntiAlias(true);//开启抗锯齿
        //圆弧形态的大小界限
        oval=new RectF(circleCenter-radius,circleCenter-radius,circleCenter+radius,circleCenter+radius);
    }
//绘制进度条
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //根据步数计算进度
        float share=360/tarGet;
        //根据步数显示刻度值
        int scale= (int) tarGet;
        //设置背景渐变色
        paint.setShader(sweepGradient);
        //绘制渐变色
        canvas.drawArc(oval,-90,(progress*share),false,paint);
        paint.setShader(null);
        //是否是线条模式
        if(!isLing){
            float start=-90f;      //起始角度
            //计算一步的进度
            float p=maxColorNumber/tarGet;
            //当前步数的进度
            p=progress;
            for (int i = 0; i <p ; i++) {
                paint.setColor(roundBackgroundColor);
                canvas.drawArc(oval,start+singlPoint-lingWidth,lingWidth,false,paint);
                start=start+singlPoint;
                postInvalidate();
            }
        }
        //绘制剩下的空白区域
        paint.setColor(roundBackgroundColor);
        canvas.drawArc(oval,-90,(-(tarGet-progress)*share),false,paint);
        //绘制文字的刻度
        for (int i = 0; i <10; i++) {
            canvas.save();
            canvas.rotate(360/10*i,circleCenter,circleCenter);
            canvas.drawText(i*(scale/10)+"",circleCenter,circleCenter-radius+roundWidth/2+getDpValue(4)+textSize,mPaintText);
            canvas.restore();  //恢复画布

        }

    }
}
