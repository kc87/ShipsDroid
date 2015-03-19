package c4.subnetzero.shipsdroid.view;


import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import c4.subnetzero.shipsdroid.GameActivity;
import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;
import c4.subnetzero.shipsdroid.model.SeaArea;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFleetView extends LinearLayout implements AbstractFleetModel.ModelUpdateListener
{
   private static final String LOG_TAG = "AbstractFleetView";

   private volatile boolean isEnabled = true;
   private View.OnClickListener mGridButtonHandler = null;

   protected static final int DIM = SeaArea.DIM;
   protected Button[][] gridButtons = new Button[DIM][DIM];
   protected Context mContext;
   protected ViewGroup mBoardView;
   protected Animation mFadeInAnimation;
   protected Animation mFadeOutAnimation;
   protected Map<String, Drawable> mDrawableMap = new HashMap<>();

   public AbstractFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      mContext = context;
      // TODO: This is UGLY! Fix this!!
      mGridButtonHandler = ((GameActivity)mContext).getGridButtonHandler();
   }

   @Override
   protected void onFinishInflate()
   {
      super.onFinishInflate();
      mBoardView = (ViewGroup)this.getChildAt(0);
   }

   @Override
   protected void onSizeChanged (int w, int h, int oldw, int oldh)
   {
      Log.d(LOG_TAG, "onSizeChanged: w=" + w + " h=" + h + " oldw=" + oldw + " oldh=" + oldh);
      super.onSizeChanged(w,h,oldw,oldh);

      if(oldh+oldw == 0){
        setupFleetView(Math.min(w,h)-30);
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
         if (flag == AbstractFleetModel.MISS || flag == AbstractFleetModel.HIT) {
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
      if (!newGame && isEnabled == enable) {
         return;
      }

      isEnabled = enable;

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

   private void setEnabledOnUi(final boolean enable)
   {
      mBoardView.startAnimation(!enable ? mFadeOutAnimation : mFadeInAnimation);
   }

   private void setupFleetView(final int pixelSize)
   {
      Log.d(LOG_TAG, "setupFleetView()");

      DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
      mBoardView.setLayoutParams(new LinearLayout.LayoutParams(pixelSize, pixelSize));

      mFadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
      mFadeOutAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);

      mDrawableMap.put("WATER", mContext.getResources().getDrawable(R.drawable.water));
      mDrawableMap.put("SHIP", mContext.getResources().getDrawable(R.drawable.ship));
      mDrawableMap.put("MISS", mContext.getResources().getDrawable(R.drawable.miss));
      mDrawableMap.put("HIT", mContext.getResources().getDrawable(R.drawable.hit));
      mDrawableMap.put("DESTROYED", mContext.getResources().getDrawable(R.drawable.destroyed));
      mDrawableMap.put("DISABLED_BOARD", mContext.getResources().getDrawable(R.drawable.gray_board_bg));
      mDrawableMap.put("ENABLED_BOARD", mContext.getResources().getDrawable(R.drawable.green_board_bg));

      gridButtons = new Button[DIM][DIM];

      for (int i = 0, index = 0; i < mBoardView.getChildCount(); i++) {
         View child = mBoardView.getChildAt(i);
         if (child instanceof TableRow) {
            for (int j = 0; j < ((ViewGroup) child).getChildCount(); j++) {
               View childChild = ((ViewGroup) child).getChildAt(j);
               if (childChild instanceof Button) {
                  int col = index % DIM;
                  int row = index / DIM;
                  Button gridButton = (Button) childChild;
                  gridButton.setId(index);
                  gridButton.setOnClickListener(mGridButtonHandler);
                  gridButton.setTextSize(pixelSize / (35 * metrics.scaledDensity));
                  gridButtons[col][row] = gridButton;
                  index++;
               }
               if (childChild instanceof TextView) {
                  TextView scaleText = (TextView) childChild;
                  scaleText.setTextSize(pixelSize / (25 * metrics.scaledDensity));
               }
            }
         }
      }

      resetSeaGrid();
   }

   public void resetSeaGrid()
   {
      for (int j = 0; j < DIM; j++) {
         for (int i = 0; i < DIM; i++) {
            gridButtons[i][j].setBackground(mDrawableMap.get("WATER"));
            gridButtons[i][j].setText(null);
         }
      }
   }

}
