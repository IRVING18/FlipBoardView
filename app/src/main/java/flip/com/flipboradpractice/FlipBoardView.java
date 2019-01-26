package flip.com.flipboradpractice;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class FlipBoardView extends View {
    private Paint  paint;
    private Camera mCamera;
    private float  centerX;
    private float  centerY;

    private float bitmapWidth;
    private float bitmapHeight;

    //第一步抬起的角度
    private float degreeY;
    //第二步旋转的角度
    private float degreeZ;
    //第三步抬起的角度
    private float degreeLast;

    private Bitmap      bitmap;
    private AnimatorSet animatorSet;


    public void setDegreeY(float degreeY) {
        this.degreeY = degreeY;
        invalidate();
    }

    public void setDegreeZ(float degreeZ) {
        this.degreeZ = degreeZ;
        invalidate();
    }

    public void setDegreeLast(float degreeLast) {
        this.degreeLast = degreeLast;
        invalidate();
    }

    public FlipBoardView(Context context) {
        super(context);
    }

    public FlipBoardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FlipBoardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCamera = new Camera();

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.flip_board);

        //第一步抬起动画
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(this, "degreeY", 0, -45);
        animator1.setDuration(1000);

        //旋转动画
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(this, "degreeZ", 0, -270);
        animator2.setDuration(1000);

        //第三步抬起动画
        ObjectAnimator animator3 = ObjectAnimator.ofFloat(this,"degreeLast",0,45);
        animator3.setDuration(1000);

        animatorSet = new AnimatorSet();
        animatorSet.playSequentially(animator1, animator2,animator3);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                reset();
                animatorSet.start();
            }
        });


        //修正
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float newZ = -displayMetrics.density * 6;
        mCamera.setLocation(0, 0, newZ);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //分析动画：
        //方法：可以解开动图然后观察
        //1. 第一步肯定是抬起右边。
        //2. 让这个状态围着Z轴旋转。但是还不能让图旋转
        //3. 旋转270度之后，抬起上部分。

        //实现思路：
        //一、一半是翻起的状态
        //1. 用camera实现抬起右边，围着Y轴转-45度，就可以抬起
        //2. 最重要的一步，画抬起的一半，首先思路是这样的，就像移动坐标原点一样，这个抬起的状态旋转但是图片不旋转的效果也可以这么实现。
        //2.1 先让canvas围着Z轴，也就是平面旋转，然后旋转的时候把camera投影到canvas，然后顺便剪裁右边。因为旋转的是坐标轴，所以只剪裁右边就ok。
        //2.2 这时候不做操作那就是这个图转了，但是神奇的地方来了。这时候可以让canvas围着Z轴旋转回来。然后图片就没有旋转，但是抬起状态却保留下来了。
        //二、另一半是不动的
        //3. 第三步画不动的一半。思路和第二步类似了。

        //注意：
        //camera使用时需要矫正


        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();

        float bpLeft = centerX - bitmapWidth / 2;
        float bpTop = centerY - bitmapHeight / 2;

        //画变化一半
        canvas.save();
        mCamera.save();
        //移动到中心点
        canvas.translate(centerX, centerY);
        //围着Z轴旋转
        canvas.rotate(degreeZ);

        //围着Y轴旋转，抬起右边
        mCamera.rotateY(degreeY);
        //投影到canvas
        mCamera.applyToCanvas(canvas);

        //裁剪右边,此时的坐标轴在centerX,centerY处
        canvas.clipRect(0, -centerY, centerX, centerY);

        //旋转回来，图片转回来但是抬起状态会保留在原来的坐标轴处
        canvas.rotate(-degreeZ);
        //平移回来
        canvas.translate(-centerX, -centerY);

        //画bitmap
        canvas.drawBitmap(bitmap, bpLeft, bpTop, paint);
        mCamera.restore();
        canvas.restore();


        //画不动的一半
        canvas.save();
        mCamera.save();
        canvas.translate(centerX, centerY);
        //和变化的一半一样也是旋转，然后剪切另一半
        canvas.rotate(degreeZ);
        //剪切不变化的一半,此时坐标轴原点在centerX,centerY
        canvas.clipRect(-centerX, -centerY, 0, centerY);

        //最终抬起动作
        //因为设置动画的时候已经是degreeZ为270,才会执行第三个抬起动作动画，所以这是的坐标系已经变化了，横着的就是Y轴
        mCamera.rotateY(degreeLast);
        mCamera.applyToCanvas(canvas);

        canvas.rotate(-degreeZ);
        canvas.translate(-centerX, -centerY);

        canvas.drawBitmap(bitmap, bpLeft, bpTop, paint);
        mCamera.restore();
        canvas.restore();

    }


    public void reset() {
        degreeLast = 0;
        degreeY = 0;
        degreeZ = 0;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animatorSet.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        animatorSet.end();
        animatorSet.cancel();
    }
}
