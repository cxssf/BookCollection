package com.x22.bookcollection.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.x22.bookcollection.app.db.DatabaseHelper;
import com.x22.bookcollection.app.model.BookItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainActivity extends FragmentActivity implements ItemFragment.OnFragmentInteractionListener {

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(getApplicationContext());
        dbHelper.deleteAllBooks();

        /*BookItem book1 = new BookItem("The Target", "David Baldacci");
        BookItem book2 = new BookItem("King & Maxwell", "David Baldacci");

        dbHelper = new DatabaseHelper(getApplicationContext());
        dbHelper.deleteAllBooks();
        dbHelper.createBook(book1);
        dbHelper.createBook(book2);

        dbHelper.closeDb();*/
    }

    @Override
    public void onResume() {
        super.onResume();
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

        switch (item.getItemId()) {
            case R.id.action_scan:
                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.initiateScan();

                return true;

            case R.id.action_settings:
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            String scanContent = scanningResult.getContents();
            String scanFormat = scanningResult.getFormatName();

            if (scanContent != null && scanFormat != null && scanFormat.equalsIgnoreCase("EAN_13")) {
                String bookSearchString = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + scanContent;
                new GetBookInfo().execute(bookSearchString);

            } else {
                Log.i("MainActivity", "R.string.scan_failed");
                createDialog(R.string.scan_failed);
            }
        } else {
            Log.i("MainActivity", "R.string.scan_failed");
            createDialog(R.string.scan_failed);
        }
    }

    public AlertDialog createDialog(int message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage(message);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public AlertDialog createDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage(message);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private class GetBookInfo extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... bookURLs) {
            StringBuilder bookBuilder = new StringBuilder();
            for (String bookSearchURL : bookURLs) {

                Log.i("MainActivity", "bookSearchURL: " + bookSearchURL);
                //createDialog(bookSearchURL);

                HttpClient bookClient = new DefaultHttpClient();

                try {
                    //get the data
                    HttpGet bookGet = new HttpGet(bookSearchURL);
                    HttpResponse bookResponse = bookClient.execute(bookGet);
                    StatusLine bookSearchStatus = bookResponse.getStatusLine();
                    if (bookSearchStatus.getStatusCode() == 200) {
                        //we have a result
                        HttpEntity bookEntity = bookResponse.getEntity();
                        InputStream bookContent = bookEntity.getContent();
                        InputStreamReader bookInput = new InputStreamReader(bookContent);
                        BufferedReader bookReader = new BufferedReader(bookInput);

                        String lineIn;
                        while ((lineIn = bookReader.readLine()) != null) {
                            bookBuilder.append(lineIn);
                        }

                        Log.i("MainActivity", "bookBuilder.toString(): " + bookBuilder.toString());
                        //createDialog(bookBuilder.toString());
                    } else {
                        Log.i("MainActivity", "R.string.no_result");
                        //createDialog(R.string.no_result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return bookBuilder.toString();
        }

        protected void onPostExecute(String result) {
            dbHelper = new DatabaseHelper(getApplicationContext());

            try {
                JSONObject resultObject = new JSONObject(result);
                Log.i("MainActivity", "Result: " + resultObject.toString());

                JSONArray bookArray = resultObject.getJSONArray("items");
                JSONObject bookObject = bookArray.getJSONObject(0);
                JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");

                BookItem book = new BookItem();

                try {
                    book.setTitle(volumeObject.getString("title"));
                } catch (JSONException jse) {
                    jse.printStackTrace();
                }

                StringBuilder authorBuild = new StringBuilder("");
                try {
                    JSONArray authorArray = volumeObject.getJSONArray("authors");
                    for (int a = 0; a < authorArray.length(); a++) {
                        if (a > 0) authorBuild.append(", ");
                        authorBuild.append(authorArray.getString(a));
                    }
                    book.setAuthor(authorBuild.toString());
                } catch (JSONException jse) {
                    jse.printStackTrace();
                }

                dbHelper.createBook(book);
            } catch (Exception e) {
                //no result
                e.printStackTrace();
            }

            dbHelper.closeDb();

            //ItemFragment itemFragment = (ItemFragment) findViewById(R.id.fragment);

            /*Fragment f1 = getSupportFragmentManager().findFragmentById(R.id.fragment);
            Log.i("MainActivity", "Fragment1: " + (f1 == null));

            for (Fragment f : getSupportFragmentManager().getFragments()) {
                Log.i("MainActivity", f.getTag());
            }


            ItemFragment fragment = (ItemFragment) getSupportFragmentManager().findFragmentByTag("com.x22.bookcollection.app.ItemFragment");
            Log.i("MainActivity", "Fragment2: " + (fragment == null));
            fragment.updateListview();*/

            //(BaseAdapter) mMyListView.getAdapter()).notifyDataSetChanged();
        }
    }

    /*private class GetBookInfo extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... bookURLs) {

            StringBuilder bookBuilder = new StringBuilder();

            for (String bookSearchURL : bookURLs) {

                HttpClient bookClient = new DefaultHttpClient();

                try {
                    HttpGet bookGet = new HttpGet(bookSearchURL);
                    HttpResponse bookResponse = bookClient.execute(bookGet);
                    StatusLine bookSearchStatus = bookResponse.getStatusLine();
                    if (bookSearchStatus.getStatusCode() == 200) {

                        HttpEntity bookEntity = bookResponse.getEntity();

                        InputStream bookContent = bookEntity.getContent();
                        InputStreamReader bookInput = new InputStreamReader(bookContent);
                        BufferedReader bookReader = new BufferedReader(bookInput);

                        String lineIn;
                        while ((lineIn = bookReader.readLine()) != null) {
                            bookBuilder.append(lineIn);
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return bookBuilder.toString();
        }

        protected void onPostExecute(String result) {
            try {
                JSONObject resultObject = new JSONObject(result);
                JSONArray bookArray = resultObject.getJSONArray("items");

                JSONObject bookObject = bookArray.getJSONObject(0);
                JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");

                try {
                    Toast toast = Toast.makeText(getApplicationContext(), "TITLE: "+ volumeObject.getString("title"), Toast.LENGTH_LONG);
                    toast.show();
                } catch(JSONException jse) {
                    jse.printStackTrace();
                }
            } catch(Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(), "No book found", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }*/
}
