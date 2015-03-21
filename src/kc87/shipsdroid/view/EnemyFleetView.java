package kc87.shipsdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import kc87.shipsdroid.model.AbstractFleetModel;


public class EnemyFleetView extends AbstractFleetView
{
   private static final String LOG_TAG = "EnemyFleetView";

   public EnemyFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }

   /*
      Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];

      Button gridButton = gridButtons[i][j];

      // Just water
      if (gridValue == 0 || gridValue == AbstractFleetModel.MISS) {
         gridButton.setBackground(gridValue == 0 ? mDrawableMap.get("WATER") : mDrawableMap.get("MISS"));
         return;
      }

      if (gridValue == AbstractFleetModel.HIT) {
         gridButton.setBackground(mDrawableMap.get("HIT"));
         return;
      }

      if (gridValue > 0 && gridValue < AbstractFleetModel.NUMBER_OF_SHIPS + 1) {
         gridButton.setBackground(mDrawableMap.get("DESTROYED"));
      }

   }
}
