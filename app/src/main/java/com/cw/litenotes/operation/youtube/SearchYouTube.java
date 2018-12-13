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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cw.litenotes.R;
import com.cw.litenotes.util.Util;
import com.cw.litenotes.util.image.UtilImage_bitmapLoader;
import com.cw.litenotes.util.uil.UilCommon;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;


/**
 * Search YouTube data by keyword
 */
public class SearchYouTube extends ListActivity {

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final String PROPERTIES_FILE_PATH = "/assets/youtube.properties";

    private static final long NUMBER_OF_VIDEOS_RETURNED = 25;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    EditText editKeyword;
    Button btnSearch;

    List<SearchResult> searchResultList;

    List<String> listTitles;
    List<String> listImages;
    List<String> listIDs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_youtube);

        // keyword
        editKeyword = findViewById(R.id.editSearchText);
        String str = getIntent().getExtras().getString("search_keywords");
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

    }


    @Override
    protected void onResume() {
        super.onResume();
        // init
        search(editKeyword.getText().toString());
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
//            System.out.println("SearchYouTube / _search / in = " + in.toString());
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
//            System.out.println("SearchYouTube / _search / apiqueryTermKey = " + queryTerm);

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = properties.getProperty("youtube.apikey");
//            System.out.println("SearchYouTube / _search/ apiKey = " + apiKey);
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
                            searchResultList = searchResponse.getItems();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            while (searchResultList == null)
            {}

            getSearchResult(searchResultList.iterator());

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

    // Get search result
    private void getSearchResult(Iterator<SearchResult> iteratorSearchResults) {

        listTitles = new ArrayList<>();
        listIDs = new ArrayList<>();
        listImages = new ArrayList<>();

        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId();

            // Confirm that the result represents a video. Otherwise, the
            // item will not contain a video ID.
            if (rId.getKind().equals("youtube#video")) {

                // System.out.println(" Thumbnail: " + thumbnail.getUrl());
                Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
                listImages.add(thumbnail.getUrl());

                // System.out.println(" Video Id" + rId.getVideoId());
                String videoId = rId.getVideoId();
                listIDs.add(videoId);

                // System.out.println(" Title: " + title);
                String title = singleVideo.getSnippet().getTitle();
                listTitles.add(title);

                // System.out.println(" URL = https://youtu.be/" + rId.getVideoId());
            }
        }
    }

    // Array for setting focus and file name, note: without generic will cause unchecked or unsafe operations warning
    class SearchYouTubeAdapter extends ArrayAdapter<String>
    {
        SearchYouTubeAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
//            System.out.println("SearchYouTube / SearchYouTubeAdapter / _constructor ");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

//            System.out.println("SearchYouTube / SearchYouTubeAdapter / _getView / position = " + position);
            if(convertView == null)
            {
                convertView = getLayoutInflater().inflate(R.layout.search_youtube_list_row, parent, false);
            }

            ProgressBar progressBar = convertView.findViewById(R.id.searched__progress);

            convertView.setFocusable(true);
            convertView.setClickable(true);

            // bitmap
            ImageView imageView = convertView.findViewById(R.id.searched_image);
            new UtilImage_bitmapLoader(imageView,
                    listImages.get(position),
                    progressBar,
                    UilCommon.optionsForFadeIn,
                    SearchYouTube.this);

            // title
            TextView tv = (TextView)convertView.findViewById(R.id.searched_keyword);
            tv.setText((position+1) + ". " + listTitles.get(position));

            // item listener
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

}