package agu.test.basic;

import java.io.File;

import agu.bitmap.BitmapDecoder;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		imageView = (ImageView) findViewById(R.id.image_view);

		Bitmap bitmap = BitmapDecoder.from(new File(Environment.getExternalStorageDirectory(), "lb.png").getPath())
				.useBuiltInDecoder()
				.region(145, 192, 145 + 200, 192 + 400)
				.decode();
		imageView.setImageBitmap(bitmap);
		
		imageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				
				Log.e("asdf", "width = " + imageView.getWidth() + ", height = " + imageView.getHeight());
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
