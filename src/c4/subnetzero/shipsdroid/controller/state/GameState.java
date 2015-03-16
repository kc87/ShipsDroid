package c4.subnetzero.shipsdroid.controller.state;

public interface GameState
{
   public void connectPeer();
   public void newGame();
   public void pauseGame();
   public void resumeGame();
   public void abortGame();
   public void finishGame();
}
