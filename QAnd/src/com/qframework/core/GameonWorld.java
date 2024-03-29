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

import java.util.Iterator;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;



public class GameonWorld {

    
    private Vector<RenderDomain> mDomains = new Vector<RenderDomain>();
    
    private GameonModel mSplashModel;
    private float[] mAmbientLight = { 1.0f , 1.0f, 1.0f, 1.0f};
    private boolean mAmbientLightChanged = false;
    private GameonApp mApp;
	private float mViewWidth;
	private float mViewHeight;
	private Vector<GameonModel> mModelList = new Vector<GameonModel>();
	private Vector<GameonModel> mModelList2 = new Vector<GameonModel>();
	private Vector<GameonModel> mNewModels = new Vector<GameonModel>();
	
	GameonWorld(GameonApp app) {
		mApp = app;
		addDomain("world",0, true);
		addDomain("hud",10000, true);
	}
	
	public void add(GameonModel model)
	{
		if (model.isValid())
		{
		model.generate();
		mNewModels.add(model);
		}

	}
	
	public void remove(GameonModel model)
	{
		if (mModelList.indexOf(model) >= 0)
		{
			mModelList.remove(model);
		}
		if (mModelList2.indexOf(model) >= 0)
		{
			mModelList2.remove(model);
		}

		for (RenderDomain domain: mDomains)
		{

			domain.remVisible(model , true);
		}
		
	}

	public void reinit() {
		int len = mModelList.size();
		for (int a=0; a< len; a++) {
			GameonModel model = mModelList.get(a);
			model.reset();
		}
		
	}
		
	
	public void addModels()
	{
		Iterator<GameonModel> iter = mNewModels.iterator();
		while (iter.hasNext()) {
			GameonModel model= iter.next();
			if (model.mIsModel)
			{
				mModelList2.add(model);				
			}else
			{
				mModelList.add(model);
			}
		}
		
		mNewModels.clear();


	}
	
	public void initSplash(GL10 gl , String name, float x1,float y1, float x2, float y2)
		{
		GameonModel model = new GameonModel("splash", mApp, null);
		model.createPlane(x1, y1, 0.0f, x2, y2, 0.0f, mApp.colors().white, null);
		mApp.textures().newTexture(gl, "q_splash", name, true);
		model.setTexture( mApp.textures().getTexture("q_splash"));
		GameonModelRef ref = new GameonModelRef(null, Integer.MAX_VALUE);
		ref.set();
		model.addref(ref);
		mSplashModel = model;
        model.generate();
        model.mEnabled = true;		
		}

		
		
	public void draw(GL10 gl, long delay) {
		if (mAmbientLightChanged)
		{
			gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, mAmbientLight,0);
			mAmbientLightChanged = false;
		}
		
		for (RenderDomain domain: mDomains)
		{
			domain.draw(gl, delay);
		}
		
	}

	public void prepare(GL10 gl) {
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
    	gl.glEnable( GL10.GL_COLOR_MATERIAL);
    	gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);	
    	gl.glEnable(GL10.GL_CULL_FACE);
    	gl.glFrontFace(GL10.GL_CCW);
    	gl.glEnable(GL10.GL_TEXTURE_2D);
    	gl.glActiveTexture(GL10.GL_TEXTURE0);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
        		GL10.GL_NICEST);

    	gl.glEnable(GL10.GL_DEPTH_TEST);
    	gl.glDepthFunc(GL10.GL_LEQUAL);
    	gl.glDepthMask(true);
		gl.glEnable(GL10.GL_BLEND); 
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
    	gl.glEnable(GL10.GL_ALPHA_TEST);
    	gl.glAlphaFunc(GL10.GL_GREATER,0.02f);
        
    	gl.glEnable(GL10.GL_LIGHTING);
        gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, mAmbientLight,0);
        
        gl.glClearColor(0.0f, 0.0f, 0.0f,1.0f);
        gl.glShadeModel(GL10.GL_SMOOTH);
	}

	public void clear() {
		mModelList.clear();
		mNewModels.clear();
		for (RenderDomain domain: mDomains)
		{
			domain.clear();
	}
		}
		
	public void drawSplash(GL10 gl) {
		if (mSplashModel != null)
		{
			if (mAmbientLightChanged)
			{
				gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, mAmbientLight,0);
				mAmbientLightChanged = false;
			}
			
			mSplashModel.setState(LayoutArea.State.VISIBLE);
			mSplashModel.draw(gl, Integer.MAX_VALUE);
			mSplashModel.setState(LayoutArea.State.HIDDEN);
		}
		
	}
	
	public void setAmbientLight(float a , float r, float g, float b)
	{
		this.mAmbientLight[0] = a;
		this.mAmbientLight[1] = r;
		this.mAmbientLight[2] = g;
		this.mAmbientLight[3] = b;
		this.mAmbientLightChanged = true;
	}
	
	public float[] getAmbientLight()
	{
		float [] ret = new float[4];
		ret[0] = this.mAmbientLight[0];
		ret[1] = this.mAmbientLight[1];
		ret[2] = this.mAmbientLight[2];
		ret[3] = this.mAmbientLight[3]; 
		return ret;
	}

	public void setAmbientLightGl(float a , float r, float g, float b, GL10 gl)
		{
		this.mAmbientLight[0] = a;
		this.mAmbientLight[1] = r;
		this.mAmbientLight[2] = g;
		this.mAmbientLight[3] = b;
		this.mAmbientLightChanged = true;
		gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, this.mAmbientLight,0);

		
	}

	RenderDomain getDomain(int id)
			{
		for (RenderDomain domain: mDomains)
				{
			if (domain.mRenderId == id)
					{
				return domain;
					}
				}
		return null;
			}
	
	RenderDomain getDomainByName(String name)
			{
		for (RenderDomain domain: mDomains)
				{
			if (domain.mName.equals(name))
					{
				return domain;
			}		
		}
		return null;
	}

	private RenderDomain addDomain(String name, int i, boolean visible) {
		for (RenderDomain domain: mDomains)
	{
			if (domain.mName.equals(name) || domain.mRenderId == i)
		{
				return null;
			}
		}
		RenderDomain newdomain = new RenderDomain(name, mApp , mViewWidth, mViewHeight);
		if (visible)
			{
			newdomain.show();
			}
		newdomain.mRenderId = i;
		
		boolean inserted = false;
		for (int a= 0 ; a< mDomains.size(); a++)
		{
			RenderDomain old = mDomains.get(a);
			if (old.mRenderId > i)
			{
				mDomains.add(a, newdomain);
				inserted = true;
				break;
			}		
		}

		if (!inserted)
		{
			mDomains.add(newdomain);
		}
		return newdomain;
	}	

	
    public void onSurfaceChanged(GL10 gl, int width, int height) 
		{
    	mViewWidth = (float)width;
    	mViewHeight = (float)height;
    	for (RenderDomain domain: mDomains)
			{
    		domain.onSurfaceChanged(gl, width, height);
			}
		}
		
    public void onSurfaceCreated(GL10 gl)
    {
    	for (RenderDomain domain: mDomains)
		{
    		domain.onSurfaceCreated(gl);
		}
	}
	
	public void domainCreate(GL10 gl , String name, String id, String coordsstr) {
		RenderDomain domain = getDomainByName(name);
		if (domain != null)
	{
			return;
	}
	
		RenderDomain newdomain  = this.addDomain(name, Integer.parseInt(id), false);
		if (newdomain != null && coordsstr != null && coordsstr.length() > 0)
	{
			float coords[] = new float[4];
			ServerkoParse.parseFloatArray(coords, coordsstr);
			newdomain.setBounds(gl , (int)mViewWidth, (int)mViewHeight , coords);
			
			
		}
	}

	public void domainRemove(String name) 
	{
		RenderDomain domain = getDomainByName(name);
		if (domain != null)
		{
			domain.clear();
			this.mDomains.remove(domain);
		}
	}

	public float gerRelativeX(float x) {
		return x/mViewWidth;
	}
		
	public float gerRelativeY(float y) {
		return y/mViewHeight;
	}

	public void domainShow(String name) {
		RenderDomain domain = getDomainByName(name);
		if (domain != null)
	{
			domain.show();
		}
	}
	
	public void domainHide(String name) {
		RenderDomain domain = getDomainByName(name);
		if (domain != null)
	{
			domain.hide();
		}		
	}
	
	public AreaIndexPair onTouchModel(float x, float y, boolean click) 
	{
		AreaIndexPair data= null;
		for (int a= mDomains.size()-1 ; a>=0;  a--)
		{
			RenderDomain domain = mDomains.get(a);
			data= domain.onTouchModel(x,y, click, true);
			if (data != null)
			{
				return data;
			}
		}
		return null;
	}

	public void domainPan(String name, String mode, String scrollers,
			String coords) 
	{
		RenderDomain domain = getDomainByName(name);
		if (domain != null)
		{
			domain.pan(mode, scrollers,coords);
		}
		
	}

	public boolean panDomain(float x, float y) 
	{
		for (int a= mDomains.size()-1 ; a>=0;  a--)
		{
			RenderDomain domain = mDomains.get(a);
			if (domain.onPan(x,y))
			{
				return true;
			}
		}
		return false;
	}

	public void resetDomainPan() 
	{
		for (int a= mDomains.size()-1 ; a>=0;  a--)
		{
			RenderDomain domain = mDomains.get(a);
			domain.resetPan();
		}
	}
}


