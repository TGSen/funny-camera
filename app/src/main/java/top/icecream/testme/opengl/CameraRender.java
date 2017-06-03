package top.icecream.testme.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;

import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import top.icecream.testme.R;
import top.icecream.testme.camera.Camera;
import top.icecream.testme.opengl.filter.LineFilterRender;
import top.icecream.testme.opengl.filter.FilterRender;
import top.icecream.testme.opengl.filter.GrayFilterRender;
import top.icecream.testme.opengl.filter.AsciiFilterRender;
import top.icecream.testme.opengl.filter.OriginalFilterRender;
import top.icecream.testme.opengl.filter.ReliefFilterRender;
import top.icecream.testme.opengl.sticker.StickerRender;
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
    private Texture texture;
    private FilterRender filterRender;
    private StickerRender stickerRender;
    private SurfaceTexture cameraTexture;
    private GLSurfaceView glSV;
    private List<FilterRender> filterRenderList = new LinkedList<>();
    private List<Integer> stickerList = new LinkedList<>();

    private final float[] projectionMatrix = new float[16];
    private Camera camera;

    public CameraRender(Context context, GLSurfaceView glSV) {
        this.context = context;
        this.glSV = glSV;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        filterRenderList.add(new OriginalFilterRender(context));
        filterRenderList.add(new GrayFilterRender(context));
        filterRenderList.add(new ReliefFilterRender(context));
        filterRenderList.add(new AsciiFilterRender(context));
        filterRenderList.add(new LineFilterRender(context));

        stickerList.add(0);
        stickerList.add(TextureHelper.loadTexture(context, R.raw.glasses));
        stickerList.add(TextureHelper.loadTexture(context, R.raw.mustache));
        stickerList.add(TextureHelper.loadTexture(context, R.raw.nose));


        glClearColor(0f,0f,0f,1f);

        texture = new Texture();
        stickerRender = new StickerRender(context);
        filterRender = filterRenderList.get(0);
        stickerId = stickerList.get(0);
        imageId = TextureHelper.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        cameraTexture = new SurfaceTexture(imageId);
        cameraTexture.setOnFrameAvailableListener(this);

        camera = new Camera(context);
        camera.setSurfaceTexture(cameraTexture);
        camera.openCamera();
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
        texture.bindData(filterRender);
        texture.draw();

        if (stickerId != 0) {
            stickerRender.useProgram();
            stickerRender.setUniforms(projectionMatrix, stickerId);
            texture.bindData(stickerRender);
            texture.draw();
        }

        cameraTexture.updateTexImage();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSV.requestRender();
    }

    public void changCamera() {
        camera.changeCamera();
    }

    public void selectFilter(int position) {
        filterRender = filterRenderList.get(position);
    }

    public void selectSticker(int position) {
        stickerId = stickerList.get(position);
    }
}
