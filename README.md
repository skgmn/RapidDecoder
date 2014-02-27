Features
========
- Facilitated bitmap decoding
- Simplified bitmap scaling
- Decoding bitmap regionally with built-in decoder (Supports down to Froyo)
- Decoding bitmap as mutable with built-in decoder (Supports down to Froyo)
- Scaling bitmap corresponds to the frame with specific size
- Resource pool to avoid frequent garbage collection, which provides caching [Paint](http://developer.android.com/reference/android/graphics/Paint.html), [Rect](http://developer.android.com/reference/android/graphics/Rect.html), [RectF](http://developer.android.com/reference/android/graphics/RectF.html), [Point](http://developer.android.com/reference/android/graphics/Point.html), [Matrix](http://developer.android.com/reference/android/graphics/Matrix.html), and [BitmapFactory.Options](http://developer.android.com/reference/android/graphics/BitmapFactory.Options.html)
- Drawable and view which can render animating gif

Installation
============
1. Download the [zip file](https://github.com/nirvanfallacy/AndroidGraphicsUtility/blob/master/Binary/agu.zip?raw=true).
2. Extract all the files and folders into your project's **libs/** folder.

Basic decoding
==============

```java
import agu.bitmap.BitmapDecoder;

Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image)
                             .decode();
```

Decoding bitmap scaled
----------------------

```java
int width = 400;
int height = 300;

Bitmap bitmap = BitmapDecoder.from(getResouces(), R.drawable.image)
                             .scale(width, height)
                             .decode();
```

**BitmapDecoder** calculates the size of the image to be decoded and automatically fills in [inSampleSize](http://developer.android.com/reference/android/graphics/BitmapFactory.Options.html#inSampleSize) parameter internally. You don't need to be concerend about it.

Decoding bitmap scaled by ratio
-------------------------------

```java
Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image)
                             .scaleBy(0.5)
                             .decode();
```

Decoding regionally
-------------------

```java
int left = 10;
int top = 20;
int right = 30;
int bottom = 40;

Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image)
                             .region(left, top, right, bottom)
                             .decode();
```

Decoding bitmap as mutable
--------------------------

```java
Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image)
                             .mutable()
                             .decode();
```

Framing
=======

Concept
-------

Source image <br/>
<img src="https://raw.github.com/nirvanfallacy/AndroidGraphicsUtility/master/Sample/IntegratedSample/res/drawable-nodpi/amanda.jpg" width="150" height="200" />

Framed images <br/>
![](https://raw.github.com/nirvanfallacy/AndroidGraphicsUtility/master/Image/Framing.png)

Usage
-----

```java
import agu.scaling.BitmapFrameBuilder;
import agu.scaling.FrameAlignment;

BitmapDecoder source = BitmapDecoder.from(getResources(), R.drawable.amanda);
int frameWidth = 200;
int frameHeight = 200;

Drawable background = getResources.getDrawable(R.drawable.background);

Bitmap bitmap = new BitmapFrameBuilder(source, frameWidth, frameHeight)
                        .align(FrameAlignment.LEFT_OR_TOP)
                        .background(background)
                        .fitIn();
                        
Bitmap bitmap2 = new BitmapFrameBuilder(source, frameWidth, frameHeight)
                        .align(FrameAlignment.CENTER)
                        .cutOut();
```

Resource pool
=============

```java
import static agu.caching.ResourcePool.*;

Rect rect = RECT.obtain();
try {
    // Do something with the rect instance.
} finally {
    RECT.recycle(rect);
}
```

Animating gif
=============

```xml
<agu.widget.AnimatingGifView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:src="@+id/gif_file" />
```
