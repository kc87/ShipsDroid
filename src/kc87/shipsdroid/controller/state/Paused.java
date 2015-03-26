package kc87.shipsdroid.controller.state;

import kc87.shipsdroid.R;
import kc87.shipsdroid.controller.GameEngine;
import kc87.shipsdroid.p2p.Message;

public class Paused extends GameStateAdapter
{

   private GameEngine mGameEngine = null;

   public Paused(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      mGameEngine.showDialog(R.string.abort_game_first_msg);
   }

   @Override
   public void pauseGame()
   {
      mGameEngine.showDialog(R.string.game_already_paused);
   }

   @Override
   public void resumeGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.RESUME;
      mGameEngine.getP2pService().sendMessage(abortGameMsg);
   }

   @Override
   public void abortGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.ABORT;
      mGameEngine.getP2pService().sendMessage(abortGameMsg);
   }
}
