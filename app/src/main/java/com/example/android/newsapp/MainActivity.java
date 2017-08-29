package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements LoaderManager
        .LoaderCallbacks<List<News>> {

    private static final String TAG = "MainActivity";

    // Loader ID
    private static final int NEWS_LOADER_ID = 1;

    // Key to save the list position
    private static final String LIST_SCROLL_POSITION = "SCROLL_POSITION";

    // The Guardian News API URL
    private static final String THE_GUARDIAN_NEWS_API_URL = "http://content.guardianapis" +
            ".com/search";

    private static final String QUERY_PARAM = "q";
    private static final String SHOW_FIELDS_PARAM = "show-fields";
    private static final String API_KEY_PARAM = "api-key";
    private static final String TAG_PARAM = "tag";

    // The news list adapter
    private NewsListAdapter mNewsListAdapter;

    // The views
    /* Start */
    @BindView(R.id.edit_query)
    EditText mEditQuery;

    @BindView(R.id.news_list_view)
    ListView mNewsListView;

    @BindView(R.id.empty_view)
    TextView mEmptyView;

    @BindView(R.id.loading_indicator)
    ProgressBar mLoadingIndicator;
    /* End */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        disableConnectionReuseIfNecessary();

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mNewsListAdapter = new NewsListAdapter(this, 0);

        mNewsListView.setEmptyView(mEmptyView);
        mNewsListView.setAdapter(mNewsListAdapter);

        mNewsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                News currentNews = mNewsListAdapter.getItem(position);
                Uri earthquakeUri = Uri.parse(currentNews.getUrl());
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, earthquakeUri);
                startActivity(websiteIntent);
            }
        });

        // Sets the EditText to have a Search button in the keyboard
        mEditQuery.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mEditQuery.setSingleLine();

        mEditQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (!isInternetAvailable()) {
                    mLoadingIndicator.setVisibility(View.GONE);

                    mEmptyView.setText(R.string.no_internet_connection);

                    return false;
                }

                // Treats the search action from the EditText
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mNewsListAdapter.clear();

                    // Hides the no results TextView and shows the ProgresBar
                    mEmptyView.setVisibility(View.GONE);
                    mLoadingIndicator.setVisibility(View.VISIBLE);

                    // Restarts the Loader to search the books
                    getSupportLoaderManager().restartLoader(NEWS_LOADER_ID, null, MainActivity.this);

                    return true;
                }

                return false;
            }
        });

        // Initializes or attaches to an existing loader
        getSupportLoaderManager().initLoader(NEWS_LOADER_ID, null, this);
    }

    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        Uri.Builder uriBuilder = Uri.parse(THE_GUARDIAN_NEWS_API_URL)
                .buildUpon()
                .appendQueryParameter(SHOW_FIELDS_PARAM, "headline,byline,firstPublicationDate")
                .appendQueryParameter(API_KEY_PARAM, "36a7884f-b99c-41d7-9bbf-e94206c36fbd")
                .appendQueryParameter(TAG_PARAM, "politics/politics");


        if (!"".equals(mEditQuery.getText().toString())) {
            uriBuilder.appendQueryParameter(QUERY_PARAM, mEditQuery.getText().toString());
        }

        return new NewsLoader(this, uriBuilder.build().toString());
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> newses) {

        // Hides the ProgressBar and sets the no results text
        mLoadingIndicator.setVisibility(View.GONE);
        mEmptyView.setText(R.string.no_results);

        // Clears the last data and adds the new ones
        mNewsListAdapter.clear();
        if (newses != null && !newses.isEmpty()) {
            mNewsListAdapter.addAll(newses);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {

        // Clear the data that will be released from the loader
        mNewsListAdapter.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        int currentPosition = mNewsListView.getFirstVisiblePosition();
        outState.putInt(LIST_SCROLL_POSITION, currentPosition);

        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);

        int savedPosition = savedInstanceState.getInt(LIST_SCROLL_POSITION);
        mNewsListView.setSelection(savedPosition);
    }

    /**
     * Work around pre-Froyo bugs in HTTP connection reuse.
     */
    private void disableConnectionReuseIfNecessary() {

        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * Checks if the internet connection is available
     *
     * @return <code>true</code> when internet is available, <code>false</code> otherwise
     */
    private boolean isInternetAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected())
            return false;

        return true;
    }
}