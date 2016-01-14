package com.vladimir.calculator;

import android.content.pm.PackageManager;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;


public class MainActivity extends AppCompatActivity {

    Long firstNumber = null;
    String operation = null;
    String currentOperation = null;
    Long secondNumber = null;
    Boolean numberSet = false;
    TextView numberField;
    TextView historyField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        numberField = (TextView) findViewById(R.id.textNumberField);
        historyField = (TextView) findViewById(R.id.textHistoryField);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("firstNumber"))
                firstNumber = savedInstanceState.getLong("firstNumber");
            else
                firstNumber = null;

            if (savedInstanceState.containsKey("operation"))
                operation = savedInstanceState.getString("operation");
            else
                operation = null;

            numberSet = savedInstanceState.getBoolean("numberSet");
            numberField.setText(savedInstanceState.getString("numberField"));
            historyField.setText(savedInstanceState.getString("historyField"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (firstNumber != null)
            savedInstanceState.putLong("firstNumber", firstNumber);

        if (operation != null)
            savedInstanceState.putString("operation", operation);

        savedInstanceState.putBoolean("numberSet", numberSet);
        savedInstanceState.putString("numberField", numberField.getText().toString());
        savedInstanceState.putString("historyField", historyField.getText().toString());

        super.onSaveInstanceState(savedInstanceState);
    }

    public void buttonNumberClicked (View view) {
        Button button = (Button) view;
        String pressedButton = button.getText().toString();
        String numberOnScreen = numberField.getText().toString();

        if (historyField.getText().toString().contains("Error"))
            clear(true, false);

        if (!numberSet) {
            numberOnScreen = "";
            numberSet = true;
        }

        if (numberOnScreen.equals("0") && pressedButton.equals("0")) {
            return;
        } else if (numberOnScreen.equals("0")) {
            numberOnScreen = pressedButton;
        } else {
            numberOnScreen += pressedButton;
        }
        numberField.setText(numberOnScreen);
    }

    public void buttonOperationClicked (View view) {
        Button button = (Button) view;
        currentOperation = button.getText().toString();
        Long num = Long.parseLong(numberField.getText().toString());

        if (!numberSet && !currentOperation.equals("C") && !currentOperation.equals("=")) {
            operation = currentOperation;
            changeHistory();
            return;
        }

        if (currentOperation.equals("C")){
            clear(true, true);
        } else {
            if (firstNumber == null && operation == null) { //initially or after '=' or 'C'
                operation = currentOperation;
                firstNumber = num;
                addHistory(firstNumber);
            } else { //if we have first number and operation then take the second nr and do math
                secondNumber = num;
                calculateAndSetResult();
            }
            numberSet = false;
        }
    }

    public void clear(boolean clearHistory, boolean clearResult) {
        firstNumber = null;
        secondNumber = null;
        operation = null;

        if (clearResult)
            numberField.setText("0");

        if (clearHistory)
            historyField.setText("");
    }

    public void setResult(String result, String message){
        if (message != null && message.contains("Error")) {
            addHistoryMessage(message);
            clear(false, true);
            return;
        } else {
            numberField.setText(result);
            firstNumber = Long.parseLong(result);
        }

        if (currentOperation.equals("=")) {
            clear(true, false);
            numberSet = true;
        } else {
            addHistory(secondNumber);
            operation = currentOperation;
        }
    }

    public void addHistory(Long number, String text, boolean changeOperation){
        String history = historyField.getText().toString();

        if (text != null && text.length() > 1) {
            historyField.setText(text);
            return;
        }

        if (changeOperation && history.length() > 1) {
            history = history.substring(0, history.length() - 1);
            historyField.setText(history.concat(text));
            return;
        }

        if (number != null){
            history += " " + number.toString() + " " + text;
            historyField.setText(history);
        }
    }

    public void changeHistory(){
        addHistory(null, currentOperation, true);
    }

    public void addHistory(Long number){
        addHistory(number, currentOperation, false);
    }

    public void addHistoryMessage(String text){
        addHistory(null, text, false);
    }

    public void calculateAndSetResult(){
        if (operation != null && firstNumber != null && secondNumber != null) {
            busy(true);
            Intent intent = new Intent();
            intent.setAction("com.vladimir.calculateRequest");

            PackageManager packageManager = getPackageManager();
            if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                intent.putExtra("firstNumber", firstNumber);
                intent.putExtra("secondNumber", secondNumber);
                intent.putExtra("operation", operation);
                sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle resultExtras = getResultExtras(false);
                        String message = "";
                        String result = getResultData();
                        if (resultExtras != null && result != null) {
                            message = resultExtras.getString("Message");
                            MainActivity.this.setResult(result, message);
                        } else {
                            MainActivity.this.setResult(null, getString(R.string.ERR_BCST_EX));
                        }
                        busy(false);
                    }
                }, null, Activity.RESULT_OK, null, null);
            } else {
                MainActivity.this.setResult(null, getString(R.string.ERR_BCST_SRV));
                busy(false);
            }
        }
    }

    public void busy(boolean showBusy) {
        findViewById(R.id.loading).setVisibility(showBusy ? View.VISIBLE : View.GONE);
        PercentRelativeLayout layout = (PercentRelativeLayout) findViewById(R.id.mainLayout);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(!showBusy);
        }
    }
}
