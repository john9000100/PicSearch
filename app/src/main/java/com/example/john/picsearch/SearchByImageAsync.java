package com.example.john.picsearch;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by John on 26/12/2014.
 */
public class SearchByImageAsync extends AsyncTask<Object, Void, String[]> {

    private static final String UPLOAD_IMAGE_URL = "https://www.google.ca/searchbyimage/upload";
    /*scale down factor for faster image uploading to google*/
    private static final float SCALE_FACTOR = 7;

    /*minimum height and width for image to be uploaded to Google. actual width and height of uploaded
    image should typically be close to, but >= these values*/

    private static final int MIN_SEARCH_WIDTH = 200;
    private static final int MIN_SEARCH_HEIGHT = MIN_SEARCH_WIDTH;


    private Context mContext;
    private ImageAdapter imageAdapter;
    private ProgressDialog progressDialog;

    public SearchByImageAsync(Context a, ImageAdapter i) {
        mContext = a;
        imageAdapter = i;
    }

    public static Bitmap loadScaledImage(String path, int reqwidth, int reqheight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inMutable = true;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqwidth, reqheight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private File exportToInternalStorage(Bitmap bitmap, String name) {

        File resizedImageFile = new File(mContext.getFilesDir(), name);

        try {
            FileOutputStream fOut = new FileOutputStream(resizedImageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) { // TODO
            e.printStackTrace();
        }

        return resizedImageFile;

    }


    private HttpResponse uploadImage(File imageFile) throws IOException {
        HttpPost post = new HttpPost(UPLOAD_IMAGE_URL);

        HttpClientParams.setRedirecting(post.getParams(), false);

        MultipartEntity entity = new MultipartEntity();
        entity.addPart("encoded_image", new FileBody(imageFile));
        entity.addPart("image_url",new StringBody(""));
        entity.addPart("image_content",new StringBody(""));
        entity.addPart("filename",new StringBody(""));
        entity.addPart("h1",new StringBody("en"));
        entity.addPart("bih",new StringBody("179"));
        entity.addPart("biw",new StringBody("1600"));

        post.setEntity(entity);

        HttpResponse response = new DefaultHttpClient().execute(post);

        return response;

    }


    private String getResultsURL(HttpResponse response) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String resultsURL = "";
        try {
            //get the search result URL out of the line that contains it, and open it in the browser
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.indexOf("https://www.google.ca/search?tbs=sbi") > 0) {
                    resultsURL = line.substring(9, line.length() - 12);
                    continue;
                }
            }
        } finally {
            rd.close();
        }

        return resultsURL; // if there is no exception thrown, then resultsURL will always contain the URL of the results page at this point, not the empty string
    }

    private void openResultsPage(String resultsURL) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(resultsURL));
        mContext.startActivity(browserIntent);
    }


    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(mContext, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Processing image");
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(String[] result) {

        progressDialog.dismiss();
    }

    @Override
    protected String[] doInBackground(Object... params) {
        if (params.length == 1) {
            try {
                // search by image pipeline:
                // 1. shrink image and delete the original from the camera gallery.
                // 2. export shrunken image to internal storage for the app's gallery to access.
                // 3. query the google search by image server with the shrunken image.
                // 4. parse the response for the URL of the results page, then open it
                // 5. save the resized image's path and its corresponding results URL

                File image = (File) params[0];
                Bitmap shrunkBitmap = loadScaledImage(image.getAbsolutePath(), MIN_SEARCH_WIDTH, MIN_SEARCH_HEIGHT);
                image.delete();
                //This removes the image's metadata from the media content provider, so that blank thumbnails don't appear in the normal camera Gallery
                MediaScannerConnection.scanFile(mContext,
                        new String[]{image.getAbsolutePath()}, null, null);

                //makes sure saved files have unique names
                File resizedImage = exportToInternalStorage(shrunkBitmap, "resized_" + System.currentTimeMillis());
                String path = resizedImage.getAbsolutePath();
                MainScreen.addBitmapToMemoryCache(path, shrunkBitmap);
                HttpResponse response = uploadImage(resizedImage);
                String resultsURL = getResultsURL(response);

                String[] result = {path, resultsURL};
                //Because saveImagePathAndURL calls notifyDataSetChanged() to update the GridView UI, this must be done on the UI thread, BUT
                //This isn't true anymore, so it doesn't need to be done on post execute
                try {
                    imageAdapter.saveImagePathAndURL(result[0], result[1]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                openResultsPage(resultsURL);

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}

