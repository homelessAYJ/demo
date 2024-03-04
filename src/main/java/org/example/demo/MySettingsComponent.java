package org.example.demo;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @Description
 * @Author yujie
 * @Date 2024/2/20 15:48
 */
public class MySettingsComponent {
    private final JPanel myPanel;
    private final JBTextField appIdText = new JBTextField();
    private final JBTextField appKeyText = new JBTextField();

    private final JBTextField annotateText = new JBTextField();

    public MySettingsComponent() {
        myPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("百度开发者appId"), appIdText, 1, false)
                .addLabeledComponent(new JBLabel("百度开发者appKey"), appKeyText, 1, false)
                .addLabeledComponent(new JBLabel("需要插入的注解包"), annotateText, 1, false)
                .getPanel();
    }

    public JPanel getPanel() {
        return myPanel;
    }

    public JBTextField getAppIdTextJB() {
        return appIdText;
    }

    public String getAppIdText() {
        return appIdText.getText();
    }

    public String getAppKeyText() {
        return appKeyText.getText();
    }

    public String getAnnotateText() {
        return annotateText.getText();
    }

    public void setAppIdText(@NotNull String appIdTextNew) {
        appIdText.setText(appIdTextNew);
    }

    public void setAppKeyText(@NotNull String appKeyTextNew) {
        appKeyText.setText(appKeyTextNew);
    }

    public void setAnnotateText(@NotNull String annotateTextNew) {
        annotateText.setText(annotateTextNew);
    }
}
