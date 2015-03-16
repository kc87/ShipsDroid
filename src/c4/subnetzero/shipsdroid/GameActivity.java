package c4.subnetzero.shipsdroid;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.p2p.P2pService;
import c4.subnetzero.shipsdroid.view.EnemyFleetView;
import c4.subnetzero.shipsdroid.view.GridButtonHandler;
import c4.subnetzero.shipsdroid.view.OwnFleetView;

public class GameActivity extends Activity implements Handler.Callback, ServiceConnection
{
   private static final String LOG_TAG = "GameActivity";
   private static final int REQUEST_ENABLE_BT = 1;

   private Handler mUiHandler;
   private Menu mMenu;
   private EnemyFleetView mEnemyFleetView;
   private OwnFleetView mOwnFleetView;
   private GameEngine mGameEngine;
   private P2pService mP2pService;
   private GridButtonHandler mGridButtonHandler;
   private TextView mShotClockView;
   private TextView mEnemyShipsView;
   private TextView mMyShipsView;
   private ImageButton mConState;
   //private ActionBar mActionBar;

   public static final int UPDATE_SHOT_CLOCK = 1;
   public static final int UPDATE_SCORE_BOARD = 2;
   public static final int UPDATE_GAME_MENU = 3;
   public static final int STATE_CHANGED = 4;
   public static final int PEER_QUIT_APP = 5;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate(savedInstanceState);
      setContentView(R.layout.game);

      mUiHandler = new Handler(this);
      //mRestarted = false;
      setup();
   }

   @Override
   protected void onRestart()
   {
      Log.d(LOG_TAG, "onRestart()");
      super.onRestart();
      //mRestarted = true;
   }

   @Override
   protected void onStart()
   {
      Log.d(LOG_TAG, "onStart()");
      super.onStart();
   }


   @Override
   protected void onResume()
   {
      Log.d(LOG_TAG, "onResume()");
      super.onResume();

      if (mGameEngine != null) {
         mGameEngine.setHidden(false);
      }
   }


   @Override
   protected void onPause()
   {
      Log.d(LOG_TAG, "onPause()");
      super.onPause();

      if (mGameEngine != null) {
         if (mGameEngine.getStateName().equals("Playing")) {
            mGameEngine.pauseGame();
         } else {
            mGameEngine.setHidden(true);
         }
      }
   }

   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      if (mGameEngine != null) {
         mGameEngine.shutDown();
      }
      unbindService(this);
      super.onDestroy();
   }

   @Override
   public void onBackPressed()
   {
      quitApp();
      finish();
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


   // Now the size of the views should be available
   @Override
   public void onWindowFocusChanged(boolean hasFocus)
   {
      super.onWindowFocusChanged(hasFocus);

      if (mEnemyFleetView == null) {
         buildGameBoard();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.game, menu);
      mMenu = menu;
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId()) {
         case R.id.connect_peer:
            mGameEngine.connectPeer();
            break;
         case R.id.new_game:
            mGameEngine.newGame();
            break;
         case R.id.pause_game:
            mGameEngine.pauseGame();
            break;
         case R.id.resume_game:
            mGameEngine.resumeGame();
            break;
         case R.id.abort_game:
            mGameEngine.abortGame();
            break;
         case R.id.quit_game_app:
            quitApp();
            finish();
            break;
      }

      return true;
   }

   @Override
   public boolean handleMessage(Message msg)
   {
      switch (msg.what) {
         case UPDATE_SHOT_CLOCK:
            mShotClockView.setText(String.valueOf(msg.arg1));
            break;
         case UPDATE_SCORE_BOARD:
            mMyShipsView.setText(String.valueOf(msg.arg1));
            mEnemyShipsView.setText(String.valueOf(msg.arg2));
            break;
         case UPDATE_GAME_MENU:
            switch (mGameEngine.getStateName()) {
               case "Disconnected":
                  // Connect,New,Pause,Resume,Abort
                  setMenuItemVisibility(true,false,false,false,false);
                  break;
               case "PeerReady":
                  // Connect,New,Pause,Resume,Abort
                  setMenuItemVisibility(false,true,false,false,false);
                  break;
               case "Playing":
                  // Connect,New,Pause,Resume,Abort
                  setMenuItemVisibility(false,false,true,false,true);
                  break;
               case "Paused":
                  // Connect,New,Pause,Resume,Abort
                  setMenuItemVisibility(false,false,false,true,true);
                  break;
               default:
                  break;
            }
            break;
         case STATE_CHANGED:
            Drawable stateDrawable;
            switch (mP2pService.getState()) {
               case DISABLED:
                  stateDrawable = getResources().getDrawable(R.drawable.state_gray_bg);
                  Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                  startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                  break;
               case CONNECTING:
                  //case UNREACHABLE:
                  stateDrawable = getResources().getDrawable(R.drawable.state_yellow_bg);
                  Toast.makeText(this,"Trying " + mP2pService.getPeerName(),Toast.LENGTH_SHORT).show();
                  break;
               case DISCONNECTED:
                  stateDrawable = getResources().getDrawable(R.drawable.state_red_bg);
                  break;
               case CONNECTED:
                  stateDrawable = getResources().getDrawable(R.drawable.state_green_bg);
                  mUiHandler.sendEmptyMessage(GameActivity.UPDATE_GAME_MENU);
                  Toast.makeText(this,"Connected to " + mP2pService.getPeerName(),Toast.LENGTH_LONG).show();
                  break;
               default:
                  stateDrawable = getResources().getDrawable(R.drawable.state_gray_bg);
                  break;
            }
            mConState.setBackground(stateDrawable);
            break;
         case PEER_QUIT_APP:
            finish();
         default:
            /* EMPTY */
            break;
      }
      return true;
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      if (requestCode == REQUEST_ENABLE_BT) {
         mP2pService.start();
      }
   }


   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mP2pService = ((P2pService.LocalBinder) service).getService();

      mGameEngine = new GameEngine(this, mP2pService);
      mGameEngine.setModelUpdateListener(mOwnFleetView, mEnemyFleetView);

      mGridButtonHandler.setGameEngine(mGameEngine);
      mP2pService.start();
   }


   // Only gets called when service has crashed!!
   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected(): Service has crashed!!");
      mP2pService = null;
   }


   public Handler getUiHandler()
   {
      return mUiHandler;
   }

   private void setup()
   {
      mShotClockView = (TextView) findViewById(R.id.shot_clock);
      ActionBar mActionBar = getActionBar();

      if (mActionBar != null) {
         LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         @SuppressLint("InflateParams") View scoreBoardView = inflater.inflate(R.layout.score, null);

         mConState = (ImageButton) scoreBoardView.findViewById(R.id.con_state);
         mEnemyShipsView = (TextView) scoreBoardView.findViewById(R.id.enemy_ships);
         mMyShipsView = (TextView) scoreBoardView.findViewById(R.id.my_ships);

         mActionBar.setDisplayShowHomeEnabled(false);
         mActionBar.setDisplayHomeAsUpEnabled(false);
         mActionBar.setDisplayUseLogoEnabled(false);
         mActionBar.setDisplayShowTitleEnabled(false);
         mActionBar.setDisplayShowCustomEnabled(true);

         mActionBar.setCustomView(scoreBoardView);
      }

      mGridButtonHandler = new GridButtonHandler(this);
      bindService(new Intent(this, P2pService.class), this, BIND_AUTO_CREATE);
   }

   private void buildGameBoard()
   {
      DisplayMetrics metrics = getResources().getDisplayMetrics();
      ViewGroup enemyFrame = (ViewGroup) findViewById(R.id.enemy_fleet_frame);
      ViewGroup ownFrame = (ViewGroup) findViewById(R.id.own_fleet_frame);

      int minEnemyFrameSize = enemyFrame.getMeasuredWidth() > enemyFrame.getMeasuredHeight() ?
              enemyFrame.getMeasuredHeight() : enemyFrame.getMeasuredWidth();

      int minOwnFrameSize = ownFrame.getMeasuredWidth() > ownFrame.getMeasuredHeight() ?
              ownFrame.getMeasuredHeight() : ownFrame.getMeasuredWidth();

      minEnemyFrameSize = 12 * (int) (minEnemyFrameSize / 12.0f);
      minOwnFrameSize = 12 * (int) (minOwnFrameSize / 12.0f);

      LayoutInflater inflater = LayoutInflater.from(this);

      View mEnemyBoard = inflater.inflate(R.layout.board, enemyFrame, false);
      mEnemyFleetView = new EnemyFleetView(this, (ViewGroup) mEnemyBoard, mGridButtonHandler, minEnemyFrameSize - 12);
      enemyFrame.addView(mEnemyBoard);

      View mOwnBoard = inflater.inflate(R.layout.board, enemyFrame, false);
      mOwnFleetView = new OwnFleetView(this, (ViewGroup) mOwnBoard, minOwnFrameSize - (int) (30 * metrics.density));
      ownFrame.addView(mOwnBoard);

      if (mGameEngine != null) {
         mGameEngine.setModelUpdateListener(mOwnFleetView, mEnemyFleetView);
      }

   }

   private void setMenuItemVisibility(final boolean... isVisibleList)
   {
      mMenu.findItem(R.id.connect_peer).setVisible(isVisibleList[0]);
      mMenu.findItem(R.id.new_game).setVisible(isVisibleList[1]);
      mMenu.findItem(R.id.pause_game).setVisible(isVisibleList[2]);
      mMenu.findItem(R.id.resume_game).setVisible(isVisibleList[3]);
      mMenu.findItem(R.id.abort_game).setVisible(isVisibleList[4]);
   }

   private void quitApp()
   {
      if (mP2pService != null) {
         mP2pService.setListener(null);
      }

      if (mGameEngine != null) {
         mGameEngine.shutDown();
      }
   }
}
