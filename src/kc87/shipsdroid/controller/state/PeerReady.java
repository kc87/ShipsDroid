package kc87.shipsdroid.controller.state;


import kc87.shipsdroid.controller.GameEngine;
import kc87.shipsdroid.p2p.Message;

public class PeerReady extends GameStateAdapter
{
   private GameEngine mGameEngine = null;

   public PeerReady(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      Message newGameMsg = new Message();
      newGameMsg.TYPE = Message.GAME;
      newGameMsg.SUB_TYPE = Message.NEW;
      mGameEngine.getP2pService().sendMessage(newGameMsg);
   }

   @Override
   public void pauseGame()
   {
      //Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void resumeGame()
   {
      //Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void abortGame()
   {
      //Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

}
