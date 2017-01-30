package com.example.yuao.a571stocksearch;

import android.app.*;
import android.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.os.*;
import android.content.*;
import android.content.Intent;
import android.support.v7.app.*;

/**
 * Created by yuao on 4/20/16.
 */
public class MyAlertDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(getArguments() == null) {
            System.out.println("No args");
        }
        String message = getArguments().getString(getResources().getString(R.string.errormsg_key));
        if(message == "" || message == null) {
            builder.setMessage("Want to delete "+ getArguments().getString("name") + " from favorites?");
            builder.setPositiveButton(R.string.delete_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    ((MainActivity)getContext()).removeFromFavList(Integer.valueOf(getArguments().getString("position")));
                }
            });
            builder.setNegativeButton(R.string.delete_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            return builder.create();
        }
        builder.setMessage(message)
                .setNegativeButton(R.string.error_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
