package top.icecream.testme.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.SparseArray;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.nio.IntBuffer;
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
import top.icecream.testme.opengl.sticker.FaceStickerRender;
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
    private volatile boolean isTakePic = false;
    private int previewWidth;
    private int previewHeight;

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
        stickerRenderList.add(new FaceStickerRender(context));

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
        this.previewWidth = width;
        this.previewHeight = height;

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

        if (isTakePic) {
            try {
                isTakePic = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void takePicture() {
        isTakePic = true;
    }

    private Bitmap createBitmapFromGLSurface(int x, int y, int w, int h) {
        int bitmapBuffer[] = new int[w * h];
        int bitmapSource[] = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);

        try {
            GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }


/*    private void takingPicture() {
        int width = previewWidth;
        int height = previewHeight;
        int b[] = new int[width * height];
        int bt[] = new int[width * height];
        IntBuffer buffer = IntBuffer.wrap(b);
        buffer.position(0);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pix = b[i * width + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(height - i - 1) * width + j] = pix1;
            }
        }
        Bitmap inBitmap;
        inBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        inBitmap.copyPixelsFromBuffer(buffer);
        inBitmap = Bitmap.createBitmap(bt, width, height, Bitmap.Config.RGB_565);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        inBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
        byte[] bitmapData = bos.toByteArray();
        ByteArrayInputStream fis = new ByteArrayInputStream(bitmapData);
        String tempPicFile = "temp_" + System.currentTimeMillis() + ".jpeg";
        File tempDir = new File(Environment.getExternalStorageDirectory() + File.separator +
                "SurfaceScreenShot" + File.separator + "Images");
        tempDir.mkdirs();
        try {
            File tmpFile = new File(tempDir, tempPicFile);
            FileOutputStream fos = new FileOutputStream(tmpFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }
            fis.close();
            fos.close();
            inBitmap.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private void drawImage() {
        filterRender.useProgram();
        filterRender.bindTexture(imageId, projectionMatrix);
        texture.bindData(filterRender);
        texture.draw();
    }

    private void drawSticker(SparseArray<Face> faces) {
        PointF faceCenter;
        PointF leftEye = null;
        PointF rightEye = null;
        PointF noseBase = null;
        PointF bottomMouth = null;
        final float PI = 3.1415926f;
        final float DEG = PI / 180;
        float[] rotationZMatrix = new float[16];
        float[] rotationYMatrix = new float[16];
        float[] scratch = new float[16];
        float faceWidth;
        float faceHeight;

        if (faces.size() <= 0) {
            return;
        }
        Face face = faces.get(faces.keyAt(0));
        Matrix.setRotateM(rotationZMatrix, 0, face.getEulerZ(), 0, 0, -1);
        Matrix.setRotateM(rotationYMatrix, 0, face.getEulerY(), 0, -1, 0);
        Matrix.multiplyMM(scratch, 0, projectionMatrix, 0, rotationZMatrix, 0);
        Matrix.multiplyMM(scratch, 0, scratch, 0, rotationYMatrix, 0);
        List<Landmark> landmarks = face.getLandmarks();
        for (Landmark landmark : landmarks) {
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:leftEye = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.RIGHT_EYE:rightEye = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.NOSE_BASE:noseBase = rawPointToRealPoint(landmark.getPosition());break;
                case Landmark.BOTTOM_MOUTH:bottomMouth = rawPointToRealPoint(landmark.getPosition());break;
            }
        }
        faceCenter = rawPointToRealPoint(face.getPosition());
        faceWidth = face.getWidth() / RAW_HEIGHT;
        faceHeight = face.getHeight() / RAW_HEIGHT;
        faceCenter.x = faceCenter.x - faceWidth;
        faceCenter.y = faceCenter.y - faceHeight;

        stickerRender.useProgram();
        stickerRender.setMatrix(scratch);
        stickerRender.bindTexture();

        if (noseBase != null && stickerRender instanceof GlassesStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            xRadius = faceWidth / 1.2f;
            yRadius = xRadius * 128 / 408;
            centerX = noseBase.x;
            centerY = noseBase.y + yRadius * 1.9f;
            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        if (leftEye != null && rightEye != null && noseBase != null && bottomMouth != null && stickerRender instanceof NoseStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            xRadius = faceWidth / 1.5f;
            yRadius = xRadius * 1.0f;
            centerX = noseBase.x;
            centerY = noseBase.y + yRadius / 4.0f;

            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        if (noseBase != null && stickerRender instanceof MustacheStickerRender) {
            float centerX, centerY;
            float xRadius, yRadius;
            xRadius = faceWidth / 4.0f;
            yRadius = xRadius * 1.0f;
            centerX = noseBase.x;
            centerY = noseBase.y - yRadius / 2.0f;
            stickerRender.setPosition(new float[]{centerX, centerY}, xRadius, yRadius);
        }

        if (stickerRender instanceof FaceStickerRender) {
            stickerRender.setPosition(new float[]{faceCenter.x, faceCenter.y}, faceWidth, faceHeight);
        }

        texture.bindData(stickerRender);
        texture.draw();
    }

    private float getLength(PointF a, PointF b) {
        return (float) Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
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
