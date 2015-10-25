package com.example.john.picsearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.example.john.picsearch.util.SystemUiHider;

import java.io.File;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainScreen extends Activity {

    private static LruCache<String, Bitmap> mMemoryCache;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /*Camera intent request code*/
    private static final int START_CAMERA = 0;

    private static final String DIALOG_TAG = "grid_dialog";
    /*Reusable camera bitmap*/
  //  private static WeakReference<Bitmap> mReusableBitmap = new WeakReference<Bitmap>(null);

    /*Views*/
    private View controlsView;
    private GridView gridView;
    private ImageAdapter imageAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_screen);

        controlsView = findViewById(R.id.fullscreen_content_controls);
        gridView = (GridView) findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this);
        gridView.setAdapter(imageAdapter);

        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
                String resultsURL = (String) imageAdapter.getItem(position);
                GridItemFragment fragment = new GridItemFragment();
                fragment.setResultsURL(resultsURL);
                fragment.setImageAdapter(imageAdapter);
                fragment.setDeletePos(position);
                fragment.show(getFragmentManager(), DIALOG_TAG);

                return true;
            }
        });

        findViewById(R.id.camera_button).setOnClickListener(mClickListener);


        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/4 of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;

        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());

        mMemoryCache = retainFragment.mRetainedCache;
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
            retainFragment.mRetainedCache = mMemoryCache;
        }
    }


    public static synchronized void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public static synchronized void deleteBitmapFromMemoryCache(String key) {
        if (getBitmapFromMemCache(key) != null) {
            mMemoryCache.remove(key);
        }
    }

    public static Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {

        Cursor cursor = null;
        try {
            /* Get the string in the column "_data" - this is the absolute path to the image file saved on the SD card*/
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == START_CAMERA) {
            if (resultCode == RESULT_OK) {

                final String filePath = getRealPathFromURI(this, data.getData());
                File imageFile = new File(filePath);

                new SearchByImageAsync(this, imageAdapter).execute(imageFile);


            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
                Toast.makeText(this, "Image capture failed. Try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        imageAdapter.persistPictureHistory();
        super.onPause();
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */

    View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, START_CAMERA);
          
        }
    };

    public void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, START_CAMERA);
    }

//    public static void setReusableCameraBitmap(Bitmap bitmap) {
//
//        mReusableBitmap = new WeakReference<Bitmap>(bitmap);
//    }


    @Override
    protected void onRestart() {
        super.onRestart();
        imageAdapter.notifyDataSetChanged();
    }
}
