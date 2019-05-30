package com.akcay.themoviedb;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText nameText;
    TextView infoView, progressView;
    ListView resultListView;
    Button searchButton, fullListButton, partialListButton;
    private final String ACTORS_NOT_FETCHED = "Actor list haven't been fetched from database yet",
                         INPUT_CONTAINS_SPACE = "Name to be searched must not contain spaces",
                         BUSY_TEXT = "There is an ongoing fetch operation";
    private ArrayList<Actor> mainActorList = new ArrayList<>();
    private boolean searchAvailable = false,
                    isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameText = (EditText) findViewById(R.id.nameText);
        searchButton = (Button) findViewById(R.id.searchButton);
        fullListButton = (Button) findViewById(R.id.fullListButton);
        partialListButton = (Button) findViewById(R.id.partialListButton);
        infoView = (TextView) findViewById(R.id.infoView);
        progressView = (TextView) findViewById(R.id.progressView);
        resultListView = (ListView) findViewById(R.id.resultListView);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);   // prevent keyboard popping up on startup

        updateListView(null);

        setupButtons();

    }

    /*****
     */
    private void setupButtons()
    {
        searchButton.setOnClickListener(new View.OnClickListener() {    //SEARCH
            @Override
            public void onClick(View view) {
                String input = nameText.getText().toString();
                infoView.setText("");
                updateListView(null);
                if (!searchAvailable)                       // data is not fetched or currently being fetched for first time
                {
                    infoView.setText(ACTORS_NOT_FETCHED);
                }
                else if (input.contains(" "))               // firstname can't contain spaces
                {
                    infoView.setText(INPUT_CONTAINS_SPACE);
                }
                else if (input.equals(""))                  // empty input --> show all fetched actors (mainActorList)
                {
                    infoView.setText(ALL_ACTORS(mainActorList.size()));
                    updateListView(mainActorList);
                }
                else                                                // search is possible
                {
                    ArrayList<Actor> searchResult = search(input);
                    int count = searchResult.size();
                    if (count == 0)                                 // there is no actors for given name
                    {
                        infoView.setText(NAME_NOT_FOUND(input));
                    }
                    else                                             // actors with given name is found
                    {
                        infoView.setText(ITEMS_FOUND_TEXT(input, count));   // printing count of firstnames in mainActorList
                        updateListView(searchResult);
                    }
                }
            }
        });


        fullListButton.setOnClickListener(new View.OnClickListener() {      // GET FULL LIST
            @Override
            public void onClick(View view) {

                if(!isBusy)                             // if there is not ongoing fetch operation
                    new DataFetcher().execute(false);   // fetch full list with isPartial = false
                else
                    infoView.setText(BUSY_TEXT);

            }
        });


        partialListButton.setOnClickListener(new View.OnClickListener() {   // GET PARTIAL LIST
            @Override
            public void onClick(View view) {
                if(!isBusy)                             // if there is not ongoing fetch operation
                    new DataFetcher().execute(true);    // fetch partial list with isPartial = true
                else
                    infoView.setText(BUSY_TEXT);
            }
        });

    }

    /**
     * assign given list to resultListView
     * @param list
     */
    private void updateListView(ArrayList<Actor> list)
    {
        Actor[] items;
        if (list == null)
            items = new Actor[0];
        else
            items = list.toArray(new Actor[list.size()]);
        ArrayAdapter<Actor> adapter = new ArrayAdapter<Actor>(this, R.layout.list_config, items);
        resultListView.setAdapter(adapter);
    }


    /****
     * search mainActorList for firstname
     * @param firstname
     * @return  --> found actor list
     */
    private ArrayList<Actor> search(String firstname)
    {
        firstname += " ";
        String fullname, partialName;
        ArrayList<Actor> resultList = new ArrayList<>();
        int lenOfName = firstname.length();

        for (Actor actor : mainActorList)                          // iterating over list to find given firstname
        {
            fullname = actor.getFullname();

            if (fullname.length() < firstname.length())     //
                continue;                                   //
                                                            //  comparison operations
            partialName = fullname.substring(0, lenOfName); //  actors have same first name
                                                            //  with given "firstname"
            if (partialName.equalsIgnoreCase(firstname))    //  are added to resultList
            {                                               //
                resultList.add(actor);                      //
            }                                               //
        }
        return resultList;
    }



    private String ITEMS_FOUND_TEXT(String input, int count)
    {
        return "*" + Integer.toString(count) + "*   \"" + input + "\"  found.";
    }



    private String NAME_NOT_FOUND(String name)
    {
        return "Name \"" + name + "\" is not found.";
    }

    private String ACTORS_FETCHED(int count)
    {
        return String.format("*%d* actors fetched.", count);
    }

    private String ALL_ACTORS(int size)
    {
        return String.format("ALL FETCHED ACTORS \t(%d)", size);
    }


    public void setMainActorList(ArrayList<Actor> list)
    {
        mainActorList = list;
    }

    ///////////////////////////////
    private class DataFetcher extends AsyncTask<Boolean,Void,ArrayList<Actor>> {


        private final String API_KEY = "&api_key=597d02faf5c9b5685f8c4aaf3c10f097",
                LANGUAGE = "&language=en-US",
                PAGE = "page=",
                URL_STRING = "https://api.themoviedb.org/3/person/popular?",
                PHOTO_URL = "https://image.tmdb.org/t/p/w500";
        private int total_pages = 0, total_results = 0, current_page = 0, page_limit = 0;
        private int REQUEST_LIMIT = 10,
                REQUEST_COUNTER = 0;

        private ArrayList<Actor> resultList;

        /*****
         * Thread for performing queries to API
         * @param params --> specifies if partial or full list requested
         * @return --> list of actors
         */
        protected ArrayList<Actor> doInBackground(Boolean... params)
        {
            resultList = new ArrayList<>();
            boolean isPartial = params[0];
            JSONArray queryResult;

            isBusy = true;          //  to provide one fetcher at a time

            queryResult = getQueryResult(1);    // perform API query for page 1
                                                     // total_pages are also determined here

            page_limit = total_pages+1;     //  upper limit of number of queries to be made

            if (isPartial)                  // if partial list button is pressed
            {                               // query 5% of total pages by reducing limit
                page_limit /= 20;
            }

            for (int currentPage=2; currentPage<=page_limit; currentPage++)     // page 1 is queried -> currentPage starts from 2
            {
                function(queryResult);                      //  make queries for each page until reaching limit
                queryResult = getQueryResult(currentPage);  //  and call function for each queryResult
            }

            return resultList;      // list of actors
        }

        /********
         * Performs query to API with given page number
         * @param page
         * @return JSONArray consisting actor information
         */
        private JSONArray getQueryResult(int page)
        {
            JSONArray results = new JSONArray();
            try
            {
                URL url = new URL(URL_STRING + PAGE + Integer.toString(page) + LANGUAGE + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();      //
                                                                                        //
                InputStream ip = conn.getInputStream();                                 //
                BufferedReader br = new BufferedReader(new InputStreamReader(ip));      //
                StringBuilder response = new StringBuilder();                           //      neccessary part for performing queries
                String responseSingle = null;                                           //
                while ((responseSingle = br.readLine()) != null)                        //
                {                                                                       //
                    response.append(responseSingle);                                    //
                }                                                                       //

                JSONObject resultObject = new JSONObject(response.toString());          //  converting query response to JSONObject
                total_pages = resultObject.getInt("total_pages");

                results = resultObject.getJSONArray("results");

                current_page = page;                //////////      global current_page is assigned
                publishProgress();                  //////////      to determine progress
                REQUEST_COUNTER = 0;        /// neccessary for some limit set by API
            }
            catch (MalformedURLException e)
            {
                Log.e("ERROR", e.getStackTrace().toString(), e);;
            }
            catch (JSONException e)
            {
                Log.e("ERROR", e.getStackTrace().toString(), e);;
            }
            catch (IOException e)
            {
                if(REQUEST_COUNTER < REQUEST_LIMIT)
                {
                    REQUEST_COUNTER++;              // results of queries can be unexpected due to some limits set by API
                    return getQueryResult(page);    // so same query is made multiple times to get result
                }                                   // REQUEST_LIMIT is not exceeded and infinite recursion is not posssible.

            }

            return results;              // returning query result consisting array of actors (JSONArray)
        }

        /*********
         * Extracts relevant actor information from
         * given list of actors that existed in specified page (API)
         * and adds them to resultList.
         * @param queryResult
         */
        private void function(JSONArray queryResult)
        {
            int sizeOfResult;
            JSONObject tmpJson;
            String name, photoURL;
            double popularity;

            sizeOfResult = queryResult.length();
            try
            {
                for (int j = 0; j < sizeOfResult; j++)
                {
                    tmpJson = queryResult.getJSONObject(j);     /// function ( queryResult )

                    name = tmpJson.getString("name");
                    photoURL = PHOTO_URL + tmpJson.getString("profile_path");
                    popularity = tmpJson.getDouble("popularity");

                    resultList.add(new Actor(name, photoURL, popularity));
                }

            }
            catch (JSONException e)
            {
                Log.e("JSONError", e.getStackTrace().toString(), e);
            }
        }

        /*****
         * informs user about progress of queries to API
         * by setting progressView text to current_page/page_limit
         * @param params
         */
        public void onProgressUpdate(Void... params)
        {
            super.onProgressUpdate(params);
            progressView.setText(Integer.toString(current_page )+ "/" + Integer.toString(page_limit));
        }

        /******
         * fetch operation ended,
         * resultListView and infoView is updated accordingly
         * @param list  --> resultList that is returned from doInBackground
         */
        @Override
        protected void onPostExecute(ArrayList<Actor> list)
        {
            super.onPostExecute(list);
            setMainActorList(list);                                  // setting mainActorList for further search operations
            progressView.setText("Finished");                   // progress = finished
            updateListView(mainActorList);
            infoView.setText(ALL_ACTORS(mainActorList.size()));  // printing number of actors fetched
            searchAvailable = true;                             // search operation now could be done
            isBusy = false;                                     // allowing further fetch operations
        }
    }

}
