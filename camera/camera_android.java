package org.gioui.x.camera;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import android.app.Activity;
import android.os.Handler;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import android.media.Image;
import android.media.ImageReader;
import android.graphics.ImageFormat;
import android.Manifest;
import android.content.Context;
import android.view.View;
import android.os.HandlerThread;

public class camera_android {
  private static CameraDevice cameraDevice;
  private static CameraCaptureSession cameraCaptureSession;
  private static ImageReader imageReader;
  private static Handler backgroundHandler;
  private static HandlerThread backgroundThread;

  static public native void FeedCallback(byte[] data, String err);

  public static void openCameraFeed(View view, String cameraId, int width, int height) {
    if (cameraDevice != null) {
      return;
    }

    askPermission(view);

    Activity activity = (Activity) view.getContext();
    Context context = activity.getApplicationContext();
    Handler handler = new Handler(context.getMainLooper());

    try {
      CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
          cameraDevice = camera;
          createCaptureSession(width, height);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
          closeCameraFeed();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
          closeCameraFeed();
        }
      }, handler);
    } catch (CameraAccessException e) {
      return;
    }
  }

  public static String[] getCameraIdList(Context context) {
    try {
      CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      return cameraManager.getCameraIdList();
    } catch (CameraAccessException e) {
      return new String[0];
    }
  }

  public static int getCameraLensFacing(Context context, String cameraId) {
    try {
      CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
      return characteristics.get(CameraCharacteristics.LENS_FACING);
    } catch (CameraAccessException e) {
      return -1;
    }
  }

  public static int getCameraSensorOrientation(Context context, String cameraId) {
    try {
      CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
      return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    } catch (CameraAccessException e) {
      return 0;
    }
  }

  private static void createCaptureSession(int width, int height) {
    // int imageWidth = 640;
    // int imageHeight = 480;
    imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

    startBackgroundThread();
    imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      @Override
      public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] imageData = new byte[buffer.remaining()];
        buffer.get(imageData);

        try {
          FeedCallback(imageData, null);
        } catch (Exception e) {
          FeedCallback(null, e.toString());
        }

        if (image != null) {
          image.close();
        }
      }
    }, backgroundHandler);

    List<Surface> surfaces = new ArrayList<>();
    surfaces.add(imageReader.getSurface());

    try {
      CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      captureRequestBuilder.addTarget(imageReader.getSurface());
      cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
          cameraCaptureSession = session;
          try {
            CaptureRequest previewRequest = captureRequestBuilder.build();
            cameraCaptureSession.setRepeatingRequest(previewRequest, null, backgroundHandler);
          } catch (CameraAccessException e) {
            closeCameraFeed();
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          closeCameraFeed();
        }
      }, null);
    } catch (CameraAccessException e) {
      closeCameraFeed();
    }
  }

  private static void startBackgroundThread() {
    backgroundThread = new HandlerThread("CameraBackground");
    backgroundThread.start();
    backgroundHandler = new Handler(backgroundThread.getLooper());
  }

  private static void stopBackgroundThread() {
    if (backgroundThread != null) {
      backgroundThread.quitSafely();
      try {
        backgroundThread.join();
        backgroundThread = null;
        backgroundHandler = null;
      } catch (InterruptedException e) {

      }
    }
  }

  public static void closeCameraFeed() {
    if (cameraDevice != null) {
      cameraDevice.close();
      cameraDevice = null;
    }

    if (cameraCaptureSession != null) {
      cameraCaptureSession.close();
      cameraCaptureSession = null;
    }

    if (imageReader != null) {
      imageReader.close();
      imageReader = null;
    }

    stopBackgroundThread();
  }

  private static boolean askPermission(View view) {
    Activity activity = (Activity) view.getContext();

    if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      activity.requestPermissions(new String[] { Manifest.permission.CAMERA }, 200);
      return true;
    }

    return false;
  }
}
