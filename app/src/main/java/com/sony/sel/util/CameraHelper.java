// Copyright (C) 2013 Sony Mobile Communications AB.
// All rights, including trade secret rights, reserved.

package com.sony.sel.util;

import android.app.admin.DevicePolicyManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

import static android.hardware.Camera.CameraInfo;

public class CameraHelper {

  public interface CameraListener {
    void onErrorReceived(int errorMessageResourceId);
  }

  private static final String LOG_TAG = "CameraHelper";
  private static final int MAX_RETRY_OPEN_CAMERA = 4;
  private Camera mCamera;
  private Camera.Size mPictureSize;
  private Camera.Size mPreviewSize;
  private Camera.Parameters mParameters;
  private final DevicePolicyManager mDevicePolicyManager;
  private int mZoomMax;
  private int mOrientation;
  private boolean mSmoothZoomSupported = false;
  private CameraListener mCameraListener;

  public CameraHelper(DevicePolicyManager devicePolicyManager) {
    mDevicePolicyManager = devicePolicyManager;
  }

  /**
   * Acquire the camera interface and configure camera parameters.
   * <p/>
   */
  public Camera getCameraInstance(int cameraId) {
    Log.d(LOG_TAG, "getCameraInstance");
    if (isCameraDisabled()) {
      throw new IllegalStateException("Camera disabled.");
    }

    if (mCamera == null) {
      int retry = 0;
      //Fix for BZ9063: try a few times to see if you can get hold of the camera service
      while ((mCamera == null) && (retry++ < MAX_RETRY_OPEN_CAMERA)) {
        mCamera = open(cameraId);
      }
    }
    if (mCamera == null) {
      throw new IllegalStateException("Couldn't acquire camera.");
    }
    return mCamera;
  }

  public void initCameraParameters() {
    if (mCamera != null) {
      try {
        getParameters();
      } catch (Exception e) {
        Log.d(LOG_TAG, "initCameraParameters : Exception init camera parameters: " + e);
      }
    }
  }

  public void setCameraListener(CameraListener listener) {
    mCameraListener = listener;
  }

  private void alertOpenCameraError(int stringResourceId) {
    if (mCameraListener != null) {
      mCameraListener.onErrorReceived(stringResourceId);
    }
  }

  private void getParameters() throws Exception {
    if (mCamera != null) {
      mParameters = mCamera.getParameters();
    }
  }

  private void setParameters() {
    if (mCamera != null) {
      try {
        // update camera parameters
        mCamera.setParameters(mParameters);
      } catch (Exception e) {
        Log.d(LOG_TAG, "Exception setting camera parameters: " + e);
      }
    }
  }

  public Camera.Size getPictureSize() {
    return mPictureSize;
  }

  /**
   * Release the camera so other applications can use it (i.e. the Camera app).
   * <p/>
   * Needs to be called when the parent Activity is paused / goes into background.
   */
  public void releaseCamera() {
    Log.d(LOG_TAG, "releaseCamera");
    if (mCamera != null) {
      try {
        mCamera.release();
      } catch (Exception e) {
        Log.d(LOG_TAG, "Exception releasing camera: " + e);
      }
      mCamera = null; //make mCamera null even if there is exception in release.
    }
  }

  /**
   * Is the camera in use?
   */
  public boolean isCameraActive() {
    return mCamera != null;
  }

  /**
   * Return the Camera ID for the main (back-facing) camera on the device.
   * <p/>
   * If no back-facing camera is found, then return the first camera in the list.
   *
   * @return Camera ID
   */
  public int getBackFacingCameraId() {
    Log.d(LOG_TAG, "getBackFacingCameraId");
    int nCameras = 0;
    try {
      nCameras = Camera.getNumberOfCameras();
    } catch (Exception e) {
      //return the first camera
      Log.d(LOG_TAG, "Exception getting number of cameras: " + e);
    }

    for (int id = 0; id < nCameras; id++) {
      CameraInfo info = new CameraInfo();
      Camera.getCameraInfo(id, info);
      if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
        return id;
      }
    }
    // just return the first camera
    return 0;
  }

  /**
   * Return the amount in degrees that the camera's capture
   * image needs to be rotated to match the current display rotation.
   *
   * @param cameraId             ID of the camera to use.
   * @param displayRotation      TODO
   * @param isOrientationChanged TODO
   * @return The number of degrees the camera image needs to be rotated to display properly.
   */
  public int getCameraRotation(int cameraId, int displayRotation, boolean isOrientationChanged) {
    Log.i(LOG_TAG, "getCameraRotation (" + cameraId + ", " + displayRotation + ", " + isOrientationChanged + ")");

    CameraInfo info = new CameraInfo();
    if (isOrientationChanged) {
      Log.i(LOG_TAG, "Orientation is changed");
      Camera.getCameraInfo(cameraId, info);
    }
    int result;
    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
      Log.i(LOG_TAG, "Front facing camera");
      result = (info.orientation + displayRotation) % 360;
      result = (360 - result) % 360; // compensate the mirror
    } else { // back-facing
      result = (info.orientation - displayRotation + 360) % 360;
      Log.i(LOG_TAG, "Back facing camera orientation: " + result);
    }
    Log.i(LOG_TAG, "DisplayRotation: " + displayRotation);
    return result;
  }

  /**
   * Request camera hardware to set the focus mode if it is supported.
   *
   * @param focusMode The focusmode to be used for camera parameters.
   */
  public void setCameraFocusMode(final String focusMode) {
    Log.d(LOG_TAG, "setCameraFocusMode to: " + focusMode);
    try {
      getParameters();
      if (!isSupported(focusMode, mParameters.getSupportedFocusModes())) {
        return;
      }
      mParameters.setFocusMode(focusMode);
      setParameters();
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception initializing camera focus mode (" + focusMode + "): " + e);
    }
  }

  /**
   * Request camera hardware to set the flash mode if it is supported.
   *
   * @param flashType The flashType to be used for camera parameters.
   */
  public void setFlashMode(String flashType) throws Exception {
    Log.d(LOG_TAG, "setFlashMode to: " + flashType);
    getParameters();
    if (!isSupported(flashType, mParameters.getSupportedFlashModes())) {
      return;
    }
    mParameters.setFlashMode(flashType);
    setParameters();
  }

  /**
   * Function name is misleading.
   * This sets the best preview and picture size.
   *
   * @param displaySize Size of the display.
   * @return The best preview size match in the list of Sizes.
   */
  public Camera.Size getPreviewSize(Point displaySize) {
    Log.d(LOG_TAG, "getPreviewSize");
    try {
      getParameters();
      // figure out and save the best picture size
      if (mParameters.getSupportedPictureSizes() != null) {
        mPictureSize = getBestPictureSize(mParameters.getSupportedPictureSizes(), displaySize);
      }
      if (mPictureSize != null) {
        mParameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        // figure out and save the best preview size
        mPreviewSize = getBestPreviewSize(mParameters.getSupportedPreviewSizes(), mPictureSize, displaySize);
      }

      Camera.Size original = mParameters.getPreviewSize();
      if (mPreviewSize != null && !original.equals(mPreviewSize)) {
        mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        Log.v(LOG_TAG, "Changing camera preview SIZE");
      }
      setParameters();
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception initializing camera parameters: " + e);
    }

    return mPreviewSize;
  }


  /**
   * Start Smooth Zoom in on camera
   */
  public void smoothZoomIn() {
    try {
      if (mSmoothZoomSupported) {
        mCamera.startSmoothZoom(mZoomMax);
      }
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception startSmoothZoom: " + e);
    }
  }

  /**
   * Start Smooth Zoom out on camera
   */
  public void smoothZoomOut() {
    try {
      if (mSmoothZoomSupported) {
        mCamera.startSmoothZoom(0);
      }
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception startSmoothZoom: " + e);
    }
  }

  /**
   * Stop Smooth Zoom
   */
  public void stopZoom() {
    try {
      if (mSmoothZoomSupported) {
        mCamera.stopSmoothZoom();
      }
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception stopSmoothZoom: " + e);
    }
  }

  /**
   * KitKat: Zooming changed so that [Vol+ to zoom in, Vol- to zoom out] for all orientations
   * Pre-KitKat: Landscape was [Vol+ to zoom out, Vol- to zoom in]  Portrait was vise-versa
   *
   * @param keyCode - Key being pressed
   * @return true if the event was handled
   */
  public boolean startZoom(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      switch (getOrientation()) {
        // Phone is in portrait
        case 0:
          if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            smoothZoomOut();
            return true;
          }
          break;

        default:
          if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
              smoothZoomOut();
              return true;
            }
          } else {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
              smoothZoomOut();
              return true;
            }
          }
          break;
      }
      smoothZoomIn();
      return true;
    }
    // not the right type of event
    return false;
  }

  /**
   * Get the preview size that is the best match to aspect ratio
   * of the full-size picture and closest to the display size.
   *
   * @param sizes       List of available preview sizes.
   * @param pictureSize Size being used for the full-scale picture
   * @param displaySize Size of the display.
   * @return The best match in the list of Sizes.
   */
  private static Camera.Size getBestPreviewSize(List<Camera.Size> sizes, Camera.Size pictureSize,
                                                Point displaySize) {
    if (sizes == null) {
      return null;
    }

    Log.d(LOG_TAG, "Display size: {" + displaySize.y + ", " + displaySize.x + "}");
    Log.d(LOG_TAG, "Picture size: {" + pictureSize.height + ", " + pictureSize.width + "}");

    Log.d(LOG_TAG, "Preview sizes to choose from");
    for (Camera.Size size : sizes) {
      Log.d(LOG_TAG, "Size (HxW): {" + size.height + ", " + size.width + "}");
    }

    // normalize width based on landscape orientation
    int targetWidth = displaySize.x > displaySize.y ? displaySize.x : displaySize.y;
    // target ratio is the picture's aspect ratio
    final float targetRatio = ScalingHelper.getScale(pictureSize.width, pictureSize.height);

    // set up size, ratio, diff
    Camera.Size bestSize = null;
    float bestRatio = Float.MAX_VALUE;
    float minDiff = Float.MAX_VALUE;
    final double aspectTolerance = 0.001;

    // iterate size list
    for (Camera.Size size : sizes) {
      float mRatio = ScalingHelper.getScale(size.width, size.height);

      //if (Math.abs(mRatio - targetRatio) > Math.abs(bestRatio - targetRatio)) {
      if (Math.abs(mRatio - targetRatio) > aspectTolerance) {
        // not a better ratio
        continue;
      }
      if (Math.abs(size.width - targetWidth) < minDiff) {
        // a better ratio and a closer size match
        bestSize = size;
        bestRatio = mRatio;
        minDiff = Math.abs(size.width - targetWidth);
      }
    }

    if (bestSize != null) {
      Log.d(LOG_TAG, "'Best' Size (HxW): {" + bestSize.height + ", " + bestSize.width + "}");
    } else {
      Log.d(LOG_TAG, "(getBestPreviewSize) bestSize == null!!!");
    }
    return bestSize;
  }


  /**
   * Get the picture size that is the closest match to the display's aspect ratio
   * and nearest to our target pixel count of 12 megapixels.
   *
   * @param sizes       List of available preview sizes.
   * @param displaySize Size of the display.
   * @return The best match in the list of Sizes.
   */
  private static Camera.Size getBestPictureSize(List<Camera.Size> sizes, Point displaySize) {

    // target pixel count
    int targetPixelCount = 1024 * 1024 * 12;

    // figure out target aspect ratio based on landscape orientation
    float targetRatio = displaySize.x > displaySize.y ? ScalingHelper.getScale(displaySize.x, displaySize.y) :
            ScalingHelper.getScale(displaySize.y, displaySize.x);

    // set up size, ratio, diff
    Camera.Size bestSize = null;
    float bestRatio = Float.MAX_VALUE;
    int minDiff = Integer.MAX_VALUE;

    // iterate size list
    for (Camera.Size size : sizes) {
      float ratio = ScalingHelper.getScale(size.width, size.height);
      if (Math.abs(ratio - targetRatio) > Math.abs(bestRatio - targetRatio)) {
        // not a better ratio
        continue;
      }
      if (Math.abs((size.width * size.height) - targetPixelCount) < minDiff) {
        // a better ratio and a closer megapixel match
        bestSize = size;
        bestRatio = ratio;
        minDiff = Math.abs(size.width * size.height);
      }
    }
    return bestSize;
  }

  boolean isCameraDisabled() {
    return mDevicePolicyManager.getCameraDisabled(null);
  }

  /**
   * Open Camera with cameraId
   *
   * @param cameraId ID of the camera to use.
   * @return Camera
   */
  Camera open(int cameraId) {
    Log.d(LOG_TAG, "Trying to open camera: " + cameraId);
    try {
      return Camera.open(cameraId);
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception getting the camera: " + e.getMessage());
    }
    return null;
  }

  /**
   * Initialize zoom parameters and set listener for zoom change.
   */
  public void initializeZoom() {
    try {
      if (!mParameters.isZoomSupported()) {
        return;
      }
      mZoomMax = mParameters.getMaxZoom();
      mSmoothZoomSupported = mParameters.isSmoothZoomSupported();
      Log.v(LOG_TAG, "initializeZoom: mZoomMax = " + mZoomMax);
    } catch (Exception e) {
      Log.d(LOG_TAG, "Exception initializeZoom: " + e.getMessage());
    }
  }

  public void setOrientation(int orientation) {
    this.mOrientation = orientation;
  }

  public int getOrientation() {
    return mOrientation;
  }

  private static boolean isSupported(String value, List<String> supported) {
    return supported == null ? false : supported.indexOf(value) >= 0;
  }
}
