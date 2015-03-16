package c4.subnetzero.shipsdroid.p2p.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import c4.subnetzero.shipsdroid.p2p.P2pConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class BtConnector extends P2pConnector
{
   private static final String LOG_TAG = "BtConnector";
   private static final String UUID_STRING = "069c9397-7da9-4810-849c-f52f6b1dead";
   private BluetoothAdapter mBluetoothAdapter;
   private BluetoothDevice mRemoteDevice;
   private volatile AcceptThread mAcceptThread;
   private volatile ConnectThread mConnectThread;
   private volatile ConnectedThread mConnectedThread;


   public BtConnector(final Listener listener)
   {
      super(listener);
      mState = State.DISABLED;
   }

   public boolean isBtEnabled()
   {
      return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
   }

   public void startDiscovery()
   {
      if (mBluetoothAdapter != null && !mBluetoothAdapter.isDiscovering()) {
         mBluetoothAdapter.startDiscovery();
      }
   }

   public void stopDiscovery()
   {
      if (mBluetoothAdapter != null) {
         mBluetoothAdapter.cancelDiscovery();
      }
   }

   @Override
   public void connectPeer()
   {
      if (mRemoteDevice == null) {
         mRemoteDevice = findRemoteDevice();
      }

      if (mRemoteDevice != null && mState == State.DISCONNECTED && mConnectThread == null) {
         mConnectThread = new ConnectThread(mRemoteDevice);
         mConnectThread.start();
      }
   }


   @Override
   public void disconnectPeer()
   {
      if (mConnectedThread != null) {
         mConnectedThread.close();
      }
   }

   @Override
   public void sendString(final String msgStr)
   {
      ConnectedThread connectedThread;
      synchronized (this) {
         if (mState != State.CONNECTED) {
            return;
         }
         connectedThread = mConnectedThread;
      }

      try {
         byte[] pktData = msgStr.getBytes("UTF-8");
         connectedThread.write(pktData);
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e.getMessage());
      }
   }

   @Override
   public void start()
   {
      setup();
      if(mState != State.DISABLED) {
         startAcceptThread();
      }
   }

   @Override
   public void stop()
   {
      disconnectPeer();

      if (mAcceptThread != null) {
         mAcceptThread.close();
      }

      stopDiscovery();
   }

   public void startAcceptThread()
   {
      if (mConnectedThread != null) {
         mConnectedThread.close();
      }

      if (mAcceptThread == null) {
         mAcceptThread = new AcceptThread();
         mAcceptThread.start();
      }
   }

   private void setup()
   {
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

      if (mBluetoothAdapter == null) {
         setState(State.DISABLED);
         return;
      }

      if (!mBluetoothAdapter.isEnabled()) {
         setState(State.DISABLED);
      }else {
         setState(State.DISCONNECTED);
      }
   }

   private BluetoothDevice findRemoteDevice()
   {
      BluetoothDevice remoteDevice = null;
      Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

      if (pairedDevices.size() > 0) {
         for (BluetoothDevice device : pairedDevices) {
            if (!device.getAddress().equals("00:00:DE:AD:BE:EF")) {
               Log.d(LOG_TAG,"Remote Device: "+device.getAddress());
               remoteDevice = device;
               break;
            }
         }
      }

      return remoteDevice;
   }

   private class ConnectThread extends Thread
   {
      private static final String LOG_TAG = "ConnectThread";
      private final BluetoothSocket mmSocket;
      private final BluetoothDevice mmDevice;

      public ConnectThread(final BluetoothDevice bluetoothDevice)
      {
         BluetoothSocket tmp = null;
         mmDevice = bluetoothDevice;

         try {
            tmp = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
         } catch (final Exception e) {
            Log.e(LOG_TAG, "Unable to get connection socket: ", e);
            throw new RuntimeException(e.getMessage());
         }
         mmSocket = tmp;
      }


      @Override
      public void run()
      {
         if (mmSocket == null) {
            mConnectThread = null;
            return;
         }

         try {
            setState(BtConnector.State.CONNECTING);
            mmSocket.connect();
         } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            setState(BtConnector.State.DISCONNECTED);
            close();
            return;
         }

         if(mConnectedThread == null) {
            setState(BtConnector.State.CONNECTED);
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
         }

         mConnectThread = null;
      }

      public void close()
      {
         try {
            mmSocket.close();
         } catch (IOException e) {
            /* IGNORED */
         } finally {
            mConnectThread = null;
         }
      }
   }

   private class AcceptThread extends Thread
   {
      private static final String LOG_TAG = "AcceptThread";
      private final BluetoothServerSocket mmServerSocket;

      public AcceptThread()
      {
         BluetoothServerSocket tmp = null;
         try {
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(LOG_TAG, UUID.fromString(UUID_STRING));
         } catch (final Exception e) {
            Log.e(LOG_TAG, "Unable to get listen socket: ", e);
            throw new RuntimeException(e.getMessage());
         }
         mmServerSocket = tmp;
      }

      @Override
      public void run()
      {
         BluetoothSocket connectionSocket;
         setName(LOG_TAG);

         if (mmServerSocket == null) {
            mAcceptThread = null;
            return;
         }

         try {
            connectionSocket = mmServerSocket.accept();
         } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            close();
            return;
         }

         if(mConnectedThread == null) {
            setState(BtConnector.State.CONNECTED);
            mConnectedThread = new ConnectedThread(connectionSocket);
            mConnectedThread.start();
         }

         close();
      }

      public void close()
      {
         try {
            mmServerSocket.close();
         } catch (IOException e) {
            /* IGNORED */
         } finally {
            mAcceptThread = null;
         }
      }
   }

   private class ConnectedThread extends Thread
   {
      private static final String LOG_TAG = "ConnectedThread";
      private final BluetoothSocket mmSocket;
      private final InputStream mmInStream;
      private final OutputStream mmOutStream;

      public ConnectedThread(BluetoothSocket socket)
      {
         setName(LOG_TAG);
         InputStream tmpIn = null;
         OutputStream tmpOut = null;
         mmSocket = socket;

         try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
         } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
         }

         mmInStream = tmpIn;
         mmOutStream = tmpOut;
      }

      @Override
      public void run()
      {
         byte[] buffer = new byte[1024];
         int bytes;

         if (mmInStream == null || mmOutStream == null) {
            close();
            return;
         }

         while (true) {
            try {
               bytes = mmInStream.read(buffer);
               parsePacket(new String(buffer, 0, bytes, "UTF-8"));
            } catch (Exception e) {
               Log.e(LOG_TAG, e.getMessage());
               close();
               /*
               if (e instanceof IOException) {
                  startAcceptThread();
               }*/
               break;
            }
         }
      }

      public void write(byte[] bytes)
      {
         if (mmSocket.isConnected()) {
            try {
               mmOutStream.write(bytes);
               mmOutStream.flush();
            } catch (IOException e) {
               /* IGNORED */
            }
         }
      }

      public void close()
      {
         try {
            mmSocket.close();
         } catch (Exception e) {
            /* IGNORED */
         } finally {
            mConnectedThread = null;
            setState(BtConnector.State.DISCONNECTED);
         }
      }

   }


}
