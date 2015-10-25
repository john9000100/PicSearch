package com.example.john.picsearch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by John on 02/01/2015.
 */
public class DeleteDialogFragment extends DialogFragment {
    private ImageAdapter imageAdapter;
    private int deletePos;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()); //!!! should sensibly inherit the parent dialog's activity, as there would be no other choices
        builder.setMessage(R.string.delete_question)
               .setPositiveButton(R.string.yes_delete, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                        imageAdapter.deleteImage(deletePos);
                   }
               })
                .setNegativeButton(R.string.no_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //the dialog is canceled
                    }
                });

        return builder.create();
    }

    public void setImageAdapter(ImageAdapter imageAdapter) {
        this.imageAdapter = imageAdapter;
    }

    public void setDeletePos(int deletePos) {
        this.deletePos = deletePos;
    }
}
