package com.example.john.picsearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/*deals with the app's image/results url data, including saving and deleting and producing ImageViews from the saved images*/

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mPictureHistory = null;
    public static final String PICTURE_HISTORY_FILE = "mPictureHistory.john";
    private static final int MIN_THUMBNAIL_WIDTH = 150;
    private static final int MIN_THUMBNAIL_HEIGHT = MIN_THUMBNAIL_WIDTH;
    private Bitmap mPlaceHolderBitmap;

    public ImageAdapter(Context c) {
        mContext = c;
        mPictureHistory = loadPictureHistory();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        mPlaceHolderBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.placeholder, options);
    }

    public void loadBitmap(String path, ImageView imageView) {
        final String imageKey = path;

        final Bitmap bitmap = MainScreen.getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            if (cancelPotentialWork(path, imageView)) {
                final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
                final BitmapWorkerTask.AsyncDrawable asyncDrawable =
                        new BitmapWorkerTask.AsyncDrawable(mContext.getResources(), mPlaceHolderBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                task.execute(path);
            }
        }
    }

    public static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.getData();
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || !bitmapData.equals(data)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof BitmapWorkerTask.AsyncDrawable) {
                final BitmapWorkerTask.AsyncDrawable asyncDrawable = (BitmapWorkerTask.AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    public int getCount() {
        return mPictureHistory.size()/2;
    }

    public Object getItem(int position) {
        return mPictureHistory.get(2*position + 1);
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(150, 150));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        loadBitmap(mPictureHistory.get(2*position), imageView);

        return imageView;
    }

    private List<String> loadPictureHistory() {
        List<String> pictureHistory = new ArrayList<String>();

        try {
            FileInputStream fileInputStream = new FileInputStream(mContext.getFilesDir() + PICTURE_HISTORY_FILE);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            pictureHistory = (ArrayList<String>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (FileNotFoundException f) {
            //!!! you should make sure that files in internal memory are ALWAYS readable by the application.
            //Otherwise, if f was thrown when a picture history file DID exist, then all picture history that existed would
            //be overwritten by persistPictureHistory()

            //if the picture history file doesn't exist, return an empty list.
            //the picture history file is saved to internal memory every time onPause() is called. if it doesn't yet exist, a new one will be created at THAT point.
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pictureHistory;

    }

    /*image paths are always in even numbered positions x, and corresponding urls in x + 1*/
    public void saveImagePathAndURL(String path, String url) throws IOException {
        //add the image path and then results URL
        mPictureHistory.add(path);
        mPictureHistory.add(url);
        //notifyDataSetChanged();

    }

    /*to be called by onPause()*/
    public void persistPictureHistory() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(mContext.getFilesDir() + PICTURE_HISTORY_FILE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(mPictureHistory);
            objectOutputStream.close();
        } catch (IOException e) {
            Toast.makeText(mContext, "Error saving recent gallery changes. " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void deleteImage(int pos) {
        if (mPictureHistory.size() > 2 * pos + 1) {
            String path = mPictureHistory.get(pos * 2);
            mPictureHistory.remove(pos * 2 + 1); //delete results URL
            mPictureHistory.remove(pos * 2);  //delete image path
            new File(path).delete();
            MainScreen.deleteBitmapFromMemoryCache(path);
            notifyDataSetChanged();
        }
    }
}