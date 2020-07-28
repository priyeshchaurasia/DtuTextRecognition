package com.example.dtutextrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.yalantis.ucrop.UCrop;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnClickListener {
    private AutoFitTextureView mTextureView;
    private Button mSearchButton, mFlashButton,mSign,mOK;
    private EditText editText;
    private boolean isFlashOn=false;
    private CameraCharacteristics mCameraCharacterstics;
    private CameraDevice mCameraDevice;
    private TextView mTextView;
    BottomSheetDialog dialog;
    private String mCameraID;
    private CameraCaptureSession mCameraCaptureSession;
    private Size mPreviewDimesion;
    private CameraManager cameraManager;
    private ImageReader captImageReader;
     boolean isWebOn=false;
    private ImageReader mImageReader;
    private byte[] bytes;
    Bitmap rotatedBitmap;
    private Image mImage;
    HandlerThread handlerThread;
    private Handler backgroundHandler = new Handler();
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private ProgressBar mContentFindingProgressBar;
    private static final int CAMERA_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = (AutoFitTextureView) findViewById(R.id.photoTexture);
        mTextureView.setSurfaceTextureListener(this);
        mSearchButton = findViewById(R.id.search);
        mSearchButton.setOnClickListener(this);
        mFlashButton = findViewById(R.id.flash);
        mTextView = findViewById(R.id.textView);
        editText = findViewById(R.id.apiLink);
        mSign = findViewById(R.id.sign);
        mOK = findViewById(R.id.ok);
        mOK.setOnClickListener(this);
        mSign.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mContentFindingProgressBar = findViewById(R.id.content_find_progress);
    }
    private void openCropActivity(Uri sourceUri, Uri destinationUri) {
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(1920, 1080)
                .withOptions(options)
                .withAspectRatio(8,12)
                .start(this);
    }
        private void getbitmap(Bitmap sourceBitmap) throws IOException {
        Matrix m = new Matrix();
        int sRotation = mCameraCharacterstics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int dRotation = getWindowManager().getDefaultDisplay().getRotation();
        int jpegOrientation=(sRotation + dRotation) % 360;
        m.setRotate((float) jpegOrientation, sourceBitmap.getWidth(), sourceBitmap.getHeight());
        rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), m, true);
            FileOutputStream fos = null;
            File dir  = File.createTempFile("image", ".jpg");
            File dir2  = File.createTempFile("image2", ".jpg");
            String apiCheck = editText.getText().toString();
                try {
                    if(isWebOn){
                        if(apiCheck.length()==0){
                            mContentFindingProgressBar.setVisibility(View.GONE);
                            Toast.makeText(this,"Api Address Cannot be Empty",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            fos = new FileOutputStream(dir);
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 5, fos);
                            fos.flush();
                            fetchSignData(dir);}
                    }

                    else {
                        fos = new FileOutputStream(dir);
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                        fos.flush();

                        Toast.makeText(this,"Image Captured Succesfully",Toast.LENGTH_SHORT).show();
                    openCropActivity(Uri.fromFile(dir), Uri.fromFile(dir2));}

                } catch (Exception e) {
                }


        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),null,null);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            try {
                mContentFindingProgressBar.setVisibility(View.GONE);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                dialog = new BottomSheetDialog(MainActivity.this);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                View view = getLayoutInflater().inflate(R.layout.layout_summary, null);


                TextView txt = view.findViewById(R.id.summarytxt);
                TextView txt1 = view.findViewById(R.id.summyheading);
                txt.setVisibility(View.GONE);
                txt1.setVisibility(View.GONE);
                dialog.setContentView(view);
                dialog.show();
                runTextRecognition(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (resultCode == RESULT_CANCELED && requestCode == UCrop.REQUEST_CROP) {

            mContentFindingProgressBar.setVisibility(View.GONE);
        }
        else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);

            mContentFindingProgressBar.setVisibility(View.GONE);
        }

    }





    private void runTextRecognition(Bitmap rotatedBitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(rotatedBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result = detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        mContentFindingProgressBar.setVisibility(View.GONE);
                        // mTextView.setVisibility(View.VISIBLE);
                        mTextView.setText(firebaseVisionText.getText().toString());//Image Text
                        fetchData();//Coverts text and gives output
                        // processTextRecognitionResult(firebaseVisionText);
//                        Log.d("TAGG",firebaseVisionText.getText());

                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"No Internet Connection", Toast.LENGTH_SHORT).show();
                                Log.d("TAGG",e.toString());

                            }
                        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mTextureView.isAvailable() && mCameraCaptureSession!=null) {
            try {
                handleCamera(mTextureView.getWidth(),mTextureView.getHeight());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    protected void onPause() {
         isFlashOn=!isFlashOn;
        if(isFlashOn){
            mFlashButton.setBackground(getDrawable(R.drawable.ic_flash_off));
        }else{
            mFlashButton.setBackground(getDrawable(R.drawable.ic_flash_on));
        }
        super.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {

        super.onStop();
        Log.d("TAGGG", "onStop");
        try {
            mCameraCaptureSession.close();
            cameraManager = null;
            mCameraDevice.close();
            mCameraCaptureSession = null;
            stopBackgroundThread();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private void stopBackgroundThread() throws InterruptedException {

        Log.d("TAGGG", "onStopBack");
        try {
            handlerThread.quitSafely();
            handlerThread.join();
            backgroundHandler = null;
            handlerThread = null;
        }catch (Exception e) {
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onStart() {
        super.onStart();
        Log.d("TAGGG", "onStart");
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.INTERNET}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    handleCamera(mTextureView.getWidth(), mTextureView.getHeight());
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "You Cannot Use the App", Toast.LENGTH_SHORT).show();
                finish();
            }

        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {
            handleCamera(width,height);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    private  CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            try {
                cameraPreview();
            } catch (Exception e) {

            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private void handleCamera(int width, int height) throws CameraAccessException {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraID = cameraManager.getCameraIdList()[0];
        mCameraCharacterstics = cameraManager.getCameraCharacteristics(mCameraID);
        setupCamera(width,height);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        cameraManager.openCamera(mCameraID, stateCallback, backgroundHandler);
    }

    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private int mTotalRotation;
    private void setupCamera(int width, int height) {
        StreamConfigurationMap map = mCameraCharacterstics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
        mTotalRotation = sensorToDeviceRotation(mCameraCharacterstics, deviceOrientation);
        boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
        int rotatedWidth = width;
        int rotatedHeight = height;
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;
        if(swapRotation) {
            rotatedWidth = height;
            rotatedHeight = width;
            maxPreviewWidth = displaySize.y;
            maxPreviewHeight = displaySize.x;
        }
        captImageReader = ImageReader.newInstance(1920, 1080,
                ImageFormat.JPEG, /*maxImages*/2);
        captImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, backgroundHandler);

        mPreviewDimesion = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                rotatedWidth, rotatedHeight,maxPreviewWidth,
                maxPreviewHeight);
        return;
    }


    private void cameraPreview() throws CameraAccessException {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewDimesion.getWidth(), mPreviewDimesion.getHeight());
        Surface surface = new Surface(surfaceTexture);
        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(surface);

        List<Surface> surfaces = Arrays.asList(surface, captImageReader.getSurface());
        mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if(mCameraDevice !=null){
                    mCameraCaptureSession = session;
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        //put a delay to fix crashes in some devices
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, backgroundHandler);
                                } catch (CameraAccessException e) {
                                    Log.e("********", "Failed to start camera preview because it couldn't access camera", e);
                                } catch (IllegalStateException e) {
                                    Log.e("********", "Failed to start camera preview.", e);
                                }
                            }
                        }, 200);
                    }catch (Exception e){}}}
            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

            }
        },null);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mImageReader = reader;
            mImage = mImageReader.acquireLatestImage();
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byteBuffer.rewind();
            bytes = new byte[byteBuffer.capacity()];
            byteBuffer.get(bytes);
            Bitmap sourceBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            mImage.close();
            try {
                getbitmap(sourceBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();

        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * 9/16) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.search : {
                mContentFindingProgressBar.setVisibility(View.VISIBLE);
                captureImage();
                break;
            }
            case R.id.ok:{
                editText.setVisibility(View.GONE);
                mOK.setVisibility(View.GONE);
                break;


            }
            case  R.id.sign :{
                if(!isWebOn) {
                    mSign.setBackground(getDrawable(R.drawable.ic_link_on));
                    editText.setVisibility(View.VISIBLE);
                    mOK.setVisibility(View.VISIBLE);

                }else{
                    mSign.setBackground(getDrawable(R.drawable.ic_link_off));
                    editText.setVisibility(View.GONE);
                    mOK.setVisibility(View.GONE);

                }
                isWebOn=!isWebOn;
                break;

            }
            case R.id.flash:{
                if(isFlashOn){
                    mFlashButton.setBackground(getDrawable(R.drawable.ic_flash_off));
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                }else{
                    mFlashButton.setBackground(getDrawable(R.drawable.ic_flash_on));
                    mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                }
                isFlashOn=!isFlashOn;
                HandlerThread temp = new HandlerThread("Update Preview Thread");
                temp.start();
                if(mCameraDevice !=null){
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    private void captureImage() {
        if (mCameraDevice !=null){
            if(mCameraCaptureSession !=null){
                try {
                    CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                    captureRequest.addTarget(captImageReader.getSurface());
                    mCameraCaptureSession.capture(captureRequest.build(),captureCallback,backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d("YY","KDKAJDKA");
        }
    };

    void fetchData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    OkHttpClient client = new OkHttpClient();
                    MediaType mediaType = MediaType.parse("text/plain");
                    RequestBody body = RequestBody.create(mediaType, mTextView.getText().toString());
                    Request request = new Request.Builder()
                            .url("https://api.smrzr.io/summarize?ratio=0.15")
                            .method("POST", body)
                            .addHeader("Content-Type", "text/plain")
                            .build();
                    Response response = client.newCall(request).execute();
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    final String Jarray = Jobject.getString("summary");
                    Log.d("************", Jarray);
                    if(Jarray==null || Jarray.length()==0) {

                        View view = getLayoutInflater().inflate(R.layout.layout_summary, null);
                        ProgressBar progressBar = view.findViewById(R.id.progress_horizontal);
                        progressBar.setVisibility(View.GONE);
                        TextView txt = view.findViewById(R.id.summarytxt);
                        txt.setText(mTextView.getText().toString());

                        dialog.setContentView(view);
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                        dialog.show();
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                View view = getLayoutInflater().inflate(R.layout.layout_summary, null);
                                ProgressBar progressBar = view.findViewById(R.id.progress_horizontal);
                                progressBar.setVisibility(View.GONE);
                                TextView txt = view.findViewById(R.id.summarytxt);
                                txt.setText(Jarray);

                                dialog.setContentView(view);
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                                dialog.show();
                            }
                        });


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    void fetchSignData(final File dir){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    okhttp3.RequestBody body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), dir);
                    MultipartBody.Part part = MultipartBody.Part.createFormData("file", dir.getName(), body);
                    Retrofit retrofit = NetworkClient.getRetrofit(editText.getText().toString());
                    UploadApis uploadApis = retrofit.create(UploadApis.class);
                    Call call = uploadApis.uploadImage(part);
                    call.enqueue(new Callback() {
                        @Override
                        public void onResponse(Call call, retrofit2.Response response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    mContentFindingProgressBar.setVisibility(View.GONE);
                                    try {
                                        String str = ((ApiResponse) response.body()).getResult();
                                        SpannableStringBuilder biggerText = new SpannableStringBuilder(str);
                                        biggerText.setSpan(new RelativeSizeSpan(2f), 0, str.length(), 0);

                                        Toast.makeText(MainActivity.this,biggerText , Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });


                        }

                        @Override
                        public void onFailure(Call call, Throwable t) {
                            Log.d("TAGG",t.toString());

                            mContentFindingProgressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Api Call Failed", Toast.LENGTH_SHORT).show();

                        }
                    });


                } catch (Exception e) {

                    mContentFindingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }}).start();

    }

}
