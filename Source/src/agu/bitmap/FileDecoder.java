package agu.bitmap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.os.Build;

class FileDecoder extends BitmapDecoder {
	private String pathName;
	
	public FileDecoder(String pathName) {
		this.pathName = pathName;
	}
	
	protected FileDecoder(FileDecoder other) {
		super(other);
		pathName = other.pathName;
	}
	
	@Override
	protected Bitmap decode(Options opts) {
		return BitmapFactory.decodeFile(pathName, opts);
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return new FileInputStream(pathName);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	@Override
	protected BitmapRegionDecoder createBitmapRegionDecoder() {
		try {
			return BitmapRegionDecoder.newInstance(pathName, false);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public BitmapDecoder clone() {
		return new FileDecoder(this);
	}
}
