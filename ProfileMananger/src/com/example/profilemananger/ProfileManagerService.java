package com.example.profilemananger;

import java.io.FileInputStream; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;


public class ProfileManagerService extends Service 
{
	public final String TAG = "ProfileManagerService.ProfileManager";
	boolean started = false;
	@Override
	public void onCreate() 
	{
		Log.i("ProfileManager","In Create");
		if(on_silent == 0)
		{
			Log.i("ProfileManager","Registering reciever");
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.provider.Telephony.SMS_RECEIVED");
			filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
			registerReceiver(receiver, filter);
		}			
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		if(started == false)
		{
			Log.i("ProfileManager","STARTED STARTED");
			Thread t1 = new Thread(new Runnable() { public void run(){ loop() ; }});
			t1.start();
			started = true;
		}
		return START_STICKY;	
	}
	
	private int on_silent = 0;
	Gson gson_object = new Gson();
	public static class Profile
	{
		int OnTime = 0;
		int OffTime = 0;
		List <String> contacts = new ArrayList();
		public String ToString()
		{
			StringBuilder builder = new StringBuilder();
			builder.append("From " + OnTime + " hrs to " + OffTime);
			if(contacts.isEmpty())
			{
				builder.append(" hrs\n No exceptions.");
			}
			else
			{
				builder.append(" hrs\n Only ");
				for(int i = 0 ; i < contacts.size(); i++)
				{
					if(i > 0)
						builder.append(", ");
					builder.append(contacts.get(i));
					
				}
				builder.append(" can ring");
			}
			return builder.toString();
		}
	}
	List <Profile> silenceList = new ArrayList();
	Set <String> currentImportantCallers = new HashSet<String>();
	public ProfileManagerService(String name) 
	{

	}
	public ProfileManagerService() 
	{
		
	}
	private synchronized void updateProfile()
	{
		// De-serialize profile here
		// Todo: Don't do this in loop. Better set a dirty flag somewhere to read the file
		Log.i(TAG, "service Reading from file now..");
		StringBuilder builder = new StringBuilder();
		try {
			FileInputStream inputStream = openFileInput("Profiles.xml");
			int ch;
			while ((ch = inputStream.read()) != -1) 
			{
				builder.append((char) ch);
			}
			inputStream.close();
		} catch (Exception e) 
		{
			e.printStackTrace();
			return ;
		}

		if (builder.length() == 0) 
		{
			Log.i(TAG , "No Text");
			return ;
		}

		
		ProfileManagerService.Profile[] profileList = gson_object.fromJson(builder.toString(), ProfileManagerService.Profile[].class);
		Log.i("ProfileManager", "Casting to list "+ builder.toString());
		silenceList = Arrays.asList(profileList);
		return ;

		//profile.contacts.add("Home_landline");
		//silenceList.add(profile);
	}
	private String getContactName(Context context, String number) 
	{

		String name = null;

		// define the columns I want the query to return
		String[] projection = new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

		// encode the phone number and build the filter URI
		Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		// query time
		Cursor cursor = context.getContentResolver().query(contactUri, projection, null, null, null);

		if(cursor != null) 
		{
			if (cursor.moveToFirst()) {
				name =      cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
				Log.v("ProfileManager", "Started uploadcontactphoto: Contact Found @ " + number);            
				Log.v("ProfileManager", "Started uploadcontactphoto: Contact name  = " + name);
			} else {
				Log.v("ProfileManager", "Contact Not Found @ " + number);
			}
			cursor.close();
		}
		return name;
	}
	private synchronized void updateCurrentImportatCallers()
	{
		Calendar c = Calendar.getInstance(); 
		int hour = c.get(Calendar.HOUR_OF_DAY);
		currentImportantCallers.clear();
		for(int i = 0; i < silenceList.size(); i++)
		{
			if((silenceList.get(i).OnTime < hour)||(hour < silenceList.get(i).OffTime))
			{
				
				for(int j = 0; j < silenceList.get(i).contacts.size(); j++)
				{
					currentImportantCallers.add(silenceList.get(i).contacts.get(j));
					Log.i("ProfileManager", "Adding as important caller "+ silenceList.get(i).contacts.get(j));
				}
			}
		}
	}

	private synchronized boolean isSilentTime()
	{
		Calendar c = Calendar.getInstance(); 
		int hour = c.get(Calendar.HOUR_OF_DAY);
		for(int i = 0; i < silenceList.size(); i++)
		{
			Log.i("ProfileManager", "Checking " + hour + " with "+ silenceList.get(i).OnTime +" and "+silenceList.get(i).OffTime);
			if((silenceList.get(i).OffTime < silenceList.get(i).OnTime)&&
					((silenceList.get(i).OnTime <= hour)||(hour <= silenceList.get(i).OffTime)))
			{
				return true;
			}
			if((silenceList.get(i).OnTime <= hour)&&(hour <= silenceList.get(i).OffTime))
			{
				return true;
			}
		}
		return false;
	}
	private synchronized boolean shouldRingThisCall(Context context, Intent intent)
	{
		String incommingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		String contactName = getContactName(context, incommingNumber);
		Log.i("ProfileManager",contactName + " calling" );
		if(currentImportantCallers.contains(contactName))
			return true;
		return false;
	}
	
	private void setTimeBasedProfile()
	{
		if((isSilentTime())&&(on_silent !=  1))
		{
			// Set phone on silent
			Log.i("ProfileManager","Setting Phone on silent");
			AudioManager audiomanager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			audiomanager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
			on_silent = 1;
		}
		if((!isSilentTime())&&(on_silent !=  2))
		{
			Log.i("ProfileManager","unsetting Phone on silent");
			AudioManager audiomanager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			audiomanager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
			on_silent = 2;
		}
	}
	protected void loop() 
	{
		try
		{
			for(;;)
			{
				Log.i("ProfileManager","PING PING");
				updateProfile();
				updateCurrentImportatCallers();
				setTimeBasedProfile();
				//Sleep for 10 minutes
				Thread.sleep(2*60*1000);
			}
		}
		catch(InterruptedException e)
		{
		}

	}
	

	public final BroadcastReceiver receiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			String extra = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);
//			if(action.equals("android.provider.Telephony.SMS_RECEIVED"))
//			{
//				Log.i("ProfileManager","Recvd SMS");
//			}
			if(action.equals(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED))
			{
				Log.i("ProfileManager","Got call  = "+ extra );
				if(extra.equals("RINGING"))
				{
					if(shouldRingThisCall(context, intent))
					{
						Log.i("ProfileManager","This call shud ring" );
						AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
						audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL); 	
					}
				}
				else
				{
					// Once the call is over, reset the phone on silent
					on_silent = 0;
					setTimeBasedProfile();   		
				}
			}
		}
	};


	@Override
	public void onDestroy() 
	{
		Log.i("ProfileManager","In Destroy");
		unregisterReceiver(receiver);
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}

