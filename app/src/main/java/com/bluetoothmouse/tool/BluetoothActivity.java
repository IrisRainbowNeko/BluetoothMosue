package com.bluetoothmouse.tool;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class BluetoothActivity extends Activity 
{
	// 获取到蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter;
	// 用来保存搜索到的设备信息
	private ArrayList<String> bluetoothDevices = new ArrayList<String>();
	private ArrayList<BluetoothDevice> bluetoothDevices_real = new ArrayList<BluetoothDevice>();
	
	// ListView组件
	private ListView lvDevices;
	// ListView的字符串数组适配器
	private ArrayAdapter<String> arrayAdapter;
	// UUID，蓝牙建立链接需要的
	private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// 选中发送数据的蓝牙设备，全局变量，否则连接在方法执行完就结束了
	private BluetoothDevice selectDevice;
	
	@Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		// 获取到蓝牙默认的适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// 获取到ListView组件
		lvDevices = (ListView) findViewById(R.id.lvDevices);
		// 为listview设置字符换数组适配器
		arrayAdapter = new ArrayAdapter<String>(this,
												android.R.layout.simple_list_item_1, android.R.id.text1,
												bluetoothDevices);
		// 为listView绑定适配器
		lvDevices.setAdapter(arrayAdapter);
		// 为listView设置item点击事件侦听
		lvDevices.setOnItemClickListener(new ListView.OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
					if (mBluetoothAdapter.isDiscovering()) {
						mBluetoothAdapter.cancelDiscovery();
					}
					selectDevice=bluetoothDevices_real.get(position);
					new ConnectThread(selectDevice).start();
				}
			});

		// 用Set集合保持已绑定的设备
		Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
		if (devices.size() > 0) {
			for (BluetoothDevice bluetoothDevice : devices) {
				// 保存到arrayList集合中
				bluetoothDevices.add(bluetoothDevice.getName() + ":"
									 + bluetoothDevice.getAddress() + "\n");
				bluetoothDevices_real.add(bluetoothDevice);
			}
		}
		// 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的
		// 这里注册找到设备和完成搜索广播
		IntentFilter filter = new IntentFilter(
			BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, filter);
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(receiver, filter);

    }
	public void onSearch(View view) {
		Toast.makeText(this,"正在扫描...",2000).show();
		// 点击搜索周边设备，如果正在搜索，则暂停搜索
		if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		}
		mBluetoothAdapter.startDiscovery();
	}
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			// 获取到广播的action
			String action = intent.getAction();
			// 判断广播是搜索到设备还是搜索完成
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				// 找到设备后获取其设备
				BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// 判断这个设备是否是之前已经绑定过了，如果是则不需要添加，在程序初始化的时候已经添加了
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					// 设备没有绑定过，则将其保持到arrayList集合中
					bluetoothDevices.add(device.getName() + ":"
										 + device.getAddress() + "\n");
					bluetoothDevices_real.add(device);
					// 更新字符串数组适配器，将内容显示在listView中
					arrayAdapter.notifyDataSetChanged();
				}
			} else if (action
					   .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				setTitle("搜索完成");
			}
		}
	};
	class ConnectThread extends Thread {  
        private BluetoothSocket clientSocket;  
        private final BluetoothDevice mmDevice;  
        public ConnectThread(BluetoothDevice device) {  
            mmDevice = device;  
            // 得到一个bluetoothsocket  
            try {  
                clientSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);  
            } catch (IOException e) {  
                clientSocket = null;  
            }  
        }  

        public void run() {  
			try {
				// This is a blocking call and will only return on a successful connection or an exception
				clientSocket.connect();
			} catch (IOException e) {
				Log.e("fuck", e.toString());
				//  e.printStackTrace();

				try {
					clientSocket =(BluetoothSocket) selectDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class}).invoke(selectDevice,1);
					clientSocket.connect();
					Log.i("666","Connected");
				} catch (Exception e2) {
					Log.e("fuck", "Couldn't establish Bluetooth connection!");
					try {
						clientSocket.close();
					} catch (IOException e3) {
						//Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e3);
					}
					//return;
				}
			}
			MainService.recivebs=clientSocket;
			Intent intent=new Intent(BluetoothActivity.this,MainService.class);
			startService(intent); 
			Intent intent_home = new Intent();
			intent_home.setAction("android.intent.action.MAIN");
			intent_home.addCategory("android.intent.category.HOME");
			startActivity(intent_home);
        }  

        public void cancel() {  
            try {  
                clientSocket.close();  
            } catch (IOException e) {  
                
            }  
        }  
	}
	
}
