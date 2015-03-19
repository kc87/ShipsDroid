package c4.subnetzero.shipsdroid;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import c4.subnetzero.shipsdroid.p2p.P2pService;
import c4.subnetzero.shipsdroid.view.EnemyFleetView;
import c4.subnetzero.shipsdroid.view.OwnFleetView;

public class GameActivity extends Activity implements GameView
{
   private static final String LOG_TAG = "GameActivity";
   private static final int REQUEST_ENABLE_BT = 1;

   private GamePresenter mGamePresenter;
   private Menu mMenu;
   private Animation mBtnAnim;
   private EnemyFleetView mEnemyFleetView;
   private TextView mShotClockView;
   private TextView mEnemyShipsView;
   private TextView mMyShipsView;
   private ImageButton mConState;
   private volatile boolean isEnemyFleetViewEnabled;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate(savedInstanceState);
      setContentView(R.layout.game);
      mGamePresenter = new GamePresenter(this);
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
      mGamePresenter.onResume();
   }

   @Override
   protected void onPause()
   {
      Log.d(LOG_TAG, "onPause()");
      super.onPause();
      mGamePresenter.onPause();
   }

   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      mGamePresenter.onDestroy();
      unbindService(mGamePresenter);
      super.onDestroy();
   }

   @Override
   public void onBackPressed()
   {
      mGamePresenter.quit();
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
      mGamePresenter.executeMenuAction(item.getItemId());
      return true;
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      if (requestCode == REQUEST_ENABLE_BT) {
         mGamePresenter.startP2pService();
      }
   }

   private void setup()
   {
      mBtnAnim = AnimationUtils.loadAnimation(this, R.anim.grow);
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

      mEnemyFleetView = (EnemyFleetView)findViewById(R.id.enemy_fleet_view);
      mGamePresenter.initialize((OwnFleetView)findViewById(R.id.own_fleet_view),mEnemyFleetView);
      bindService(new Intent(this, P2pService.class), mGamePresenter, Context.BIND_AUTO_CREATE);
   }


   @Override
   public void enableBluetooth()
   {
      final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
   }

   @Override
   public void finishView()
   {
      finish();
   }

   @Override
   public void updateP2pState(final int bgResourceId)
   {
      final Drawable stateDrawable = getResources().getDrawable(bgResourceId);

      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mConState.setBackground(stateDrawable);
         }
      });
   }

   @Override
   public void updateShotClock(final String value)
   {
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mShotClockView.setText(value);
         }
      });
   }

   @Override
   public void updateScoreBoard(final String myScore, final String enemyScore)
   {
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mMyShipsView.setText(myScore);
            mEnemyShipsView.setText(enemyScore);
         }
      });
   }

   @Override
   public void setMenuItemVisibility(final boolean... isVisibleList)
   {
      if (isVisibleList.length != 5) {
         return;
      }

      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            if(mMenu != null) {
               mMenu.findItem(R.id.connect_peer).setVisible(isVisibleList[0]);
               mMenu.findItem(R.id.new_game).setVisible(isVisibleList[1]);
               mMenu.findItem(R.id.pause_game).setVisible(isVisibleList[2]);
               mMenu.findItem(R.id.resume_game).setVisible(isVisibleList[3]);
               mMenu.findItem(R.id.abort_game).setVisible(isVisibleList[4]);
            }
         }
      });
   }

   @Override
   public void setEnemyFleetViewEnabled(boolean enable, boolean newGame)
   {
      isEnemyFleetViewEnabled = enable;
      mEnemyFleetView.setEnabled(enable, newGame);
   }

   public View.OnClickListener getGridButtonHandler()
   {
      return mGridButtonHandler;
   }

   private View.OnClickListener mGridButtonHandler = new View.OnClickListener()
   {
      @Override
      public void onClick(final View view)
      {
         if(isEnemyFleetViewEnabled) {
            view.startAnimation(mBtnAnim);
            mGamePresenter.shoot(view.getId());
         }
      }
   };

}
