# FYT Dialer Role redirecting app
Android dialer role app for FYT-series head units (like Dudu Auto with UIS7870/UIS7862 SoC).

Redirects the standard Android invoked dial requests to the in-build `com.syu.bt` Bluetooth app which initiate the Bluetooth HFP call (`cmd 7 strs=[phoneNumber]`)


Below is an example of making a voice call using Google Gemini:
<video src="https://github.com/user-attachments/assets/7f7666b2-b544-4e7d-a3c9-df029d7bb3bb"></video>
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

### Command types: 
**Update codes** (prefix `U` like `U_PHONE_NAME`) - reading value from this type of code must be implemented as observer even if you want to retrieve value only once.

**Get codes** (prefix `G_...`) - no know examples for Bluetooth module,

**Command codes** (prefix `C` like `C_DIAL`) - commands changing module state, doing some action.

### Protip about encryption
Dudu firmware uses Tencent Legu encryption in its APKs so static analyse it's not easy in that case, it's better to decompile (to analyse) firmwares from other FYT-based manufacturers
