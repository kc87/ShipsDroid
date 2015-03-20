package c4.subnetzero.shipsdroid.p2p;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import c4.subnetzero.shipsdroid.p2p.bluetooth.BtConnector;


public class P2pService extends Service implements P2pConnector.Listener
{
   private static final String LOG_TAG = "P2pService";
   private LocalBinder mLocalBinder = new LocalBinder();
   private P2pConnector mP2pConnector;
   private Listener mListener;

   @Override
   public void onCreate()
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate();
      mP2pConnector = new BtConnector(this);
   }

   @Override
   public IBinder onBind(Intent intent)
   {
      Log.d(LOG_TAG, "onBind()");
      return mLocalBinder;
   }

   @Override
   public boolean onUnbind (Intent intent)
   {
      Log.d(LOG_TAG, "onUnbind()");
      return false;
   }

   @Override
   public void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      super.onDestroy();
   }

   public void start()
   {
      mP2pConnector.start();
   }

   public void stop()
   {
      Log.d(LOG_TAG, "stop()");
      mListener = null;
      mP2pConnector.stop();
   }

   public void setListener(Listener listener)
   {
      if (listener != null && mListener != null) {
         throw new IllegalStateException("Listener must be null before set!");
      } else {
         mListener = listener;
      }
   }

   public String getPeerName()
   {
      return mP2pConnector.getPeerName();
   }

   public P2pConnector.State getState()
   {
      return mP2pConnector.getState();
   }

   public void sendMessage(final String message)
   {
      mP2pConnector.sendString(message);
   }

   public void sendMessage(final Message message)
   {
      mP2pConnector.sendMessage(message);
   }

   public void connect()
   {
      mP2pConnector.connectPeer();
   }

   public void disconnect()
   {
      mP2pConnector.disconnectPeer();
   }

   @Override
   public void onMessage(Message newMsg)
   {
      if(mListener != null) {
         mListener.onMessage(newMsg);
      }
   }

   @Override
   public void onMessage(String newMsg)
   {
   }

   @Override
   public void onStateChanged(P2pConnector.State newState)
   {
      if(mListener != null) {
         mListener.onStateChanged(newState);
      }
   }

   public class LocalBinder extends Binder
   {
      public P2pService getService()
      {
         return P2pService.this;
      }
   }

   public interface Listener
   {
      void onMessage(Message newMsg);
      void onStateChanged(P2pConnector.State newState);
   }

}
