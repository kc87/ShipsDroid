package kc87.shipsdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import kc87.shipsdroid.GamePresenter;
import kc87.shipsdroid.model.AbstractFleetModel;


public class EnemyFleetView extends AbstractFleetView
{
   private GamePresenter mGamePresenter;

   public EnemyFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
      mGridButtonHandler = new View.OnClickListener()
      {
         @Override
         public void onClick(final View view)
         {
            if (mIsEnabled) {
               mGamePresenter.enemyFleetViewOnClick(view.getId());
            }
         }
      };
   }

   /*
    *  Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      final int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];
      final Button gridButton = mGridButtons[i][j];

      switch (gridValue) {
         case 0:
            gridButton.setBackground(mDrawableMap.get("WATER"));
            break;
         case AbstractFleetModel.MISS:
            gridButton.setBackground(mDrawableMap.get("MISS"));
            break;
         case AbstractFleetModel.HIT:
            animateTile(gridButton,"HIT",mSplashAnimation);
            break;
         default:
            if (gridValue > 0 && gridValue < AbstractFleetModel.NUMBER_OF_SHIPS + 1) {
               animateTile(gridButton,"DESTROYED",mFlipAnimation);
            }
            break;
      }
   }

   public void setPresenter(final GamePresenter presenter)
   {
      mGamePresenter = presenter;
   }
}
