package top.icecream.testme.opengl;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import top.icecream.testme.camera.Camera;
import top.icecream.testme.opengl.filter.AsciiFilterRender;
import top.icecream.testme.opengl.filter.FilterRender;
import top.icecream.testme.opengl.filter.GrayFilterRender;
import top.icecream.testme.opengl.filter.LineFilterRender;
import top.icecream.testme.opengl.filter.OriginalFilterRender;
import top.icecream.testme.opengl.filter.ReliefFilterRender;
import top.icecream.testme.opengl.sticker.GlassesStickerRender;
import top.icecream.testme.opengl.sticker.MustacheStickerRender;
import top.icecream.testme.opengl.sticker.NoseStickerRender;
import top.icecream.testme.opengl.sticker.StickerRender;
import top.icecream.testme.opengl.utils.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.orthoM;
import static top.icecream.testme.camera.Camera.RAW_HEIGHT;
import static top.icecream.testme.camera.Camera.RAW_WIDTH;

/**
 * AUTHOR: 86417
 * DATE: 5/4/2017
 */

public class CameraRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraRender";

    private final Context context;
    private int imageId;
    private Texture texture;
    private FilterRender filterRender;
    private StickerRender stickerRender;
    private SurfaceTexture cameraTexture;
    private GLSurfaceView glSV;
    private List<FilterRender> filterRenderList = new LinkedList<>();
    private List<StickerRender> stickerRenderList = new LinkedList<>();

    private final float[] projectionMatrix = new float[16];
    private Camera camera;
    private Lock stickerRenderLock = new ReentrantLock();

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

        stickerRenderList.add(null);
        stickerRenderList.add(new GlassesStickerRender(context));
        stickerRenderList.add(new NoseStickerRender(context));
        stickerRenderList.add(new MustacheStickerRender(context));

        glClearColor(0f,0f,0f,1f);

        texture = new Texture();
        stickerRender = stickerRenderList.get(0);
        filterRender = filterRenderList.get(0);
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
        drawImage();
        stickerRenderLock.lock();
        try {
            if (stickerRender != null) {
                SparseArray<Face> faces = camera.getFaces();
                if (faces != null) {
                    drawSticker(faces);
                }
            }
        }finally {
            stickerRenderLock.unlock();
        }

        cameraTexture.updateTexImage();
    }

    private void drawImage() {
        filterRender.useProgram();
        filterRender.bindTexture(imageId, projectionMatrix);
        texture.bindData(filterRender);
        texture.draw();
    }

    private void drawSticker(SparseArray<Face> faces) {
        PointF leftEye = null;
        PointF rightEye = null;
        PointF noseBase = null;
        PointF bottomMouth = null;
        float[] mRotationMatrix = new float[16];
        float[] scratch = new float[16];

        if (faces.size() <= 0) {
            return;
        }
        Face face = faces.get(faces.keyAt(0));
        Matrix.setRotateM(mRotationMatrix, 0, face.getEulerZ(), 0f, 0f, -1.0f);
        Matrix.multiplyMM(scratch, 0, projectionMatrix, 0, mRotationMatrix, 0);
        List<Landmark> landmarks = face.getLandmarks();
        for (Landmark landmark : landmarks) {
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:leftEye = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.RIGHT_EYE:rightEye = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.NOSE_BASE:noseBase = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.BOTTOM_MOUTH:bottomMouth = rawPointToRealPoint(landmark.getPosition());break;
            }
        }

        stickerRender.useProgram();
        stickerRender.setMatrix(scratch);
        stickerRender.bindTexture();

        if (leftEye != null && rightEye != null && stickerRender instanceof GlassesStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            centerX = (leftEye.x + rightEye.x) / 2;
            centerY = (leftEye.y + rightEye.y) / 2;
            xRadius = Math.abs(leftEye.x - rightEye.x) / 0.8f;
            yRadius = xRadius * 128 / 408;
            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        if (noseBase != null && bottomMouth != null && stickerRender instanceof NoseStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            xRadius = Math.abs(noseBase.y - bottomMouth.y) * 1.7f;
            yRadius = xRadius * 1.0f;
            centerX = noseBase.x;
            centerY = noseBase.y + (noseBase.y - bottomMouth.y) / 1.5f;
            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        if (bottomMouth != null && noseBase != null && stickerRender instanceof MustacheStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            xRadius = Math.abs(noseBase.y - bottomMouth.y) / 1.2f;
            yRadius = xRadius * 1.0f;
            centerX = bottomMouth.x;
            centerY = bottomMouth.y +  (noseBase.y - bottomMouth.y) / 2.0f;
            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        texture.bindData(stickerRender);
        texture.draw();
    }

    private PointF rawPointToRealPoint(PointF rawPoint) {
        return new PointF(
                1 - rawPoint.x / RAW_HEIGHT * 2.0f,
                1.0f*RAW_WIDTH / RAW_HEIGHT - rawPoint.y / RAW_WIDTH * 2.0f * RAW_WIDTH / RAW_HEIGHT
        );
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        glSV.requestRender();
    }

    public void changCamera() {
        camera.changeCamera();
        filterRender.changeCameraDirection();
    }

    public void selectFilter(int position) {
        filterRender = filterRenderList.get(position);
    }

    public void selectSticker(int position) {
        stickerRenderLock.lock();
        try {
            stickerRender = stickerRenderList.get(position);
        }finally {
            stickerRenderLock.unlock();
        }
        if (position == 0) {
            camera.setDetectFace(false);
        } else {
            camera.setDetectFace(true);
        }
    }
}
