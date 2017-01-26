package com.omnitracs.navigation;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class SearchPlacesActivity extends AppCompatActivity {

    private static final String TAG = "SearchPlacesActivity";
    private ListView resultsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_places);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultsList = (ListView) findViewById(R.id.results_list);
        TextView empty = (TextView) findViewById(R.id.empty);

        resultsList.setEmptyView(empty);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.options_menu, menu);

        MenuItem myActionMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "onQueryTextSubmit: Query submitted: " + query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "onQueryTextChange: Query changed: " + s);
                }
                return false;
            }
        });
        return true;
    }

}

