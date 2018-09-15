package com.example.tpz.intensify;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.interfaces.OnSeekbarFinalValueListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    Button capture;
    ImageView imageView, scaleBar;
    TextView min, max, tvMin, tvMax;
    CrystalRangeSeekbar seekBar;

    int minNo = 5 * 255;
    int maxNo = 0;
    static final int CAM_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        capture = (Button) findViewById(R.id.capture);
        imageView = (ImageView) findViewById(R.id.imageView);
        scaleBar = (ImageView) findViewById(R.id.scaleBar);

        min = (TextView) findViewById(R.id.min);
        max = (TextView) findViewById(R.id.max);
        tvMin = (TextView) findViewById(R.id.tvMin);
        tvMax = (TextView) findViewById(R.id.tvMax);

        setSeekBar();

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File file = getFile("cam_image.jpg");
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                startActivityForResult(camera_intent, CAM_REQUEST);
            }
        });
    }

    private File getFile(String filename){
        File folder = new File("sdcard/intensify");
        if(!folder.exists()){
            folder.mkdir();
        }
        File image_file = new File(folder, filename);
        return image_file;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String path = "sdcard/intensify/cam_image.jpg";
        Bitmap new_bitmap = rgbToGrayscale(Drawable.createFromPath(path));

        int orientation = -1;

        try {
            ExifInterface exif = new ExifInterface(path);

            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = 270;

                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = 180;

                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = 90;

                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    orientation = 0;

                    break;
                default:
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        imageView.setImageBitmap(new_bitmap);
        imageView.setRotation(orientation);

        //save processed image
        try {
            File file = getFile("processed_image.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            new_bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        //reset seekbar
        seekBar.setMinValue(0);
        seekBar.setMaxValue(100);
    }

    private Bitmap rgbToGrayscale(Drawable drawable){
        //reset min and max light intensity
        minNo = 5 * 255;
        maxNo = 0;

        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        int picw = bitmap.getWidth();
        int pich = bitmap.getHeight();

        int[] pix = new int[picw * pich];
        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

        int acc = 10;
        for (int y = 0; y < pich; y+=acc){
            for (int x = 0; x < picw; x+=acc)
            {
                int index = y * picw + x;
                int R = (pix[index] >> 16) & 0xff;     //bitwise shifting
                int G = (pix[index] >> 8) & 0xff;
                int B = pix[index] & 0xff;

                int intensity = (int)(5 * (0.2126 * R + 0.7152 * G + 0.0722 * B));

                R = 0;
                G = 0;
                B = 0;
                if(x % 100 == 0 && y % 100 == 0) {
                    if (minNo > intensity)
                        minNo = intensity;
                    if (maxNo < intensity)
                        maxNo = intensity;
                }
                if(intensity < 255) {
                    R = 255 - intensity;
                    B = 255;
                }else if(intensity < 255 * 2) {
                    G = intensity - 255;
                    B = 255;
                }else if(intensity < 255 * 3){
                    G = 255;
                    B = 255 - (intensity - 255 * 2);
                }else if(intensity < 255 * 4){
                    R = intensity - 255 * 3;
                    G = 255;
                }else if(intensity < 255 * 5){
                    R = 255;
                    G = 255 - (intensity - 255 * 4);
                }

                for(int i = 0; i < acc; i++) {
                    for(int j = 0; j < acc; j++) {
                        if((y+j) * picw + x+i < pix.length)
                            pix[(y+j) * picw + x+i] = 0xff000000 | (R << 16) | (G << 8) | B;
                    }
                }
            }
        }
        // Create bitmap
        Bitmap new_bitmap = Bitmap.createBitmap(picw, pich, Bitmap.Config.ARGB_8888);
        // Set the pixels
        new_bitmap.setPixels(pix, 0, picw, 0, 0, picw, pich);

        return new_bitmap;
    }

    private void setScaleBarColor(){
        int picw = scaleBar.getWidth();
        int pich = scaleBar.getHeight();

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(picw, pich, Bitmap.Config.ARGB_8888);

        int[] pix = new int[picw * pich];
        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

        for (int y = 0; y < pich; y++){
            for (int x = 0; x < picw; x++)
            {
                int index = y * picw + x;

                double intensity = 5 * 255 * x / picw;
                int R = 0;
                int G = 0;
                int B = 0;
                if(intensity < 255) {
                    R = 255 - (int) intensity;
                    B = 255;
                }else if(intensity < 255 * 2) {
                    G = (int) intensity - 255;
                    B = 255;
                }else if(intensity < 255 * 3){
                    G = 255;
                    B = 255 - ((int) intensity - 255 * 2);
                }else if(intensity < 255 * 4){
                    R = (int) intensity - 255 * 3;
                    G = 255;
                }else if(intensity < 255 * 5){
                    R = 255;
                    G = 255 - ((int) intensity - 255 * 4);
                }

                pix[index] = 0xff000000 | (R << 16) | (G << 8) | B;
            }
        }
        // Set the pixels
        bitmap.setPixels(pix, 0, picw, 0, 0, picw, pich);

        scaleBar.setImageBitmap(bitmap);
    }

    private void setMinMaxPosition(){
        min.setVisibility(View.VISIBLE);
        max.setVisibility(View.VISIBLE);

        min.setText(String.format("Max:\n%.2f", maxNo / 2.55 / 5));
        max.setText(String.format("Min:\n%.2f", minNo / 2.55 / 5));

        float maxPx = minNo / 5.000f / 255 * scaleBar.getWidth() + scaleBar.getX() - max.getWidth() / 2;
        float minPx = maxNo / 5.000f / 255 * scaleBar.getWidth() + scaleBar.getX() - min.getWidth() / 2;

        max.setX(maxPx);
        min.setX(minPx);
    }

    private void setSeekBar(){
        seekBar = (CrystalRangeSeekbar) findViewById(R.id.rangeSeekbar);

        seekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener() {
            @Override
            public void valueChanged(Number minValue, Number maxValue) {
                DecimalFormat twoDForm = new DecimalFormat("#.##");
                tvMin.setText(String.valueOf(twoDForm.format(minValue)));
                tvMax.setText(String.valueOf(twoDForm.format(maxValue)));
            }
        });

        seekBar.setOnRangeSeekbarFinalValueListener(new OnRangeSeekbarFinalValueListener() {
            @Override
            public void finalValue(Number minValue, Number maxValue) {
                String path = "sdcard/intensify/cam_image.jpg";
                Bitmap new_bitmap = rangeIntensity(Drawable.createFromPath(path), (int) (minValue.floatValue() * 255 * 5 / 100), (int) (maxValue.floatValue() * 255 * 5 / 100));
                imageView.setImageBitmap(new_bitmap);
            }
        });
    }

    private Bitmap rangeIntensity(Drawable drawable, int minNo, int maxNo){
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        int picw = bitmap.getWidth();
        int pich = bitmap.getHeight();

        int[] pix = new int[picw * pich];
        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

        int acc = 30;
        for (int y = 0; y < pich; y+=acc){
            for (int x = 0; x < picw; x+=acc)
            {
                int index = y * picw + x;
                int R = (pix[index] >> 16) & 0xff;     //bitwise shifting
                int G = (pix[index] >> 8) & 0xff;
                int B = pix[index] & 0xff;

                int intensity = (int)(5 * (0.2126 * R + 0.7152 * G + 0.0722 * B));

                R = 0;
                G = 0;
                B = 0;
                if(intensity >= minNo && intensity <= maxNo){
                    if (intensity < 255) {
                        R = 255 - intensity;
                        B = 255;
                    } else if (intensity < 255 * 2) {
                        G = intensity - 255;
                        B = 255;
                    } else if (intensity < 255 * 3) {
                        G = 255;
                        B = 255 - (intensity - 255 * 2);
                    } else if (intensity < 255 * 4) {
                        R = intensity - 255 * 3;
                        G = 255;
                    } else if (intensity < 255 * 5) {
                        R = 255;
                        G = 255 - (intensity - 255 * 4);
                    }
                }

                for(int i = 0; i < acc; i++) {
                    for(int j = 0; j < acc; j++) {
                        if((y+j) * picw + x+i < pix.length)
                            pix[(y+j) * picw + x+i] = 0xff000000 | (R << 16) | (G << 8) | B;
                    }
                }
            }
        }
        // Create bitmap
        Bitmap new_bitmap = Bitmap.createBitmap(picw, pich, Bitmap.Config.ARGB_8888);
        // Set the pixels
        new_bitmap.setPixels(pix, 0, picw, 0, 0, picw, pich);

        return new_bitmap;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setScaleBarColor();
        if(minNo < maxNo)
            setMinMaxPosition();
    }
}
