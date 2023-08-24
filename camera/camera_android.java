package org.gioui.x.camera;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
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
  private static CameraManager cameraManager;
  private static CameraDevice cameraDevice;
  private static CameraCaptureSession cameraCaptureSession;
  private static ImageReader imageReader;
  private static Handler backgroundHandler;
  private static HandlerThread backgroundThread;

  static public native void ImageCallback(byte[] data, String err);

  public static void openCamera(View view) {
    if (cameraDevice != null) {
      return;
    }

    askPermission(view);

    Activity activity = (Activity) view.getContext();
    Context context = activity.getApplicationContext();
    Handler handler = new Handler(context.getMainLooper());
    cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

    try {
      String cameraId = cameraManager.getCameraIdList()[0]; // 0 for rear camera
      cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
          cameraDevice = camera;
          createCaptureSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
          closeCamera();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
          closeCamera();
        }
      }, handler);
    } catch (CameraAccessException e) {
      return;
    }
  }

  private static void createCaptureSession() {
    int imageWidth = 640;
    int imageHeight = 480;
    imageReader = ImageReader.newInstance(imageWidth, imageHeight, ImageFormat.JPEG, 1);

    startBackgroundThread();
    imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      @Override
      public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] imageData = new byte[buffer.remaining()];
        buffer.get(imageData);

        try {
          ImageCallback(imageData, null);
        } catch (Exception e) {
          ImageCallback(null, e.toString());
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
            closeCamera();
          }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
          closeCamera();
        }
      }, null);
    } catch (CameraAccessException e) {
      closeCamera();
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

  public static void closeCamera() {
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
