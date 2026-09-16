# fyt-dialer 
Android dialer role app for FYT-series head units (like DUDU Auto with UIS7870/UIS7862S SoC).

Redirects the standard Android-invoked dial requests to the Bluetooth module (in fact, there are two Bluetooth modules in these devices; we're referring here to the non-Android one that handles HFP headsets) and the system-installed `com.syu.bt` app, which initiates the  call using your connected smartphone using the `cmd 7 strs=[phoneNumber]` command


Below is an example of making a voice call using Google Gemini:
<video src="https://github.com/user-attachments/assets/7f7666b2-b544-4e7d-a3c9-df029d7bb3bb"></video>
https://github.com/user-attachments/assets/7f7666b2-b544-4e7d-a3c9-df029d7bb3bb

## Code architecture
My goal was to separate the library layer from the app layer so that the client wouldn’t see the “hard” low-level binder logic, and I managed to achieve that (“facade architecture”):
```kotlin
 val fytModule = BluetoothModule.get(context)
 fytModule.dialNumber("+48123456789") { success ->
   Log.d(TAG,"Connection successful: $success")
   fytModule.disconnect()
 }
 ```
However, the library part is certainly far from optimal, and I’d like to rewrite it in the future. 
Beware: Kotlin isn't my first-choice language (or even my second), and I've never used low-level Binders either, so the code may be far from perfect. 

## Communication using Binders in FYT-based units

### Code types: 
**Update codes** (prefix `U` like `U_PHONE_NAME`) - reading value from this type of code must be implemented as an observer even if you want to retrieve the value only once.

**Get codes** (prefix `G_...`) - no known examples for the Bluetooth module,

**Command codes** (prefix `C` like `C_DIAL`) - commands changing module state, doing some action.

### Code reversing 
Dudu firmware uses Tencent Legu encryption in its APKs, so static analyse it's not easy in that case; it's better to decompile (to analyse) firmware from other FYT-based manufacturers
