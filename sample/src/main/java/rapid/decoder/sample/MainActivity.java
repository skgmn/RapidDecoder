package rapid.decoder.sample;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout mDrawer;
    private ArrayAdapter<String> mAdapterDrawerMenu;
    private int nextContent = -1;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer);
        ListView listDrawerMenu = (ListView) findViewById(R.id.list_drawer_menu);

        drawerToggle = new ActionBarDrawerToggle(this, mDrawer,
                R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (nextContent >= 0) {
                    loadContent(nextContent);
                    nextContent = -1;
                }
            }
        };
        mDrawer.addDrawerListener(drawerToggle);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(drawerToggle.getDrawerArrowDrawable());
        }

        mAdapterDrawerMenu = new ArrayAdapter<>(this,
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

        supportInvalidateOptionsMenu();
        loadContent(0);
    }

    private void loadContent(int index) {
        Fragment fragment;

        switch (index) {
            case 0:
                fragment = new ScaledDecodingFragment();
                break;
//            case 1:
//                fragment = new RegionalDecodingFragment();
//                break;
//            case 2:
//                fragment = new MutableDecodingFragment();
//                break;
//            case 3:
//                fragment = new FrameFragment();
//                break;
//            case 4:
//                fragment = new GalleryFragment();
//                break;
//            case 5:
//                fragment = new ContactsFragment();
//                break;
//            case 6:
//                fragment = new WrapContentFragment();
//                break;
//            case 7:
//                fragment = new ResetFragment();
//                break;
            default:
                return;
        }

        setTitle(mAdapterDrawerMenu.getItem(index));
        getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();

        mDrawer.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        drawerToggle.syncState();
    }
}
