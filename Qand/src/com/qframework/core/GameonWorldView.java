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

import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLU;


public class GameonWorldView {

	private GameonWorld mWorld;
	private GameonApp	mApp;
	private float mWidth;
	private float mHeight;
	private boolean doOnce = false;
	private boolean mLockedDraw = false;
    
    public GameonWorldView(GameonWorld world , GameonApp app) {
        
        mWorld = world;
        mApp = app;
    }

    
    public void onDrawFrame(GL10 gl) {
    	
	    if (mLockedDraw)return;
	    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);// | GL2.GL_DEPTH_BUFFER_BIT);
        mWorld.draw(gl);
    }


    public void onSurfaceChanged(GL10 gl, int width, int height) {
    	mWidth = (float)width;
    	mHeight = (float)height;
    	mWorld.prepare(gl);

    	gl.glViewport(0, 0, width, height);
    	mWorld.onSurfaceChanged(gl, width, height);

    	mApp.setScreenBounds();
    }

    public void onSurfaceCreated(GL10 gl) {
    	if (!doOnce)
    	{
    		mApp.textures().clear();
    		mApp.textures().init(gl,mApp.context());
    		mApp.sounds().init(mApp);	
    		if (mApp.mSplashScreen != null && mApp.mSplashScreen.length() > 0)
    		{
    			mWorld.initSplash(gl, mApp.mSplashScreen, mApp.mSplashX1,mApp.mSplashY1,mApp.mSplashX2,mApp.mSplashY2);	
    		}
    		
    		doOnce = true;
    	}else
    	{
    		mApp.textures().init(gl,mApp.context());
    	}

    	mWorld.onSurfaceCreated(gl);

   }

   public boolean drawSplash(GL10 gl)    
   {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        perspective(gl , 45.0f , (float)mWidth/(float)mHeight , 0.1f , 28.7f);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();		

        GLU.gluLookAt(gl,0.0f, 0.0f, 5.0f, 
	              0.0f, 0.0f, 0.0f,    
	              0.0f, 1.0f, 0.0f);            
        mWorld.drawSplash(gl);
        gl.glPopMatrix();
		
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();

		
		return true;
	}
	
	public void lockDraw(boolean lock)	
	{
		
		mLockedDraw = lock;
	}


		
    private void perspective(GL10 gl, float fovy, float aspect, float zmin , float  zmax)
    {
        float xmin, xmax, ymin, ymax;
        ymax = zmin * (float)Math.tan(fovy * Math.PI / 360.0f);
        ymin = -ymax;
        xmin = ymin * aspect;
        xmax = ymax * aspect;
    	gl.glFrustumf(xmin, xmax, ymin, ymax, zmin, zmax);
    }


}
