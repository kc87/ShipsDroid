package c4.subnetzero.shipsdroid.controller;

import android.util.Log;
import c4.subnetzero.shipsdroid.GamePresenter;
import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.state.Disconnected;
import c4.subnetzero.shipsdroid.controller.state.GameState;
import c4.subnetzero.shipsdroid.controller.state.Paused;
import c4.subnetzero.shipsdroid.controller.state.PeerReady;
import c4.subnetzero.shipsdroid.controller.state.Playing;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;
import c4.subnetzero.shipsdroid.model.EnemyFleetModel;
import c4.subnetzero.shipsdroid.model.OwnFleetModel;
import c4.subnetzero.shipsdroid.model.Ship;
import c4.subnetzero.shipsdroid.p2p.Message;
import c4.subnetzero.shipsdroid.p2p.P2pConnector;
import c4.subnetzero.shipsdroid.p2p.P2pService;

import java.util.ArrayList;


public final class GameEngine implements P2pService.Listener, ShotClock.Listener
{
   private static final String LOG_TAG = "GameEngine";
   private ShotClock mShotClock;
   private P2pService mP2pService;
   private OwnFleetModel ownFleetModel = null;
   private EnemyFleetModel enemyFleetModel = null;
   private volatile boolean myTurnFlag = false;
   private boolean mIsHidden = false;
   private GameState currentState = new Disconnected(this);
   private GamePresenter mGamePresenter;

   public GameEngine(final GamePresenter gamePresenter, final P2pService p2pService)
   {
      mGamePresenter = gamePresenter;
      mP2pService = p2pService;
      mP2pService.setListener(this);

      mShotClock = new ShotClock();
      mShotClock.setListener(this);
   }

   public void setState(final GameState newState)
   {
      currentState = newState;
   }

   public void setHidden(final boolean isHidden)
   {
      mIsHidden = isHidden;
   }

   public P2pService getP2pService()
   {
      return mP2pService;
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

   public void shutDown()
   {
      mShotClock.stop();
      mShotClock.shutdown();
      Message quitMsg = new Message();
      quitMsg.TYPE = Message.CTRL;
      quitMsg.SUB_TYPE = Message.PEER_QUIT;
      mP2pService.sendMessage(quitMsg);
   }

   public void showMsg(final int resourceId)
   {
      mGamePresenter.showToast(resourceId);
   }

   // FIXME: This method obviously needs some refactoring ;)
   @Override
   public void onMessage(Message msg)
   {
      if (msg.TYPE == Message.CTRL) {
         if (msg.SUB_TYPE == Message.PEER_QUIT) {
            mShotClock.stop();
            setState(new PeerReady(this));
            mGamePresenter.closeOkDialog();
            showMsg(R.string.peer_has_quit_msg);
            mGamePresenter.executeMenuAction(R.id.quit_game_app);
            return;
         }
         if (msg.SUB_TYPE == Message.PEER_IS_HIDDEN) {
            showMsg(R.string.peer_is_hidden_msg);
            return;
         }
      }

      if (Utils.sIsDialogOpen || mIsHidden) {
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
                  mGamePresenter.updateGameState(getStateName());
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
                  mGamePresenter.updateGameState(getStateName());
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
                  mGamePresenter.updateGameState(getStateName());
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
                  mGamePresenter.updateGameState(getStateName());
                  return;
               }

               if (msg.SUB_TYPE == Message.GAME_OVER) {
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  mGamePresenter.updateGameState(getStateName());
                  mGamePresenter.showOkDialog(R.string.game_lose_msg);
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
                  mGamePresenter.updateGameState(getStateName());
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
                        mGamePresenter.updateGameState(getStateName());
                        mGamePresenter.showOkDialog(R.string.game_win_msg);
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

      mGamePresenter.updateP2pState(newState);
      mGamePresenter.updateGameState(getStateName());
   }


   private void startNewGame()
   {
      ownFleetModel = new OwnFleetModel();
      enemyFleetModel = new EnemyFleetModel();

      mGamePresenter.setFleetModelUpdateListener(ownFleetModel,enemyFleetModel);
      ownFleetModel.placeNewFleet();
      enemyFleetModel.triggerTotalUpdate();

      setScore(AbstractFleetModel.NUMBER_OF_SHIPS, AbstractFleetModel.NUMBER_OF_SHIPS);
      setPlayerEnabled(myTurnFlag, true);
   }

   private void setScore(final int myShips, final int enemyShips)
   {
      mGamePresenter.updateScoreBoard(myShips,enemyShips);
   }


   public void setPlayerEnabled(final boolean enable, final boolean newGame)
   {
      if (enable) {
         mShotClock.start();
      } else {
         mShotClock.stop();
      }
      mGamePresenter.setPlayerEnabled(enable, newGame);
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
      mGamePresenter.updateShotClock(tick);
   }
}
