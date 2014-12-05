package com.icmonroe.calendarview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by Ian Monroe on 10/24/2014.
 *
 * Creates a circle view that can be used as a plain circle,
 * as a binary pie chart, or a rounded image view.
 */
public class AdaptableCircleView extends View {

    public AdaptableCircleView(Context context) {
        super(context);
        sharedConstructor();
    }

    public AdaptableCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor();
    }

    public AdaptableCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        sharedConstructor();
    }

    // Initially does not display percentage
    private float percentage = 0.0f;

    private Paint backgroundPaint;
    private Paint foregroundPaint;
    private Paint imagePaint;
    private RectF paddedRectF;
    private Rect originalRect;
    private BitmapShader shader;
    private float absoluteX;
    private float absoluteY;

    /**
     * Builds the paints for the background, pie fill foreground and image. Rectangles initialized.
     */
    private void sharedConstructor(){
        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Style.FILL);
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setAntiAlias(true);

        foregroundPaint = new Paint();
        foregroundPaint.setStyle(Style.FILL);
        foregroundPaint.setColor(Color.RED);
        foregroundPaint.setAntiAlias(true);

        imagePaint = new Paint();
        imagePaint.setDither(true);
        imagePaint.setAntiAlias(true);
        imagePaint.setFilterBitmap(true);
        imagePaint.setShader(shader);

        paddedRectF = new RectF(0,0,getWidth(),getHeight());
        originalRect = new Rect(0,0,getWidth(),getHeight());
    }

    /**
     * Few modification to onLayout being overridden. Essentially we adjust the rectangle for
     * layout changes and padding adjustments. We also process a user assigned image if here
     * so we can ensure width and height are set before hand.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if(changed){
            paddedRectF.set(
                    getPaddingLeft()+insetPadding,
                    getPaddingTop()+insetPadding,
                    getWidth() - getPaddingRight()-insetPadding,
                    getHeight() - getPaddingBottom()-insetPadding
            );
            if(imageBitmap!=null){
                absoluteX = (getWidth() - imageBitmap.getWidth()) / 2.0f;
                absoluteY = (getHeight() - imageBitmap.getHeight()) / 2.0f;

                shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                imagePaint.setShader(shader);
            }
            originalRect.set(0, 0, getWidth(), getHeight());
        }
    }

    public void setPercentage(float percentage){
        this.percentage = percentage;
        invalidate();
    }

    public void setBackgroundColor(int color){
        backgroundPaint.setColor(color);
        invalidate();
    }

    public void setForegroundColor(int color){
        foregroundPaint.setColor(color);
        invalidate();
    }

    private Handler animationHandler;
    private Runnable animationRunnable;
    private Interpolator animationInterpolator = new DecelerateInterpolator();

    private float timePast;
    private float diff;
    // fps not guaranteed
    private int fps = 60;
    // Time frame takes (in a perfect world, not guaranteed)
    private float frameTime;

    /**
     * Sets the percentage of the circle view with an animation transition for given time
     * @param newPercentage New percentage the pie should file (0.0f - 1.0f);
     * @param animationTime Time animation should take
     */
    public void setPercentage(final float newPercentage, final int animationTime){
        frameTime = 1000/fps;
        timePast = 0;
        diff = newPercentage - percentage;

        // Create a runnable that will represent the actions of a frame
        animationRunnable = new Runnable() {
            @Override
            public void run() {
                timePast += frameTime;
                // The percent of the way done with the animation
                float percentInAnimation = (timePast / animationTime);

                // Use of interpolation can be set (default is Decelaration)
                float interpolationValue =
                        (diff*animationInterpolator.getInterpolation(percentInAnimation))+(newPercentage-diff);
                setPercentage(interpolationValue);

                // If the percent has not completed than call it again at the appropriate time
                if(percentInAnimation<1.0f){
                    animationHandler.postDelayed(animationRunnable, (long) frameTime);
                    // Else set the final percent
                }else{
                    setPercentage(newPercentage);
                }
            }
        };
        // Start initial handler and frame
        animationHandler = new Handler();
        animationHandler.post(animationRunnable);
    }

    public void setInterpolator(Interpolator interpolator){
        animationInterpolator = interpolator;
    }

    private Bitmap imageBitmap;
    private Bitmap squareImageBitmap;

    /**
     * Adds image into queue. We the view and ensure its height and width then it will be
     * processed.
     * @param image bitmap image you want viewed
     * @param round whether to round the image
     */
    public void setImageBitmap(Bitmap image,boolean round){
        imageBitmap = image;
        if(image!=null){
            // Handle fill
            shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imagePaint.setShader(shader);
            // Handle not fill
            absoluteX = (getWidth() - imageBitmap.getWidth()) / 2.0f;
            absoluteY = (getHeight() - imageBitmap.getHeight()) / 2.0f;

            invalidate();
        }
    }

    /**
     * @return largest padding in view
     */
    private int largestPadding(){
        return Math.max(Math.max(getPaddingLeft(), getPaddingRight()), Math.max(getPaddingTop(), getPaddingRight()));
    }

    int imagePadding = 0;

    public void setImagePadding(int imagePadding){
        this.imagePadding = imagePadding;
        invalidate();
    }

    public enum IMAGE_TYPE{
        FILL,
        CENTER_CROP,
        ABSOLUTE
    }

    IMAGE_TYPE imageType = IMAGE_TYPE.CENTER_CROP;

    public void setImageType(IMAGE_TYPE type){
        imageType = type;
        shader = new BitmapShader(renderSquareImage(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        imagePaint.setShader(shader);
        invalidate();
    }

    private Bitmap renderSquareImage(){
        if(getWidth()>0 && getHeight()>0){
            /*
            squareImageBitmap = ThumbnailUtils.extractThumbnail(imageBitmap,getWidth(),getHeight());
            if(imagePadding>0) {
                squareImageBitmap = padBitmap(squareImageBitmap, imagePadding);
            }
            */
            squareImageBitmap = padBitmap(imageBitmap,imagePadding);
            return squareImageBitmap;
        }
        return imageBitmap;
    }

    private Bitmap padBitmap(Bitmap bitmap,int padding){
        Bitmap paddedBitmap = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawBitmap(
                bitmap,
                imageType== IMAGE_TYPE.CENTER_CROP ? squareImageRect(bitmap) : new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),
                new RectF(padding,padding,getWidth()-padding,getHeight()-padding),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        return paddedBitmap;
    }


    private Rect squareImageRect(Bitmap bitmap){
        int halfMin = Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2;
        int left = (bitmap.getWidth()/2) - halfMin;
        int top = (bitmap.getHeight()/2) - halfMin;
        int right = (bitmap.getWidth()/2) + halfMin;
        int bottom = (bitmap.getHeight()/2) + halfMin;
        return new Rect(left,top,right,bottom);
    }

    private int insetPadding = 0;

    public void setInsetPadding(int padding){
        insetPadding = padding;
        paddedRectF.set(
                getPaddingLeft()+insetPadding,
                getPaddingTop()+insetPadding,
                getWidth() - getPaddingRight()-insetPadding,
                getHeight() - getPaddingBottom()-insetPadding
        );
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Draw background circle
        canvas.drawCircle(getWidth()/2,getHeight()/2,getWidth()/2 - largestPadding(),backgroundPaint);
        // Draw arc (pie thing) based on current percentage
        float sweepValue = 360f * percentage;
        canvas.drawArc(paddedRectF,270f,sweepValue,true,foregroundPaint);
        // Draw image if need be
        if(imageBitmap!=null) {
            if(imageType!= IMAGE_TYPE.ABSOLUTE) canvas.drawCircle(getWidth()/2 ,getHeight()/2,getWidth()/2 - largestPadding() - insetPadding, imagePaint);
            else canvas.drawBitmap(imageBitmap, absoluteX, absoluteY, imagePaint);
        }
    }

    /* Volley required
    public void setImageUrl(String imageUrl,ImageLoader imageLoader){
        imageLoader.get(imageUrl,new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean isImmediate) {
                setImageBitmap(imageContainer.getBitmap(),false);
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
            	setBackgroundColor(getResources().getColor(R.color.gray));
            }
        });
    }
    */

    public void clearImage() {
        imageBitmap = null;
        invalidate();
    }
}