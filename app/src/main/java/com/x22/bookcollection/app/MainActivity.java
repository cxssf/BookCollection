package com.x22.bookcollection.app;

import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.x22.bookcollection.app.db.DatabaseHelper;
import com.x22.bookcollection.app.model.Book;


public class MainActivity extends FragmentActivity implements ItemFragment.OnFragmentInteractionListener {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Book book1 = new Book("The Target", "David Baldacci");
        Book book2 = new Book("King & Maxwell", "David Baldacci");

        dbHelper = new DatabaseHelper(getApplicationContext());
        dbHelper.createBook(book1);
        dbHelper.createBook(book2);

        dbHelper.closeDb();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String id) {

    }
}
