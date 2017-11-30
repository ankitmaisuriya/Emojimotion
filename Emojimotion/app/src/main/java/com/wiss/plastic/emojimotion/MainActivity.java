package com.wiss.plastic.emojimotion;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;




import java.io.ByteArrayOutputStream;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0 ;
    private ImageView imageHolder;
    private final int requestCode = 20;
    private Button capturedImageButton;
    private Button upload_button;

    //firebase storage reference
    private StorageReference storageReference;
    private Bitmap bitmap;
    private ImageView imageSmily;
    private RelativeLayout relative_layout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relative_layout = (RelativeLayout)findViewById(R.id.relative);
        imageHolder = (ImageView)findViewById(R.id.captured_photo);
        imageSmily = (ImageView)findViewById(R.id.emoji);
        capturedImageButton = (Button)findViewById(R.id.photo_button);
        upload_button = (Button)findViewById(R.id.upload_cloud);


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        //getting firebase storage reference
        storageReference = FirebaseStorage.getInstance().getReference();
        capturedImageButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(photoCaptureIntent, requestCode);
            }
        });

        upload_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                uploadFile();
                bitmap = ((BitmapDrawable)imageHolder.getDrawable()).getBitmap();
                if(bitmap == null){
                  /*  Toast.makeText(MainActivity.this,
                            "myBitmap == null",
                            Toast.LENGTH_LONG).show();*/
                }else{
                    detectFace();

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(this.requestCode == requestCode && resultCode == RESULT_OK){
            Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            imageHolder.setImageBitmap(bitmap);
        }
    }

    private void uploadFile() {
        //if there is a file to upload
        Bitmap bitmap = ((BitmapDrawable)imageHolder.getDrawable()).getBitmap();
        Uri path = getImageUri(getApplicationContext(),bitmap);
     //   String imgPath = path.toString();
        if (path != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            StorageReference riversRef = storageReference.child("images/pic.jpg");
            riversRef.putFile(path)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying a success toast
                         //   Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();

                            //and displaying error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @SuppressWarnings("VisibleForTests")
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            //you can display an error toast
        }
    }


    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        Uri imageUri = null;
        if(path!= null) {
            imageUri=Uri.parse(path);
        }
        return imageUri;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] ==  PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /*
    reference:
    https://search-codelabs.appspot.com/codelabs/face-detection
     */
    private void detectFace(){

        //Create a Paint object for drawing with
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.GREEN);
        myRectPaint.setStyle(Paint.Style.STROKE);

        Paint landmarksPaint = new Paint();
        landmarksPaint.setStrokeWidth(10);
        landmarksPaint.setColor(Color.RED);
        landmarksPaint.setStyle(Paint.Style.STROKE);

        Paint smilingPaint = new Paint();
        smilingPaint.setStrokeWidth(4);
        smilingPaint.setColor(Color.YELLOW);
        smilingPaint.setStyle(Paint.Style.STROKE);

        boolean somebodySmiling = false;

        //Create a Canvas object for drawing on
        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(bitmap, 0, 0, null);

        //Detect the Faces

        //!!!
        //Cannot resolve method setTrackingEnabled(boolean)
        //FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).build();
        //faceDetector.setTrackingEnabled(false);

        FaceDetector faceDetector =
                new FaceDetector.Builder(getApplicationContext())
                        .setTrackingEnabled(false)
                        .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                        .build();

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        //Draw Rectangles on the Faces
        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);

            //get Landmarks for the first face
            List<Landmark> landmarks = thisFace.getLandmarks();
            for(int l=0; l<landmarks.size(); l++){
                PointF pos = landmarks.get(l).getPosition();
                tempCanvas.drawPoint(pos.x, pos.y, landmarksPaint);
            }

            //check if this face is Smiling
            final float smilingAcceptProbability = 0.5f;
            float smilingProbability = thisFace.getIsSmilingProbability();
            if(smilingProbability > smilingAcceptProbability){
                tempCanvas.drawOval(new RectF(x1, y1, x2, y2), smilingPaint);
                somebodySmiling = true;
            }
        }

        imageHolder.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));

        if(somebodySmiling){
/*
            Toast.makeText(MainActivity.this,
                    "Your Smile is very cute...",
                    Toast.LENGTH_LONG).show();
*/

            Snackbar snackbar = Snackbar
                    .make(relative_layout, "Your Smile is very nice...", Snackbar.LENGTH_LONG);
            snackbar.show();



            snackbar.show();

            imageSmily.setImageResource(R.drawable.smilely);
        }else{
            /*Toast.makeText(MainActivity.this,
                    "Please Don't be Sad...Smily Please",
                    Toast.LENGTH_LONG).show();*/

            Snackbar snackbar = Snackbar
                    .make(relative_layout, "Please Don't be Sad...Smily Please", Snackbar.LENGTH_LONG);
            snackbar.show();
            imageSmily.setImageResource(R.drawable.sad_mood);
        }

    }

}
