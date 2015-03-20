package c4.subnetzero.shipsdroid;


import android.app.Application;
import android.util.Log;
import c4.subnetzero.shipsdroid.controller.GameEngine;

public class ShipsDroidApp extends Application
{
   private static final String LOG_TAG = "MainApp";
   private GameEngine mGameEngine;

   @Override
   public void onCreate()
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate();
      mGameEngine = new GameEngine(this);
   }

   @Override
   public void onTerminate()
   {
      Log.d(LOG_TAG,"onTerminate()");
      super.onTerminate();
   }

   public GameEngine getEngine()
   {
      return mGameEngine;
   }

}
