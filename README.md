# AutoVibe
Sample android app to set phone automatically on silent.
Users specify time intervals during the day for which the phone must automatically go on silent/vibe. 
Users also specify important contacts as 'exceptions' for which phone will ring even when on silent/vibe.

The code consists of:
- A minimal UI to configure the time intervals and important contacts to exclude. 
    - Two activities: ServiceStarterActivity and ProfileCreatorActivity to let the user configure the silent intervals during the day.
- Background service that automatically sets the phone on silent/vibe based  on time and configured intervals.
    - The service intercepts calls using a broadcast reciever and temporarily changes mode if an 'important caller' calls.
- BroadcastReciever to start service automaticlly when phone boots up. 


