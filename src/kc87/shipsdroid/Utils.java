package kc87.shipsdroid;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;


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
               Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
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

}
