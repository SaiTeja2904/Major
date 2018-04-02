package example.sai.voiceassistant;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by Sai Teja on 04-03-2018.
 */

public class CallsManager {
    public Context context;

    CallsManager(Context con) {
        this.context=con;
    }
    public HashMap<String, String> getNumber(String inputData_voice)
    {

        HashMap<String,String> contacts=new HashMap<>();
        inputData_voice=inputData_voice.toLowerCase();
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            Log.e("Count",cur.getCount()+"");
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if(name==null)
                    name="null";
                name=name.toLowerCase();
                Log.e("Contact",name);
                if(inputData_voice.contains(name)) {

                    if (cur.getInt(cur.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            String phoneNo = pCur.getString(pCur.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));
                            //return phoneNo+","+name;
                            contacts.put(name,phoneNo);

                            //Toast.makeText(getBaseContext(), "Name: " + name
                            //      + ", Phone No: " + phoneNo, Toast.LENGTH_SHORT).show();
                        }
                        pCur.close();
                    }
                }
            }
        }
        return contacts;
    }
}
