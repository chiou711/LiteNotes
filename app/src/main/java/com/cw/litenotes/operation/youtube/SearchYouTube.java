/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cw.litenotes.operation.youtube;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cw.litenotes.R;
import com.cw.litenotes.util.Util;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

//import com.google.api.services.samples.youtube.cmdline.Auth;

/**
 * Print a list of videos matching a search term.
 *
 * @author Jeremy Walker
 */
public class SearchYouTube extends ListActivity {

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final String PROPERTIES_FILE_PATH = "/assets/youtube.properties";

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;//25;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;
    EditText editKeyword;
    Button btnSearch;
    List<SearchResult> searchResultList;
    List<String> listTitles;
    static List<String> listIDs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_youtube);

        // keyword
        editKeyword = findViewById(R.id.editSearchText);
        String str = getIntent().getExtras().getString("search_title");
        editKeyword.setText(str);
        editKeyword.setSelection(editKeyword.length());

        // search button
        btnSearch = findViewById(R.id.button_search);
        btnSearch.setOnClickListener(v -> {
            search(editKeyword.getText().toString());
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            // hide soft input
            if (getCurrentFocus() != null)
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        });

        listTitles = new ArrayList<>();
    }


    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return super.onCreateView(parent, name, context, attrs);
    }

    /**
     * Initialize a YouTube object to search for videos on YouTube. Then
     * display the name and thumbnail image of each video in the result set.
     *
     * @param args command line args.
     */
    public void  search(String args) {
    	System.out.println("SearchYouTube / _search / args = " + args);
        // Read the developer key from the properties file.
        Properties properties = new Properties();
        try {
            InputStream in = SearchYouTube.class.getResourceAsStream(PROPERTIES_FILE_PATH);
//				InputStream in = MainAct.mAct.getResources().openRawResource(R.raw.youtube_properties);
            System.out.println("SearchYouTube / _search / in = " + in.toString());
            properties.load(in);

        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILE_PATH + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            //https://stackoverflow.com/questions/24065065/having-trouble-importing-google-api-services-samples-youtube-cmdline-auth
//            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
//                    System.out.println("SearchYouTubeByKeyword / _main / Builder IOException ");
                }
            }
            ).setApplicationName("youtube-cmdline-search-sample").build();

            // Prompt the user to enter a query term.
            String queryTerm = args;//getInputQuery();//args
            System.out.println("SearchYouTube / _search / apiqueryTermKey = " + queryTerm);

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = properties.getProperty("youtube.apikey");
            System.out.println("SearchYouTube / _search/ apiKey = " + apiKey);
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        SearchListResponse searchResponse = search.execute();

                        if (searchResponse == null)
                            return;
                        else
//                            searchResultList = searchResponse.getItems();
                            searchResultList = searchResponse.getItems();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            while (searchResultList == null)
            {}

//            listTitles = new ArrayList<>();
            listTitles = getSearchResult(searchResultList.iterator());

            for (int i = 0; i < listTitles.size(); i++) {
                System.out.println("---- list title = " + listTitles.get(i));
            }

            SearchYouTubeAdapter fileListAdapter = new SearchYouTubeAdapter(this,
                    R.layout.search_youtube_list_row,
                    listTitles);

            setListAdapter(fileListAdapter);

            searchResultList = null;
        }
        catch (GoogleJsonResponseException e)
        {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        }
        catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    // File name array for setting focus and file name, note: without generic will cause unchecked or unsafe operations warning
    class SearchYouTubeAdapter extends ArrayAdapter<String>
    {
        SearchYouTubeAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            System.out.println("SearchYouTube / SearchYouTubeAdapter / _constructor ");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            System.out.println("SearchYouTube / SearchYouTubeAdapter / _getView / position = " + position);
            if(convertView == null)
            {
                convertView = getLayoutInflater().inflate(R.layout.search_youtube_list_row, parent, false);
            }

            convertView.setFocusable(true);
            convertView.setClickable(true);

            TextView tv = (TextView)convertView.findViewById(R.id.text1);
            tv.setText((position+1) + "\n" + listTitles.get(position));
//            tv.setText(position);


            final int item = position;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String idStr = listIDs.get(position);
                    // apply native YouTube
                    Util.openLink_YouTube(SearchYouTube.this, idStr);
                }
            });
            return convertView;
        }
    }

    /*
     * Prompt the user to enter a query term and return the user-specified term.
     */
    private static String getInputQuery() throws IOException {

        String inputQuery = "";

        System.out.print("Please enter a search term: ");
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        inputQuery = bReader.readLine();

        if (inputQuery.length() < 1) {
            // Use the string "YouTube Developers Live" as a default.
            inputQuery = "YouTube Developers Live";
        }
        return inputQuery;
    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query SearchYouTubeByKeyword query (String)
     */
    private static void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) {

        System.out.println("\n=============================================================");
        System.out.println(
                "   First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        int count = 0;
        while (iteratorSearchResults.hasNext()) {

            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
                count++;
//                System.out.println(" Video Id" + rId.getVideoId());
                System.out.println(" Count: " + count);
                System.out.println(" Title: " + singleVideo.getSnippet().getTitle());
                System.out.println(" URL = https://youtu.be/" + rId.getVideoId());
//                System.out.println(" Thumbnail: " + thumbnail.getUrl());
                System.out.println("\n-------------------------------------------------------------\n");
            }
        }
    }

    // Get titles of searched items
    private static List<String> getSearchResult(Iterator<SearchResult> iteratorSearchResults) {
        System.out.println("SearchYouTube / _getSearchResult");
        listIDs = new ArrayList<>();
        List<String> list = new ArrayList<>();
        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {
//                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
//                System.out.println(" Video Id" + rId.getVideoId());
//                System.out.println(" Count: " + count);
                String title = singleVideo.getSnippet().getTitle();
//                System.out.println(" Title: " + title);
//                System.out.println(" URL = https://youtu.be/" + rId.getVideoId());
//                System.out.println(" Thumbnail: " + thumbnail.getUrl());
                list.add(title);
                listIDs.add(rId.getVideoId());
            }
        }
        return list;
    }

}
