package agu.sample.integrated;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends FragmentActivity {
	private ListView listDrawerMenu;
	private ArrayAdapter<String> adapterDrawerMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		listDrawerMenu = (ListView) findViewById(R.id.list_drawer_menu);
		
		adapterDrawerMenu = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.drawer_menu_items));
		listDrawerMenu.setAdapter(adapterDrawerMenu);
		
		listDrawerMenu.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				
				loadContent(position);
			}
		});
		
		loadContent(0);
	}
	
	private void loadContent(int index) {
		Fragment fragment;
		
		switch (index) {
		case 0: fragment = new ScaledDecodingFragment(); break;
		case 1: fragment = new RegionalDecodingFragment(); break;
		default: return;
		}
		
		setTitle(adapterDrawerMenu.getItem(index));
		getSupportFragmentManager().beginTransaction().replace(R.id.content, fragment).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
