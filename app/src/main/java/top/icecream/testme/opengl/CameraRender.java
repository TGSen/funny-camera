package top.icecream.testme.opengl;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;

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

public class CameraRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraRender";

    private final Context context;
    private int imageId;
    private int stickerId;
    private Image image;
    private filterRender filterRender;
    private SurfaceTexture cameraTexture;
    private GLSurfaceView glSV;

    private Sticker sticker;
    private StickerRender stickerRender;
    private final float[] projectionMatrix = new float[16];

    private static final int PREVIEW_WIDTH = 640;
    private static final int PREVIEW_HEIGHT = 480;
    private FaceDetector mFaceDetector;

    public CameraRender(Context context) {
        this.context = context;
        glSV = (GLSurfaceView) ((Activity) context).findViewById(R.id.glSV);
        /*SpeechUtility.createUtility(context, "appid=5903347f");
        mFaceDetector = FaceDetector.createDetector(context, null);*/
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0f,0f,0f,1f);

        sticker = new Sticker();
        stickerRender = new StickerRender(context);
        stickerId = TextureHelper.loadTexture(context, R.raw.hat);

        image = new Image();
        filterRender = new filterRender(context);
        imageId = TextureHelper.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);


        cameraTexture = new SurfaceTexture(imageId);
        cameraTexture.setOnFrameAvailableListener(this);

        final byte[] previewBuffer = new byte[460800];
        Camera camera = Camera.open(1);
        camera.addCallbackBuffer(previewBuffer);
        Camera.Parameters params = camera.getParameters();
        params.setPreviewFormat(ImageFormat.NV21);
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        camera.setParameters(params);
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (previewBuffer) {
                    camera.addCallbackBuffer(previewBuffer);
                }
            }
        });

        /*new Thread(){
            @Override
            public void run() {
                while (true) {
                    synchronized (previewBuffer){
                        String result = mFaceDetector.trackNV21(previewBuffer, PREVIEW_WIDTH, PREVIEW_HEIGHT, 1, 3);
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
            camera.setPreviewTexture(cameraTexture);
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

        filterRender.useProgram();
        filterRender.bindTexture(imageId);
        image.bindData(filterRender);
        image.draw();

        stickerRender.useProgram();
        stickerRender.setUniforms(projectionMatrix, stickerId);
        sticker.bindData(stickerRender);
        /*sticker.setCoor(stickerRender, deltX, deltY);*/
        sticker.draw();

        cameraTexture.updateTexImage();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSV.requestRender();
    }
}
