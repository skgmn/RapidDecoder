package agu.test.basic;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.async.AsyncBitmapLoader;
import agu.bitmap.async.FadeInEffect;
import agu.bitmap.async.ImageViewBinder;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		imageView = (ImageView) findViewById(R.id.image_view);
		
		BitmapDecoder.initMemoryCache(this);
		BitmapDecoder.initDiskCache(this);
		
		Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.artist_kim_2)/*.scaleBy(0.5f)*/.region(0, 0, 100, 100).decode();
		imageView.setImageBitmap(bitmap);
		
		Log.e("asdf", "width = " + bitmap.getWidth());
		Log.e("asdf", "height = " + bitmap.getHeight());
		
//		Button button = (Button) findViewById(R.id.button);
//		button.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				AsyncBitmapLoader loader = new AsyncBitmapLoader();
//				loader.load(
//						BitmapDecoder
//								.from(MainActivity.this, Uri.parse("http://upload.inven.co.kr/upload/2012/10/31/bbs/i3758565816.jpg"))
//								.scaleBy(0.8f),
////								.from(MainActivity.this, Uri.parse("http://upload.wikimedia.org/wikipedia/commons/4/4e/Pleiades_large.jpg"))
////								.scaleBy(0.1f),
//						new ImageViewBinder(imageView)
//								.effect(new FadeInEffect(500)));
//			}
//		});

//		imageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
//			@SuppressWarnings("deprecation")
//			@Override
//			public void onGlobalLayout() {
//				imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//				
//				Log.e("asdf", "width = " + imageView.getWidth() + ", height = " + imageView.getHeight());
//			}
//		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
