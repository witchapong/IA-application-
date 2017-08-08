package com.indooratlas.android.sdk.examples.imageview;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.indooratlas.android.sdk.examples.R;

/**
 * Extends great ImageView library by Dave Morrissey. See more:
 * https://github.com/davemorrissey/subsampling-scale-image-view.
 */
public class BlueDotView extends SubsamplingScaleImageView{


    private static final String TAG = "IndoorAtlasExample";
    private float radius = 1.0f;
    private PointF dotCenter = null;

    private float x1,y1;
    private boolean firstTouchEvent = false;

    int width,height;

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setDotCenter(PointF dotCenter) {
        this.dotCenter = dotCenter;
    }

    public void invokeFirstTouch() {
        this.firstTouchEvent = true;
    }

    public boolean getTouchStatus() {
        return this.firstTouchEvent;
    }

    public void setTouchedCoordinate(float x,float y) {
        this.x1 = x;
        this.y1 = y;
    }

    public BlueDotView(Context context) {
        this(context, null);
    }

    public BlueDotView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {

        setWillNotDraw(false);
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isReady()) {
            return;
        }

        if (dotCenter != null) {
            PointF vPoint = sourceToViewCoord(dotCenter);
            float scaledRadius = getScale() * radius;
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(getResources().getColor(R.color.ia_blue));
            canvas.drawCircle(vPoint.x, vPoint.y, scaledRadius, paint);
        }

        if (firstTouchEvent != false) {
            Bitmap marker_ori = BitmapFactory.decodeResource(getResources(),
                    R.drawable.checkin_icon);
            Bitmap marker_res = getResizedBitmap(marker_ori, 100, 100);
            canvas.drawBitmap(marker_res, x1-50, y1-50, null);
            PointF vPoint = sourceToViewCoord(dotCenter);
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            canvas.drawLine(x1,y1,vPoint.x,vPoint.y,paint);
        }

    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        width = bm.getWidth();
        height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
