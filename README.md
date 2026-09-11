# FYT Dialer Role redirecting app
Android dialer role app for FYT-series head units (like Dudu Auto with UIS7870/UIS7862 SoC).

Redirects the standard Android invoked dial requests to the in-build `com.syu.bt` Bluetooth app which initiate the Bluetooth HFP call (`cmd 7 strs=[phoneNumber]`)


Below is an example of making a voice call using Google Gemini:
https://github.com/user-attachments/assets/7f7666b2-b544-4e7d-a3c9-df029d7bb3bb

## Architecture
My goal was to separate the library layer from the app layer so that the client wouldn’t see the “hard” low-level binder logic, and I managed to achieve that (“facade architecture”):
```kotlin
 val fytModule = BluetoothModule.get(context)
 fytModule.dialNumber("+48123456789") { success ->
   Log.d(TAG,"Connection successful: $success")
   fytModule.disconnect()
 }
 ```
However, the library part is certainly far from optimal, and I’d like to rewrite it in the future. 

Tips: 
Update-type codes (e.g., `U_PHONE_NAME`) must be implemented as observers even if the value is retrieved only once. Values that can be retrieved using `get` are prefixed with `G_...`, so in practice, in the case of the Bluetooth module, there are no such commands at all (or at least `com.fyt.bt` doesn’t use any);
`CMD_`- type codes are commands that typically perform an action on the device or change states.
Dudu firmware uses Tencent Legu encryption in its APKs so static analyse it's not easy hrere, that's why it's better to analyse firmware from other FYT-based manufacturers that doesn't use it, it's analyse is much simpler 

