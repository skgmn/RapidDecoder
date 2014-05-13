package agu.scaling;

import android.graphics.drawable.Drawable;

public class FrameOptions {
	public FrameAlignment align;
	public FrameStrategy strategy;
	public Drawable background;
	
	public FrameOptions() {
		set(null);
	}
	
	public void set(FrameOptions other) {
		if (other == null) {
			align = FrameAlignment.CENTER;
			strategy = FrameStrategy.FIT;
			background = null;
		} else {
			align = other.align;
			strategy = other.strategy;
			background = other.background;
		}
	}
	
	public FrameOptions align(FrameAlignment align) {
		this.align = align;
		return this;
	}
	
	public FrameOptions strategy(FrameStrategy strategy) {
		this.strategy = strategy;
		return this;
	}
	
	public FrameOptions background(Drawable background) {
		this.background = background;
		return this;
	}
}
