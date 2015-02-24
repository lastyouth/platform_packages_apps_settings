package com.android.settings;

import java.util.ArrayList;

import com.android.settings.R;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.IDudiManagerService;
import android.os.IDudiFloatService;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class TopView extends Service{
	private Button registerButton;
	private Button sendCommandButton;
	private Button sendTouchEventButton;
	private final String TAG = "DudiFloatService";
	
	private static final int REGISTER_BUTTON = 1;
	private static final int SEND_KEY_EVENT_BUTTON = 2;
	private static final int SEND_TOUCH_EVENT_BUTTON = 3;
	
	private WindowManager.LayoutParams mParamsForRegisterButton;
	private WindowManager.LayoutParams mParamsForSendCommandButton;
	private WindowManager.LayoutParams mParamsForSendTouchEventButton;
	private WindowManager mWindowManager;
	//sbh
	private IDudiManagerService sysService;
	private ArrayList<Button> buttonList;
	
	private float START_X, START_Y;
	private int PREV_X, PREV_Y;
	private int MAX_X = -1, MAX_Y = -1;
	private boolean singletap = false;
	
	private DudiFloatServiceImpl impl = new DudiFloatServiceImpl();
	
	public class DudiFloatServiceImpl extends IDudiFloatService.Stub{
		
		public void notifyCurrentTopActivityName(String name) throws RemoteException
		{
			Log.i(TAG,"notifyCurrentTopActivityName : "+name);
			if(name != null)
			{
				if(name.startsWith("com.android.launcher"))
				{
					Log.i(TAG,"android launcher detected");
					toggleButtonState(false);
				}
				else
				{
					Log.i(TAG,"show button again");
					toggleButtonState(true);
				}
			}
		}
	}
	
	private OnClickListener mViewClickListener = new OnClickListener(){
		@Override
		public void onClick(View v){
			// application start
			switch(v.getId())
			{
			case SEND_KEY_EVENT_BUTTON:
				{
					if(singletap == true)
					{
						singletap = false;
						try {
							KeyEvent backKeyDownEvent = new KeyEvent(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),
									KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0,0,-1,0,0x48,0x101);
							sysService.sendCurrentActivityToKeyEvent(backKeyDownEvent);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//Toast.makeText(getApplicationContext(), "on Click", Toast.LENGTH_SHORT).show();	
					}
				}
				break;
			case REGISTER_BUTTON:
				{
					try{
						sysService.registerCurrentTopActivity();
					}catch(RemoteException e)
					{
						e.printStackTrace();
					}
				}
				break;
			case SEND_TOUCH_EVENT_BUTTON:
				{
					
					int X = mParamsForSendTouchEventButton.x;
					int Y = mParamsForSendTouchEventButton.y;
						
					try{
						MotionEvent.PointerCoords[] pc = MotionEvent.PointerCoords.createArray(1);
						MotionEvent.PointerProperties[] pp = MotionEvent.PointerProperties.createArray(1);
						
						pc[0].clear();
						
						pc[0].x = X;
						pc[0].y = Y;
						pc[0].pressure = MotionEvent.AXIS_PRESSURE;
						pc[0].size = MotionEvent.AXIS_SIZE;
						
						pp[0].clear();
						pp[0].id = 4;
						pp[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
						
						MotionEvent event = MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),
								android.os.SystemClock.uptimeMillis(),
								MotionEvent.ACTION_DOWN,
								1,
								pp,
								pc,
								0,
								0,
								(float)X,(float)Y,4,0,0x1002,0);	
						sysService.sendCurrentActivityToTouchEvent(event);
						event.setAction(MotionEvent.ACTION_UP);
						sysService.sendCurrentActivityToTouchEvent(event);
					}catch(RemoteException e)
					{
					e.printStackTrace();
					}
				}
				break;
			}
		}
	};
	
	private OnTouchListener mViewTouchListener = new OnTouchListener(){
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{			

			switch(event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
				singletap = true;
				if(MAX_X == -1)
					setMaxPosition();
				START_X = event.getRawX();
				START_Y = event.getRawY();
				if(v.getId() == REGISTER_BUTTON)
				{
					PREV_X = mParamsForRegisterButton.x;
					PREV_Y = mParamsForRegisterButton.y;
				}
				else if(v.getId() == SEND_KEY_EVENT_BUTTON)
				{
					PREV_X = mParamsForSendCommandButton.x;
					PREV_Y = mParamsForSendCommandButton.y;
				}else if(v.getId() == SEND_TOUCH_EVENT_BUTTON)
				{
					PREV_X = mParamsForSendTouchEventButton.x;//(int) event.getX();
					PREV_Y = mParamsForSendTouchEventButton.y;//(int) event.getY();
				}
				break;
				
			case MotionEvent.ACTION_MOVE:
				singletap = false;
				int x = (int)(event.getRawX() - START_X);
				int y = (int)(event.getRawY() - START_Y);
				
				if(v.getId() == REGISTER_BUTTON)
				{
					optimizePosition(mParamsForRegisterButton);
					mParamsForRegisterButton.x = PREV_X+x;
					mParamsForRegisterButton.y = PREV_Y+y;
					mWindowManager.updateViewLayout(registerButton, mParamsForRegisterButton);
				}
				else if(v.getId() == SEND_KEY_EVENT_BUTTON)
				{
					optimizePosition(mParamsForSendCommandButton);
					mParamsForSendCommandButton.x = PREV_X + x;
					mParamsForSendCommandButton.y = PREV_Y + y;
					mWindowManager.updateViewLayout(sendCommandButton, mParamsForSendCommandButton);
				}else if(v.getId() == SEND_TOUCH_EVENT_BUTTON)
				{
					optimizePosition(mParamsForSendTouchEventButton);
					mParamsForSendTouchEventButton.x = PREV_X +x;
					mParamsForSendTouchEventButton.y = PREV_Y +y;
					mWindowManager.updateViewLayout(sendTouchEventButton, mParamsForSendTouchEventButton);
				}
				break;
			}

			return false;
		}
	};
	
	private void setMaxPosition()
	{
		DisplayMetrics matrix = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(matrix);
		
		MAX_X = matrix.widthPixels-registerButton.getWidth();
		MAX_Y = matrix.heightPixels-registerButton.getHeight();
		
	}
	
	private void optimizePosition(WindowManager.LayoutParams myParams)
	{
		if(myParams.x > MAX_X) myParams.x = MAX_X;
		if(myParams.y > MAX_Y) myParams.y = MAX_Y;
		if(myParams.x < 0) myParams.x = 0;
		if(myParams.y < 0) myParams.y = 0;
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return (IBinder) impl;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		return super.onStartCommand(intent, flags, startId);
	}
	
	
	@Override
	public void onCreate(){
		super.onCreate();
		sysService = IDudiManagerService.Stub.asInterface(ServiceManager.getService("DudiManagerService"));
		buttonList = new ArrayList<Button>();
		
		
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		
		addButton();
		
		// send connect order to DudiManagerService
		
		try
		{
			sysService.bindWithFloatService();
		}catch(RemoteException e)
		{
			
		}
	}
	private void addButton()
	{
		buttonList.clear();
		
		Point mPoint = new Point();
		Display mDisplay = mWindowManager.getDefaultDisplay();
		mDisplay.getSize(mPoint);
		
		registerButton = new Button(this);
		registerButton.setOnClickListener(mViewClickListener);
		registerButton.setOnTouchListener(mViewTouchListener);
		registerButton.setAlpha(255);
		registerButton.setBackgroundResource(R.drawable.ic_sync_red);
		registerButton.setId(REGISTER_BUTTON);
		
		sendCommandButton = new Button(this);
		sendCommandButton.setOnClickListener(mViewClickListener);
		sendCommandButton.setOnTouchListener(mViewTouchListener);
		sendCommandButton.setAlpha(255);
		sendCommandButton.setBackgroundResource(R.drawable.ic_sync_green);
		sendCommandButton.setId(SEND_KEY_EVENT_BUTTON);
		
		sendTouchEventButton = new Button(this);
		sendTouchEventButton.setOnClickListener(mViewClickListener);
		sendTouchEventButton.setOnTouchListener(mViewTouchListener);
		sendTouchEventButton.setAlpha(255);
		sendTouchEventButton.setBackgroundResource(R.drawable.ic_btn_next);
		sendTouchEventButton.setId(SEND_TOUCH_EVENT_BUTTON);
		
		mParamsForRegisterButton = new WindowManager.LayoutParams(
				mPoint.x/10,
				mPoint.y/15,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.RGBA_8888);
		mParamsForRegisterButton.gravity = Gravity.LEFT | Gravity.TOP;
		mParamsForRegisterButton.verticalMargin = 0.01f;
		mParamsForRegisterButton.horizontalMargin = 0.01f;
		
		mWindowManager.addView(registerButton,  mParamsForRegisterButton);
		
		mParamsForSendCommandButton = new WindowManager.LayoutParams(mPoint.x/10,mPoint.y/15,
				mPoint.x+25,mPoint.y+25,WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.RGBA_8888);
		mParamsForSendCommandButton.gravity = Gravity.LEFT | Gravity.TOP;
		mParamsForSendCommandButton.verticalMargin = 0.01f;
		mParamsForSendCommandButton.horizontalMargin = 0.01f;
		
		mWindowManager.addView(sendCommandButton, mParamsForSendCommandButton);
		
		mParamsForSendTouchEventButton = new WindowManager.LayoutParams(mPoint.x/10,mPoint.y/15,
				250,100,WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.RGBA_8888);
		mParamsForSendTouchEventButton.gravity = Gravity.LEFT | Gravity.TOP;
		mParamsForSendTouchEventButton.verticalMargin = 0.01f;
		mParamsForSendTouchEventButton.horizontalMargin = 0.01f;
		
		mWindowManager.addView(sendTouchEventButton, mParamsForSendTouchEventButton);
		
		buttonList.add(registerButton);
		buttonList.add(sendCommandButton);
		buttonList.add(sendTouchEventButton);
	}
	private void removeButton()
	{
		for(Button target : buttonList)
		{
			mWindowManager.removeView(target);
			target = null;
		}
	}
	private void toggleButtonState(boolean show)
	{
		for(Button target : buttonList)
		{
			if(target != sendCommandButton)
			{
				if(show)
				{
					
					if(target.getVisibility() != View.VISIBLE)
					{
						Log.i(TAG,"setVisibility View.VISIBLE");
						target.setVisibility(View.VISIBLE);
						
						switch(target.getId())
						{
						case REGISTER_BUTTON:
							mWindowManager.updateViewLayout(target, mParamsForRegisterButton);
							break;
						case SEND_TOUCH_EVENT_BUTTON:
							mWindowManager.updateViewLayout(target, mParamsForSendTouchEventButton);
							break;
						}
					}
				}
				else
				{
					if(target.getVisibility() == View.VISIBLE)
					{
						Log.i(TAG,"setVisibility View.INVISIBLE");
						target.setVisibility(View.GONE);
						//mWindowManager.updateViewLayout(target, target.getLayoutParams());
						switch(target.getId())
						{
						case REGISTER_BUTTON:
							mWindowManager.updateViewLayout(target, mParamsForRegisterButton);
							break;
						case SEND_TOUCH_EVENT_BUTTON:
							mWindowManager.updateViewLayout(target, mParamsForSendTouchEventButton);
							break;
						}
					}
				}
				
			}
		}
	}

	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		removeButton();
		mWindowManager = null;
		// send disconnect order to DudiManagerService
	}

}