package eu.f3rog.apt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import eu.f3rog.log.Logged;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doSomething("Hello", 3);
    }

    @Logged
    private void doSomething(String text, int num) {
        MainActivity_Logger.doSomething(this, text, num);
    }
}
