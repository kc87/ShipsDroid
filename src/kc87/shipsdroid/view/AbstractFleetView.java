package kc87.shipsdroid.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import kc87.shipsdroid.R;
import kc87.shipsdroid.model.AbstractFleetModel;
import kc87.shipsdroid.model.SeaArea;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFleetView extends TableLayout implements AbstractFleetModel.ModelUpdateListener
{
   private static final String LOG_TAG = "AbstractFleetView";
   private static final int DEFAULT_BOARD_SIZE = 300;
   private static final int SCALE_TEXT_SIZE_FACTOR = 25;
   private TypedArray mAttributesArray;
   private TextView[] mScaleViews;

   protected static final int DIM = SeaArea.DIM;
   protected Button[][] mGridButtons = new Button[DIM][DIM];
   protected Context mContext;
   protected Animation mFadeInAnimation;
   protected Animation mFadeOutAnimation;
   protected Animation[] mFlipAnimation;
   protected Animation[] mSplashAnimation;
   protected Map<String, Drawable> mDrawableMap = new HashMap<>();
   protected volatile boolean mIsEnabled = true;
   protected View.OnClickListener mGridButtonHandler = null;

   public AbstractFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      mContext = context;
      mAttributesArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AbstractFleetView, 0, 0);
   }

   /*
    * Force square shape of the board
    */

   @Override
   protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
   {
      final int specModeW  = MeasureSpec.getMode(widthMeasureSpec);
      final int specModeH  = MeasureSpec.getMode(heightMeasureSpec);
      final int specWidth  = MeasureSpec.getSize(widthMeasureSpec);
      final int specHeight = MeasureSpec.getSize(heightMeasureSpec);

      // Make sure we always show up
      int boardSize = DEFAULT_BOARD_SIZE;

      Log.d(LOG_TAG,"onMeasure() spec width -> "+specModeToString(specModeW)+":"+specWidth+
                                " spec height -> "+specModeToString(specModeH)+":"+specHeight);

      // Obey size constrains and be square
      if(specWidth*specHeight != 0) {
         boardSize = Math.min(specWidth, specHeight);
      }

      // super takes care of calling setMeasuredDimension()
      super.onMeasure(MeasureSpec.makeMeasureSpec(boardSize,MeasureSpec.EXACTLY),
                      MeasureSpec.makeMeasureSpec(boardSize,MeasureSpec.EXACTLY));
   }

   @Override
   protected void onFinishInflate()
   {
      super.onFinishInflate();
      setupFleetView();
   }

   @Override
   protected void onSizeChanged (int w, int h, int oldw, int oldh)
   {
      Log.d(LOG_TAG, "onSizeChanged: w=" + w + " h=" + h + " oldw=" + oldw + " oldh=" + oldh);
      super.onSizeChanged(w,h,oldw,oldh);

      if(oldh+oldw == 0){
         setScaleTextSize(w > h ? h : w);
      }
   }

   public abstract void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j);

   public void updatePartialView(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
         updatePartialViewOnUi(fleetModel, i, j);
      } else {
         ((Activity) mContext).runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               updatePartialViewOnUi(fleetModel, i, j);
            }
         });
      }
   }

   public void updateTotalView(final AbstractFleetModel model)
   {
      for (int j = 0; j < DIM; j++) {
         for (int i = 0; i < DIM; i++) {
            updatePartialView(model, i, j);
         }
      }
   }

   @Override
   public void onPartialUpdate(final AbstractFleetModel model, final int i, final int j, final int flag)
   {
      if (flag != AbstractFleetModel.AGAIN) {
         if (flag == AbstractFleetModel.MISS || flag == AbstractFleetModel.HIT ||
                 flag == AbstractFleetModel.DESTROYED) {
            updatePartialView(model, i, j);
         } else {
            onTotalUpdate(model);
         }
      }
   }

   @Override
   public void onTotalUpdate(final AbstractFleetModel model)
   {
      if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
         updateTotalView(model);
      } else {
         ((Activity) mContext).runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               updateTotalView(model);
            }
         });
      }
   }


   public void setEnabled(final boolean enable, final boolean newGame)
   {
      if (!newGame && mIsEnabled == enable) {
         return;
      }

      mIsEnabled = enable;

      if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
         setEnabledOnUi(enable);
      } else {
         ((Activity) mContext).runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               setEnabledOnUi(enable);
            }
         });
      }
   }

   protected void animateTile(final Button tile,final String drawableMapKey,final Animation[] inOutAnimation)
   {
      tile.postOnAnimationDelayed(new Runnable()
      {
         @Override
         public void run()
         {
            tile.setBackground(mDrawableMap.get(drawableMapKey));
            tile.startAnimation(inOutAnimation[1]);
         }
      },200);

      tile.startAnimation(inOutAnimation[0]);
   }


   private void setEnabledOnUi(final boolean enable)
   {
      startAnimation(!enable ? mFadeOutAnimation : mFadeInAnimation);
   }

   private void setupFleetView()
   {
      Log.d(LOG_TAG, "setupFleetView()");

      mFlipAnimation = new Animation[2];
      mSplashAnimation = new Animation[2];

      mFlipAnimation[0] = AnimationUtils.loadAnimation(mContext, R.anim.shrink_x);
      mFlipAnimation[1] = AnimationUtils.loadAnimation(mContext, R.anim.grow_x);
      mSplashAnimation[0] = AnimationUtils.loadAnimation(mContext, R.anim.shrink);
      mSplashAnimation[1] = AnimationUtils.loadAnimation(mContext, R.anim.grow);

      mFadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
      mFadeOutAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);

      mDrawableMap.put("WATER",mAttributesArray.getDrawable(R.styleable.AbstractFleetView_waterTile));
      mDrawableMap.put("SHIP",mAttributesArray.getDrawable(R.styleable.AbstractFleetView_shipTile));
      mDrawableMap.put("HIT",mAttributesArray.getDrawable(R.styleable.AbstractFleetView_hitTile));
      mDrawableMap.put("MISS",mAttributesArray.getDrawable(R.styleable.AbstractFleetView_missTile));
      mDrawableMap.put("DESTROYED", mAttributesArray.getDrawable(R.styleable.AbstractFleetView_destroyedTile));

      int scaleTextColor = mAttributesArray.getColor(R.styleable.AbstractFleetView_scaleTextColor,0);

      mAttributesArray.recycle();

      mGridButtons = new Button[DIM][DIM];
      mScaleViews = new TextView[4*DIM];

      for (int i = 0, k = 0, index = 0; i < getChildCount(); i++) {
         View child = getChildAt(i);
         if (child instanceof TableRow) {
            for (int j = 0; j < ((ViewGroup) child).getChildCount(); j++) {
               View childOfChild = ((ViewGroup) child).getChildAt(j);
               if (childOfChild instanceof Button) {
                  int col = index % DIM;
                  int row = index / DIM;
                  Button gridButton = (Button) childOfChild;
                  gridButton.setId(index);
                  gridButton.setOnClickListener(mGridButtonHandler);
                  gridButton.setBackground(mDrawableMap.get("WATER"));
                  mGridButtons[col][row] = gridButton;
                  index++;
                  continue;
               }
               // Collect all scale text views so we can adjust the size later
               if (childOfChild instanceof TextView) {
                  TextView scaleTextView = (TextView) childOfChild;
                  scaleTextView.setTextColor(scaleTextColor);
                  mScaleViews[k++] = scaleTextView;
               }
            }
         }
      }
   }

   private void setScaleTextSize(final int pixelSize)
   {
      Log.d(LOG_TAG, "setScaleTextSize():"+pixelSize);
      final float scaledDensity = mContext.getResources().getDisplayMetrics().scaledDensity;

      for(TextView scaleView: mScaleViews){
         scaleView.setTextSize(pixelSize / (SCALE_TEXT_SIZE_FACTOR * scaledDensity));
      }
   }

   private String specModeToString(int mode)
   {
      switch(mode) {
         case MeasureSpec.EXACTLY: return "EXACTLY";
         case MeasureSpec.UNSPECIFIED: return "UNSPECIFIED";
         case MeasureSpec.AT_MOST: return "AT_MOST";
         default: return "???";
      }
  }

}
