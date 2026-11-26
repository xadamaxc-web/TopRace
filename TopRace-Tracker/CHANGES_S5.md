# Session 5
Adds app/debug.keystore + signingConfigs.debug (all future CI builds share one signature).
Includes S4 changes (EXPORT CSV). build.yml unchanged. versionCode 4 / 1.0.1.
Note: the first install of this build may still fail over the old app, because the old APK used a random runner key.
