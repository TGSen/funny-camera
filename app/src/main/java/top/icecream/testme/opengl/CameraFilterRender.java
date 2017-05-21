package top.icecream.testme.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import top.icecream.testme.R;
import top.icecream.testme.opengl.utils.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.orthoM;

/**
 * AUTHOR: 86417
 * DATE: 5/4/2017
 */

public class CameraFilterRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraFilterRender";

    private final Context context;
    private int textureId;
    private int textureId2;
    private Photo photo;
    private GrayFilterRender grayFilterRender;
    private SurfaceTexture surfaceTexture;
    private GLSurfaceView glSurfaceView;

    private Table table;
    private TextureShaderProgram textureShaderProgram;
    private final float[] projectionMatrix = new float[16];

    private int PREVIEW_WIDTH = 640;
    private int PREVIEW_HEIGHT = 480;
    private FaceDetector mFaceDetector;

    private float deltX = 0;
    private float deltY = 0;
    private final int widthPixels;
    private final int heightPixels;

    public CameraFilterRender(Context context) {
        this.context = context;
        glSurfaceView = (GLSurfaceView) ((Activity) context).findViewById(R.id.glSV);
        glSurfaceView.setOnTouchListener(touchListener);

        /*SpeechUtility.createUtility(context, "appid=5903347f");
        mFaceDetector = FaceDetector.createDetector(context, null);*/

        DisplayMetrics dm =context.getResources().getDisplayMetrics();
        widthPixels = dm.widthPixels;
        heightPixels = dm.heightPixels;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(1f,1f,1f,1f);

        table = new Table();
        textureShaderProgram = new TextureShaderProgram(context);
        textureId2 = TextureHelper.loadTexture(context, R.raw.hat);

        photo = new Photo();
        grayFilterRender = new GrayFilterRender(context);
        textureId = TextureHelper.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        Camera camera = Camera.open(1);
        final byte[] mPreBuffer = new byte[460800];
        camera.addCallbackBuffer(mPreBuffer);
        Camera.Parameters params = camera.getParameters();
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        camera.setParameters(params);
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (mPreBuffer) {
                    camera.addCallbackBuffer(mPreBuffer);
                }
            }
        });

        /*new Thread(){
            @Override
            public void run() {
                while (true) {
                    synchronized (mPreBuffer){
                        String result = mFaceDetector.trackNV21(mPreBuffer, PREVIEW_WIDTH, PREVIEW_HEIGHT, 1, 3);
                        Log.d(TAG, "result:"+result);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();*/
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        final float aspectRatio = width > height ?
                (float) width / (float) height:
                (float) height / (float) width;
        if (width > height) {
            orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        grayFilterRender.useProgram();
        grayFilterRender.bindTexture(textureId);
        photo.bindData(grayFilterRender);
        photo.draw();

        textureShaderProgram.useProgram();
        textureShaderProgram.setUniforms(projectionMatrix, textureId2);
        table.bindData(textureShaderProgram);
        table.setCoor(textureShaderProgram, deltX, deltY);
        table.draw();

        surfaceTexture.updateTexImage();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSurfaceView.requestRender();
    }

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    deltX = 1.0f * event.getRawX() / widthPixels;
                    deltY = 1.0f * event.getRawY() / heightPixels;
                    Log.d(TAG, "onTouch: "+deltX+" "+deltY);
                    break;
            }
            return true;
        }
    };
}
