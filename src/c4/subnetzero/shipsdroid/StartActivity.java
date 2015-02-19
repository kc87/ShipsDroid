package c4.subnetzero.shipsdroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.net.InetAddress;

public class StartActivity extends Activity implements Handler.Callback, ServiceConnection
{
   private static final String LOG_TAG = "StartActivity";

   private static final int WIFI_DISABLED = 20;
   private static final int WIFI_ENABLED = 21;
   private static final int WIFI_P2P_START = 22;
   private static final int FINISH_ACTIVITY = 30;
   private static final int READY_TO_CONNECT = 1;
   private static final int L2_CONNECTED = 2;
   private static final int L2_CONNECTING = 3;
   private static final int L2_DISCONNECTED = 4;
   private static final int HANDSHAKE_SEND = 10;
   private static final int HANDSHAKE_RECEIVED = 11;
   private static final int L3_CONNECTED = 12;
   private static final int L2_DISCOVERY_STARTED = 7;

   private Handler mUiHandler;
   private NetService mNetService;
   private WifiP2pReceiver mWifiP2pReceiver;
   private NetService.Listener mNetServiceListener;
   private volatile boolean mIsServiceConnected;
   private volatile boolean mIsWifiP2pEnabled;
   private boolean mIsGroupOwner;
   private boolean mIsConnected;

   private InetAddress mGoIp;
   private int mGoPort;
   private String mGroupOwnerId = "0.0.0.0:0";
   private String mPeerId = "0.0.0.0:0";
   private TextView mStateLabel;
   private Button mConDisconnectBtn;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.start);
      mUiHandler = new Handler(this);
      setup();
   }

   @Override
   protected void onResume()
   {
      Log.d(LOG_TAG, "onResume()");
      super.onResume();

      if (mNetService != null) {
         mNetService.setListener(mNetServiceListener);
      }

      registerReceiver(mWifiP2pReceiver, new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION));
   }


   @Override
   protected void onPause()
   {
      Log.d(LOG_TAG, "onPause()");
      super.onPause();

      if (mNetService != null) {
         mNetService.setListener(null);
      }

      unregisterReceiver(mWifiP2pReceiver);
   }


   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");

      if (mNetService != null) {
         unbindService(this);
      }

      super.onDestroy();
   }

   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onRestoreInstanceState()");
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      Log.d(LOG_TAG, "onSaveInstanceState()");
      super.onSaveInstanceState(outState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.start, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId()) {
         case R.id.quit_app:
            finish();
            break;
      }

      return true;
   }


   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      if (requestCode == 0) {
         bindService(new Intent(this, NetService.class), this, BIND_AUTO_CREATE);
      }
   }

   @Override
   public boolean handleMessage(Message msg)
   {
      switch (msg.what) {
         case WIFI_P2P_START:
            if (!mIsServiceConnected) {
               bindService(new Intent(this, NetService.class), this, BIND_AUTO_CREATE);
            }
            break;

         case L2_DISCOVERY_STARTED:
            mStateLabel.setText(getString(R.string.searching_peers_msg));
            mConDisconnectBtn.setVisibility(View.INVISIBLE);
            break;

         case READY_TO_CONNECT:
            mConDisconnectBtn.setVisibility(View.VISIBLE);
            mConDisconnectBtn.setText(getString(R.string.connect_btn));
            mStateLabel.setText(getString(R.string.connect_ready_msg));
            break;

         case L2_CONNECTING:
            mStateLabel.setText(getString(R.string.connecting_msg));
            mConDisconnectBtn.setVisibility(View.INVISIBLE);
            break;

         case L2_CONNECTED:
            mConDisconnectBtn.setVisibility(View.VISIBLE);
            mConDisconnectBtn.setText(getString(R.string.disconnect_btn));
            mStateLabel.setText(getString(mIsGroupOwner ? R.string.wifi_connected_go : R.string.wifi_connected));
            if (!mIsGroupOwner) {
               mNetService.sendServerHello(mGroupOwnerId);
            }
            break;

         case L2_DISCONNECTED:
            mConDisconnectBtn.setText(getString(R.string.connect_btn));
            mStateLabel.setText("Disconnected!");
            break;

         case L3_CONNECTED:
            mStateLabel.setText(getString(R.string.connected_ready_msg));
            mNetService.setListener(null);
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("peerId", mPeerId);
            startActivity(intent);
            mUiHandler.sendEmptyMessageDelayed(FINISH_ACTIVITY, 2000);
            break;

         case WIFI_DISABLED:
            mStateLabel.setText(getString(R.string.wifi_disabled_msg));
            break;

         case FINISH_ACTIVITY:
            finish();
            break;
      }

      return true;
   }


   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mNetService = ((NetService.LocalBinder) service).getService();
      mNetService.setListener(mNetServiceListener);
      mNetService.start();
      mIsServiceConnected = true;
   }

   // Only gets called when service has crashed!!
   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected()");
   }


   private void setup()
   {
      mIsServiceConnected = false;
      mIsWifiP2pEnabled = false;

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
         Log.e(LOG_TAG, "API Level 16 or higher required!");
         finish();
         return;
      }

      mStateLabel = (TextView) findViewById(R.id.state_label);
      mConDisconnectBtn = (Button) findViewById(R.id.con_discon_btn);

      mConDisconnectBtn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if (!mIsConnected) {

               if (mNetService != null) {
                  mUiHandler.sendEmptyMessage(L2_CONNECTING);
                  mNetService.connect();
               }

            } else {

               if (mNetService != null) {
                  mNetService.disconnect();
               }
            }
         }
      });

      mWifiP2pReceiver = new WifiP2pReceiver();

      mNetServiceListener = new NetService.Listener()
      {
         @Override
         public void onPeerReady()
         {
            mUiHandler.sendEmptyMessage(L3_CONNECTED);
         }

         @Override
         public void onMessage(c4.subnetzero.shipsdroid.net.Message newMsg, String peerId)
         {
            //Nothing here
         }

         @Override
         public void onReachabilityChanged(final boolean reachable)
         {
            //Utils.showOkMsg(StartActivity.this,"Reachability changed: "+reachable,null);
         }

         @Override
         public void onStartDiscovery()
         {
            mUiHandler.sendEmptyMessage(L2_DISCOVERY_STARTED);
         }


         @Override
         public void onConnected(final InetAddress groupOwnerIp, final int groupOwnerPort, final boolean isGroupOwner)
         {
            mIsConnected = true;

            Log.d(LOG_TAG, "Server address: " + groupOwnerIp.getHostAddress() + ":" + groupOwnerPort);
            Log.i(LOG_TAG, "Im the " + (isGroupOwner ? "server" : "client"));

            mGoIp = groupOwnerIp;
            mGoPort = groupOwnerPort;
            mIsGroupOwner = isGroupOwner;
            mGroupOwnerId = mGoIp.getHostAddress() + ":" + mGoPort;
         }

         @Override
         public void onDisconnected()
         {
            mIsConnected = false;
            mUiHandler.sendEmptyMessage(L2_DISCONNECTED);
         }
      };
   }

   private class WifiP2pReceiver extends BroadcastReceiver
   {
      private static final String LOG_TAG = "WifiP2pReceiver";

      @Override
      public void onReceive(Context context, Intent intent)
      {
         String action = intent.getAction();

         switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
               onStateChanged(intent);
               break;
         }
      }

      private void onStateChanged(Intent intent)
      {
         int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

         if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            Log.i(LOG_TAG, "Wifi P2P is enabled");
            mIsWifiP2pEnabled = true;
            mUiHandler.sendEmptyMessageDelayed(WIFI_P2P_START, 1000);
         } else {
            Log.i(LOG_TAG, "Wifi P2P is disabled. State " + state);
            mIsWifiP2pEnabled = false;
            Utils.launchWifiSettings(StartActivity.this);
         }
      }
   }

}
