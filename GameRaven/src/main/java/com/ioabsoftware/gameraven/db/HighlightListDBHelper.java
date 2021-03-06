package com.ioabsoftware.gameraven.db;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ioabsoftware.gameraven.AllInOneV2;
import com.ioabsoftware.gameraven.R;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Locale;

public class HighlightListDBHelper extends SQLiteOpenHelper {

    private HashMap<String, HighlightedUser> highlightedUsers;

    /**
     * DO NOT STORE REFERENCES TO THIS LIST! Always call this method when needing to use the list,
     * this list will always be up to date.
     *
     * @return An up-to-date list of highlighted users.
     */
    public HashMap<String, HighlightedUser> getHighlightedUsers() {
        return highlightedUsers;
    }

    private static final String DATABASE_NAME = "highlightlists.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERS_ID = "_id";
    public static final String COLUMN_USERS_NAME = "name";
    public static final String COLUMN_USERS_LABEL = "label";
    public static final String COLUMN_USERS_COLOR = "color";

    private static final String CREATE_TABLE_USERS =
            "create table " + TABLE_USERS + "(" +
                    COLUMN_USERS_ID + " integer primary key autoincrement, " +
                    COLUMN_USERS_NAME + " text not null, " +
                    COLUMN_USERS_LABEL + " text not null, " +
                    COLUMN_USERS_COLOR + " integer not null);";

    public HighlightListDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        SQLiteDatabase db = getWritableDatabase();
        updateUsers(db);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        // nothing. ABSOLUTELY NOTHING!

    }

    public boolean hasUser(String user) {
        return highlightedUsers.containsKey(user.toLowerCase(Locale.US));
    }

    public HashMap<String, HighlightedUser> addUser(String name, String label, int color) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues vals = new ContentValues();
        vals.put(COLUMN_USERS_NAME, name);
        vals.put(COLUMN_USERS_LABEL, label);
        vals.put(COLUMN_USERS_COLOR, color);

        db.insert(TABLE_USERS, null, vals);

        updateUsers(db);

        db.close();

        return highlightedUsers;
    }

    public HashMap<String, HighlightedUser> updateUser(HighlightedUser user) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues vals = new ContentValues();
        vals.put(COLUMN_USERS_NAME, user.getName());
        vals.put(COLUMN_USERS_LABEL, user.getLabel());
        vals.put(COLUMN_USERS_COLOR, user.getColor());

        db.update(TABLE_USERS, vals, COLUMN_USERS_ID + " = " + user.getID(), null);

        updateUsers(db);

        db.close();

        return highlightedUsers;
    }

    public HashMap<String, HighlightedUser> deleteUser(String user) {
        if (hasUser(user))
            return deleteUser(highlightedUsers.get(user.toLowerCase(Locale.US)));
        else
            return highlightedUsers;
    }

    public HashMap<String, HighlightedUser> deleteUser(HighlightedUser user) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_USERS, COLUMN_USERS_ID + " = " + user.getID(), null);

        updateUsers(db);

        db.close();

        return highlightedUsers;
    }

    private void updateUsers(SQLiteDatabase db) {
        highlightedUsers = new HashMap<String, HighlightedUser>();

        Cursor cur = db.query(TABLE_USERS, null, null, null, null, null, null);

        if (cur.moveToFirst()) {
            do {
                highlightedUsers.put(cur.getString(1).toLowerCase(Locale.US),
                        new HighlightedUser(cur.getInt(0), cur.getString(1), cur.getString(2), cur.getInt(3)));
            } while (cur.moveToNext());

            cur.close();
        }
    }

    /**
     * Shows a dialog used to add or update user highlighting.
     *
     * @param activity        Activity used as a Context and for getLayoutInflator
     * @param user     HighlightedUser object. If null or ID equals -1, this is a new user being highlighted.
     * @param username Optional username to set. Does nothing if this isn't a new user being highlighted.
     * @param listener Optional listener to be fired just before the dialog gets dismissed on successful save.
     */
    @SuppressWarnings("ConstantConditions")
    public static void showHighlightUserDialog(final AppCompatActivity activity, final HighlightedUser user,
                                               String username, final HlUDDismissListener listener) {
        final boolean isAddNew = (user == null || user.getID() == -1);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.highlightuserdialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Add Highlighted User");

        final EditText dName = dialogView.findViewById(R.id.huName);
        final EditText dLabel = dialogView.findViewById(R.id.huLabel);

        final Button dSetColor = dialogView.findViewById(R.id.huSetColor);
        final TextView dColorVal = dialogView.findViewById(R.id.huColorVal);

        dSetColor.setOnClickListener(v -> {
            ColorPickerDialog.Builder b = ColorPickerDialog.newBuilder();
            if (dColorVal.length() > 0 && !dColorVal.getText().equals("0")) {
                b.setColor(NumberUtils.toInt(dColorVal.getText().toString()));
            }
            ColorPickerDialog cpd = b.create();
            cpd.setColorPickerDialogListener(new ColorPickerDialogListener() {
                @Override
                public void onColorSelected(int dialogId, int color) {
                    dSetColor.setBackgroundColor(color);
                    dSetColor.setTextColor(~color | 0xFF000000); //without alpha
                    dColorVal.setText(String.valueOf(color));
                }

                @Override
                public void onDialogDismissed(int dialogId) {

                }
            });
            cpd.show(activity.getSupportFragmentManager(), "color-picker-dialog");
        });

        if (!isAddNew) {
            dialogBuilder.setNeutralButton("Delete", (dialog, which) -> {
                AllInOneV2.getHLDB().deleteUser(user);
                if (listener != null)
                    listener.beforeDismissSuccessfulSave();
            });

            dName.setText(user.getName());
            dLabel.setText(user.getLabel());
            dSetColor.setBackgroundColor(user.getColor());
            dSetColor.setTextColor(~user.getColor() | 0xFF000000); //without alpha
            dColorVal.setText(String.valueOf(user.getColor()));
        } else if (username != null)
            dName.setText(username);

        dialogBuilder.setPositiveButton("Save", null);
        dialogBuilder.setNegativeButton("Cancel", null);

        final AlertDialog diag = dialogBuilder.create();
        diag.setOnShowListener(dialog -> diag.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            boolean shouldDismiss = true;

            if (dName.getText().toString().length() > 0
                    && dLabel.getText().toString().length() > 0
                    && !dColorVal.getText().equals("0")) {

                if (isAddNew) {
                    AllInOneV2.getHLDB().addUser(
                            dName.getText().toString(),
                            dLabel.getText().toString(),
                            NumberUtils.toInt(dColorVal
                                    .getText().toString()));
                } else {
                    user.setName(dName.getText().toString());
                    user.setLabel(dLabel.getText().toString());
                    user.setColor(NumberUtils.toInt(dColorVal
                            .getText().toString()));
                    AllInOneV2.getHLDB().updateUser(user);
                }


            } else {
                Toast.makeText(activity,
                        "Missing required info",
                        Toast.LENGTH_SHORT).show();
                shouldDismiss = false;
            }

            if (shouldDismiss) {
                if (listener != null)
                    listener.beforeDismissSuccessfulSave();

                diag.dismiss();
            }
        }));
        diag.show();
    }

}
