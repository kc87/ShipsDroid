package kc87.shipsdroid;

import android.util.Log;
import kc87.shipsdroid.controller.GameEngine;
import kc87.shipsdroid.model.AbstractFleetModel;
import kc87.shipsdroid.model.SeaArea;
import kc87.shipsdroid.p2p.P2pConnector;
import kc87.shipsdroid.model.AbstractFleetModel.ModelUpdateListener;

public class GamePresenter
{
   private static final String LOG_TAG = "GameActivityPresenter";
   private ModelUpdateListener ownFleetModelUpdateListener = null;
   private ModelUpdateListener enemyFleetModelUpdateListener = null;
   private GameEngine mGameEngine;
   private GameView mGameActivityView;


   public GamePresenter(final GameView gameActivityView,final GameEngine gameEngine)
   {
      mGameActivityView = gameActivityView;
      mGameEngine = gameEngine;
      mGameEngine.setPresenter(this);
      mGameEngine.startUp();
   }

   public void initialize(final ModelUpdateListener ownListener,final ModelUpdateListener enemyListener)
   {
      ownFleetModelUpdateListener = ownListener;
      enemyFleetModelUpdateListener = enemyListener;
   }


   public void onResume()
   {
      Log.d(LOG_TAG, "onResume()");
      setFleetModelUpdateListeners();
      mGameEngine.setHidden(false);
   }

   public void onPause(final boolean isChangingConfigurations)
   {
      Log.d(LOG_TAG, "onPause()");

      if (!isChangingConfigurations) {
         if (mGameEngine.getStateName().equals("Playing")) {
            mGameEngine.pauseGame();
         } else {
            mGameEngine.setHidden(true);
         }
      }
   }

   public void onDestroy(final boolean isChangingConfigurations)
   {
      Log.d(LOG_TAG, "onDestroy()");
      enemyFleetModelUpdateListener = null;
      ownFleetModelUpdateListener = null;
      setFleetModelUpdateListeners();
      mGameEngine.setPresenter(null);
      if(!isChangingConfigurations) {
         mGameEngine.shutDown();
      }
   }

   public void restartConnector()
   {
      mGameEngine.getP2pService().start();
   }

   public void enemyFleetViewOnClick(final int tileIndex)
   {
      final int i = tileIndex % SeaArea.DIM;
      final int j = tileIndex / SeaArea.DIM;

      mGameEngine.shoot(i, j);
   }

   public void showToast(final int resourceId)
   {
      mGameActivityView.showToast(resourceId);
   }

   public void showToast(final String toastMsg)
   {
      mGameActivityView.showToast(toastMsg);
   }

   public void showOkDialog(final int resourceId)
   {
      mGameActivityView.showOkDialog(resourceId);
   }

   public void onDialogShow()
   {
      Log.d(LOG_TAG,"onDialogShow()");
      mGameEngine.setHidden(true);
   }

   public void onDialogOkClick()
   {
      Log.d(LOG_TAG,"onDialogOkClick()");
      mGameEngine.setHidden(false);
   }


   public void executeMenuAction(final int actionId)
   {
       switch (actionId) {
         case R.id.connect_peer:
            mGameEngine.connectPeer();
            break;
         case R.id.new_game:
            mGameEngine.newGame();
            break;
         case R.id.pause_game:
            mGameEngine.pauseGame();
            break;
         case R.id.resume_game:
            mGameEngine.resumeGame();
            break;
         case R.id.abort_game:
            mGameEngine.abortGame();
            break;
         case R.id.quit_game_app:
            mGameActivityView.finishView();
            break;
      }
   }

   public void updateScoreBoard(final int myShips, final int enemyShips)
   {
      mGameActivityView.updateScoreBoard(String.valueOf(myShips),String.valueOf(enemyShips));
   }

   public void updateShotClock(final int tick)
   {
      mGameActivityView.updateShotClock(String.valueOf(tick));
   }

   public void updateGameState(final String gameStateName)
   {
      switch (gameStateName) {
         case "Disconnected":
            // Connect,New,Pause,Resume,Abort
            mGameActivityView.setMenuItemVisibility(true, false, false, false, false);
            break;
         case "PeerReady":
            // Connect,New,Pause,Resume,Abort
            mGameActivityView.setMenuItemVisibility(false, true, false, false, false);
            break;
         case "Playing":
            // Connect,New,Pause,Resume,Abort
            mGameActivityView.setMenuItemVisibility(false, false, true, false, true);
            break;
         case "Paused":
            // Connect,New,Pause,Resume,Abort
            mGameActivityView.setMenuItemVisibility(false, false, false, true, true);
            break;
         default:
            break;
      }
   }

   public void updateP2pState(final P2pConnector.State p2pState,final String peerName)
   {
      Log.d(LOG_TAG,"updateP2pState(): "+p2pState.name());

      switch (p2pState) {
         case DISABLED:
            mGameActivityView.updateP2pState(R.drawable.state_gray_bg);
            mGameActivityView.enableBluetooth();
            break;
         case CONNECTING:
            //case UNREACHABLE:
            mGameActivityView.updateP2pState(R.drawable.state_yellow_bg);
            showToast("Trying peer '"+peerName+"'...");
            break;
         case DISCONNECTED:
            mGameActivityView.updateP2pState(R.drawable.state_red_bg);
            //showToast("Disconnected");
            break;
         case CONNECTED:
            mGameActivityView.updateP2pState(R.drawable.state_green_bg);
            showToast("Connected to '" + peerName + "'");
            break;
      }
   }

   public void setFleetModelUpdateListeners()
   {
      setFleetModelUpdateListener(mGameEngine.getOwnModel(),ownFleetModelUpdateListener);
      setFleetModelUpdateListener(mGameEngine.getEnemyModel(),enemyFleetModelUpdateListener);
   }

   public void setPlayerEnabled(final boolean enable, final boolean newGame)
   {
      mGameActivityView.setEnemyFleetViewEnabled(enable, newGame);
   }

   private void setFleetModelUpdateListener(final AbstractFleetModel model, final ModelUpdateListener listener)
   {
      if(model != null)
      {
         model.setModelUpdateListener(listener);
         if(listener != null) {
            model.triggerTotalUpdate();
         }
      }
   }
}
