/*
   Copyright 2012, Telum Slavonski Brod, Croatia.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   This file is part of QFramework project, and can be used only as part of project.
   Should be used for peace, not war :)   
 */

package com.qframework.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class GameonApp{

	private GameonWorld mWorld;
	private LayoutGrid mDataGrid;
	private ServerkoParse mScript;
	private GameonWorldView mView;
	private String	mPreExec;

	private  String mAppName;
	private Vector <JSONObject> mResponsesQueue =  new Vector <JSONObject>();
	private float[] mScreenb = new float[8];
	private float[] mHudb = new float[8];        
	protected float mSplashX1 = -1.5f;
	protected float mSplashX2 = 1.5f;
	protected float mSplashY1 = -1.0f;
	protected float mSplashY2 = 1.0f;
	protected String mSplashScreen;

	private long mSplashTime;
	private long mSplashTimeStart;
	private boolean mDrawSPlash = false;
	private boolean mSplashOutStart = false;
	private boolean mTouchEnabled = true;
	private AreaIndexPair		mFocused;

	private AnimFactory		mAnims;
	private ColorFactory		mColors;
	private ItemFactory		mItems;
	private TextureFactory	mTextures;
	private SoundFactory		mSounds;
	private Settings			mSettings;
	private ObjectsFactory	mObjectsFact;
	private Box2dWrapper	mBox2dWrapper;
	//private GameonCS			mCS;

	private Context mContext;
	private boolean	mCameraSet = false;
	private Random mRandom = new Random(System.currentTimeMillis());

	private boolean 			mDataChange = false;
	private long mFrameDeltaTime = -1;
	private long mFrameLastTime = -1;

	private EAGLViewInterface mEaglView;
	private boolean			  mRendering  = false;

	private float mLastDrag[] = new float[3];
	private float mLastDist = 0;
	private long mLastDragTime = 0;
	private long mLastClickTime = 0;
	protected boolean mSupportOld = false;

	private String 	mOnTouchCallback = null;
	private String 	mOnTouchEndCallback = null;
	private String 	mOnTouchStartCallback = null;
        private boolean mRenderThisFrame = false;
	private String mStartScript;
	private boolean mFirstRun = true;
	public Context context()
	{
		return mContext;
	}

	public GameonApp(Context context, String appname, EAGLViewInterface view)
	{
		//mContext = context;
		mEaglView = view;
		mWorld = new GameonWorld(this);
		mView = new GameonWorldView(mWorld, this);
		mDataGrid = new LayoutGrid(mWorld, this);
		mContext = context;
		mScript = new ServerkoParse(this);
		mAppName = appname;
		mSettings = new Settings(this);
		mSettings.init(mScript, context, appname);

		mObjectsFact = new ObjectsFactory(this);
		mAnims = new AnimFactory(this);
		mColors = new ColorFactory();
		mSounds = new SoundFactory();
		mTextures = new TextureFactory(this);
		mItems = new ItemFactory(this);
		mBox2dWrapper = new Box2dWrapper(this);
		mLastDrag[0] = 1e07f;
	}

	public void onJSONData(GL10 gl,JSONObject jsonData) {
		JSONObject gs;
		try {
			gs = jsonData.getJSONObject("gs");
			JSONArray room = gs.getJSONArray("room");
			for (int a=0; a< room.length(); a++)
			{
				JSONObject roomobj = room.getJSONObject(a);
				String type = roomobj.getString("res");
				if (type.equals("event"))
				{
					// on event
					onEvent2(gl,roomobj);
				}else if (type.equals("layout")){
					// onlayout
					mDataGrid.initLayout2(roomobj);
				}else if (type.equals("texts")){
					// onlayout
					mTextures.initTextures(gl,roomobj);
				}else if (type.equals("objs")){
					// onlayout
					mObjectsFact.initObjects(gl,roomobj);
				}else if (type.equals("models")){
					// onlayout
					this.mItems.initModels(gl,roomobj);
				}else if (type.equals("animation")){
					// onlayout
					this.mAnims.initAnimation(gl,roomobj);
				}else if (type.equals("box2dobjs")){
					// onlayout
					mBox2dWrapper.initObjects(gl,roomobj);
				}	   	            
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}



	private synchronized void execResponses(GL10 gl)
	{
		while( mResponsesQueue.size() > 0)
		{
			mDataChange = true;
			JSONObject jsonData = mResponsesQueue.get(0);
			onJSONData(gl,jsonData);
			mResponsesQueue.remove(0);
			//Log.d("model", "exec execResponses done" );
		}
	}

	synchronized void queueResponse(JSONObject response)
	{
		mResponsesQueue.add(response);
	}

	public void start(String script, String preexec) {

		mSettings.setParser(mScript);
		mPreExec = preexec;
		mStartScript = script;
	}

	private void doStart()
	{
		if (mPreExec != null)
		{
			mScript.execScript(mPreExec);
		}
		// exec scripts
		//mScript.execScriptFromFile(script);
		mScript.loadScript(mStartScript, 200);
	}

	public void onClick(float x, float y) {
		if (!mTouchEnabled)
			return;    
		long delay = System.currentTimeMillis() - mLastClickTime;

		AreaIndexPair field = mDataGrid.onClickNearest(x, y);
    	        if (field == null)
    	        {
    	         	field = mWorld.onTouchModel(x,y,true);
    	        }
		if (field != null && field.mOnclick != null) {
			// send data
			String datastr = field.mOnclick;
			if (datastr.startsWith("js:"))
			{
				if (datastr.endsWith(";"))
				{
					mScript.execScript(datastr.substring(3));
				}else
				{
					String cmd  = datastr.substring(3);
    				if (field.mAlias == null)
    				{
					cmd += "('" + field.mArea + "',"+ field.mIndex;
    				}else
    				{
    					cmd += "('" + field.mArea + "',"+ field.mIndex+",'" + field.mAlias + "'" ;
    				}
					cmd += "," + delay + ",[" + field.mLoc[0] + "," + field.mLoc[1] + "," + field.mLoc[2] + "]";
					cmd += ","+mLastDist;
					cmd += ");" ;
					mScript.execScript(cmd);
				}
			}else
			{
				datastr += "," + field.mArea + "," + field.mIndex;
				mScript.sendUserData( datastr);
			} 
		}
		if (mFocused != null)
		{
			onFocusLost(mFocused);
			mFocused = null;
		}
		mLastDist = 0;
	}

	public void init(GL10 gl )
	{
		mScript.loadFramework();
		//mScript.set();
	}

	void setScreenBounds()
	{
		RenderDomain hud = mWorld.getDomainByName("hud");
		RenderDomain world = mWorld.getDomainByName("world");
		hud.mCS.getScreenBounds(mHudb);
		world.mCS.getScreenBounds(mScreenb);

		String script = "Q.layout.canvasw =";
		script += world.mCS.getCanvasW(); 
		script += ";Q.layout.canvash = ";
		script += world.mCS.getCanvasH();

		script += ";Q.layout.worldxmin = ";
		script += mScreenb[0];
		script += ";Q.layout.worldxmax = ";
		script += mScreenb[2];

		script += ";Q.layout.worldymin = ";
		script += mScreenb[5];
		script += ";Q.layout.worldymax = ";
		script += mScreenb[3];

		script += ";Q.layout.hudxmin = ";
		script += mHudb[0];
		script += ";Q.layout.hudxmax = ";
		script += mHudb[2];

		script += ";Q.layout.hudymin = ";
		script += mHudb[5];
		script += ";Q.layout.hudymax = ";
		script += mHudb[3];

		script += ";";	    
		mScript.execScript(script);    
	}


	private void onTextInput(String resptype, String respdata) {
		mEaglView.onTextInput(resptype, respdata);

	}
	private void sendEvent(String resptype , String respdata)
	{
		int delay = Integer.parseInt(resptype);
		mScript.execScript(respdata , delay);    	
	}

	public void sendExec(String resptype , String respdata)
	{
		int delay = Integer.parseInt(resptype);
		mScript.execScript(respdata , delay);    	
	}
	public void onPause() {
		//mScript.onPause();
	}

	public void loadModule(String resptype , String respdata)
	{
		mScript.loadModule(resptype);            
	}

	public void loadModule2(String resptype , String respdata)
	{
		mScript.loadModule2(resptype);    
	}

	public void surfaceChanged(GL10 gl, int width, int height)
	{
		//mCS.init((float)width, (float)height, 1);
		mView.onSurfaceChanged(gl, width, height);
		if (mFirstRun)
		{
			mFirstRun = false;
			doStart();
		}
	}    


	public void execScript(String script)
	{
		mScript.execScript(script , 10);
	}

	public void goUrl(String type , String  data)
	{
		mEaglView.goUrl(type, data);
	}

	public void touchStart(int x, int y) {
		if (!mTouchEnabled)
			return;    	
		fireTouchEvent(1,(float)x, (float)y, 0);//rayVec , rayVecHud);
	}

    public void touchEnd(int x, int y, long pressdelay, boolean dotouch) {
		if (!mTouchEnabled)
			return;    	
		if (dotouch)
		{
		fireTouchEvent(2,(float)x, (float)y, pressdelay);//rayVec , rayVecHud);
		}
		onClick((float)x, (float)y );//rayVec , rayVecHud);
		mWorld.resetDomainPan();
		
	}

	public void drawFrame(GL10 gl) {
		// 
		if (mRendering)
			return;
		mRendering = true;
		calcFrameDelay();
		//Log.d("model", " time "+ this.mFrameDeltaTime);
		processData(gl);		

		if (mDrawSPlash)
		{
			mSplashTimeStart += this.mFrameDeltaTime;
			//
			if (mSplashTimeStart > mSplashTime-500)
			{
				if (this.mSplashOutStart == false)
				{
					this.mDataGrid.animScreen("color" , "500,FFFFFFFF,00000000");
					this.mSplashOutStart = true;
				}
			}			
			if (mSplashTimeStart > mSplashTime)
			{
				mDrawSPlash = false;
				mView.lockDraw(false);
				this.mDataGrid.animScreen("color" , "500,00000000,FFFFFFFF");
			}
			else
			{
				mView.lockDraw(true);
				mView.drawSplash(gl);								
			}

		}else
		{
			mView.onDrawFrame(gl, mFrameDeltaTime);
		}

		if (!mCameraSet)
		{
			mDataGrid.onCameraFit("fit", "4.0,0","world");
			mDataGrid.onCameraFit("fit", "4.0,0","hud");
			mCameraSet = true;
		}
		mRendering = false;

	}


	private void connect(String serverip , String script)
	{
		mScript.connect(serverip , script);
	}

	private void disconnect()
	{
		mScript.disconnect();
	}

	private void join(String data , String script)
	{
		StringTokenizer tok =  new StringTokenizer(data, "|");
		String addr = null;
		String user = null;
		if (tok.hasMoreTokens())
		{
			addr = tok.nextToken();
		}
		if (tok.hasMoreTokens())
		{
			user = tok.nextToken();
		}    	

		mScript.join(addr, user , script);
	}


	private void send(String data)
	{
		mScript.send(data);
	}

	public boolean  mouseDragged(int x, int y, boolean notimecheck) {
		// 
		System.out.println( " mouse drag " + x + y + notimecheck);
		if (!mTouchEnabled)
			return false;

		if (this.mFrameDeltaTime == 0)
			mLastDragTime += 100;
		else
			mLastDragTime += this.mFrameDeltaTime;
		if (!notimecheck && mLastDragTime < 100)
		{
			return false;
		}
		fireTouchEvent(0,(float)x, (float)y, 0);


		mLastDragTime = 0;


		AreaIndexPair field = mDataGrid.onDragNearest((float)x, (float)y);
		if (field == null)
		{
			field = mWorld.onTouchModel(x,y,false);
		}
		if (field != null && mFocused != null)
		{
			if (field.mArea.equals( mFocused.mArea) )
			{
				if (mLastDrag[0] == 1e07f)
				{
					mLastDrag[0] = field.mLoc[0];
					mLastDrag[1] = field.mLoc[1];
					mLastDrag[2] = field.mLoc[2];
    				return true;
				}else
				{
					float delta0 = field.mLoc[0]-mLastDrag[0];
					float delta1 = field.mLoc[1]-mLastDrag[1];
					float delta2 = field.mLoc[2]-mLastDrag[2];
					mLastDist = (float)Math.sqrt( (delta0*delta0)+(delta1*delta1)+(delta2*delta2) );

					LayoutArea area = mDataGrid.getArea(field.mArea);
					if (area != null)
					{
						area.onDragg(field.mLoc[0] -mLastDrag[0],
								field.mLoc[1] -mLastDrag[1],
								field.mLoc[2] -mLastDrag[2]);
					}
				}
				System.out.println( (field.mLoc[0] -mLastDrag[0]) + " " + 
						(field.mLoc[1] -mLastDrag[1]) + " " + 
						(field.mLoc[2] -mLastDrag[2]));
				mLastDrag[0] = field.mLoc[0];
				mLastDrag[1] = field.mLoc[1];
				mLastDrag[2] = field.mLoc[2];
			}
			if (field.mArea.equals( mFocused.mArea) && 
					field.mIndex == mFocused.mIndex)
			{
				// moving around focused item
    			return true;
			}else
			{
				onFocusLost(mFocused);
				mFocused = null;
				mLastDrag[0] = 1e07f;
			}
		}else if (mFocused != null)
		{
			onFocusLost(mFocused);
			mFocused = null;        		
		}
		mFocused = field;
		if (field != null)
		{
			onFocusGain(field);
		}

		if (field == null)
		{
			if (mWorld.panDomain((float)x, (float)y))
			{
				mRenderThisFrame = true;
			}
		}

		
		mLastDrag[0] = 1e07f;		
    	if (field != null)
    	{
    		return true;
    	}else
    	{
    		return false;
    	}
	}


	public void onFocusGain(AreaIndexPair field)
	{
		if (field == null || field.mOnFocusGain == null)
			return;

		String datastr = field.mOnFocusGain;
		if (datastr.startsWith("js:"))
		{
			if (datastr.endsWith(";"))
			{
				mScript.execScript(datastr.substring(3));
			}else
			{			
				String cmd  = datastr.substring(3);
				if (field.mAlias == null)
				{
					cmd += "('" + field.mArea + "',"+ field.mIndex+ ");" ;
				}else
				{				
				cmd += "('" + field.mArea + "',"+ field.mIndex + ");" ;
				}
				mScript.execScript(cmd);
			}

		}else
		{
			datastr += "," + field.mArea + "," + field.mIndex;
			mScript.sendUserData( datastr);
		} 		
	}

	public void onFocusLost(AreaIndexPair field)
	{
		if (field == null)
			return;
		if (field == null || field.mOnFocusLost == null)
			return;


		String datastr = field.mOnFocusLost;
		if (datastr.startsWith("js:"))
		{
			if (datastr.endsWith(";"))
			{
				mScript.execScript(datastr.substring(3));
			}else
			{			
				String cmd  = datastr.substring(3);
				if (field.mAlias == null)
				{
				cmd += "('" + field.mArea + "',"+ field.mIndex + ");" ;
				}else
				{				
					cmd += "('" + field.mArea + "',"+ field.mIndex + ");" ;
				}
				mScript.execScript(cmd);
			}
		}else
		{
			datastr += "," + field.mArea + "," + field.mIndex;
			mScript.sendUserData( datastr);
		} 		

	}

	public boolean onFocusProbe(int x, int y)
	{
		mLastClickTime = System.currentTimeMillis();
		return this.mouseDragged(x, y , true);
	}   

	public void setSplash(String splash, long delay)
	{
		mSplashTime = delay;
		mSplashScreen = splash;

		mSplashTimeStart = 0;
		if (mSplashScreen != null && this.mSplashScreen.length() > 0)
		{
			mDrawSPlash = true;
		}
	}

	public void setEnv(String name, String value) {
		if ( name.equals("textparam"))
		{
			StringTokenizer tok =  new StringTokenizer(value, ",");
			float u1 = Float.parseFloat( tok.nextToken() );
			float v1 = Float.parseFloat( tok.nextToken() );
			float u2 = Float.parseFloat( tok.nextToken() );
			float v2 = Float.parseFloat( tok.nextToken() );
			float p = 0;
			if (tok.hasMoreTokens())
				p = Float.parseFloat( tok.nextToken() );
			mTextures.setParam( u1,v1,u2,v2,p );
		}else
			if ( name.equals("touch"))
			{
				if (value.equals("on"))
				{
					mTouchEnabled = true;
				}else if (value.equals("off"))
				{
					mTouchEnabled = false;
				}
			}

	}

	public void setSplashSize(float x1, float y1, float x2, float y2) {
		mSplashX1 = x1;
		mSplashX2 = x2;
		mSplashY1 = y1;
		mSplashY2 = y2;

	}

	public void processData(GL10 gl) {
		mBox2dWrapper.doFrame(mFrameDeltaTime);
		mAnims.process(gl , this.mFrameDeltaTime);
		execResponses(gl);
		mWorld.addModels();

		// flush textures 
		mTextures.flushTextures(gl);
	}	

	public boolean hasData()
	{
		if (mRenderThisFrame)
		{
			mRenderThisFrame = false;
			return true;
		}
		//System.out.println("to skip " + mResponsesQueue.size() + " " + mAnims.getCount());
		if (mDrawSPlash || mBox2dWrapper.isActive())
		{
			return true;
		}
		if (mDataChange)
		{
			mDataChange = false;
			return true;
		}
		if (mRendering)
		{
			return false;
		}

		if (mResponsesQueue.size() == 0 && mAnims.getCount() == 0)
		{
			//System.out.println("skipping");
			mFrameLastTime  = -1;
			return false;
		}
		return true;
	}	

	void onEvent2(GL10 gl, JSONObject response)
	{
		try {
			String respid = response.getString("id");

			String respdata = null;
			String respdata2 = null;
			String respdata3 = null;
			String respdata4 = null;
			String resptype = response.getString("type");
			if (response.has("data"))
			{
				respdata = response.getString("data");
			}
			if (response.has("data2"))
			{
				respdata2 = response.getString("data2");
			}
			if (response.has("data3"))
			{
				respdata3 = response.getString("data3");
			}
			if (response.has("data4"))
			{
				respdata4 = response.getString("data4");
			}						

			int eventid = Integer.parseInt( respid);
			switch (eventid) {
			case 100:
				sendEvent(resptype, respdata);
				break;
			case 101:
				sendExec(resptype, respdata);
				break;
			case 102:
				loadModule(resptype , respdata);
				break;
			case 103:
				loadModule2(resptype , respdata);
				break;            
			case 200:
				setEnv(resptype, respdata);
				break;	            
			case 201:
				registerOnTouch(resptype , 0);
				break;
			case 202:
				registerOnTouch(resptype , 1);
				break;
			case 203:
				registerOnTouch(resptype , 2);
				break;					
			case 1002:
				onTextInput(resptype , respdata);
				break;
			case 1200:
				goUrl(resptype, respdata);
				break;            
			case 2600:
				mSettings.open();
				break;
			case 2601:
				mSettings.save();
				break;
			case 2610:
				mSettings.writeInt(resptype , respdata);
				break;
			case 2611:
				mSettings.writeStr(resptype , respdata);
				break;            
			case 2620:
				mSettings.loadInt(resptype , respdata);
				break;
			case 2621:
				mSettings.loadStr(resptype , respdata);
				break;
			case 2622:
				mSettings.loadArray(resptype , respdata);
				break;		
			case 4000:
				mTextures.newTexture(gl ,resptype  , respdata, true);
				break;
			case 4001:
				mTextures.deleteTexture(gl ,resptype );
				break;					
			case 4100:
				mObjectsFact.create(resptype , respdata, respdata2 , respdata3);
				break;
			case 4110:
					mObjectsFact.place(resptype , respdata, respdata2);
				break;
			case 4120:
				mObjectsFact.scale(resptype , respdata);
				break;
			case 4130:
				mObjectsFact.texture(resptype , respdata , respdata2);
				break;
			case 4140:
				mObjectsFact.state(resptype , respdata);
				break;
			case 4150:
				mObjectsFact.remove(resptype , respdata);
				break;			
			case 4160:
				mObjectsFact.rotate(resptype , respdata);
				break;							
			case 4200:
				mAnims.move(resptype , respdata , respdata2,respdata3);
				break;			
			case 4210:
				mAnims.rotate(resptype , respdata, respdata2,respdata3);
				break;			
			case 5000:
				mSounds.newSound(resptype , respdata);
				break;
			case 5010:
				mSounds.onPlaySound(resptype  , respdata);
				break;
			case 5011:
				float val = Float.parseFloat(resptype);
				mSounds.setVolume(val);
				break;
			case 5012:
				int mutval = Integer.parseInt(respdata);
				mSounds.setMute(mutval);
				break;

			case 6001:
				mItems.newFromTemplate(resptype, respdata , null);
				break;
			case 6002:
				mItems.setTexture(resptype, respdata);
				break;        	  
			case 6003:
				mItems.createModel(resptype);
				break;        	          	  
			case 6004:
				mItems.setSubmodels(resptype, respdata);
				break;
			case 6005:
				mItems.newEmpty(resptype);
				break;
			case 6006:
				mItems.addShape(resptype, respdata, respdata2, respdata3, respdata4);
				break;
			case 6007:
				mItems.addShapeFromData(resptype, respdata, respdata2 , respdata3);
				break;					
				case 6008:
					mItems.createModelFromFile(gl, resptype, respdata);
					break;															
			case 7000:
				connect(resptype, respdata);
				break;            
			case 7001:
				join(resptype , respdata);
				break;                        
			case 7002:
				send(resptype);
				break;
			case 7003:
				disconnect();
				break;    		
			case 7005:
				get(resptype , respdata);
				break;
			case 8000:
				mWorld.domainCreate(gl,resptype , respdata , respdata2);
				break;
			case 8001:
				mWorld.domainRemove(resptype);
				break;
			case 8002:
				mWorld.domainShow(resptype);
				break;
			case 8003:
				mWorld.domainHide(resptype);
				break;    							
				case 8004:
					mWorld.domainPan(resptype,respdata, respdata2, respdata3);
					break;    												
			case 9000:
				mBox2dWrapper.initWorld(resptype, respdata , respdata2);
				break;
			case 9001:
				mBox2dWrapper.removeWorld(resptype);
				break;

			case 4300:
					mAnims.animObject(resptype,respdata,respdata2,respdata3,null);
			default:
				mDataGrid.onEvent2(gl, response);
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}        
	}

	public void stop() {
		mScript.disconnect();

	}


	public void calcFrameDelay()
	{
		if (mFrameLastTime  == -1)
		{
			mFrameLastTime  = System.currentTimeMillis(); 
			mFrameDeltaTime = 0;
		}else
		{
			mFrameDeltaTime = System.currentTimeMillis() - mFrameLastTime;
			mFrameLastTime += mFrameDeltaTime; 
		}
	}

	public AnimFactory anims()
	{
		return mAnims;
	}
	public ColorFactory	colors()
	{
		return mColors;
	}
	public ItemFactory items()
	{
		return mItems;
	}
	public TextureFactory textures()
	{
		return mTextures;
	}
	public SoundFactory	sounds()
	{
		return mSounds;
	}
	public Settings settings()
	{
		return mSettings;
	}
	public ObjectsFactory objects()
	{
		return mObjectsFact;
	}
	public GameonWorld world()
	{
		return mWorld;
	}
	public LayoutGrid grid()
	{
		return mDataGrid;
	}
	public ServerkoParse script()
	{
		return mScript;
	}
	public GameonWorldView view()
	{
		return mView;
	}

	public long frameDelta() {
		return mFrameDeltaTime;
	}

	public Random random()
	{
		return mRandom;
	}

	public void supportOld(boolean support)
	{
		mSupportOld = support;		
	}

	private void get(String uri, String callback)
	{
		mScript.get(uri, callback);
	}

	private void registerOnTouch(String resptype , int type) {
		if (type == 0)
			mOnTouchCallback = resptype;
		if (type == 1)
			mOnTouchStartCallback = resptype;
		if (type == 2)
			mOnTouchEndCallback = resptype;    	
	}

	private void fireTouchEvent(int type, float x , float y, long delay)
	{
		// 0 touch event
		if (type == 0 && mOnTouchCallback != null && mOnTouchCallback.length() > 0)
		{
			String data = mOnTouchCallback + "(" + mWorld.gerRelativeX(x) + ","+ mWorld.gerRelativeY(y)+ ");";
			mScript.execScript(data , 0);        	        	        	
		}

		// 1 touch start
		if (type == 1 && mOnTouchStartCallback != null && mOnTouchStartCallback.length() > 0 )
		{
			String data = mOnTouchStartCallback + "(" + mWorld.gerRelativeX(x) + ","+ mWorld.gerRelativeY(y)+ ");";
			mScript.execScript(data , 0);        	        	
		}

		// 2 touch end + delay        
		if (type == 2 && mOnTouchEndCallback != null && mOnTouchEndCallback.length() > 0)
		{
			String data = mOnTouchEndCallback + "(" + mWorld.gerRelativeX(x) + ","+ mWorld.gerRelativeY(y)+ ", " + delay+");";
			mScript.execScript(data , 0);        	
		}
	}

	public String convertStreamToString(InputStream is)
            throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }	
    
	public String getStringFromFile(String location) 
	{
		InputStream instream;
 
		try {
			instream = mContext.getAssets().open("res/"+location);
			String objstr = convertStreamToString(instream);
			return objstr;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


}
