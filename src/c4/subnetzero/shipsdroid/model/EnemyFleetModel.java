package c4.subnetzero.shipsdroid.model;

public class EnemyFleetModel extends AbstractFleetModel
{
   public void update(final int i, final int j, final int resultFlag, final Ship ship)
   {
      if (ship != null) {
         shipsDestroyed++;
         ships[ship.getNumber() - 1] = ship;
         for (int m = 0, ix = ship.getStartI(), jy = ship.getStartJ(); m < ship.getSize(); m++) {
            seaGrid[ix][jy] = ship.getNumber();
            ix += (ship.getDir() == 0) ? 1 : 0;
            jy += (ship.getDir() != 0) ? 1 : 0;
         }
         if(listener != null) {
            listener.onTotalUpdate(this);
         }
      } else {
         seaGrid[i + 1][j + 1] = resultFlag;
         if(listener != null) {
            listener.onPartialUpdate(this, i, j, resultFlag);
         }
      }
   }
}
