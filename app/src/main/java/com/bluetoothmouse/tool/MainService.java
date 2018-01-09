package com.bluetoothmouse.tool;

import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.WindowManager.*;
import android.widget.*;
import java.io.*;

import java.lang.Process;
import android.util.*;
import android.graphics.drawable.*;

public class MainService extends Service 
{
	public static float rate=6;
	public static MainService ms;
	public static int fblx=1080,fbly=1920;
	private int sbw,sbh;
	
	public static BluetoothSocket recivebs;
	Context mContext;
	WindowManager mWinMng;
	LayoutParams param ;
	ImageView imv;
	
	OutputStream os;
	
	@Override
	public IBinder onBind(Intent p1)
	{
		// TODO: Implement this method
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		new ReceiveThread(recivebs).start();
		mContext = getApplicationContext();
		mWinMng = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		addView();
	}
	
	public void addView()
	{
		if(imv == null)
		{
			imv = new ImageView(mContext);
			imv.setImageResource(R.drawable.sb);
			Bitmap sb=((BitmapDrawable)(imv.getDrawable())).getBitmap();
			sbw=sb.getWidth();
			sbh=sb.getHeight();
			
			param=new LayoutParams();
			param.type =2010;//让悬浮窗显示在最上层
			param.format = PixelFormat.RGBA_8888;
			param.flags =WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
			// mParam.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
			param.gravity=Gravity.TOP;
			param.width =LayoutParams.WRAP_CONTENT;
			param.height =LayoutParams.WRAP_CONTENT;
			param.x=0;
			param.y=fbly/2;
			mWinMng.addView(imv, param);
		}
	}

	public void removeView()
	{
		if(imv != null)
		{
			mWinMng.removeView(imv);
			imv = null;
		}
	}
	
	private void execShellCmd(String cmd) {

		try {
			// 申请获取root权限，这一步很重要，不然会没有作用
			Process process = Runtime.getRuntime().exec("su");
			// 获取输出流
			OutputStream outputStream = process.getOutputStream();
			outputStream.write(cmd.getBytes());
			outputStream.flush();
			outputStream.close();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg)
		{
			
			if(msg.what==0){
				Toast.makeText(mContext,(String)(msg.obj),2000).show();
			}else if(msg.what==1){
				String[] res=((String)(msg.obj)).split(" ");
				String head=res[0].trim();
				//Toast.makeText(mContext,(String)(msg.obj),2000).show();
				switch(head){
					case "back":
						execShellCmd("input keyevent 4");
						break;
					case "home":
						execShellCmd("input keyevent 3");
						break;
					case "power":
						execShellCmd("input keyevent 26");
						break;
					case "swipe":
						int swx=(int)(Double.parseDouble(res[1])*fblx/2),swy=(int)(Double.parseDouble(res[2])*fblx/2);
						execShellCmd("input swipe "+(param.x+fblx/2-1-sbw/2)+" "+(param.y-1)+" "+(param.x+fblx/2-1-sbw/2+swx)+" "+(param.y-1+swy));
						break;
					case "tap":
						execShellCmd("input tap "+(param.x+fblx/2-1-sbw/2)+" "+(param.y-1));
						break;
					case "X":
						param.x+=Double.parseDouble(res[1])*rate;
						param.x=Math.min(Math.max(-fblx/2,param.x),fblx/2+sbw/2);
						if(param.x+fblx/2>fblx-sbw/2)imv.setTranslationX(param.x+fblx/2-(fblx-sbw/2));
						else imv.setTranslationX(0);
						mWinMng.updateViewLayout(imv, param);
						break;
					case "Y":
						param.y+=Double.parseDouble(res[1])*rate;
						param.y=Math.min(Math.max(0,param.y),fbly);
						if(param.y>fbly-sbh)imv.setTranslationY(param.y-(fbly-sbh));
						else imv.setTranslationY(0);
						mWinMng.updateViewLayout(imv, param);
						break;
					case "toswipe":
						Toast.makeText(mContext,"滑动模式",2000).show();
						break;
					case "totap":
						Toast.makeText(mContext,"点击模式",2000).show();
						break;
				}
			}
			
		}
	};
	class ReceiveThread extends Thread {  
        private BluetoothSocket clientSocket;  
        OutputStream os;
		InputStream is;
		String text="";

		public ReceiveThread(BluetoothSocket msocket) {  
            clientSocket=msocket;
			try
			{
				os = clientSocket.getOutputStream();
				is=clientSocket.getInputStream();
			}
			catch (IOException e)
			{}
        }  
        public void run() {  
			handler.obtainMessage(0,"连接成功").sendToTarget();
			// 监听输入流  
			while (true) {  
				try {  
					byte[] buffer = new byte[128];  
					// 读取输入流  
					int count = is.read(buffer);  
					// 发送获得的字节的ui activity  
					int pointer=0;
					if(count!=-1){
						text+=new String(buffer,0,count);
						while((pointer=text.indexOf("\n"))!=-1){
							handler.obtainMessage(1,text.substring(0,pointer)).sendToTarget();
							text=text.substring(pointer+1);
						}
					}

				} catch (IOException e) {  
					break;  
				}  
			}  
        }    
	}
}
