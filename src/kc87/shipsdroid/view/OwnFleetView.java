package kc87.shipsdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import kc87.shipsdroid.model.AbstractFleetModel;
import kc87.shipsdroid.model.Ship;


public class OwnFleetView extends AbstractFleetView
{
   public OwnFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   /*
    *  Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      final int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];
      final Button gridButton = mGridButtons[i][j];

      if (gridValue == 0) {
         gridButton.setBackground(mDrawableMap.get("WATER"));
         return;
      }

      if (gridValue == AbstractFleetModel.MISS) {
         gridButton.setBackground(mDrawableMap.get("MISS"));
         return;
      }

      if (gridValue > 0) {
         Ship ship = fleetModel.getShips()[gridValue - 1];
         if(ship.isDestroyed()){
            animateTile(gridButton,"DESTROYED",mFlipAnimation);
         }else {
            gridButton.setBackground(mDrawableMap.get("SHIP"));
         }
         return;
      }

      if (gridValue < 0) {
         animateTile(gridButton,"HIT",mSplashAnimation);
      }
   }
}
