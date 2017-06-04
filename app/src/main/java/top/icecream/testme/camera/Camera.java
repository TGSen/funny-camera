package top.icecream.testme.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * AUTHOR: 86417
 * DATE: 4/22/2017
 */

public class Camera {

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

    public void openCamera() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = cameraManager.getCameraCharacteristics(cameraId + "");
            StreamConfigurationMap map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizes = map.getOutputSizes(SurfaceHolder.class);
            previewSize = sizes[0];

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

                Frame outputFrame = null;
                byte[] yuv = null;
                synchronized (lock) {
                    yuv = quarterNV21(data, previewSize.getWidth(), previewSize.getHeight());
                }
                outputFrame = new Frame.Builder()
                        .setImageData(ByteBuffer.wrap(yuv), previewSize.getWidth()/4,
                                previewSize.getHeight()/4, ImageFormat.NV21)
                        .build();

                SparseArray<Face> faces = detector.detect(outputFrame);
                for (int i = 0; i < faces.size(); i++) {
                    Face face = faces.valueAt(i);
                    List<Landmark> landmarks = face.getLandmarks();
                    for (Landmark landmark : landmarks) {
                        Log.d("logFaceData: ", "type + "+landmark.getType() +":"+ landmark.getPosition().toString());
                    }
                }

            }
        }

        public void setData(byte[] data) {
            synchronized (lock) {
                this.data = Arrays.copyOf(data, data.length);
            }
        }

        private void setRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        private byte[] quarterNV21(byte[] data, int iWidth, int iHeight) {
            byte[] yuv = new byte[iWidth/4 * iHeight/4 * 3 / 2];
            int i = 0;
            for (int y = 0; y < iHeight; y+=4) {
                for (int x = 0; x < iWidth; x+=4) {
                    yuv[i] = data[y * iWidth + x];
                    i++;
                }
            }
            return yuv;
        }

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