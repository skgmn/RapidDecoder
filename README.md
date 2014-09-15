Installation
============

Add this repository to _build.gradle_ in your project's root.

```
allprojects {
    repositories {
        maven {
            url 'https://github.com/suckgamony/RapidDecoder/raw/master/repository'
        }
    }
}
```

Add dependencies to _build.gradle_ in your module.

```
dependencies {
    compile 'rapid.decoder:library:0.2.4'
    compile 'rapid.decoder:jpeg-decoder:0.2.4'
    compile 'rapid.decoder:png-decoder:0.2.4'
}
```

**jpeg-decoder** and **png-decoder** are optional. Refer to [Builtin decoder](#builtin-decoder).

Basic decoding
==============

To decode a bitmap from resource:

```java
import rapid.decoder.BitmapDecoder;

Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image).decode();
```

Bitmap can also be decoded from other sources like this:

```java
// Decodes bitmap from byte array
byte[] bytes;
Bitmap bitmap = BitmapDecoder.from(bytes).decode();

// Decodes bitmap from file
Bitmap bitmap = BitmapDecoder.from("/sdcard/image.png").decode();

// Decodes bitmap from network
Bitmap bitmap = BitmapDecoder.from("http://server.com/image.jpeg").decode();

// Decodes bitmap from content provider
Bitmap bitmap = BitmapDecoder.from("content://app/user/0/profile").decode();

// Decodes bitmap from other app's resource
Bitmap bitmap = BitmapDecoder.from("android.resource://com.app/drawable/ic_launcher")
                             .decode();

// Decodes bitmap from stream
InputStream is;
Bitmap bitmap = BitmapDecoder.from(is).decode();

// Decodes from database
final SQLiteDatabase db;
Bitmap bitmap = BitmapDecoder
        .from(new Queriable() {
            @Override
            public Cursor query() {
                return db.query("table", new String[]{"column"}, "id=1", null, null,
                        null, null);
            }
        })
        .decode();
```

Advanced decoding
=================

Scaling
-------

All of the scaling operations automatically decode bounds of bitmaps and calculate [inSampleSize](http://developer.android.com/reference/android/graphics/BitmapFactory.Options.html#inSampleSize), so you don't need to consider about them at all.

```java
// Scaling to 400x300
Bitmap bitmap = BitmapDecoder.from(getResouces(), R.drawable.image)
                             .scale(400, 300)
                             .decode();

// Scaling by 50%
Bitmap bitmap = BitmapDecoder.from("/sdcard/image.png")
                             .scaleBy(0.5)
                             .decode();

```

Regional decoding
-----------------

Only partial area of bitmap can be decoded. (Supports down to Froyo)


```java
// Decodes the area (100, 200)-(300, 400) of the bitmap scaling it by 50%.
Bitmap bitmap = BitmapDecoder.from("/sdcard/image.jpeg")
                             .region(100, 200, 300, 400)
                             .scaleBy(0.5)
                             .decode();

```

Mutable decoding
----------------

You can directly modify decoded bitmap if it was decoded as mutable. This also supports down to Froyo.

```java
Bitmap bitmap2 = something;
Bitmap bitmap = BitmapDecoder.from(getResources(), R.drawable.image)
                             .mutable()
                             .decode();
Canvas canvas = new Canvas(bitmap);
canvas.draw(bitmap2, 0, 0, null):
```

Direct drawing
--------------

It is possible to draw bitmap directly to canvas from BitmapDecoder. It's generally faster than drawing bitmap after full decoding.

```java
Bitmap bitmap = Bitmap.createBitmap(width, height, Config.RGB_565);
Canvas canvas = new Canvas(bitmap);
BitmapDecoder.from("/image.png").scaleBy(0.2, 0.5).draw(canvas, x, y);
```

Post processing
---------------

You can hook decoded bitmap and replace it to something you want.

```java
// Make rounded image
Bitmap bitmap = BitmapDecoder.from("http://somewhere.com/image.jpeg")
        .postProcessor(new BitmapPostProcessor() {
                @Override
                public Bitmap process(Bitmap bitmap) {
                    Bitmap bitmap2 = Bitmap.createBitmap(bitmap.getWidth(),
                        bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap2);
                    
                    Paint paint = new Paint();
                    paint.setColor(0xffffffff);
                    RectF area = new RectF(0, 0, bitmap2.getWidth(), bitmap2.getHeight());
                    canvas.drawRoundRect(area, 10, 10, paint);
                    
                    paint.reset();
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                    canvas.drawBitmap(bitmap, 0, 0, paint);
                    
                    return bitmap2;
                }
        })
        .decode();
```

In this case you **MUST NOT** recycle the given bitmap.

Builtin decoder
---------------

If requsted operations can not be done just by Android APIs, RapidDecoder uses builtin decoder to accomplish them. Currently there are 2 builtin decoders.

* png-decoder - [LibPNG](http://www.libpng.org/pub/png/libpng.html)
* jpeg-decoder - Modified version of [jpgd](http://code.google.com/p/jpgd/)

You have to add appropriate dependencies to get benefits of backward compatibility. See [Installation](#installation).

Then when will it be needed? It will require builtin decoder if one of the following operations are requsted.

* Regional decoding on Android < 2.3.3
* Regional & mutable decoding
* Mutable decoding on Android < 3.0
* Scaling more than 50% without scale filter

Framing
=======

You can apply [ScaleType](http://developer.android.com/reference/android/widget/ImageView.ScaleType.html) to make a decoded bitmap fit into certain size.

```java
Bitmap bitmap = BitmapDecoder.from("content://authority/path")
        .frame(frameWidth, frameHeight, ImageView.ScaleType.CENTER_CROP)
        .decode();
```

When the image needs to be cropped, it uses region() internally so that only required area can be decoded. In this reason, it takes less memory and less time than implementing it only in APIs provided by Android.

You can also provide your own custom framing method by extending FramedDecoder and FramingMethod. But note that it's not documened yet.

Caching
=======

BitmapDecoder provides memory cache and disk cache. Disk cache only works for network images. Memory cache can be used for decoding bitmaps from network, file, and Android resource by default.

It is needed to initialize caches before any decoding operation.

```java
// Allocate 2MB for memory cache
BitmapDecoder.initMemoryCache(2 * 1024 * 1024);
// Allocate proper amount proportional to screen size for memory cache
BitmapDecoder.initMemoryCache(context);

// Allocate default 8MB for disk cache
BitmapDecoder.initDiskCache(context);
// Allocate 32MB for disk cache
Bitmapdecoder.initDiskCache(context, 32 * 1024 * 1024);
```

That's it. There's nothing to set anymore. Subsequent decoding will automatically uses caches.

It's also able to make caches disabled temporarily.

```java
// Do not use memory cache this time
Bitmap bitmap = BitmapDecoder.from("/image.jpeg")
        .useMemoryCache(false)
        .decode();
        
// Do not use disk cache this time
Bitmap bitmap = BitmapDecoder.from("http://web.com/image.png", false)
        .decode();
```

Loading bitmap into view
========================

Bitmaps can be loaded directly into view using into().

```java
BitmapDecoder.from("/image.png").into(view);
```

Decoding is done in background and it will be displayed fading in on the view. Bitmap will be loaded as an image if the view is ImageView, or it will be loaded as a background.

View binders
------------

If you want to set more parameters to customize behaviours, you should use view binders. Above code is exactly equivalent to below:

```java
import rapid.decoder.binder.ImageViewBinder;
import rapid.decoder.binder.ViewBackgroundBinder;

if (view instanceof ImageView) {
    BitmapDecoder.from("/image.png").into(
            ImageViewBinder.obtain((ImageView) view));
} else {
    BitmapDecoder.from("/image.png").into(
            ViewBackgroundBinder.obtain(view));
}
```

You can also load bitmaps into TextView's compound drawable by using TextViewBinder.

```java
BitmapDecoder.from("/image.png").into(
        TextViewBinder.obtain(textView, Gravity.LEFT, width, height));
```

Bitmap will be fade in on loaded by default. That behaviour can be changed the way like this:

```java
BitmapDecoder.from("/image.png").into(
        ImageViewBinder.obtain(imageView).effect(Effect.NO_EFFECT));
```

There are currently 3 effects provided: NO_EFFECT, FADE_IN, FADE_IN_IF_SYNC. All of these are defined in Effect class. Also you can create your own effect by inheriting Effect class. It's not yet documented but it's easy to understand source code.

Placeholder and error image can be set as following:

```java
ViewBinder<ImageView> binder = ImageViewBinder.obtain(imageView)
        .placeholder(R.drawable.placeholder)
        .errorImage(R.drawable.error);
BitmapDecoder.from("/image.png").into(binder);
```

By the way, you may want to create a drawable which displays decoded bitmap other than BitmapDrawable. In this case, you can achieve it by overriding createDrawable() from ViewBinder.

```java
BitmapDecoder.from("/image.png").into(
        new ImageViewBinder(imageView) {
            @Override
            public Drawable createDrawable(Context context, Bitmap bitmap) {
                return new YourOwnDrawable(context, bitmap);
            }
        });
```

It can also be framed with ScaleType. In this case, frame size is determined by the given view's layout.

```java
ViewBinder<ImageView> binder = ImageViewBinder.obtain(imageView)
        .scaleType(ImageView.ScaleType.CENTER_CROP);
BitmapDecoder.from("/image.png").into(binder);
```
