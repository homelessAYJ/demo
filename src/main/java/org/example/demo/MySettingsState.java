package org.example.demo;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @Description
 * @Author yujie
 * @Date 2024/2/20 15:33
 */
@State(name = "org.example.demo.settings.MySettingsState", storages = @Storage("plugin.xml"))
public class MySettingsState implements PersistentStateComponent<MySettingsState> {

    public String appId = "";
    public String appKey = "";

    public String annotate = "";

    public static MySettingsState getInstance() {
        return ApplicationManager.getApplication().getService(MySettingsState.class);
    }

    @Override
    @Nullable
    public  MySettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MySettingsState state) {
        this.appId = state.appId;
        this.appKey = state.appKey;
        this.annotate = state.annotate;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAnnotate() {
        return annotate;
    }
}
