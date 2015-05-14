package de.tudarmstadt.tk.carsensing.activity.view;

import java.text.DecimalFormat;

import de.tudarmstadt.tk.carsensing.R;
import de.tudarmstadt.tk.carsensing.activity.LiveDataActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Shader.TileMode;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class TextGauge extends View implements OnClickListener{

	public static final int NO_CONNECTION = 0;
	public static final int CONNECTED = 1;
	public static final int DATA = 2;


	private int mRPMValue;
	private RectF mOuterRimRect;
	private RectF mBorderLightRect;
	private RectF mBorderDarkRect;
	private RectF mTextDisplayRect;
	private RectF mStatusRect;
	private Bitmap mBackground;
	private Paint mBackgroundPaint;
	private int mStatusInt=NO_CONNECTION;
	private String mSpeedText="-";
	private String mMAFText="-";
	private String mInTempText="-";
	private String mCoolTempText="-";
	private boolean metricUnit = true;
	
	

	private final String TAG = "TextGauge";
	public static final float TOP = 0.0f;
	public static final float LEFT = 0.0f;
	public static final float RIGHT = 1.0f;
	public static final float BOTTOM = 1.0f;
	public static final float CENTER = 0.5f;

	public static final float INNER_RIM_WIDTH = 0.03f;
	public static final float INNER_RIM_BORDER_WIDTH = 0.005f;
	public TextGauge(Context context) {
		this(context, null, 0);


	}

	public TextGauge(Context context, AttributeSet attrs) {
		this(context, attrs, 0);


	}

	public TextGauge(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();

	}

	private void init() {
		Log.v(TAG,"Init RMPText View");

		mBackgroundPaint = new Paint();
		mBackgroundPaint.setFilterBitmap(true);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
		this.setOnClickListener(this);
		mOuterRimRect = new RectF(LEFT,TOP,RIGHT,BOTTOM);
		mBorderDarkRect = new RectF(mOuterRimRect.left+INNER_RIM_WIDTH,mOuterRimRect.top+INNER_RIM_WIDTH,mOuterRimRect.right-INNER_RIM_WIDTH,mOuterRimRect.bottom-INNER_RIM_WIDTH);
		mBorderLightRect = new RectF(mBorderDarkRect.left-INNER_RIM_BORDER_WIDTH, mBorderDarkRect.top - INNER_RIM_BORDER_WIDTH, mBorderDarkRect.right + INNER_RIM_BORDER_WIDTH, mBorderDarkRect.bottom + INNER_RIM_BORDER_WIDTH);
		mTextDisplayRect = new RectF(mBorderDarkRect.left,mBorderDarkRect.top,mBorderDarkRect.right, mBorderDarkRect.bottom );
		mStatusRect = new RectF(mTextDisplayRect.centerX()+0.15f,0.10f,mTextDisplayRect.centerX()+0.30f,0.13f);
		

	}

	@Override
	protected void onDraw(final Canvas canvas) {
		
		
		canvas.scale(getWidth(), getHeight());

		drawTextGauge(canvas);



	}

	private void drawBackground(final Canvas canvas) {
		if (null != mBackground) {
			canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);
		}
	}


	private void drawTextGauge(Canvas canvas) {
		
		float radius = 0.05f;

		canvas.drawRoundRect(mOuterRimRect, radius,radius, getDefaultOuterRimPaint());
		canvas.drawRoundRect(mBorderDarkRect, radius, radius, getDefaultInnerRimBorderDarkPaint());
		canvas.drawRoundRect(mBorderLightRect, radius, radius, getDefaultInnerRimBorderLightPaint());
		canvas.drawRoundRect(mTextDisplayRect, 0.05f, 0.05f, getDefaultTextDisplayPaint());
		canvas.drawLine(mTextDisplayRect.centerX(), 0.20f, mTextDisplayRect.centerX(), 0.80f, getLinePaint());
		//Left side of the gauge
		float textsize = 0.08f;
		canvas.drawText("STATUS", mTextDisplayRect.centerX()-0.025f, 0.15f, getDefaultTextValuePaint(Align.RIGHT,textsize));
		canvas.drawText("SPEED", mTextDisplayRect.centerX()-0.025f, 0.35f, getDefaultTextValuePaint(Align.RIGHT,textsize));
		canvas.drawText("MAF", mTextDisplayRect.centerX()-0.025f, 0.45f, getDefaultTextValuePaint(Align.RIGHT,textsize));
		canvas.drawText("IN TEMP", mTextDisplayRect.centerX()-0.025f, 0.55f, getDefaultTextValuePaint(Align.RIGHT,textsize));
		canvas.drawText("COOL TEMP", mTextDisplayRect.centerX()-0.025f, 0.65f, getDefaultTextValuePaint(Align.RIGHT,textsize));
		
		//Right side of the gauge - Dynamic text
		canvas.drawRect(mStatusRect, getStatusPaint(mStatusInt));
		canvas.drawText(mSpeedText, mTextDisplayRect.centerX()+0.025f, 0.35f, getDefaultTextValuePaint(Align.LEFT,textsize));
		canvas.drawText(mMAFText, mTextDisplayRect.centerX()+0.025f, 0.45f, getDefaultTextValuePaint(Align.LEFT,textsize));
		canvas.drawText(mInTempText, mTextDisplayRect.centerX()+0.025f, 0.55f, getDefaultTextValuePaint(Align.LEFT,textsize));
		canvas.drawText(mCoolTempText, mTextDisplayRect.centerX()+0.025f, 0.65f, getDefaultTextValuePaint(Align.LEFT,textsize));
		

	}
	
	


	public void setStatus(int status){
		mStatusInt = status;
		invalidate();
	}
	public void setSpeed(String speed){
		
		mSpeedText = speed;
		invalidate();
	}
	public void setMAF(String maf){
		mMAFText = maf;
		invalidate();
	}
	
	public void setInTemp(String temp){
		mInTempText = temp;
		invalidate();
	}
	
	public void setCoolTemp(String temp){
		mCoolTempText = temp;
		invalidate();
	}

	public void setValue(int rpmvalue) {
		mRPMValue = rpmvalue;

	}

	private Paint getStatusPaint(int status){
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		switch (status){
		case CONNECTED: paint.setColor(Color.YELLOW);
		break;
		case DATA: paint.setColor(Color.GREEN);
		break;
		default:
		case NO_CONNECTION: paint.setColor(Color.RED);
		}
		return paint;
	}
	private Paint getLinePaint(){
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.argb(40, 255, 255, 255));
		paint.setStrokeWidth(0.002f);

		return paint;

	}
	private Paint getDefaultTextDisplayPaint() {
		final Paint paint = new Paint (Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.BLACK);
		int[] colorrange = new int[]{Color.rgb(30, 30, 30),Color.rgb(50, 50, 50),Color.rgb(10, 10, 10)};
		float[] colorpos = new float[]{0.0f,0.5f,1.0f};



		final LinearGradient reflectionGradient = new LinearGradient(mTextDisplayRect.left, mTextDisplayRect.top, mTextDisplayRect.right,
				mTextDisplayRect.bottom, colorrange,colorpos, TileMode.CLAMP);
		final LinearGradient reflectionGradientUniform = new LinearGradient(mTextDisplayRect.left, mTextDisplayRect.top, mTextDisplayRect.right,
				mTextDisplayRect.bottom, Color.rgb(10,10,10),Color.rgb(30,30,30), TileMode.CLAMP);
		paint.setShader(new ComposeShader(reflectionGradient,reflectionGradientUniform,PorterDuff.Mode.MULTIPLY));
		paint.setShader(reflectionGradient);
		return paint;
	}

	public Paint getDefaultTextValuePaint(Align align,float size) {
		Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.WHITE);
		paint.setStyle(Paint.Style.FILL);
		//paint.setStrokeWidth(0.01f);
		paint.setTextSize(size);
		paint.setTextAlign(align);
		paint.setTypeface(Typeface.MONOSPACE);
		// paint.setTextScaleX(1.2f);
		// paint.setShadowLayer(0.01f, 0.002f, 0.002f, Color.BLACK);
		return paint;
	}



	public Paint getDefaultOuterRimPaint() {

		final LinearGradient verticalGradient = new LinearGradient(mOuterRimRect.left, mOuterRimRect.top, mOuterRimRect.right,
				mOuterRimRect.bottom, Color.rgb(255, 255, 255), Color.rgb(84, 90, 100), TileMode.REPEAT);


		final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.light_alu);
		final BitmapShader aluminiumTile = new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
		final Matrix matrix = new Matrix();
		matrix.setScale(1.0f / bitmap.getWidth(), 1.0f / bitmap.getHeight());
		aluminiumTile.setLocalMatrix(matrix);

		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setShader(new ComposeShader(verticalGradient, aluminiumTile, PorterDuff.Mode.MULTIPLY));
		paint.setFilterBitmap(true);
		return paint;
	}
	private Paint getDefaultInnerRimBorderLightPaint() {
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.argb(100, 255, 255, 255));
		paint.setStrokeWidth(0.005f);
		return paint;
	}

	private Paint getDefaultInnerRimBorderDarkPaint() {
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.argb(100, 81, 84, 89));
		paint.setStrokeWidth(0.005f);
		return paint;
	}

	@Override
	public void onClick(View v) {
		Log.v(TAG,"Clicked");
		if(metricUnit){
			metricUnit = false;
			LiveDataActivity.tempUnit = LiveDataActivity.UNIT_CELSIUS;
			LiveDataActivity.speedUnit = LiveDataActivity.UNIT_KPH;
		}else{
			metricUnit = true;
			LiveDataActivity.tempUnit = LiveDataActivity.UNIT_FAHRENHEIT;
			LiveDataActivity.speedUnit = LiveDataActivity.UNIT_MPH;
			
		}
		
		
	}

}
