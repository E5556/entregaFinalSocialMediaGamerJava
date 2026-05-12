package com.optic.socialmediagamer;

import android.app.Application;
import com.optic.socialmediagamer.utils.ThemeHelper;

public class GamerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}
