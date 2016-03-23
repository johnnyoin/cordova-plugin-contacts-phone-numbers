package com.dbaq.cordova.contactsPhoneNumbers;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;

import android.net.Uri;
import android.content.ContentUris;


public class ContactsManager extends CordovaPlugin {

    private CallbackContext callbackContext;

    private JSONArray executeArgs;

    public static final String ACTION_LIST_CONTACTS = "list";

    private static final String LOG_TAG = "Contact Phone Numbers";

    public ContactsManager() {}

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;
        this.executeArgs = args;

        if (ACTION_LIST_CONTACTS.equals(action)) {
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    callbackContext.success(list());
                }
            });
            return true;
        }

        return false;
    }

    private JSONArray list() {
        JSONArray contacts = new JSONArray();
        try{

            ContentResolver cr = this.cordova.getActivity().getContentResolver();
            String[] projection = new String[] {
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_ID,
                    ContactsContract.CommonDataKinds.Email.DATA,
            };
            String order = "CASE WHEN "
                    + ContactsContract.Contacts.DISPLAY_NAME
                    + " NOT LIKE '%@%' THEN 1 ELSE 2 END, "
                    + ContactsContract.Contacts.DISPLAY_NAME
                    + ", "
                    + ContactsContract.CommonDataKinds.Email.DATA
                    + " COLLATE NOCASE";
            String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
            // Retrieve only the contacts with an email at least
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    projection,
                    filter,
                    null,
                    order);

            contacts = populateContactArray(cursor);
        } catch (Exception e) {
            return contacts;
        }
        return contacts;
    }


    /**
     * Creates an array of contacts from the cursor you pass in
     *
     * @param c            the cursor
     * @return             a JSONArray of contacts
     */
    private JSONArray populateContactArray(Cursor c) {

        JSONArray contacts = new JSONArray();

        String contactId = null;
        String oldContactId = null;
        boolean newContact = true;
        String mimetype = null;

        JSONObject contact = new JSONObject();

        try {
            if (c.moveToFirst()) {
                do {
                    contact = new JSONObject();
                    contactId = c.getString(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                    contact.put("id", contactId);
                    contact.put("displayName", c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
                    contact.put("email", c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)));

                    String photoId = c.getString(c.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                    if (photoId != null) {
                        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, (Long.valueOf(contactId)));
                        Uri photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                        contact.put("photo", photoUri.toString());
                    }

                    contacts.put(contact);

                } while (c.moveToNext());
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        c.close();
        return contacts;
    }

    /**
     * Create a phone number JSONObject
     * @param cursor the current database row
     * @return a JSONObject representing a phone number
     */
    private JSONObject getPhoneNumber(Cursor cursor) throws JSONException {
        JSONObject phoneNumber = new JSONObject();
        String number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
        String normalizedNumber = cursor.getString(cursor.getColumnIndex(Phone.NORMALIZED_NUMBER));
        phoneNumber.put("number", number);
        phoneNumber.put("normalizedNumber", (normalizedNumber == null) ? number : normalizedNumber);
        phoneNumber.put("type", getPhoneTypeLabel(cursor.getInt(cursor.getColumnIndex(Phone.TYPE))));
        return phoneNumber;
    }


    /**
     * Retrieve the type of the phone number based on the type code
     * @param type the code of the type
     * @return a string in caps representing the type of phone number
     */
    private String getPhoneTypeLabel(int type) {
        String label = "OTHER";
        if (type == Phone.TYPE_HOME)
            label = "HOME";
        else if (type == Phone.TYPE_MOBILE)
            label = "MOBILE";
        else if (type == Phone.TYPE_WORK)
            label = "WORK";

        return label;
    }
}
