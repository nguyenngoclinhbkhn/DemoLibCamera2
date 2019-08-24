package com.cpr.lib_camera2.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class FocusView extends View {
    private Paint paintView;
    private int color = Color.WHITE;


    private void init(){
        paintView = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintView.setStyle(Paint.Style.STROKE);
        paintView.setStrokeCap(Paint.Cap.ROUND);
        paintView.setColor(color);
        paintView.setStrokeWidth(3f);
    }
    public FocusView(Context context) {
        super(context);
        init();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusView(Context context,AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLine(0, 0, getWidth(), 0, paintView);
        canvas.drawLine(0,0, 0, getHeight(), paintView);
        canvas.drawLine(getWidth(), 0, getWidth(), getHeight(), paintView);
        canvas.drawLine(getWidth(), getHeight(), 0, getHeight(), paintView);
    }

    public void setColor(int color){
        paintView.setColor(color);
        invalidate();
    }
}
