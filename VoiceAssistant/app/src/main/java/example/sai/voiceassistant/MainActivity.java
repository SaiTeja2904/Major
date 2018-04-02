package example.sai.voiceassistant;

import android.Manifest;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextToSpeech voice;
    protected static final int RESULT_SPEECH = 1;
    ArrayList<String> textData;
    String inputData_voice="";
    Intent intentSpeech;
    String source="";
    HashMap output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        intentSpeech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        //Voice Module Initialization
        voice=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    voice.setLanguage(Locale.US);
                }
            }
        });
    }

    public void startAssistant(View view) {
        Toast.makeText(this, "Hello Sai Teja", Toast.LENGTH_SHORT).show();
        voice.speak("Hello Sai Teja...How may I Assist you..?", TextToSpeech.QUEUE_FLUSH, null);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        source="startAssistant";
        if(textData!=null) textData.removeAll(textData);


        // Toast.makeText(getBaseContext(),"1.You can also open app,\"say open Google Play <name> for google apps\"\n2.Call <contactname>",Toast.LENGTH_LONG).show();
        try {
            startActivityForResult(intentSpeech, RESULT_SPEECH);
        }
        catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(), "Opps! Your device doesn't support Speech to Text", Toast.LENGTH_SHORT);
            t.show();
        }
    }

    //Voice Input module
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {
                    textData = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputData_voice=textData.get(0);
                    inputData_voice=inputData_voice.toLowerCase();
                    Toast.makeText(this, inputData_voice, Toast.LENGTH_SHORT).show();
                    if(source.equals("startAssistant"))
                        assistantModule();
                    else if(source.equals("caller"))
                        Toast.makeText(getApplicationContext(),"Number"+output.get(inputData_voice),Toast.LENGTH_LONG).show();

                    break;
                }
            }
        }
    }



    public void assistantModule()
    {
        voice.speak(inputData_voice,TextToSpeech.QUEUE_FLUSH, null);
        inputData_voice=inputData_voice.toLowerCase();
        if(inputData_voice.contains("call"))
        {
            callNumber(inputData_voice);
        }
    }


    public void doGoogle(String search_String)
    {
        voice.speak("Searching in Google",TextToSpeech.QUEUE_FLUSH,null);
        try{
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, search_String);
            startActivity(intent);
        }
        catch (Exception e) {
            Toast.makeText(getBaseContext(),"Some Error ",Toast.LENGTH_LONG).show();
        }

    }

    public void callNumber(String inputData_voice){

        CallsManager callsManager=new CallsManager(getApplicationContext());

        output=callsManager.getNumber(inputData_voice);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        if(output.size()!=0)
        {
            //String container[]=output.split(",");
            if(output.size()==1)
            {
                Iterator<String> keySetIterator = output.keySet().iterator();
                String key = keySetIterator.next();
                voice.speak("Dailing "+key,TextToSpeech.QUEUE_FLUSH,null);
                callIntent.setData(Uri.parse("tel:" + output.get(key)));
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException ie){

                }
                startActivity(callIntent);
            }

            else
            {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                voice.speak("I found multiple numbers...please tell me whom to call?",TextToSpeech.QUEUE_FLUSH,null);
                Iterator<String> keySetIterator = output.keySet().iterator();
                while(keySetIterator.hasNext()) {
                    String key = keySetIterator.next();
                    voice.speak(key, TextToSpeech.QUEUE_FLUSH, null);
                }
                source="caller";

                try {
                    startActivityForResult(intentSpeech, RESULT_SPEECH);
                }
                catch (ActivityNotFoundException a) {
                    Toast t = Toast.makeText(getApplicationContext(), "Opps! Your device doesn't support Speech to Text", Toast.LENGTH_SHORT);
                    t.show();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }



            }
        }
        else
            doGoogle(inputData_voice);
    }


}
