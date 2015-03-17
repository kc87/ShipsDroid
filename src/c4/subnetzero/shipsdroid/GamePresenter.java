package c4.subnetzero.shipsdroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;
import c4.subnetzero.shipsdroid.model.SeaArea;
import c4.subnetzero.shipsdroid.p2p.P2pConnector;
import c4.subnetzero.shipsdroid.p2p.P2pService;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel.ModelUpdateListener;

public class GamePresenter implements ServiceConnection
{
   private static final String LOG_TAG = "GameActivityPresenter";
   private ModelUpdateListener ownFleetModelUpdateListener = null;
   private ModelUpdateListener enemyFleetModelUpdateListener = null;
   private GameEngine mGameEngine;
   private GameView mGameActivityView;
   private P2pService mP2pService;

   public GamePresenter(final GameView gameActivityView)
   {
      mGameActivityView = gameActivityView;
   }

   public void initialize(final ModelUpdateListener ownListener,final ModelUpdateListener enemyListener)
   {
      ownFleetModelUpdateListener = ownListener;
      enemyFleetModelUpdateListener = enemyListener;
   }

   public void startP2pService()
   {
      if(mP2pService != null){
         mP2pService.start();
      }
   }

   public void quit()
   {
      if (mP2pService != null) {
         mP2pService.setListener(null);
      }

      if (mGameEngine != null) {
         mGameEngine.shutDown();
      }
   }

   public void onResume()
   {
      Log.d(LOG_TAG, "onResume()");

      if (mGameEngine != null) {
         mGameEngine.setHidden(false);
      }
   }

   public void onPause()
   {
      Log.d(LOG_TAG, "onPause()");

      if (mGameEngine != null) {
         if (mGameEngine.getStateName().equals("Playing")) {
            mGameEngine.pauseGame();
         } else {
            mGameEngine.setHidden(true);
         }
      }
   }

   public void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      quit();
   }

   public void shoot(final int buttonIndex)
   {
      final int i = buttonIndex % SeaArea.DIM;
      final int j = buttonIndex / SeaArea.DIM;

      mGameEngine.shoot(i, j);
   }

   public void showToast(final int resourceId)
   {
      final Context ctx = (Context)mGameActivityView;
      Utils.showToast(ctx,resourceId);
   }

   public void showToast(final String toastMsg)
   {
      final Context ctx = (Context)mGameActivityView;
      Utils.showToast(ctx,toastMsg);
   }

   public void showOkDialog(final int resourceId)
   {
      final Context ctx = (Context)mGameActivityView;
      Utils.showOkMsg(ctx, resourceId, null);
   }

   public void closeOkDialog()
   {
      if(Utils.sIsDialogOpen){
         final Context ctx = (Context)mGameActivityView;
         Utils.closeOkMsg(ctx);
      }
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
            quit();
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

   public void updateP2pState(final P2pConnector.State p2pState)
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
            showToast("Trying " + mP2pService.getPeerName());
            break;
         case DISCONNECTED:
            mGameActivityView.updateP2pState(R.drawable.state_red_bg);
            break;
         case CONNECTED:
            mGameActivityView.updateP2pState(R.drawable.state_green_bg);
            showToast("Connected to " + mP2pService.getPeerName());
            break;
      }
   }

   public void setFleetModelUpdateListener(final AbstractFleetModel ownModel,final AbstractFleetModel enemyModel)
   {
      ownModel.setModelUpdateListener(ownFleetModelUpdateListener);
      enemyModel.setModelUpdateListener(enemyFleetModelUpdateListener);
   }

   public void setPlayerEnabled(final boolean enable, final boolean newGame)
   {
      mGameActivityView.setEnemyFleetViewEnabled(enable, newGame);
   }

   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mP2pService = ((P2pService.LocalBinder) service).getService();
      mGameEngine = new GameEngine(this, mP2pService);
      startP2pService();
   }

   // Only gets called when service has crashed!!
   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected(): Service has crashed!!");
      mP2pService = null;
   }
}