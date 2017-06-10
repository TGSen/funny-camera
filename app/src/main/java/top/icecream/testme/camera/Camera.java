package top.icecream.testme.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * AUTHOR: 86417
 * DATE: 4/22/2017
 */

public class Camera {

    public static final int RAW_WIDTH = 640;
    public static final int RAW_HEIGHT = 480;

    private static final int BACK = 0;
    private static final int FRONT = 1;

    private Context context;
    private Handler handler;

    private int cameraId = FRONT;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder requestBuilder;

    private Size previewSize;

    private SurfaceTexture surfaceTexture;
    private FaceDetector detector;
    private SparseArray<Face> faces = null;

    private boolean isDetectFace = false;

    public Camera(Context context) {
        this.context = context;
        initLooper();
        initFaceDetection();
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    @SuppressWarnings("MissingPermission")
    public void openCamera() {
        try {
            previewSize = new Size(RAW_WIDTH, RAW_HEIGHT);
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            cameraManager.openCamera("" + cameraId, cameraStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void changeCamera() {
        cameraDevice.close();
        cameraId = (cameraId == BACK ? FRONT : BACK);
        openCamera();
    }

    public SparseArray<Face> getFaces() {
        return faces;
    }

    public void setDetectFace(boolean detectFace) {
        this.isDetectFace = detectFace;
    }

    private void initLooper() {
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void initFaceDetection() {
        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
    }

    private void startPreview(@NonNull CameraDevice camera) {
        ImageReader imagePreviewReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
        imagePreviewReader.setOnImageAvailableListener(previewAvailableListener, handler);

        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        try {
            requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(surface);
            requestBuilder.addTarget(imagePreviewReader.getSurface());
            camera.createCaptureSession(Arrays.asList(surface, imagePreviewReader.getSurface()), sessionStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Camera.this.cameraDevice = camera;
            startPreview(camera);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                session.setRepeatingRequest(requestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                    }
                }, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    private final ImageReader.OnImageAvailableListener previewAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            if (isDetectFace) {
                detectFaces(image);
            }
            image.close();
        }
    };

    private void detectFaces(Image image) {
        byte[] data = convertYUV420888ToNV21(image);
        int rotation = cameraId == BACK ? Frame.ROTATION_90 : Frame.ROTATION_270;
        Frame outputFrame = new Frame.Builder()
                .setImageData(ByteBuffer.wrap(data), previewSize.getWidth(),
                        previewSize.getHeight(), ImageFormat.NV21)
                .setRotation(rotation)
                .build();
        faces = detector.detect(outputFrame);
    }

    private byte[] convertYUV420888ToNV21(Image imgYUV420) {
        byte[] data;
        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        data = new byte[buffer0_size + buffer2_size];
        buffer0.get(data, 0, buffer0_size);
        buffer2.get(data, buffer0_size, buffer2_size);
        return data;
    }
}