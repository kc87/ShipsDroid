package kc87.shipsdroid;


public interface GameView
{
   void showToast(final int resourceId);
   void showToast(final String toastMsg);
   void showOkDialog(final int resourceId);
   void showOkDialog(final String okMsg);
   void updateP2pState(final int bgResourceId);
   void updateShotClock(final String value);
   void updateScoreBoard(final String myScore,final String enemyScore);
   void setMenuItemVisibility(final boolean... isVisibleList);
   void setEnemyFleetViewEnabled(final boolean enable, final boolean newGame);
   void enableBluetooth();
   void finishView();
}
