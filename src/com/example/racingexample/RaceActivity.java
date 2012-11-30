package com.example.racingexample;


import java.util.ArrayList;

import org.andengine.engine.camera.BoundCamera;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.FixedStepPhysicsWorld;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.extension.tmx.TMXLayer;
import org.andengine.extension.tmx.TMXLoader;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.extension.tmx.TMXObject;
import org.andengine.extension.tmx.TMXObjectGroup;
import org.andengine.extension.tmx.TMXProperties;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.extension.tmx.TMXTileProperty;
import org.andengine.extension.tmx.TMXTiledMap;
import org.andengine.extension.tmx.util.exception.TMXLoadException;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.debug.Debug;
import org.andengine.util.math.MathUtils;

import android.opengl.GLES20;
import android.util.DisplayMetrics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * @author Nicolas Gramlich
 * @since 22:43:20 - 15.07.2010
 */
public class RaceActivity extends SimpleBaseGameActivity {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int RACETRACK_WIDTH = 64;

	private static final int OBSTACLE_SIZE = 16;
	private static final int CAR_SIZE = 16;

	
	
	private int CAMERA_WIDTH = 0;
	private int CAMERA_HEIGHT = 0;

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera mCamera;
	private BoundCamera bCamera;

	private BitmapTextureAtlas mVehiclesTexture;
	private TiledTextureRegion mVehiclesTextureRegion;

	private BitmapTextureAtlas mBoxTexture;
	private ITextureRegion mBoxTextureRegion;

	private BitmapTextureAtlas mRacetrackTexture;
	private ITextureRegion mRacetrackStraightTextureRegion;
	private ITextureRegion mRacetrackCurveTextureRegion;

	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;

	private Scene mScene;

	private PhysicsWorld mPhysicsWorld;

	private Body mCarBody;
	private TiledSprite mCar;

	private TMXTiledMap mTMXTiledMap;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public EngineOptions onCreateEngineOptions() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		CAMERA_HEIGHT = metrics.heightPixels / 2;
		CAMERA_WIDTH =  metrics.widthPixels / 2;
		
		this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		this.bCamera = new BoundCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.bCamera);
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mVehiclesTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 16, TextureOptions.BILINEAR);
		this.mVehiclesTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mVehiclesTexture, this, "vehicles.png", 0, 0, 6, 1);
		this.mVehiclesTexture.load();

		this.mRacetrackTexture = new BitmapTextureAtlas(this.getTextureManager(), 128, 256, TextureOptions.REPEATING_NEAREST);
		this.mRacetrackStraightTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_straight.png", 0, 0);
		this.mRacetrackCurveTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mRacetrackTexture, this, "racetrack_curve.png", 0, 128);
		this.mRacetrackTexture.load();

		this.mOnScreenControlTexture = new BitmapTextureAtlas(this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mOnScreenControlTexture, this, "onscreen_control_knob.png", 128, 0);
		this.mOnScreenControlTexture.load();

		this.mBoxTexture = new BitmapTextureAtlas(this.getTextureManager(), 32, 32, TextureOptions.BILINEAR);
		this.mBoxTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBoxTexture, this, "box.png", 0, 0);
		this.mBoxTexture.load();
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		this.mScene = new Scene();
		this.mScene.setBackground(new Background(0, 0, 0));
		
		try {
			final TMXLoader tmxLoader = new TMXLoader(this.getAssets(), this.mEngine.getTextureManager(), TextureOptions.BILINEAR_PREMULTIPLYALPHA, this.getVertexBufferObjectManager(), new ITMXTilePropertiesListener() {
				@Override
				public void onTMXTileWithPropertiesCreated(final TMXTiledMap pTMXTiledMap, final TMXLayer pTMXLayer, final TMXTile pTMXTile, final TMXProperties<TMXTileProperty> pTMXTileProperties) {
					/* We are going to count the tiles that have the property "cactus=true" set. */
					if(pTMXTileProperties.containsTMXProperty("cactus", "true")) {
						
					}
				}
			});
			this.mTMXTiledMap = tmxLoader.loadFromAsset("tmx/desert.tmx");
				
			
		} catch (final TMXLoadException e) {
			Debug.e(e);
		}
		
		TMXLayer tmxLayer = this.mTMXTiledMap.getTMXLayers().get(0);
		
		
		
		
		final ArrayList<TMXLayer> tmxLayers = this.mTMXTiledMap.getTMXLayers();
		
		for (int layerID = 0; layerID < tmxLayers.size(); layerID++) {
			mScene.attachChild(tmxLayers.get(layerID));
		}
		
		
		
		
		this.mPhysicsWorld = new FixedStepPhysicsWorld(60, new Vector2(0, 0), false, 8, 1);

		
		
		
		
		this.initCar();
		this.initOnScreenControls();
		this.bCamera.setBounds(0, 0, tmxLayer.getHeight(), tmxLayer.getWidth());
		this.bCamera.setBoundsEnabled(true);
		this.bCamera.setChaseEntity(mCar);
		
		
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		final Rectangle ground = new Rectangle(0, tmxLayer.getHeight() - 0, tmxLayer.getWidth(), 2, vertexBufferObjectManager);
		final Rectangle roof = new Rectangle(0, 0, tmxLayer.getWidth(), 0, vertexBufferObjectManager);
		final Rectangle left = new Rectangle(0, 0, 0, tmxLayer.getHeight(), vertexBufferObjectManager);
		final Rectangle right = new Rectangle(tmxLayer.getWidth() - 0, 0, 0, tmxLayer.getHeight(), vertexBufferObjectManager);
		
		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.0f, 1.0f);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);
		
		createMapObjects(this.mTMXTiledMap);
		
		this.mScene.registerUpdateHandler(this.mPhysicsWorld);

		return this.mScene;
	}
	

	

	private void createMapObjects(TMXTiledMap map) {
		
		// Loop through the object groups
        for(final TMXObjectGroup group: this.mTMXTiledMap.getTMXObjectGroups()) {

                if(group.getTMXObjectGroupProperties().containsTMXProperty("Wall", "true")){
                        createWallLayerObjects(group.getTMXObjects());
                }
        }
}
	

	private void createWallLayerObjects(ArrayList<TMXObject> objects) {
		
		// This is our "wall" layer. Create the boxes from it
        for(final TMXObject object : objects) {
                
                final Rectangle rect = new Rectangle(object.getX(), object.getY(), object.getWidth(), object.getHeight(), this.getVertexBufferObjectManager());
                final FixtureDef boxFixtureDef = PhysicsFactory.createFixtureDef(0, 0, 1f);
               
                Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, rect, BodyType.StaticBody, boxFixtureDef);
                body.setUserData("wall");
               
                rect.setVisible(true);
                this.mScene.attachChild(rect);
        }
	}

	@Override
	public void onGameCreated() {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void initOnScreenControls() {
		final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(0, CAMERA_HEIGHT - this.mOnScreenControlBaseTextureRegion.getHeight(), this.bCamera, this.mOnScreenControlBaseTextureRegion, this.mOnScreenControlKnobTextureRegion, 0.1f, this.getVertexBufferObjectManager(), new IAnalogOnScreenControlListener() {
			@Override
			public void onControlChange(final BaseOnScreenControl pBaseOnScreenControl, final float pValueX, final float pValueY) {
				final Body carBody = RaceActivity.this.mCarBody;

				final Vector2 velocity = Vector2Pool.obtain(pValueX * 10, pValueY * 10);
				carBody.setLinearVelocity(velocity);
				Vector2Pool.recycle(velocity);

				final float rotationInRad = (float)Math.atan2(-pValueX, pValueY);
				carBody.setTransform(carBody.getWorldCenter(), rotationInRad);

				RaceActivity.this.mCar.setRotation(MathUtils.radToDeg(rotationInRad));
			}

			@Override
			public void onControlClick(final AnalogOnScreenControl pAnalogOnScreenControl) {
				/* Nothing. */
			}
		});
		analogOnScreenControl.getControlBase().setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		analogOnScreenControl.getControlBase().setAlpha(0.5f);
				analogOnScreenControl.getControlBase().setScaleCenter(0, 128);
				analogOnScreenControl.getControlBase().setScale(0.75f);
				analogOnScreenControl.getControlKnob().setScale(0.75f);
		analogOnScreenControl.refreshControlKnobPosition();

		this.mScene.setChildScene(analogOnScreenControl);
	}

	private void initCar() {
		this.mCar = new TiledSprite(20, 20, CAR_SIZE, CAR_SIZE, this.mVehiclesTextureRegion, this.getVertexBufferObjectManager());
		this.mCar.setCurrentTileIndex(0);

		final FixtureDef carFixtureDef = PhysicsFactory.createFixtureDef(0, 0, 1.0f);
		this.mCarBody = PhysicsFactory.createBoxBody(this.mPhysicsWorld, this.mCar, BodyType.DynamicBody, carFixtureDef);

		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(this.mCar, this.mCarBody, true, false) {
			@Override
			public void onUpdate(float pSecondsElapsed) {
				super.onUpdate(pSecondsElapsed);
				bCamera.updateChaseEntity();
			}
		});

		this.mScene.attachChild(this.mCar);
	}


	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}