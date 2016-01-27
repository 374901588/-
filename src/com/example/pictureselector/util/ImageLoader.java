package com.example.pictureselector.util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * ͼƬ������ �����õ���ģʽ��
 * 
 * @author Just
 * 
 */
public class ImageLoader {

	private static ImageLoader mInstance;

	// ��Ա����
	/**
	 * ͼƬ����ĺ��Ķ���
	 */
	private LruCache<String, Bitmap> mLruCache;// String��ʾͼƬ��·��
	/**
	 * �̳߳أ�����ͳһ����Task ��ImageLoader�еĴ���һ����̨�߳���ȡ����Ȼ����뵽�̳߳��У�
	 */
	private ExecutorService mThreadPool;
	private static final int DEAFULT_THREAD_COUNT = 1;// �̳߳ص�Ĭ���߳���
	/**
	 * ���еĵ��ȷ�ʽ ����������¼ͼƬ�ļ��ز��ԣ�Ĭ��ΪLIFO��
	 */
	private Type mType = Type.LIFO;
	/**
	 * ������У����̳߳�ȡ����
	 */
	private LinkedList<Runnable> mTaskQueue;
	/**
	 * ��̨��ѯ�߳�
	 */
	private Thread mPoolThread;
	private Handler mPoolThreadHandler;// �������涨����̰߳���һ��ģ����ڸ��̷߳�����Ϣ
	/**
	 * UI�߳��е�Handler ���ڴ���һ��Task�Ժ󣬵�ͼƬ��ȡ�ɹ��Ժ�
	 * ��mUIHandler������Ϣ��ΪͼƬ���ûص����ص���ʾͼƬ��Bitmap��
	 */
	private Handler mUIHandler;

	/**
	 * Semaphore(�ź���)ͨ���������ƿ��Է���ĳЩ��Դ��������߼��ģ����߳���Ŀ��
	 * ͨ���ź�������mPoolThread����̨��ѯ�̣߳��г�ʼ��mPoolThreadHandler��
	 * ʹ�õ�loadImage��loadImage�еĵ�����addTask��addTask��ʹ����mPoolThraedhandler�������̵߳�˳��
	 */
	private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);// ��ʼ��ʱ���ÿ��õ����֤����Ϊ0��ʹ�õ�addTask��mPoolThreadHandler��ʼ��֮ǰִ��ʱ���ڵȴ�״̬

	private Semaphore mSemaphoreThreadPool;// ��init()�и����߳�����ָ���ź�������ʼ��

	public enum Type {
		FIFO, LIFO;
	}

	private ImageLoader(int mThreadCount, Type type) {// �������û������̳߳ص��߳����Լ����еĵ��ȷ�ʽ
		init(mThreadCount, type);
	}

	/**
	 * ��ʼ��
	 * @param mThreadCount
	 * @param type
	 */
	private void init(int mThreadCount, Type type) {
		
		// ��̨��ѯ�̣߳������漰��Looper�����Ķ���ƪ���� http://blog.csdn.net/lmj623565791/article/details/38377229
		mPoolThread = new Thread() {
			@Override
			public void run() {
				Looper.prepare();//Looper.prepare()���ں�̨��ѯ�߳��е��õ�
				// Looper���ڷ�װ��android�߳��е���Ϣѭ����Ĭ�������һ���߳��ǲ�������Ϣѭ����message loop���ģ�
				// ��Ҫ����Looper.prepare()�����̴߳���һ����Ϣѭ��������Looper.loop()��ʹ��Ϣѭ�������ã�
				// ����Ϣ������ȡ��Ϣ��������Ϣ��
				

				// "�Ҳ��� url -> ����һ��Task -> ��Task����TaskQueue�ҷ���һ��֪ͨȥ���Ѻ�̨��ѯ�߳�"
				// �������һ������mPoolThreadHandler�ᷢ��һ��Message��Looper�У����ջ����handleMessage
				mPoolThreadHandler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// �̳߳�ȡ���������ִ��
						mThreadPool.execute(getTask());

						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				};
				// ����һ�����֤���Ӷ����Ի���addTask�ĵȴ�
				mSemaphorePoolThreadHandler.release();
				Looper.loop();
			}
		};

		mPoolThread.start();

		int maxMemory = (int) Runtime.getRuntime().maxMemory();// ��ȡӦ���������ڴ�
		int cacheMemory = maxMemory / 8;// ���ڳ�ʼ��mLruCache
		mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
			@Override
			protected int sizeOf(String key, Bitmap value) {// ����ÿ��Bitmap��ռ�ݵ��ڴ沢����
				return value.getRowBytes() * value.getHeight();// ÿ��ռ�ݵ��ֽ���*�߶�
			}
		};

		// �����̳߳�
		mThreadPool = Executors.newFixedThreadPool(mThreadCount);
		mTaskQueue = new LinkedList<Runnable>();
		mType = type;

		mSemaphoreThreadPool = new Semaphore(mThreadCount);
	}

	/**
	 * ���������ȡ��һ��Runnable
	 * 
	 * @return
	 */
	private Runnable getTask() {
		if (mType == Type.FIFO) {
			return mTaskQueue.removeFirst();
		} else if (mType == Type.LIFO) {
			return mTaskQueue.removeLast();
		}
		return null;
	}

	public static ImageLoader getInstance() {
		// ����if�жϲ���ͬ���������ǿ��Թ��˵��󲿷ֵĴ��룬��mInstance��ʼ���������if������Ĵ���Ͳ�Ҫִ����
		// �����ڸտ�ʼmInstanceδ��ʼ����ʱ����δ��ͬ���Ĵ������ܻ���һ�����߳�ͬʱ����if������
		// �ȵ������������synchronized����������ʹ����Ҫ��ͬ�����̼߳��٣�ֻ��һ��ʼ�ļ���
		if (mInstance == null) {
			// �����������ܻ��м����߳�ͬʱ����
			synchronized (ImageLoader.class) {
				// ����if�ж��Ǳ�Ҫ�ģ���Ϊ�����if�ж�֮����Ǹ�����ܻ��м����߳�ͬʱ���
				// �����ڽ���synchronized����������жϿ��ܻ�new������ʵ��
				if (mInstance == null)
					mInstance = new ImageLoader(DEAFULT_THREAD_COUNT, Type.LIFO);
			}
		}

		// ��������ִ��������public synchronized static ImageLoader getInstance()�����Ч��

		return mInstance;
	}

	public static ImageLoader getInstance(int threadCount, Type type) {
		if (mInstance == null) {
			synchronized (ImageLoader.class) {
				if (mInstance == null)
					mInstance = new ImageLoader(threadCount, type);
			}
		}

		return mInstance;
	}

	/**
	 * ����pathΪimageView����ͼƬ
	 * 
	 * @param path
	 * @param imageView
	 */
	public void loadImage(final String path, final ImageView imageView) {
		imageView.setTag(path);// ��ֹitem���õ�ʱ��imageView�������ͼƬ���ң�imageView����ͼƬʱ�����Tag�Ա�path
		//������Բο�һ��http://blog.csdn.net/lmj623565791/article/details/24333277��ƪ����

		if (mUIHandler == null) {
			mUIHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					// ��ȡ�õ���ͼƬ��ΪimageView�ص�����ͼƬ
					ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
					Bitmap b = holder.bitmap;
					ImageView iv = holder.imageView;
					String p = holder.path;
					// ��path��getTag�洢·�����бȽ�

					if (iv.getTag().toString().equals(p)) {
						iv.setImageBitmap(b);
					}
				}
			};
		}

		// ����path�ڻ����л�ȡbitmap
		Bitmap bm = getBitmapFromLruCache(path);
		if (bm != null) {
			refreashBitmap(path, imageView, bm);
		} else {
			// Task���������ѹ��ͼƬ��Ȼ��ͼƬ���뵽����֮�У�Ȼ��ص�ͼƬ������ͼƬ���뵽ָ����imageView�У�
			addTask(new Runnable() {
				@Override
				public void run() {
					// ����ͼƬ���漰ͼƬ��ѹ��
					// 1�����ͼƬ��Ҫ��ʾ�Ĵ�С�����պ�ΪimageView�Ĵ�С��
					ImageSize imageSize = getImageViewSize(imageView);
					// 2��ѹ��ͼƬ
					Bitmap b = decodeSampledBitmapFromPath(path,
							imageSize.width, imageSize.height);
					// ��ͼƬ���뵽������
					addBitmapToLruCache(path, b);

					refreashBitmap(path, imageView, b);

					mSemaphoreThreadPool.release();// ������һ��ִ���꣬���ͷ�һ�����֤
				}
			});
		}
	}

	private void refreashBitmap(final String path, final ImageView imageView,
			Bitmap b) {
		Message message = Message.obtain();// ������Messge���з���һ���µ�Messageʵ������������µĶ��󣬼����ڴ濪��
		ImgBeanHolder holder = new ImgBeanHolder();
		holder.bitmap = b;
		holder.path = path;// loadImage���β�
		holder.imageView = imageView;// loadImage���β�
		message.obj = holder;
		mUIHandler.sendMessage(message);
	}

	/**
	 * ��ͼƬ���뵽LruCache
	 * 
	 * @param path
	 * @param b
	 */
	private void addBitmapToLruCache(String path, Bitmap b) {
		if (getBitmapFromLruCache(path) == null) {// ��Ҫ�жϻ������Ƿ��Ѿ�����
			if (b != null) {
				mLruCache.put(path, b);
			}
		}
	}

	/**
	 * ����ͼƬ��Ҫ����ʾ�Ŀ�͸߽���ѹ��
	 * 
	 * @param path
	 * @param width
	 * @param height
	 * @return
	 */
	private Bitmap decodeSampledBitmapFromPath(String path, int width,
			int height) {
		// ��ȡͼƬʵ�ʵĿ�͸ߣ�������ͼƬ���ص��ڴ���
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;// ʹ��ͼƬ�����ص��ڴ��У�����options.outWidth��options.outHeight�����ֵ
		BitmapFactory.decodeFile(path, options);// options�л����ͼƬʵ�ʵĿ�͸ߣ���options.outWidth��options.outHeight��
		options.inSampleSize = caculateInSampleSize(options, width, height);

		// ʹ�û�ȡ����inSampleSize�ٴν���ͼƬ���Ҽ��ص��ڴ���
		options.inJustDecodeBounds = false;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);

		return bitmap;
	}

	/**
	 * ��������Ŀ�͸��Լ�ʵ�ʵĿ�͸߼���SampleSize��ѹ��������
	 * 
	 * @param options
	 * @param width
	 * @param height
	 * @return
	 */
	private int caculateInSampleSize(Options options, int reqWidth,
			int reqHeight) {
		int width = options.outWidth;
		int height = options.outHeight;

		int inSampleSize = 1;

		// ���ֻ��һ����ͨ��ѹ�����ԣ���ȻҲ�����Լ������ر��ѹ������
		if (width > reqWidth || height > reqHeight) {
			int widthRadio = Math.round(width * 1.0f / reqWidth);// ��������������
			int heightRadio = Math.round(height * 1.0f / reqHeight);

			inSampleSize = Math.max(widthRadio, heightRadio);// Ϊ��ͼƬ��ʧ֡������ԭ����һ��ȡСֵ�����ǵõ���ͼƬ��ʼ�ձ���ʾ�����һЩ������Ϊ�˽�ʡ�ڴ棬���Ծ�����ѹ��
		}

		return inSampleSize;
	}

	/**
	 * ����ImageVIew��ȡ�ʵ���ѹ���Ŀ�͸�
	 * 
	 * @param imageView
	 * @return
	 */
	// @SuppressLint("NewApi")
	// ��Android�����У�������ʱ��ʹ�ñ�������AndroidManifest�����õ�android:minSdkVersion�汾���ߵķ�����
	// ��ʱ����������ʾ���棬����������ڷ����ϼ���@SuppressLint("NewApi"������@TargetApi()
	// SuppressLint("NewApi")���ý���������android lint���������ڷ����л�Ҫ�жϰ汾����ͬ�Ĳ���
	private ImageSize getImageViewSize(ImageView imageView) {
		ImageSize imageSize = new ImageSize();

		DisplayMetrics displayMetrics = imageView.getContext().getResources()
				.getDisplayMetrics();

		// imageView�ڲ����еĴ�С�����ǹ̶���С�ģ�Ҳ�п�������Ե�

		LayoutParams lp = imageView.getLayoutParams();

		int width = imageView.getWidth();// ��ȡimageView��ʵ�ʿ��
		// �п���ImageView�ձ�new������û����ӵ������У���������ԭ�򣬵����޷���ȡ������width�������Ҫ�ж�һ��
		if (width <= 0) {
			width = lp.width;// ����Ϊ�ڲ����������Ŀ�ȣ������п����ڲ�������wrap_content(-1)��match_parent(-2),������Ҫ��һ�����ж�
		}
		if (width <= 0) {
			// ��ֵΪ���ֵ,�������û�����õĻ���width��Ȼ�ǻ�ȡ�����������ֵ�ģ���˻���Ҫ��һ���ж�

			// getMaxWidth��������API16�в��У�����Ҫ����һ�£����÷����ȡ,�Ա���ݵ�16���µİ汾
			// width=imageView.getMaxWidth();
			width = getImageViewFieldValue(imageView, "mMaxWidth");// "mMaxWidth"����ȥImageView��Դ���в鿴
		}
		if (width <= 0) {
			width = displayMetrics.widthPixels;// ���û�취��ֻ�ܵ�����Ļ�Ŀ��
		}

		int height = imageView.getHeight();
		if (height <= 0) {
			height = lp.height;
		}
		if (height <= 0) {
			// height=imageView.getMaxHeight();
			height = getImageViewFieldValue(imageView, "mMaxHeight");
		}
		if (height <= 0) {
			height = displayMetrics.heightPixels;
		}

		imageSize.width = width;
		imageSize.height = height;

		return imageSize;
	}

	/**
	 * ͨ�������ȡImageView��ĳ������ֵ
	 * 
	 * @param object
	 * @param fieldName
	 * @return
	 */
	public static int getImageViewFieldValue(ImageView object, String fieldName) {
		int value = 0;

		try {
			Field field = ImageView.class.getDeclaredField(fieldName);
			// Field �ṩ�й����ӿڵĵ����ֶε���Ϣ���Լ������Ķ�̬����Ȩ�ޡ�������ֶο�����һ���ࣨ��̬���ֶλ�ʵ���ֶΡ�
			// getDeclaredField����ָ���ֶε��ֶζ���

			field.setAccessible(true);// �ڷ���ʹ����,����ֶ���˽�е�,��ô����Ҫ������ֶ�����

			int fieldValue = field.getInt(object);// ��Ϊ����ָ���������ʵ����ָ���ֶ�(����̬�ֶ�)��������������Ҫָ��ʵ������

			if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
				value = fieldValue;
			}

		} catch (Exception e) {
		}

		return value;
	}

	/**
	 * addTask��Ҫͬ��(synchronized)���������߳����mSemaphorePoolThreadHandler.acquire()
	 * �Ӷ�����������״̬ ��mTaskQueue.add(runnable)����Ҳ��Ҫͬ��
	 * 
	 * @param runnable
	 */
	private synchronized void addTask(Runnable runnable) {
		mTaskQueue.add(runnable);

		try {
			if (mPoolThreadHandler == null)
				mSemaphorePoolThreadHandler.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		mPoolThreadHandler.sendEmptyMessage(0x110);
	}

	/**
	 * ����path�ڻ����л�ȡbitmap
	 * 
	 * @param key
	 * @return
	 */
	private Bitmap getBitmapFromLruCache(String key) {
		return mLruCache.get(key);
	}

	private class ImageSize {
		public int width;
		public int height;
	}

	private class ImgBeanHolder {
		public Bitmap bitmap;
		public ImageView imageView;
		public String path;
	}
}
