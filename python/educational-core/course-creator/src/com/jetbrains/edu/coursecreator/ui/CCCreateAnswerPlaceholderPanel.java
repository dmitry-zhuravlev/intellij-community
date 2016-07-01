package com.jetbrains.edu.coursecreator.ui;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class CCCreateAnswerPlaceholderPanel extends JPanel {

  private JPanel myPanel;
  private JTextArea myHintText;
  private JTextField myAnswerPlaceholderText;

  public CCCreateAnswerPlaceholderPanel() {
    super(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);
    myHintText.setLineWrap(true);
    myHintText.setWrapStyleWord(true);
    myHintText.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    myHintText.setFont(myAnswerPlaceholderText.getFont());
    myAnswerPlaceholderText.grabFocus();
  }

  public void setAnswerPlaceholderText(String answerPlaceholderText) {
    myAnswerPlaceholderText.setText(answerPlaceholderText);
  }

  public void setHintText(String hintText) {
    myHintText.setText(hintText);
  }

  public String getAnswerPlaceholderText() {
    return myAnswerPlaceholderText.getText();
  }

  public String getHintText() {
    return myHintText.getText();
  }

  public JComponent getPreferredFocusedComponent() {
    return myAnswerPlaceholderText;
  }
}
