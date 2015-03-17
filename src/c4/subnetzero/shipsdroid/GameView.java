package c4.subnetzero.shipsdroid;


public interface GameView
{
   void updateP2pState(final int bgResourceId);
   void updateShotClock(final String value);
   void updateScoreBoard(final String myScore,final String enemyScore);
   void setMenuItemVisibility(final boolean... isVisibleList);
   void setEnemyFleetViewEnabled(final boolean enable, final boolean newGame);
   void enableBluetooth();
   void finishView();
}
