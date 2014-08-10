package rapid.decoder.sample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import rapid.decoder.BitmapDecoder;

public class MainActivity extends ActionBarActivity {
    public static final boolean TEST_BUILT_IN_DECODER = true;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawer;
    private ArrayAdapter<String> mAdapterDrawerMenu;
    private int nextContent = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapDecoder.initDiskCache(this);
        BitmapDecoder.initMemoryCache(32 * 1024 * 1024);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer);
        ListView listDrawerMenu = (ListView) findViewById(R.id.list_drawer_menu);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, R.drawable.ic_navigation_drawer,
                R.string.drawer_open, R.string.drawer_close) {

            @Override
            public void onDrawerClosed(View v) {
                super.onDrawerClosed(v);
                if (nextContent >= 0) {
                    loadContent(nextContent);
                    nextContent = -1;
                }
            }
        };

        mAdapterDrawerMenu = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array
                .drawer_menu_items));
        listDrawerMenu.setAdapter(mAdapterDrawerMenu);

        listDrawerMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                                    long arg3) {
                nextContent = position;
                mDrawer.closeDrawers();
            }
        });

        mDrawer.setDrawerListener(mDrawerToggle);
        supportInvalidateOptionsMenu();
        loadContent(0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    private void loadContent(int index) {
        Fragment fragment;

        switch (index) {
            case 0:
                fragment = new ScaledDecodingFragment();
                break;
            case 1:
                fragment = new RegionalDecodingFragment();
                break;
            case 2:
                fragment = new MutableDecodingFragment();
                break;
            case 3:
                fragment = new FrameFragment();
                break;
            case 4:
                fragment = new GalleryFragment();
                break;
            default:
                return;
        }

        setTitle(mAdapterDrawerMenu.getItem(index));
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();

        mDrawer.closeDrawers();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mDrawer.openDrawer(Gravity.LEFT);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }
}
