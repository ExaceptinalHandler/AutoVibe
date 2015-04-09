package com.example.profilemananger;

import android.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;


import android.app.Activity; 
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class ServiceStarterActivity extends Activity 
{
	public final String TAG = "ServiceStarterActivity.ProfileManager";
	Gson gson_object = new Gson();
	List<ProfileManagerService.Profile> profile_list = new ArrayList();
	List<String> string_profile_list = new ArrayList();
	ArrayAdapter <String>adapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(com.example.profilemananger.R.layout.activity_service_starter);
		
		adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_list_item_1, string_profile_list);
		ListView lv = (ListView) findViewById(com.example.profilemananger.R.id.list_view_profiles);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{

			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) 
			{
				Log.i(TAG, "Editing item at pos " + position  );
				edit_item(position);
				
			}

		});
		registerForContextMenu(lv);
		

	}

	@Override
	protected void onStart() 
	{
		super.onStart();
		// Start Service
		Log.i(TAG, "Starting Service");
		Intent serviceIntent = new Intent(ServiceStarterActivity.this, ProfileManagerService.class);
		startService(serviceIntent);
		
		

	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		Log.i(TAG, "Populating Profile");
		populate_profile_from_file();
		adapter.notifyDataSetChanged();
		if(string_profile_list.isEmpty())
		{
			//Toast toast = Toast.makeText(getApplicationContext(), "Create profiles to automatically set phone on vibration for fixed intervals during the day. Add exceptions to these profiles as contacts for whom the phone will ring.", Toast.LENGTH_SHORT);
		//	toast.show();
			
			AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
			dlgAlert.setMessage("Create profiles to automatically set phone on vibration for fixed intervals during the day.\n\nAdd exceptions to these profiles as contacts for whom the phone will ring.");
			dlgAlert.setTitle("AutoSilent");
			dlgAlert.setPositiveButton("OK",
				    new DialogInterface.OnClickListener() {
				        public void onClick(DialogInterface dialog, int which) {
				        	
				        }
				    });
			dlgAlert.setCancelable(true);
			dlgAlert.create().show();
			
		}
			
	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		Log.i("ProfileManager", "Saving Profile list");
		save_profile_to_file();
	}
	
	
	private void edit_item(int position)
	{
		Intent intent = new Intent(getApplicationContext(), ProfileCreatorActivity.class);
		String string_profile = gson_object.toJson(profile_list.get(position));
		Log.i(TAG, "Sticking " + string_profile  );
		intent.putExtra("PROFILE_TXT", string_profile);
		startActivityForResult(intent, position);
	}
	private boolean populate_profile_from_file() 
	{
		// De-serialize profile here
		Log.i(TAG, "Reading from file now..");
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
			return false;
		}

		if (builder.length() == 0) 
		{
			Log.i(TAG , "No Text");
			return false;
		}

		Log.i("ProfileManager", "Casting to list");
		ProfileManagerService.Profile[] profileList = gson_object.fromJson(builder.toString(), ProfileManagerService.Profile[].class);
		Log.i("ProfileManager", "Casting to list " + builder.toString());
		if(profile_list.isEmpty() == false)
			profile_list.clear();
		for (int i = 0; i < profileList.length; i++)
		{
			profile_list.add(profileList[i]);
		}
		//profile_list = Arrays.asList(profileList);
		if(string_profile_list.isEmpty() == false)
			string_profile_list.clear();
		for (int i =0; i < profile_list.size(); i++)
		{
			string_profile_list.add(profile_list.get(i).ToString());
		}
		return true;
	}

	private boolean save_profile_to_file() 
	{
		// serialize profile here
		if (profile_list.isEmpty())
		{
			try
			{
				String file_name = getApplicationContext().getFilesDir().getPath().toString() + "/Profiles.xml";
				File file = new File(file_name);
				file.delete();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			return true;
		}

		Log.i(TAG, "Writing to file now..");
		try 
		{
			String file_name = getApplicationContext().getFilesDir().getPath().toString() + "/Profiles.xml";
			Log.i(TAG, "Name : " + file_name);
			File file = new File(file_name);
			file.createNewFile();
			FileOutputStream outputStream = openFileOutput("Profiles.xml", Context.MODE_PRIVATE);
			String jason_string = gson_object.toJson(profile_list);
			byte[] contentInBytes = jason_string.getBytes();

			outputStream.write(contentInBytes);
			outputStream.flush();
			outputStream.close();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return false;
		}
		return true;

	}

	

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) 
	{
		Log.i(TAG, "in OnActivityResult");
		super.onActivityResult(reqCode, resultCode, data);

		if  (resultCode == Activity.RESULT_OK) 
		{

			String str_profile = data.getStringExtra("PROFILE_TXT");
			Log.i(TAG, "retrieved " + str_profile + " code= " + reqCode);
			ProfileManagerService.Profile profile = gson_object.fromJson(str_profile, ProfileManagerService.Profile.class);
			if(reqCode == 799)
			{
				Log.i(TAG, "adding new ");
				profile_list.add(profile);
				string_profile_list.add(profile.ToString());
			}
			else if(reqCode >= 0)
			{
				Log.i(TAG, "editing ");
				profile_list.set(reqCode, profile);	
				string_profile_list.set(reqCode,profile.ToString());
			}
			save_profile_to_file();
				
		}
	}

	public boolean onAddProfile(View view) 
	{
		Intent intent = new Intent(this, ProfileCreatorActivity.class);
		intent.putExtra("PROFILE_TXT", "");
		Log.i(TAG, "launching activity ");
		startActivityForResult(intent, 799);
		return true;
	}
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		if (v.getId() == com.example.profilemananger.R.id.list_view_profiles) 
		{
			Log.i(TAG, "launching context menu ");
			menu.add("Delete");
			menu.add("Edit");
		}
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) 
	{

		String s = item.getTitle().toString();
		if(s.equalsIgnoreCase("Delete"))
		{
			// Delete
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			profile_list.remove(info.position);
			string_profile_list.remove(info.position);

			adapter.notifyDataSetChanged();
			save_profile_to_file();
			return true;
		}

		else if(s.equalsIgnoreCase("Edit"))
		{
			//edit
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			edit_item(info.position);
			return true;
		}
		else
			return super.onContextItemSelected(item);



	}
}
