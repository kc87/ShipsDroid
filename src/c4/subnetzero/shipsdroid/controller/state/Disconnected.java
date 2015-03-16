package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;

public class Disconnected extends GameStateAdapter
{

   private GameEngine mGameEngine = null;

   public Disconnected(final GameEngine engine)
   {
      mGameEngine = engine;
   }


   @Override
   public void connectPeer()
   {
      mGameEngine.getP2pService().connect();
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_player_connected_msg, null);
   }

   @Override
   public void pauseGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void resumeGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void finishGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }
}
