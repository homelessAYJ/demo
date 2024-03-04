package org.example.demo;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @Description
 * @Author yujie
 * @Date 2024/2/20 15:39
 */
public class MySettingsConfigurable implements Configurable {


    private MySettingsComponent mySettingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "demo";
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getAppIdTextJB();
    }

    @Override
    public @Nullable JComponent createComponent() {
        mySettingsComponent = new MySettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        MySettingsState state = MySettingsState.getInstance();
        boolean modified = !mySettingsComponent.getAppIdText().equals(state.getAppId());
        modified |= !mySettingsComponent.getAppKeyText().equals(state.getAppKey());
        modified |= !mySettingsComponent.getAnnotateText().equals(state.getAnnotate());
        return modified;
    }

    @Override
    public void apply() {
        MySettingsState settings = MySettingsState.getInstance();
        settings.appId = mySettingsComponent.getAppIdText();
        settings.appKey = mySettingsComponent.getAppKeyText();
        settings.annotate = mySettingsComponent.getAnnotateText();
    }

    @Override
    public void reset() {
        MySettingsState settings = MySettingsState.getInstance();
        mySettingsComponent.setAppIdText(settings.appId);
        mySettingsComponent.setAppKeyText(settings.appKey);
        mySettingsComponent.setAnnotateText(settings.annotate);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
