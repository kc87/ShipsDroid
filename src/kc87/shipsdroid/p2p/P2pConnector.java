package kc87.shipsdroid.p2p;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


public abstract class P2pConnector
{
   private static final String LOG_TAG = "P2pConnector";
   protected Listener mListener;
   protected volatile State mState;

   public enum State
   {
      DISABLED,
      DISCONNECTED,
      CONNECTING,
      CONNECTED
   }

   public P2pConnector(final Listener listener)
   {
      mListener = listener;
   }

   public abstract void start();
   public abstract void stop();
   public abstract void connectPeer();
   public abstract void disconnectPeer();
   public abstract String getPeerName();
   public abstract void sendString(final String msgStr);

   public State getState()
   {
      return mState;
   }

   public void sendMessage(final Message message)
   {
      final Gson gson = new Gson();
      sendString(gson.toJson(message));
   }

   protected synchronized void setState(final State newSate)
   {
      Log.d(LOG_TAG,"New state: "+newSate.name());
      mState = newSate;
      if(mListener != null) {
         mListener.onStateChanged(mState);
      }
   }

   protected void parsePacket(String msgStr)
   {
      Gson gson = new GsonBuilder().serializeNulls().create();

      if(mListener == null){
         return;
      }

      try {
         msgStr = msgStr.trim();
         Message newMsg = gson.fromJson(msgStr, Message.class);
         mListener.onMessage(newMsg);
      } catch (JsonSyntaxException e) {
         mListener.onMessage(msgStr);
      }
   }

   public interface Listener
   {
      void onMessage(final Message newMsg);
      void onMessage(final String newMsg);
      void onStateChanged(final State newState);
   }

}
