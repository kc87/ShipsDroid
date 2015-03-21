package kc87.shipsdroid.controller.state;


import kc87.shipsdroid.R;
import kc87.shipsdroid.controller.GameEngine;
import kc87.shipsdroid.p2p.Message;

public class Playing extends GameStateAdapter
{
   private GameEngine mGameEngine = null;

   public Playing(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      mGameEngine.showMsg(R.string.abort_game_first_msg);
   }

   @Override
   public void pauseGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.PAUSE;
      mGameEngine.getP2pService().sendMessage(abortGameMsg);
   }

   @Override
   public void resumeGame()
   {
      mGameEngine.showMsg(R.string.no_game_running_msg);
   }


   @Override
   public void abortGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.ABORT;
      mGameEngine.getP2pService().sendMessage(abortGameMsg);
   }

   @Override
   public void finishGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.GAME_OVER;
      mGameEngine.getP2pService().sendMessage(abortGameMsg);

      mGameEngine.setPlayerEnabled(true, false);
      mGameEngine.getShotClock().stop();
      mGameEngine.setState(new PeerReady(mGameEngine));
   }

}
