package kc87.shipsdroid.model;

public class EnemyFleetModel extends AbstractFleetModel
{
   public void update(final int i, final int j, final int resultFlag, final Ship ship)
   {
      if (ship != null) {
         shipsDestroyed++;
         ships[ship.getNumber() - 1] = ship;
         for (int m = 0, ix = ship.getStartI(), jy = ship.getStartJ(); m < ship.getSize(); m++) {
            seaGrid[ix][jy] = ship.getNumber();
            if (listener != null) {
               listener.onPartialUpdate(this, ix - 1, jy - 1, AbstractFleetModel.DESTROYED);
            }
            ix += (ship.getDir() == 0) ? 1 : 0;
            jy += (ship.getDir() != 0) ? 1 : 0;
         }
      } else {
         seaGrid[i + 1][j + 1] = resultFlag;
         if(listener != null) {
            listener.onPartialUpdate(this, i, j, resultFlag);
         }
      }
   }
}
