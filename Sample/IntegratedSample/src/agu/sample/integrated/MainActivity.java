package agu.sample.integrated;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private DrawerLayout drawer;
	private ListView listDrawerMenu;
	private ArrayAdapter<String> adapterDrawerMenu;
	private int nextContent = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		drawer = (DrawerLayout) findViewById(R.id.drawer);
		listDrawerMenu = (ListView) findViewById(R.id.list_drawer_menu);
		
		adapterDrawerMenu = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.drawer_menu_items));
		listDrawerMenu.setAdapter(adapterDrawerMenu);
		
		listDrawerMenu.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				
				nextContent = position;
				drawer.closeDrawers();
			}
		});
		
		drawer.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int arg0) {
			}
			
			@Override
			public void onDrawerSlide(View arg0, float arg1) {
			}
			
			@Override
			public void onDrawerOpened(View arg0) {
			}
			
			@Override
			public void onDrawerClosed(View arg0) {
				if (nextContent >= 0) {
					loadContent(nextContent);
					nextContent = -1;
				}
			}
		});
		
		loadContent(0);
	}
	
	private void loadContent(int index) {
		Fragment fragment;
		
		switch (index) {
		case 0: fragment = new ScaledDecodingFragment(); break;
		case 1: fragment = new RegionalDecodingFragment(); break;
		case 2: fragment = new FrameFragment(); break;
		default: return;
		}
		
		setTitle(adapterDrawerMenu.getItem(index));
		getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();

		drawer.closeDrawers();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			drawer.openDrawer(Gravity.LEFT);
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
}
