package com.example.profilemananger;

import java.util.Calendar;
import android.R;
import com.google.gson.Gson;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

public class ProfileCreatorActivity extends Activity 
{
	public final String TAG = "ProfileCreatorActivity.ProfileManager";
	Gson gson_object = new Gson();
	ProfileManagerService.Profile profile;
	Button add_Button;
	EditText from_time, to_time;
	TextView tv_from, tv_to;
	ArrayAdapter<String> adapter;
	ListView lv;
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(com.example.profilemananger.R.layout.activity_profile_creator);
		//profile.contacts.clear();
		//profile.OnTime = profile.OffTime = 0;
		add_Button = (Button)findViewById(com.example.profilemananger.R.id.button_add_contact);
		from_time  = (EditText)findViewById(com.example.profilemananger.R.id.edit_text_from_time);
		to_time    = (EditText)findViewById(com.example.profilemananger.R.id.edit_text_to_time);
		lv         = (ListView)findViewById(com.example.profilemananger.R.id.list_of_contacts);
		tv_from    = (TextView)findViewById(com.example.profilemananger.R.id.TextViewFrom);
		tv_to      = (TextView)findViewById(com.example.profilemananger.R.id.TextViewTo);
		// Get intent here and open existing profile if needed
		
		Intent intent = getIntent();
		if(intent.hasExtra("PROFILE_TXT"))
		{
			String str_profile = new String(intent.getStringExtra("PROFILE_TXT"));
			Log.i(TAG, "Retrieved" + str_profile  );
			if(!str_profile.isEmpty())
			{
				profile = gson_object.fromJson(str_profile, ProfileManagerService.Profile.class);
				//findViewById(com.example.profilemananger.R.id.TextViewAddProfile)
			}
			else
				profile = new ProfileManagerService.Profile();
		}
		else
			profile = new ProfileManagerService.Profile();
		
		
		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		Log.i(TAG, "In resume " + profile.OnTime + " AND " + profile.OffTime );
		if(profile.OnTime != 0)
			from_time.setText(profile.OnTime + " : 00" );
		if(profile.OffTime != 0)
			to_time.setText(profile.OffTime + " : 00" );
		adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_list_item_1, profile.contacts);
		lv.setAdapter(adapter);	
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) 
	{
		super.onActivityResult(reqCode, resultCode, data);
		Log.i(TAG, "In onActivityResult" );
		if((reqCode == 1) &&  (resultCode == Activity.RESULT_OK) )
		{

			Uri contactData = data.getData();
			Cursor c =  getApplicationContext().getContentResolver().query(contactData, null, null, null, null);
			if (c.moveToFirst()) 
			{
				String name =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				Log.i(TAG, "Adding" + name );
				profile.contacts.add(name);
			}
		}
		//lv.refreshDrawableState();
		
	}
	boolean is_from = false;
	public void onClick(View v) 
	{
		Log.i(TAG, "In onClick" );
		if ((v == from_time)||(v == to_time)||(v == tv_from)|| (v == tv_to))
		{
			if(( v== from_time )||(v == tv_from)) is_from = true;
			else is_from = false;
			// Process to get Current Time
			final Calendar c = Calendar.getInstance();
			int mHour = c.get(Calendar.HOUR_OF_DAY);
			int mMinute = c.get(Calendar.MINUTE);

			// Launch Time Picker Dialog
			TimePickerDialog tpd = 
					new TimePickerDialog(this,new TimePickerDialog.OnTimeSetListener() 
					{

						@Override
						public void onTimeSet(TimePicker view, int hourOfDay,int minute) 
						{
							Log.i(TAG, "In onTimeSet" );
							if(is_from == true)
							{
								from_time.setText(hourOfDay + "." + minute);
								to_time.requestFocus();
							}
							else
								to_time.setText(hourOfDay + "." + minute);
						}
					}, mHour, mMinute, false);
			tpd.show();
		}
		if( v == add_Button)
		{
			Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(intent, 1);
		}
	}
	public void onDone(View view)
	{
		Log.i(TAG, "In onDone" );
		String s1 =  from_time.getText().toString();
		int pos = s1.indexOf(".",0);
		s1 = s1.substring(0,pos);
		
		String s2 = to_time.getText().toString();
		pos = s2.indexOf(".",0);
		s2 = s2.substring(0,pos);
		 
		Log.i(TAG, "s1 =" + s1 +"s2 = " +s2 );
		profile.OnTime = Integer.parseInt(s1);
		profile.OffTime = Integer.parseInt(s2);
		Intent data = new Intent();
		String string_profile = gson_object.toJson(profile);
		Log.i(TAG, "sticking "+ string_profile);
		data.putExtra("PROFILE_TXT", string_profile);
		setResult (RESULT_OK, data);
		finish();
	}
	
}

