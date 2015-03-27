package kc87.shipsdroid;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import kc87.shipsdroid.view.EnemyFleetView;
import kc87.shipsdroid.view.OwnFleetView;

public class GameActivity extends Activity implements GameView
{
   private static final String LOG_TAG = "GameActivity";
   private static final int REQUEST_ENABLE_BT = 1;

   private GamePresenter mGamePresenter;
   private Menu mMenu;
   private AlertDialog mOkDialog;
   private Toast mMsgToast;
   private EnemyFleetView mEnemyFleetView;
   private TextView mShotClockView;
   private TextView mEnemyShipsView;
   private TextView mMyShipsView;
   private ImageButton mConState;
   private ViewState mViewState;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate(savedInstanceState);
      setContentView(R.layout.game);
      mGamePresenter = new GamePresenter(this, ((ShipsDroidApp) getApplication()).getEngine());
      setup(savedInstanceState);
   }

   @Override
   protected void onRestart()
   {
      Log.d(LOG_TAG, "onRestart()");
      super.onRestart();
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
      mGamePresenter.onPause(isChangingConfigurations());
   }

   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      if(mOkDialog.isShowing()){
         mOkDialog.cancel();
      }
      mGamePresenter.onDestroy(isChangingConfigurations());
      super.onDestroy();
   }


   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      Log.d(LOG_TAG, "onSaveInstanceState()");
      mViewState.toBundle(outState);
      super.onSaveInstanceState(outState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      Log.d(LOG_TAG, "onCreateOptionsMenu()");
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.game, menu);
      mMenu = menu;
      setMenuItemVisibility(mViewState.mVisibilityList);
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
         mGamePresenter.restartConnector();
      }
   }

   private void setup(final Bundle savedInstanceState)
   {
      mViewState = new ViewState();
      mShotClockView = (TextView) findViewById(R.id.shot_clock);
      mMsgToast = Toast.makeText(this,"HuiBoo",Toast.LENGTH_LONG);
      mOkDialog = new AlertDialog.Builder(this).create();
      mOkDialog.setTitle("ShipsDroid");
      mOkDialog.setCancelable(false);
      mOkDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener()
      {
         @Override
         public void onClick(DialogInterface dialog, int which)
         {
            mGamePresenter.onDialogOkClick();
         }
      });

      mOkDialog.setOnShowListener(new DialogInterface.OnShowListener()
      {
         @Override
         public void onShow(DialogInterface dialog)
         {
            mGamePresenter.onDialogShow();
         }
      });

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

      mEnemyFleetView = (EnemyFleetView) findViewById(R.id.enemy_fleet_view);
      mEnemyFleetView.setPresenter(mGamePresenter);
      mViewState.fromBundle(savedInstanceState);
      applyViewState(mViewState);
      mGamePresenter.initialize((OwnFleetView) findViewById(R.id.own_fleet_view), mEnemyFleetView);
   }


   private void applyViewState(final ViewState viewState)
   {
      updateShotClock(viewState.mShotClockValue);
      updateP2pState(viewState.mStateBgResourceId);
      updateScoreBoard(viewState.mMyScore, viewState.mEnemyScore);
      setEnemyFleetViewEnabled(viewState.mEnemyFleetEnabled, false);
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
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mViewState.mStateBgResourceId = bgResourceId;
            mConState.setBackgroundResource(bgResourceId);
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
            mViewState.mShotClockValue = value;
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
            mViewState.mMyScore = myScore;
            mViewState.mEnemyScore = enemyScore;
            mMyShipsView.setText(myScore);
            mEnemyShipsView.setText(enemyScore);
         }
      });
   }

   @Override
   public void setMenuItemVisibility(final boolean[] isVisibleList)
   {
      if (isVisibleList == null || isVisibleList.length != 5) {
         return;
      }

      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            if (mMenu != null) {
               mViewState.mVisibilityList = isVisibleList;
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
      mViewState.mEnemyFleetEnabled = enable;
      mEnemyFleetView.setEnabled(enable, newGame);
   }

   @Override
   public void showToast(final int resourceId)
   {
      showToast(getString(resourceId));
   }

   @Override
   public void showToast(final String toastMsg)
   {
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mMsgToast.setDuration(Toast.LENGTH_LONG);
            mMsgToast.setText(toastMsg);
            mMsgToast.show();
         }
      });
   }

   @Override
   public void showOkDialog(final int resourceId)
   {
      showOkDialog(getString(resourceId));
   }

   @Override
   public void showOkDialog(final String okMsg)
   {
      runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            mOkDialog.setMessage(okMsg);
            mOkDialog.show();
         }
      });
   }

   private class ViewState
   {
      public String mEnemyScore = "0";
      public String mMyScore = "0";
      public String mShotClockValue = "0";
      public int mStateBgResourceId = R.drawable.state_gray_bg;
      public boolean mEnemyFleetEnabled = false;
      public boolean[] mVisibilityList = new boolean[0];

      public void toBundle(final Bundle viewBundle)
      {
         viewBundle.putBoolean("enemy_fleet_enabled", mEnemyFleetEnabled);
         viewBundle.putString("shot_clock", mShotClockValue);
         viewBundle.putString("enemy_score", mEnemyScore);
         viewBundle.putString("my_score", mMyScore);
         viewBundle.putInt("state_bg_id", mStateBgResourceId);
         viewBundle.putBooleanArray("menu_item_visibility", mVisibilityList);
      }

      public void fromBundle(final Bundle viewBundle)
      {
         if (viewBundle != null) {
            mVisibilityList = viewBundle.getBooleanArray("menu_item_visibility");
            mShotClockValue = viewBundle.getString("shot_clock");
            mStateBgResourceId = viewBundle.getInt("state_bg_id");
            mMyScore = viewBundle.getString("my_score");
            mEnemyScore = viewBundle.getString("enemy_score");
            mEnemyFleetEnabled = viewBundle.getBoolean("enemy_fleet_enabled");
         }
      }
   }
}
