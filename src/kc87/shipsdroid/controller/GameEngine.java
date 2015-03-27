package kc87.shipsdroid.controller;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import kc87.shipsdroid.GamePresenter;
import kc87.shipsdroid.R;
import kc87.shipsdroid.controller.state.Disconnected;
import kc87.shipsdroid.controller.state.GameState;
import kc87.shipsdroid.controller.state.Paused;
import kc87.shipsdroid.controller.state.PeerReady;
import kc87.shipsdroid.controller.state.Playing;
import kc87.shipsdroid.model.AbstractFleetModel;
import kc87.shipsdroid.model.EnemyFleetModel;
import kc87.shipsdroid.model.OwnFleetModel;
import kc87.shipsdroid.model.Ship;
import kc87.shipsdroid.p2p.Message;
import kc87.shipsdroid.p2p.P2pConnector;
import kc87.shipsdroid.p2p.P2pService;

import java.util.ArrayList;


public final class GameEngine implements P2pService.Listener, ShotClock.Listener, ServiceConnection
{
   private static final String LOG_TAG = "GameEngine";
   private Application mApplication;
   private ShotClock mShotClock;
   private P2pService mP2pService;
   private OwnFleetModel ownFleetModel = null;
   private EnemyFleetModel enemyFleetModel = null;
   private volatile boolean myTurnFlag = false;
   private boolean mIsHidden = false;
   private boolean mIsServiceBound = false;
   private GameState currentState = new Disconnected(this);
   private GamePresenter mGamePresenter;

   public GameEngine(final Application app)
   {
      mApplication = app;
      startUp();
   }

   public void setState(final GameState newState)
   {
      currentState = newState;
      mGamePresenter.updateGameState(currentState.toString());
   }

   public void setHidden(final boolean isHidden)
   {
      mIsHidden = isHidden;
   }


   public void setPresenter(final GamePresenter gamePresenter)
   {
      mGamePresenter = gamePresenter;
   }

   public P2pService getP2pService()
   {
      return mP2pService;
   }

   public AbstractFleetModel getEnemyModel()
   {
      return enemyFleetModel;
   }

   public AbstractFleetModel getOwnModel()
   {
      return ownFleetModel;
   }

   public String getStateName()
   {
      return currentState.toString();
   }

   public ShotClock getShotClock()
   {
      return mShotClock;
   }

   public void connectPeer()
   {
      currentState.connectPeer();
   }

   public void newGame()
   {
      currentState.newGame();
   }

   public void pauseGame()
   {
      currentState.pauseGame();
   }

   public void resumeGame()
   {
      currentState.resumeGame();
   }

   public void abortGame()
   {
      currentState.abortGame();
   }

   public void shoot(final int i, final int j)
   {
      if (currentState.toString().equals("Playing")) {
         mShotClock.pause();
         Message bombMsg = new Message();
         bombMsg.TYPE = Message.GAME;
         bombMsg.SUB_TYPE = Message.SHOOT;
         bombMsg.PAYLOAD = new Object[]{i, j};
         mP2pService.sendMessage(bombMsg);
      }
   }

   public void startUp()
   {
      if(!mIsServiceBound) {
         mShotClock = new ShotClock();
         mShotClock.setListener(this);
         ownFleetModel = new OwnFleetModel();
         enemyFleetModel = new EnemyFleetModel();
         mApplication.bindService(new Intent(mApplication, P2pService.class), this, Context.BIND_AUTO_CREATE);
         mIsServiceBound = true;
      }
   }

   public void shutDown()
   {
      //mShotClock.stop();
      mShotClock.shutdown();
      Message quitMsg = new Message();
      quitMsg.TYPE = Message.CTRL;
      quitMsg.SUB_TYPE = Message.PEER_QUIT;
      mP2pService.sendMessage(quitMsg);
      mP2pService.stop();
      mApplication.unbindService(this);
      mIsServiceBound = false;
      ownFleetModel = null;
      enemyFleetModel = null;
      mShotClock = null;
   }


   public void showDialog(final int resourceId)
   {
      if(mGamePresenter != null) {
         mGamePresenter.showOkDialog(resourceId);
      }
   }

   public void showMsg(final int resourceId)
   {
      if(mGamePresenter != null) {
         mGamePresenter.showToast(resourceId);
      }
   }

   // FIXME: This method obviously needs some refactoring ;)
   @Override
   public void onMessage(Message msg)
   {
      if (msg.TYPE == Message.CTRL) {
         if (msg.SUB_TYPE == Message.PEER_QUIT) {
            mShotClock.stop();
            setState(new PeerReady(this));
            showMsg(R.string.peer_has_quit_msg);
            if(mGamePresenter != null) {
               mGamePresenter.executeMenuAction(R.id.quit_game_app);
            }
            return;
         }
         if (msg.SUB_TYPE == Message.PEER_IS_HIDDEN) {
            showMsg(R.string.peer_is_hidden_msg);
            return;
         }
      }

      if (mIsHidden) {
         Log.d(LOG_TAG, "Open Dialog: Message ignored!");
         Message hiddenMsg = new Message();
         hiddenMsg.TYPE = Message.CTRL;
         hiddenMsg.SUB_TYPE = Message.PEER_IS_HIDDEN;
         mP2pService.sendMessage(hiddenMsg);
         return;
      }

      switch (currentState.toString()) {
         case "Paused":

            if (msg.TYPE == Message.GAME) {
               if (msg.SUB_TYPE == Message.RESUME) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mP2pService.sendMessage(msg);
                  }
                  if (myTurnFlag) {
                     mShotClock.resume();
                  }
                  setState(new Playing(this));
                  return;
               }

               if (msg.SUB_TYPE == Message.ABORT) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mP2pService.sendMessage(msg);
                     showMsg(R.string.aborted_by_peer_msg);
                  } else {
                     showMsg(R.string.game_aborted_msg);
                  }
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  return;
               }
            }
            break;

         case "PeerReady":

            if (msg.TYPE == Message.GAME) {
               if (msg.SUB_TYPE == Message.NEW) {
                  myTurnFlag = msg.ACK_FLAG;
                  if (!msg.ACK_FLAG && !msg.RST_FLAG) {
                     msg.ACK_FLAG = true;
                     mP2pService.sendMessage(msg);
                  }
                  setState(new Playing(this));
                  startNewGame();
                  return;
               }
            }
            break;

         case "Playing":

            if (msg.TYPE == Message.GAME) {
               if (msg.SUB_TYPE == Message.PAUSE) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mP2pService.sendMessage(msg);
                     showMsg(R.string.paused_by_peer_msg);
                  } else {
                     showMsg(R.string.game_paused_msg);
                  }
                  mShotClock.pause();
                  setState(new Paused(this));
                  return;
               }

               if (msg.SUB_TYPE == Message.GAME_OVER) {
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  showDialog(R.string.game_lose_msg);
                  return;
               }

               if (msg.SUB_TYPE == Message.ABORT) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mP2pService.sendMessage(msg);
                     showMsg(R.string.aborted_by_peer_msg);
                  } else {
                     showMsg(R.string.game_aborted_msg);
                  }
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  return;
               }

               if (msg.SUB_TYPE == Message.TIMEOUT) {
                  Log.d(LOG_TAG, "Timeout received");
                  setPlayerEnabled(true, false);
                  return;
               }

               if (msg.SUB_TYPE == Message.SHOOT) {

                  ArrayList payload = (ArrayList) msg.PAYLOAD;

                  int resultFlag;
                  int i = ((Double) payload.get(0)).intValue();
                  int j = ((Double) payload.get(1)).intValue();

                  if (msg.ACK_FLAG) {

                     resultFlag = ((Double) payload.get(2)).intValue();
                     Ship ship = msg.SHIP;

                     enemyFleetModel.update(i, j, resultFlag, ship);

                     if (enemyFleetModel.isFleetDestroyed()) {
                        setScore(ownFleetModel.getShipsLeft(), 0);
                        currentState.finishGame();
                        showDialog(R.string.game_win_msg);
                        return;
                     }

                     myTurnFlag = resultFlag == AbstractFleetModel.HIT || resultFlag == AbstractFleetModel.DESTROYED;

                  } else {

                     Object[] result = ownFleetModel.update(i, j);

                     resultFlag = (Integer) result[0];
                     Ship ship = (Ship) result[1];

                     msg.ACK_FLAG = true;
                     msg.PAYLOAD = new Object[]{i, j, resultFlag};
                     msg.SHIP = ship;
                     mP2pService.sendMessage(msg);

                     myTurnFlag = !(resultFlag == AbstractFleetModel.HIT || resultFlag == AbstractFleetModel.DESTROYED);
                  }

                  if (resultFlag == AbstractFleetModel.DESTROYED) {
                     setScore(ownFleetModel.getShipsLeft(), enemyFleetModel.getShipsLeft());
                  }

                  setPlayerEnabled(myTurnFlag, false);
               }
            }
            break;

         default:
            //TODO: Maybe send some sort of reject message
            break;
      }
   }

   @Override
   public void onStateChanged(P2pConnector.State newState)
   {
      switch (newState){
         case CONNECTED:
            setState(new PeerReady(this));
            break;
         case DISCONNECTED:
            setState(new Disconnected(this));
            break;
      }

      if(mGamePresenter != null) {
         mGamePresenter.updateP2pState(newState,mP2pService.getPeerName());
      }
   }


   private void startNewGame()
   {
      ownFleetModel = new OwnFleetModel();
      enemyFleetModel = new EnemyFleetModel();
      ownFleetModel.placeNewFleet();

      mGamePresenter.setFleetModelUpdateListeners();

      setScore(AbstractFleetModel.NUMBER_OF_SHIPS, AbstractFleetModel.NUMBER_OF_SHIPS);
      setPlayerEnabled(myTurnFlag, true);
   }

   private void setScore(final int myShips, final int enemyShips)
   {
      if(mGamePresenter != null) {
         mGamePresenter.updateScoreBoard(myShips, enemyShips);
      }
   }


   public void setPlayerEnabled(final boolean enable, final boolean newGame)
   {
      if (enable) {
         mShotClock.start();
      } else {
         mShotClock.stop();
      }

      if(mGamePresenter != null) {
         mGamePresenter.setPlayerEnabled(enable, newGame);
      }
   }

   /*
    * ***************************************
    * Implementation of ShotClock Callbacks *
    * ***************************************
    */

   @Override
   public void onTimeIsUp()
   {
      setPlayerEnabled(false, false);
      Message timeoutMsg = new Message();
      timeoutMsg.TYPE = Message.GAME;
      timeoutMsg.SUB_TYPE = Message.TIMEOUT;
      mP2pService.sendMessage(timeoutMsg);
   }

   @Override
   public void onTick(final int tick)
   {
      if(mGamePresenter != null) {
         mGamePresenter.updateShotClock(tick);
      }
   }

   /*
    * ***********************************************
    * Implementation of ServiceConnection Callbacks *
    * ***********************************************
    */

   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mP2pService = ((P2pService.LocalBinder) service).getService();
      mP2pService.setListener(this);
      mP2pService.start();
   }

   // Only gets called when service has crashed!!
   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected(): Service has crashed!!");
      mP2pService = null;
   }
}
