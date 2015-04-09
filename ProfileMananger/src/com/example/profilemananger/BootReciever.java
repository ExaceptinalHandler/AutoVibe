package com.example.profilemananger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReciever extends BroadcastReceiver 
{

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		String action = intent.getAction();
		
		if(action.equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Intent serviceIntent = new Intent(context, ProfileManagerService.class);
			context.startService(serviceIntent);
		}
		
	}
	

}
