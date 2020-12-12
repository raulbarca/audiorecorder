# audiorecorder
Android kotlin audio recorder

<pre>
Min sdk: 19
</pre>

In your **project**'s `build.gradle` add the following line

<pre>
allprojects {
    repositories {
        google()
        jcenter()
        <b>maven { url "https://jitpack.io" }</b>
    }
}
</pre>


In your **module**'s `build.gradle` add the dependency

<pre>
implementation("com.github.raulbarca:audiorecorder:$version")
</pre>

### Permissions

In your **manifest**, add the permission

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

`audiorecorder` does not request `RECORD_AUDIO` permission by itself, so you need to request it on a previous screen or to override `onPrepareRecorder()` and request it before calling `super.onPrepareRecorder()`.
