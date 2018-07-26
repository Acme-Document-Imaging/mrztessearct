package com.cbms.tesseractdemo;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class CameraOverlayView extends View {
    private Paint paint = new Paint();
    public CameraOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //https://stackoverflow.com/questions/48642695/capture-image-using-dynamic-co-ordinates-through-the-camera
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);

        //center
        int x0 = canvas.getWidth()/2;
        int y0 = canvas.getHeight()/2;
        int dx = canvas.getHeight()/3;
        int dy = canvas.getHeight()/3;
        //draw guide box
        canvas.drawRect(x0-dx, y0-dy, x0+dx, y0+dy, paint);

//        int h = getHeight();
//        int w = getWidth();
//        int sq = Math.min(h,w);
//        int l = (w-sq)/2;
//        int t = (h-sq)/2;
//        int r = l+sq;
//        int b = t+sq;
//        Rect innerRect = new Rect(l,t,r,b);
//        Rect outerRect = new Rect(0,0,w,h);
//
//
//        Rect above  = new Rect(outerRect.left,  outerRect.top,    innerRect.right, innerRect.top);
//        Rect left   = new Rect(outerRect.left,  innerRect.top,    innerRect.left,  innerRect.bottom);
//        Rect right  = new Rect(innerRect.right, innerRect.top,    outerRect.right, innerRect.bottom);
//        Rect bottom = new Rect(outerRect.left,  innerRect.bottom, outerRect.right, outerRect.bottom);
//
//        Paint paint = new Paint();
//        paint.setStyle(Paint.Style.FILL_AND_STROKE);
//        paint.setColor(Color.BLACK);
//        canvas.drawRect(above, paint);
//        canvas.drawRect(left, paint);
//        canvas.drawRect(right, paint);
//        canvas.drawRect(bottom, paint);
    }

}