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

    private Context context;
    private Handler handler;

    private int cameraId = 1;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder requestBuilder;

    private Size previewSize;

    private SurfaceTexture surfaceTexture;
    private FaceDetector detector;
    private FaceDetectionProcessor faceDetectThread;
    private Image image = null;
    private SparseArray<Face> faces;


    public Camera(Context context) {
        this.context = context;
        initLooper();
        initFaceDetection();
    }

    private void initFaceDetection() {
        detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();
        faceDetectThread = new FaceDetectionProcessor();
        faceDetectThread.start();
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
        cameraId = (cameraId == 0 ? 1 : 0);
        openCamera();
    }

    private void initLooper() {
        HandlerThread handlerThread = new HandlerThread("camera");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void startPreview(@NonNull CameraDevice camera) {

        ImageReader imagePreviewReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 1);
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
        public void onImageAvailable(ImageReader reader) {
            image = reader.acquireNextImage();
            faceDetectThread.setData(convertYUV420888ToNV21(image));
            image.close();
        }
    };

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


    private class FaceDetectionProcessor extends Thread {

        private boolean isRunning = true;
        private final Object lock = new Object();
        private byte[] data = null;

        @Override
        public void run() {
            while (isRunning) {

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (image == null || data == null) {
                    continue;
                }

                int rotation;
                if (cameraId == 0) {
                    rotation = Frame.ROTATION_90;
                } else {
                    rotation = Frame.ROTATION_270;
                }

                Frame outputFrame;
                byte[] yuv;
                synchronized (lock) {
                    yuv = data;
                }
                outputFrame = new Frame.Builder()
                        .setImageData(ByteBuffer.wrap(yuv), previewSize.getWidth(),
                                previewSize.getHeight(), ImageFormat.NV21)
                        .setRotation(rotation)
                        .build();

                faces = detector.detect(outputFrame);
            }
        }

        public void setData(byte[] data) {
            synchronized (lock) {
                this.data = data;
            }
        }

        private void setRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }
    }

    public SparseArray<Face> getFaces() {
        return faces;
    }

    /*private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();

                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                save(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }

        private void save(byte[] bytes) throws IOException {
            OutputStream output = null;
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/pic.jpg");
                output = new FileOutputStream(file);
                output.write(bytes);
            } finally {
                if (null != output) {
                    output.close();
                }
            }
        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(context, "saved successfully", Toast.LENGTH_SHORT).show();
            startPreview(cameraDevice);
        }
    };*/

    /*@Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }*/

        /*public void takePicture() {
        SparseIntArray ORIENTATIONS = new SparseIntArray();
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

        if (null == cameraDevice) return;

        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size imageSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG)[0];

            ImageReader reader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureController.getTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = (((Activity) context)).getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            reader.setOnImageAvailableListener(onImageAvailableListener, handler);

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureCallback, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }*/

    /*public void changeCamera() {
        cameraDevice.close();
        cameraId = (cameraId == 0 ? 1 : 0);
        openCamera();
    }*/

    /*@Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        openCamera();
    }*/


    /*@Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {
    }*/
}