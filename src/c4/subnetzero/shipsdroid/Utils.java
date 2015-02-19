package c4.subnetzero.shipsdroid;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Utils
{
   private static final String LOG_TAG = "Utils";
   public static volatile boolean sIsDialogOpen = false;
   private static AlertDialog sOkDialog = null;

   private Utils()
   {
      throw new IllegalStateException();
   }

   public static void showToast(final Context context, final int resourceId)
   {
      Utils.showToast(context, context.getString(resourceId));
   }

   public static void showToast(final Context context, final String toastMsg)
   {
      if (context != null) {
         ((Activity) context).runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
            }
         });
      }
   }

   public static void showOkMsg(final Context context, final int resourceId, final DialogInterface.OnClickListener action)
   {
      Utils.showOkMsg(context, context.getString(resourceId), action);
   }

   public static void showOkMsg(final Context context, final String okMsg, final DialogInterface.OnClickListener action)
   {
      if (context == null || Utils.sIsDialogOpen) {
         return;
      }

      Utils.sIsDialogOpen = true;

      ((Activity) context).runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            if (sOkDialog == null) {
               sOkDialog = new AlertDialog.Builder(context).create();
               sOkDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener()
               {
                  @Override
                  public void onClick(DialogInterface dialog, int which)
                  {
                     Utils.sIsDialogOpen = false;
                  }
               });
               sOkDialog.setTitle("ShipsDroid");
               sOkDialog.setCancelable(false);
            }

            sOkDialog.setMessage(okMsg);
            sOkDialog.show();
         }
      });
   }

   public static void closeOkMsg(final Context context)
   {
      if (context == null || !Utils.sIsDialogOpen) {
         return;
      }

      ((Activity) context).runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            if (sOkDialog != null) {
               sOkDialog.dismiss();
            }
            Utils.sIsDialogOpen = false;
         }
      });
   }


   public static Inet4Address getLocalIpAddress()
   {

      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface networkInterface = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
               InetAddress inetAddress = enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) {
                  if (inetAddress instanceof Inet4Address) {
                     return (Inet4Address)inetAddress;
                  }
               }
            }
         }
      } catch (Exception e) {
         Log.e(LOG_TAG, "getLocalIpAddress()",e);
      }

      Inet4Address addr = null;
      try { addr = (Inet4Address)Inet4Address.getByName("1.1.1.1"); } catch (Exception e) {/*IGNORED*/}
      return addr;
   }


   public static void launchWifiSettings(final Activity activity)
   {
      try {
         Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
         activity.startActivity(intent);
         //activity.startActivityForResult(intent,0);
      } catch (ActivityNotFoundException e) {
         Log.e(LOG_TAG, "Unable to launch wifi settings activity", e);
         activity.runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               Toast.makeText(activity, "Please enable Wifi", Toast.LENGTH_SHORT).show();
            }
         });
      }
   }

}
