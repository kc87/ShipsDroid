package kc87.shipsdroid.controller.state;


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
      //Utils.showOkMsg(mGameEngine.getContext(), R.string.abort_game_first_msg, null);
   }

   @Override
   public void pauseGame()
   {
      //Utils.showOkMsg(mGameEngine.getContext(), R.string.game_already_paused, null);
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