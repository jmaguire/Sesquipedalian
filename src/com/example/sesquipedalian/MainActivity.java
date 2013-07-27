package com.example.sesquipedalian;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.text.Html;
import android.widget.TextView;

public class MainActivity extends Activity implements OnInitListener {
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;
	private TextToSpeech tts;
	private TextView originalText;
	private EditText finalText;

	
	private String BASE_URL = "http://words.bighugelabs.com/api/2/95723594121b497f2f7f62013fc84eaa";
	private String STANFORD_URL = "http://nlp.stanford.edu:8080/parser/index.jsp?query=";
	private String newSentence = ""; 
	
	private Map<String,String> partOfSpeech = new HashMap<String,String>();
	
	private final AndroidHttpClient httpClient = AndroidHttpClient.newInstance("joe-android");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tts = new TextToSpeech(this, this);
		originalText = (TextView) findViewById(R.id.editText1);
		finalText  = (EditText) findViewById(R.id.EditText01);
		checkVoiceRecognition();
		
	}
	
	@Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
	
	@Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.UK);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
	
	public void checkVoiceRecognition() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() == 0) {
			Toast.makeText(this, "Voice recognizer not present",
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "Voice recognition enabled!",
					Toast.LENGTH_SHORT).show();
		}
	}
	
	
	public void speak(View view) {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}
	 
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		  if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)
		 
		   if(resultCode == RESULT_OK) {
		 
		    ArrayList<String> textMatchList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		 
		    if (!textMatchList.isEmpty()) {
		    	//USE THIS TO GET THE TEXT THAT WAS SPOKEN
		    	originalText.setText(textMatchList.get(0));
				replaceSentence(textMatchList.get(0));

		    } else {
		    	Toast.makeText(this, "The returned list was empty :(",
						Toast.LENGTH_SHORT).show();
		    }
		   //Result code for various error.
		   }else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
			   Toast.makeText(this, "Audio Error", Toast.LENGTH_SHORT).show();
		   }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
			   Toast.makeText(this, "Client Error", Toast.LENGTH_SHORT).show();
		   }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
			   Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show();
		   }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
			   Toast.makeText(this, "No match", Toast.LENGTH_SHORT).show();
		   }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
			   Toast.makeText(this, "Server Error", Toast.LENGTH_SHORT).show();
		   }
		  super.onActivityResult(requestCode, resultCode, data);
		 }
	 

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void clickButton(View v) {	
		 speak(v);
	}
	

	
	private void replaceSentence(String sentence){
		sentence = sentence.replaceAll("\\.","");
		sentence = sentence.replaceAll("\\,","");
		sentence = sentence.replaceAll("\\?","");
		sentence = sentence.replaceAll("\\!","");
		String[] words  = sentence.split("\\s+");
		for (int i = 0; i < words.length; i++) Log.d("poop", "Here " + words[i]);
		getPartOfSpeech(sentence);
	}
	
	
	// Get's part of speech by quering the stanford nlp server
	// uses a different get request.. get request string
	@SuppressWarnings("deprecation")
	private void getPartOfSpeech(final String sentence){

		new GetRequestString(getPOSRequest(URLEncoder.encode(sentence))) {
			@Override
			protected void onPostExecute(String string) {
				String html = Html.fromHtml(string).toString();
				
				String[] words  = sentence.split("\\s+");
				
				//JSoup was not importing properly... Here's a hack
				//It is a hackathon!
				for (int i = 0; i < words.length; i++){
					String match = words[i]+"/";
					int index1 = html.indexOf(match);
					int index2 = html.indexOf("\n", index1);
					String matchPOS = html.substring(index1 + match.length(), index2);
					partOfSpeech.put(words[i], matchPOS);
				}
				
				replaceWords(words, 0);
			}
		}.execute();
	}
	
	
	
	
	//------------------------------------------------------------
	//----------------FINISHES BELOW-----------------
	//------------------------------------------------------------
	
	
	private void replaceWords(final String[] sentence, final int index){
		if(index == sentence.length){
	    	finalText.setText(newSentence);
			tts.speak(newSentence, TextToSpeech.QUEUE_FLUSH, null);
			newSentence = "";
			return;
		}
		
		final String word = sentence[index];
		
		new GetRequest(getWordMatch(word)) {
			@Override
			protected void onPostExecute(JSONObject json) {
				String max = "";
				String pos = partOfSpeech.get(word);
				if(json == null){
					max = word;
				}else if(pos.indexOf("NN") != -1){
					max = getMaxNoun(json);
				}else if(pos.indexOf("VB") != -1){
					max = getMaxVerb(json);
				}else if(pos.indexOf("JJ") != -1){
					max = getMaxAdjective(json);
				}else{
					max = word;
				}

				if(max.length() == 0) max = word;
				
				newSentence = newSentence + max + " "; 
				//Do a get request on the next word
				replaceWords(sentence, index + 1);
			}
		}.execute();
	}
	
	
    //-----------------------------------------
	// String/JSON Helper Functions
	//-----------------------------------------
	
	//Returns largest word in JSONArray
	private String maxWord(JSONArray array) throws JSONException{
		String val = null;
		int maxLength = -1;
		for(int i = 0; i < array.length(); i++){
			String curr = array.getString(i);
			if(curr.length() > maxLength){
				val = curr;
				maxLength = curr.length();
			}	
		}
		return val;		
	}
	
	
	private String getMaxNoun(JSONObject json){
		String noun = "";
		if(json.has("noun")){
			try {
				JSONArray nouns = json.getJSONObject("noun").getJSONArray("syn");
				noun = maxWord(nouns);
			} catch (JSONException e) {
			}
		}
		return noun;
	}
	
	private String getMaxVerb(JSONObject json){
		String verb = "";
		if(json.has("verb")){
			try {
				JSONArray verbs = json.getJSONObject("verb").getJSONArray("syn");
				verb = maxWord(verbs);
			} catch (JSONException e) {
			}
		}
		return verb;
	}
	
	private String getMaxAdjective(JSONObject json){
		String adjective = "";
		if(json.has("adjective")){
			try {
				JSONArray adjectives = json.getJSONObject("adjective").getJSONArray("syn");
				adjective = maxWord(adjectives);
			} catch (JSONException e) {
			}
		}
		return adjective;
	}
	
	
	
    //-----------------------------------------
	//HTML GET SHIT
	//-----------------------------------------
	private HttpUriRequest getPOSRequest(String sentence){
		String url = STANFORD_URL + sentence;
		return new HttpGet(url);
	}
	
	
	private HttpUriRequest getWordMatch(String word){
		String url = BASE_URL + "/" + word +  "/json";
		return new HttpGet(url);
	}
	
	//JSON VERSION
    private class GetRequest extends AsyncTask<Void, Void, JSONObject> {
		private HttpUriRequest getRequest;
		
		public GetRequest(HttpUriRequest req) {
			this.getRequest = req;
		}
		
		@Override
		protected JSONObject doInBackground(Void... requests) {
			try {
				HttpResponse response = httpClient.execute(getRequest);
				if (response != null) {
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
						String builder = reader.readLine();
						JSONTokener tokener = new JSONTokener(builder.toString());
						JSONObject json = new JSONObject(tokener);
						return json;
					} catch (Exception e) {
						return null;
					}
				} else {
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		}
		
	}
    
    //REGULAR JAVA VERSION
    private class GetRequestString extends AsyncTask<Void, Void, String> {
		private HttpUriRequest getRequest;
		
		public GetRequestString(HttpUriRequest req) {
			this.getRequest = req;
		}
		
		@Override
		protected String doInBackground(Void... requests) {
			try {
				HttpResponse response = httpClient.execute(getRequest);
				if (response != null) {
					try {
						Reader reader = new InputStreamReader(response.getEntity().getContent());
						
						char buf[] = new char[16000];
						StringBuilder builder = new StringBuilder();
						while (reader.read(buf) > 0) {
							builder.append(buf);
						}
						return builder.toString();
					} catch (Exception e) {
						return null;
					}
				} else {
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		}
		
	}
}
