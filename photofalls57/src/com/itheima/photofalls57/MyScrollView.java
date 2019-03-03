package com.itheima.photofalls57;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class MyScrollView extends ScrollView implements OnTouchListener {
	private static int PAGE_SIZE = 20;
	private int page = 0;
	// 是否为第一次加载
	private boolean isOnceLoad;
	private ImageLoader mImageLoader;
	// 每列的宽度
	private int columnWidth;

	// 三个linearlayout
	private LinearLayout firstColumn;
	private LinearLayout secondColumn;
	private LinearLayout thirdColumn;

	// 三列linearlayout的高度
	private int firstColumnHeight;
	private int secondColumnHeight;
	private int thirdColumnHeight;
	
	private static int scrollViewHeight;
	private static View ll_container;
	
	private static int scrollHeight;
	
	//正在执行的异步线程集合
	private static Set<LoadImageTask> taskCollections;
	//imageview放置的集合
	private List<ImageView> imageviewList = new ArrayList<ImageView>();


	public MyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);

		mImageLoader = ImageLoader.getInstance(context);
		taskCollections = new HashSet<MyScrollView.LoadImageTask>();
		
		setOnTouchListener(this);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (!isOnceLoad) {
			firstColumn = (LinearLayout) findViewById(R.id.first_colum);
			secondColumn = (LinearLayout) findViewById(R.id.second_colum);
			thirdColumn = (LinearLayout) findViewById(R.id.third_colum);

			columnWidth = firstColumn.getWidth();
			//scrollview的高度，也就是屏幕高度
			scrollViewHeight = getHeight();
			//直接子布局的高度
			ll_container = getChildAt(0);
			
			isOnceLoad = true;
			loadMoreImages();
		}

	}

	private void loadMoreImages() {
		// 进行分页加载
		int startIndex = page * PAGE_SIZE;// 0 * 20 = 0
		int endIndex = (page + 1) * PAGE_SIZE;// 1* 20 = 20
		// 如果开始的位置小于图片的总数，那么就进行加载
		if (startIndex <= Images.imageUrls.length) {
			Toast.makeText(getContext(), "loading...", 0).show();
			if (endIndex > Images.imageUrls.length) {
				endIndex = Images.imageUrls.length;
			}
			for (int i = startIndex; i < endIndex; i++) {
				// 进行加载
				String imageUrl = Images.imageUrls[i];
				// 开一个异步线程 AsyncTask
				LoadImageTask task = new LoadImageTask();
				taskCollections.add(task);
				task.execute(imageUrl);

			}

			// 每加载一次，页数就增加
			page++;
		} else {
			Toast.makeText(getContext(), "no more picture...", 0).show();
		}

	}

	class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
		String url;
		ImageView mImageView;
		public LoadImageTask(ImageView imageView) {
			this.mImageView = imageView;
		}

		public LoadImageTask() {
		}

		// 在子线程里执行
		@Override
		protected Bitmap doInBackground(String... params) {
			// 加载图片bitmap
			url = params[0];
			// 涉及三级缓存 先从内存里取，再从本地缓存里拿，最后是网络加载
			Bitmap bitmap = mImageLoader.loadImage(url, columnWidth);

			return bitmap;
		}

		// 在子线程执行完之后执行（UI线程）
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			// 显示到界面上
			if (bitmap != null) {
				// 为了更加精确，我们展示高度重新计算一次
				double ratio = bitmap.getWidth() / (columnWidth * 1.0);
				int scaleHeight = (int) (bitmap.getHeight() / ratio);
				if(mImageView != null){
					mImageView.setImageBitmap(bitmap);
				}else{
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
							columnWidth, scaleHeight);
					ImageView imageView = new ImageView(getContext());
					imageView.setLayoutParams(params);
					imageView.setImageBitmap(bitmap);
					imageView.setScaleType(ScaleType.FIT_XY);
					imageView.setPadding(5, 5, 5, 5);
					
					imageView.setTag(R.string.image_url, url);
					
					// 要加入到最短的线性布局中
					findShortestLinearLayout(scaleHeight,imageView).addView(imageView);
					//放置到集合中
					imageviewList.add(imageView);
				}

			}
			taskCollections.remove(this);

		}
		//找到最短的那一列,顺便记录下每张imageview的头部和底部坐标
		private LinearLayout findShortestLinearLayout(int imageHeight,ImageView imageView) {
			if (firstColumnHeight <= secondColumnHeight) {
				if (firstColumnHeight <= thirdColumnHeight) {
					imageView.setTag(R.string.image_top, firstColumnHeight);
					firstColumnHeight += imageHeight;
					imageView.setTag(R.string.image_bottom, firstColumnHeight);
					
					return firstColumn;
				} else {
					imageView.setTag(R.string.image_top, thirdColumnHeight);
					thirdColumnHeight += imageHeight;
					imageView.setTag(R.string.image_bottom, thirdColumnHeight);
					return thirdColumn;
				}
			} else {
				if (secondColumnHeight <= thirdColumnHeight) {
					imageView.setTag(R.string.image_top, secondColumnHeight);
					secondColumnHeight += imageHeight;
					imageView.setTag(R.string.image_bottom, secondColumnHeight);
					return secondColumn;
				} else {
					imageView.setTag(R.string.image_top, thirdColumnHeight);
					thirdColumnHeight += imageHeight;
					imageView.setTag(R.string.image_bottom, thirdColumnHeight);
					return thirdColumn;
				}
			}

		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// 当滑动停止并且滑动最底部的时候，进行加载更多
		if (event.getAction() == MotionEvent.ACTION_UP) {
			// 由于滑动惯性，手指抬起时不一定是滑动停止时
			Message message = new Message();
			message.obj = this;
			handler.sendMessageDelayed(message, 5);
		}

		// return true的话，scrollview滑动事件失效
		return false;
	}

	private static Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			MyScrollView myScrollView = (MyScrollView) msg.obj;
			int scrollY = myScrollView.getScrollY();
			if(scrollHeight == scrollY){
				//滑动停止了
				//为了用户体验，当所有正在加载的任务停止时候，加载
				if(scrollViewHeight + scrollY >= ll_container.getHeight() && taskCollections.size() == 0 ){
					
					myScrollView.loadMoreImages();
					
				}
				//是否在屏幕内，不是的话，设置一张默认的图片，是的话显示原图
				myScrollView.checkVisible();
				
			}else{
				scrollHeight = scrollY;
				Message message = handler.obtainMessage();
				message.obj = myScrollView;
				handler.sendMessageDelayed(message, 5);
			}
			

		};
	};
	
	public void checkVisible(){
		//1.遍历所有图片
		for(int i=0; i< imageviewList.size();i++){
			//2.判断是否在屏幕内
			ImageView imageView = imageviewList.get(i);
			int top = (Integer) imageView.getTag(R.string.image_top);
			int bottom = (Integer) imageView.getTag(R.string.image_bottom);
			//可见的部分
			if(bottom > getScrollY() && top < getScrollY() + scrollViewHeight){
				//三级缓存
				String imageUrl = (String) imageView.getTag(R.string.image_url);
				Bitmap bitmap = mImageLoader.getMemoryCache(imageUrl);
				if(bitmap != null){
					imageView.setImageBitmap(bitmap);
					Log.e("tag", 123+"");
					imageView.setOnClickListener(
							
							new OnClickListener() {
						 
						@Override
						public void onClick(View v) {
							//在屏幕上显示提示 Toast 吐司
							//Toast.makeText(getApplicationContext(), "a", 1000).show();
							Log.e("tag", 123+"");
							getApplicationWindowToken();
							Vibrator vibrator = (Vibrator)getContext().getSystemService(getContext().VIBRATOR_SERVICE);
							vibrator.vibrate(1000);
						}
					});
					
					/*imageView.setOnTouchListener(new OnTouchListener(){

						@Override
						public boolean onTouch(View v, MotionEvent event) {
							// TODO Auto-generated method stub
							Log.e("tag", 123+"");
							return false;
						}
						
					});*/
				}else{
					LoadImageTask task = new LoadImageTask(imageView);
					task.execute(imageUrl);
				}
				
				
			}else{
				imageView.setImageResource(R.drawable.empty_photo);
			}
			
			
			
		}
		
		
		
	}
	
	
	
}
