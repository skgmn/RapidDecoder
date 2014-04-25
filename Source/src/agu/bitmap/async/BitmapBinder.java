package agu.bitmap.async;

public abstract class BitmapBinder implements AsyncBitmapCallback {
	public interface OnCancelListener {
		void onCancel();
	}
	
	private OnCancelListener mOnCancel;
	
	public Object getSingletonKey() {
		return null;
	}
	
	@Override
	public void onBitmapCancelled() {
		if (mOnCancel != null) {
			mOnCancel.onCancel();
		}
	}
	
	public BitmapBinder setOnCancelListener(OnCancelListener listener) {
		mOnCancel = listener;
		return this;
	}
}
