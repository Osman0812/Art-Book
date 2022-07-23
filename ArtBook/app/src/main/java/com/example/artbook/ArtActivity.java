package com.example.artbook;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.artbook.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;


public class ArtActivity extends AppCompatActivity {

    ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionResult;
    Bitmap image;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        resultLauncher();

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if(info.equals("new")){
            //new art
            binding.nameText.setText("");
            binding.authorNameText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.kamera);
        }else{
            //old art
            int artId = intent.getIntExtra("artId",-1);
            binding.button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.authorNameText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        }else{
            //portrait
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image,width,height,true);
    }

    public void save(View view){

        String name = binding.nameText.getText().toString();
        String painterName = binding.authorNameText.getText().toString();
        String year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(image,300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] bytes = outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");
            String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES(?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,bytes);
            sqLiteStatement.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();
        startActivity(intent);

    }


    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Needed permission to access your gallery!",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", view1 -> {
                    //request permission
                    permissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }).show();
            }else{
                //request permission
                permissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

        }else {
            //goto gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }
    public void resultLauncher(){

        permissionResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
            if(result){
                //permission granted
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);

            }else{
                //permission denied
                Toast.makeText(ArtActivity.this,"Permission Denied!",Toast.LENGTH_SHORT).show();
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

            if(result.getResultCode() == RESULT_OK){
                Intent intentFromGallery = result.getData();
                if(intentFromGallery != null){
                    Uri imageUri = intentFromGallery.getData();
                    try {
                        if(Build.VERSION.SDK_INT >= 28){
                            ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(),imageUri);
                            image = ImageDecoder.decodeBitmap(source);
                        }else{
                            image = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageUri);
                        }
                        binding.imageView.setImageBitmap(image);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

        });

    }
}