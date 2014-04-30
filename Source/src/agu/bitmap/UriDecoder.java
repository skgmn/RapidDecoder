package agu.bitmap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;

class UriDecoder extends BitmapDecoder {
	private static final String MESSAGE_INVALID_URI = "Invalid uri: %s";
	private static final String MESSAGE_PACKAGE_NOT_FOUND = "Package not found: %s";
	private static final String MESSAGE_RESOURCE_NOT_FOUND = "Resource not found: %s";
	private static final String MESSAGE_UNSUPPORTED_SCHEME = "Unsupported scheme: %s";
	private static final String MESSAGE_URI_REQUIRES_CONTEXT = "This type of uri requires Context. Use BitmapDecoder.from(Uri, Context) instead.";
	
	private BitmapDecoder mDecoder;

	public UriDecoder(Uri uri) {
		this(null, uri);
	}
	
	public UriDecoder(Context context, final Uri uri) {
		String scheme = uri.getScheme();
		
		if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
			if (context == null) {
				throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
			}
			
			List<String> segments = uri.getPathSegments();
			if (segments.size() != 2 || !segments.get(0).equals("drawable")) {
				throw new IllegalArgumentException(String.format(MESSAGE_INVALID_URI, uri));
			}
			
			Resources res;
			
			String packageName = uri.getAuthority();
			if (context.getPackageName().equals(packageName)) {
				res = context.getResources();
			} else {
				PackageManager pm = context.getPackageManager();
				try {
					res = pm.getResourcesForApplication(packageName);
				} catch (NameNotFoundException e) {
					throw new IllegalArgumentException(String.format(MESSAGE_PACKAGE_NOT_FOUND, packageName));
				}
			}
			
			String resName = segments.get(1);
			int id = res.getIdentifier(resName, "drawable", packageName);
			if (id == 0) {
				throw new IllegalArgumentException(String.format(MESSAGE_RESOURCE_NOT_FOUND, resName));
			}
			
			mDecoder = new ResourceDecoder(res, id);
		} else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
			if (context == null) {
				throw new IllegalArgumentException(MESSAGE_URI_REQUIRES_CONTEXT);
			}
			
			try {
				mDecoder = new StreamDecoder(context.getContentResolver().openInputStream(uri));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else if (scheme.equals(ContentResolver.SCHEME_FILE)) {
			mDecoder = new FileDecoder(uri.getPath());
		} else if (scheme.equals("http") || scheme.equals("https") || scheme.equals("ftp")) {
			mDecoder = new PendingStreamDecoder(new StreamOpener() {
				@Override
				public InputStream openInputStream() {
					try {
						return new URL(uri.toString()).openStream();
					} catch (MalformedURLException e) {
						throw new IllegalArgumentException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}					
				}
			});
		} else {
			throw new IllegalArgumentException(String.format(MESSAGE_UNSUPPORTED_SCHEME, scheme));
		}
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return mDecoder.decode(opts);
	}

	@Override
	protected InputStream getInputStream() {
		return mDecoder.getInputStream();
	}

	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		return mDecoder.createBitmapRegionDecoder();
	}
	
	@Override
	protected void onDecodingStarted(boolean builtInDecoder) {
		mDecoder.onDecodingStarted(builtInDecoder);
	}
	
	@Override
	protected void onDecodingFinished() {
		mDecoder.onDecodingFinished();
	}
}
