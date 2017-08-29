package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LoaderManager
    .LoaderCallbacks<List<News>>, OnSharedPreferenceChangeListener {

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
    private static final String PAGE_SIZE_PARAM = "page-size";

    // The news list adapter
    private NewsListAdapter mNewsListAdapter;

    // The news update scheduler
    private ScheduledExecutorService mUpdateScheduler;

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

                    showProgressBar();
                    restartLoader();

                    return true;
                }

                return false;
            }
        });

        scheduleNewsUpdate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        // Initializes or attaches to an existing loader
        getSupportLoaderManager().initLoader(NEWS_LOADER_ID, null, this);
    }

    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        String pageSize = getPreference(R.string.settings_page_size_key,
            R.string.settings_page_size_default);

        Uri.Builder uriBuilder = Uri.parse(THE_GUARDIAN_NEWS_API_URL)
            .buildUpon()
            .appendQueryParameter(SHOW_FIELDS_PARAM, "headline,byline,firstPublicationDate")
            .appendQueryParameter(API_KEY_PARAM, "36a7884f-b99c-41d7-9bbf-e94206c36fbd")
            .appendQueryParameter(TAG_PARAM, "politics/politics")
            .appendQueryParameter(PAGE_SIZE_PARAM, pageSize);

        if (!"".equals(mEditQuery.getText().toString())) {
            uriBuilder.appendQueryParameter(QUERY_PARAM, mEditQuery.getText().toString());
        }

        return new NewsLoader(this, uriBuilder.build().toString());
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> news) {
        // Hides the ProgressBar and sets the no results text
        mLoadingIndicator.setVisibility(View.GONE);
        mEmptyView.setText(R.string.no_results);

        // Clears the last data and adds the new ones
        mNewsListAdapter.clear();
        if (news != null && !news.isEmpty()) {
            mNewsListAdapter.addAll(news);
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
    public void onRestoreInstanceState(Bundle savedInstanceState,
        PersistableBundle persistentState) {
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
        if (networkInfo == null || !networkInfo.isConnected()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String getPreference(@StringRes int key, @StringRes int defValue) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPrefs.getString(getString(key), getString(defValue));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, key);

        if (key.equals(getString(R.string.settings_page_size_key)) || key
            .equals(getString(R.string.settings_update_interval_key))) {

            // Reeschedules the updater
            mUpdateScheduler.shutdown();
            scheduleNewsUpdate();

            // Reloads the data
            mNewsListAdapter.clear();
            showProgressBar();
            restartLoader();
        }
    }

    private void scheduleNewsUpdate() {
        String updateInterval = getPreference(R.string.settings_update_interval_key, R.
            string.settings_update_interval_default);

        mUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
        mUpdateScheduler.scheduleAtFixedRate
            (new Runnable() {
                public void run() {
                    restartLoader();
                }
            }, 0, Integer.valueOf(updateInterval), TimeUnit.MINUTES);
    }

    /**
     * Hides the no results TextView and shows the ProgresBar
     */
    private void showProgressBar() {
        mEmptyView.setVisibility(View.GONE);
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    private void restartLoader() {
        getSupportLoaderManager().restartLoader(NEWS_LOADER_ID, null, MainActivity.this);
    }
}