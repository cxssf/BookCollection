package com.x22.bookcollection.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.SignInButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.x22.bookcollection.app.db.DatabaseHelper;
import com.x22.bookcollection.app.model.BookItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;


public class MainActivity extends FragmentActivity implements ItemFragment.OnFragmentInteractionListener {

    private DatabaseHelper dbHelper;
    private ConnectivityManager mConnectivityManager;

    private View mProgressView;
    private SignInButton mPlusSignInButton;
    private View mSignOutButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //dbHelper = new DatabaseHelper(getApplicationContext());
        //dbHelper.deleteAllBooks();

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

                new GetBookInfo().execute(scanContent);

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

    class GetBookInfo extends AsyncTask<String, Object, JSONObject> {

        @Override
        protected void onPreExecute() {
            if (isNetworkConnected() == false) { // Check network connection.
                Log.i(getClass().getName(), "Not connected to the internet");
                cancel(true); // Cancel request.

                return;
            }
        }

        @Override
        protected JSONObject doInBackground(String... isbns) {
            // Stop if cancelled
            if (isCancelled()) {
                return null;
            }

            Log.w(getClass().getName(), "ISBNS: " + isbns);

            String apiUrlString = "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbns[0];
            try {
                HttpURLConnection connection = null;
                try { // Build Connection.
                    URL url = new URL(apiUrlString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(5000); // 5 seconds
                    connection.setConnectTimeout(5000); // 5 seconds
                } catch (MalformedURLException e) {
                    // Impossible: The only two URLs used in the app are taken from string resources.
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    // Impossible: "GET" is a perfectly valid request method.
                    e.printStackTrace();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Log.w(getClass().getName(), "GoogleBooksAPI request failed. Response Code: " + responseCode);
                    connection.disconnect();
                    return null;
                }

                // Read data from response.
                StringBuilder builder = new StringBuilder();
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = responseReader.readLine();
                while (line != null) {
                    builder.append(line);
                    line = responseReader.readLine();
                }

                String responseString = builder.toString();
                Log.d(getClass().getName(), "Response String: " + responseString);
                JSONObject responseJson = new JSONObject(responseString);
                // Close connection and return response code.
                connection.disconnect();

                return responseJson;
            } catch (SocketTimeoutException e) {
                Log.w(getClass().getName(), "Connection timed out. Returning null");
            } catch (IOException e) {
                Log.d(getClass().getName(), "IOException when connecting to Google Books API.");
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d(getClass().getName(), "JSONException when connecting to Google Books API.");
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject responseJson) {
            if (isCancelled()) {
                // Request was cancelled due to no network connection.
                //showNetworkDialog();
                createDialog(R.string.no_network);
            } else if (responseJson == null) {
                //showSimpleDialog(getResources().getString(R.string.dialog_null_response));
                createDialog(R.string.no_result);
            } else {
                dbHelper = new DatabaseHelper(getApplicationContext());

                try {
                    Log.i("MainActivity", "Result: " + responseJson.toString());

                    JSONArray bookArray = responseJson.getJSONArray("items");
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
            }
        }
    }

    protected boolean isNetworkConnected() {
        // Instantiate mConnectivityManager if necessary
        if (mConnectivityManager == null) {
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        // Is device connected to the Internet?
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /*private class GetBookInfo extends AsyncTask<String, Void, String> {
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
            fragment.updateListview();

            //(BaseAdapter) mMyListView.getAdapter()).notifyDataSetChanged();
        }
    }*/

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
