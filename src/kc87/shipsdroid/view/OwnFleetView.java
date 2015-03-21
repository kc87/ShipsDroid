package kc87.shipsdroid.view;

import android.content.Context;
import android.util.AttributeSet;
import kc87.shipsdroid.model.AbstractFleetModel;
import kc87.shipsdroid.model.Ship;


public class OwnFleetView extends AbstractFleetView
{
   public OwnFleetView(Context context, AttributeSet attrs)
   {
      super(context, attrs);
   }


   /*
   public OwnFleetView(Context context, ViewGroup boardView, int size)
   {
      super(context, boardView, null, size);
   }*/


   /*
      Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];

      // Just water
      if (gridValue == 0 || gridValue == AbstractFleetModel.MISS) {
         gridButtons[i][j].setBackground(gridValue == 0 ? mDrawableMap.get("WATER") : mDrawableMap.get("MISS"));
         return;
      }

      // Ship is undamaged or destroyed
      if (gridValue > 0) {
         Ship ship = fleetModel.getShips()[gridValue - 1];
         gridButtons[i][j].setBackground(ship.isDestroyed() ? mDrawableMap.get("DESTROYED") : mDrawableMap.get("SHIP"));
         return;
      }

      // Ship is partially damaged
      if (gridValue < 0) {
         gridButtons[i][j].setBackground(mDrawableMap.get("HIT"));
      }
   }
}
