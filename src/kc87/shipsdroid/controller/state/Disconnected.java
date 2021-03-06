package kc87.shipsdroid.controller.state;

import kc87.shipsdroid.R;
import kc87.shipsdroid.controller.GameEngine;

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
      mGameEngine.showMsg(R.string.no_player_connected_msg);
   }

   @Override
   public void pauseGame()
   {
      mGameEngine.showMsg(R.string.no_game_running_msg);
   }

   @Override
   public void resumeGame()
   {
      mGameEngine.showMsg(R.string.no_game_running_msg);
   }

   @Override
   public void abortGame()
   {
      mGameEngine.showMsg(R.string.no_game_running_msg);
   }

   @Override
   public void finishGame()
   {
      mGameEngine.showMsg(R.string.no_game_running_msg);
   }
}
