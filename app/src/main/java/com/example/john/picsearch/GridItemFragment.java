package com.example.john.picsearch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Created by John on 02/01/2015.
 */
public class GridItemFragment extends DialogFragment {
    private String resultsURL;
    private ImageAdapter imageAdapter;
    private int deletePos;

    public void setResultsURL(String resultsURL) {
        this.resultsURL = resultsURL;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.dialog_options_array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' argument contains the index position
                // of the selected item
                switch (which) {
                    //open results page
                    case 0:
                        openBrowser();
                        break;
                    case 1:
                        openDeleteDialog();
                }
            }
        });
        return builder.create();
    }

    private void openBrowser() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(resultsURL));
        startActivity(browserIntent);
    }

    private void openDeleteDialog() {
        DeleteDialogFragment dialogFragment = new DeleteDialogFragment();
        dialogFragment.setImageAdapter(imageAdapter);
        dialogFragment.setDeletePos(deletePos);
        dialogFragment.show(getFragmentManager(), "delete_dialog");
    }

    public void setImageAdapter(ImageAdapter imageAdapter) {
        this.imageAdapter = imageAdapter;
    }

    public void setDeletePos(int deletePos) {
        this.deletePos = deletePos;
    }
}
