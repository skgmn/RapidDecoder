package agu.test.basic;

import agu.bitmap.BitmapDecoder;
import agu.bitmap.async.AsyncBitmapLoader;
import agu.bitmap.async.ImageViewBinder;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private ImageView imageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		imageView = (ImageView) findViewById(R.id.image_view);
		
		AsyncBitmapLoader loader = new AsyncBitmapLoader();
		loader.load(
				BitmapDecoder
						.from(this, Uri.parse("http://upload.inven.co.kr/upload/2012/10/31/bbs/i3758565816.jpg"))
						.scaleBy(1 / 2f),
				new ImageViewBinder(imageView));

//		new AsyncTask<Object, Object, Bitmap>() {
//			@Override
//			protected Bitmap doInBackground(Object... params) {
//				try {
////					InputStream in = new URL("http://farm6.staticflickr.com/5172/5588953445_51dcf922aa_o.jpg")
//					InputStream in = new URL("http://upload.inven.co.kr/upload/2012/10/31/bbs/i3758565816.jpg")
//						.openConnection()
//						.getInputStream();
//					
//					return BitmapDecoder.from(in).scaleBy(1 / 2f).useBuiltInDecoder(false).decode();
//				} catch (MalformedURLException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				return null;
//			}
//			
//			protected void onPostExecute(Bitmap result) {
//				if (result == null) {
//					Log.e("asdf", "bitmap is null");
//				} else {
//					imageView.setImageBitmap(result);
//				}
//			}
//		}.execute();
		
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
