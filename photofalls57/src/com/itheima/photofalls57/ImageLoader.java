package com.itheima.photofalls57;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.itheima.photofalls57.DiskLruCache.Editor;
import com.itheima.photofalls57.DiskLruCache.Snapshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

public class ImageLoader {
	
	private LruCache<String, Bitmap> mLruCache;
	private DiskLruCache mDiskLruCache;
	private static ImageLoader mImageLoader;

	private ImageLoader(Context context){
		//初始化内存缓存
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int maxSize = maxMemory / 8;
		mLruCache = new LruCache<String, Bitmap>(maxSize){
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
		
		
		try {
			//获取本地缓存目录
			File directory = Utils.getDiskCacheDir(context, "thumb");
			int appVersion = Utils.getAppVersion(context);
			//初始化硬盘缓存
			mDiskLruCache = DiskLruCache.open(directory, appVersion, 1, 32 * 1024 *1024);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//32M
		
	}
	
	public static ImageLoader getInstance(Context context){
		if(mImageLoader == null){
			mImageLoader = new ImageLoader(context);
		}
		return mImageLoader;
	}
	
	
	public Bitmap loadImage(String url,int columnWidth){
		//首先从内存里取
		Bitmap bitmap = getMemoryCache(url);
		if(bitmap == null){
			//从本地硬盘里取,若没有就从网络加载
			bitmap = getBitmapFromDisk(url,columnWidth);
			
		}
		
		return bitmap;
	}
	
	private Bitmap getBitmapFromDisk(String url,int width) {
		// TODO Auto-generated method stub
		FileInputStream inputStream = null;
		FileDescriptor fd = null;
		//这里的Key会用来命令文件，所以应该用MD5加密
		String key = Utils.hashKeyForDisk(url);
		try {
			Snapshot snapshot = mDiskLruCache.get(key);
			//本地是否存在
			if(snapshot == null){
				Editor edit = mDiskLruCache.edit(key);
				OutputStream newOutputStream = edit.newOutputStream(0);
				//从网络上加载
				boolean b = Utils.downloadUrlToStream(url, newOutputStream);
				if(b){
					edit.commit();
				}else{
					edit.abort();
				}
				//再从本地获取
				snapshot = mDiskLruCache.get(key);
			}
			
			if(snapshot != null){
				inputStream = (FileInputStream) snapshot.getInputStream(0);
				fd = inputStream.getFD();
			}
			Bitmap bitmap = null;
			if(fd != null){
				bitmap = Utils.decodeSampledBitmapFromFileDescriptor(fd, width);
			}
			
			if(bitmap != null){
				//从本地文件返回的bitma，加入内存缓存中去
				addMemoryCache(url, bitmap);
			}
			
			return bitmap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(fd == null && inputStream != null){
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return null;
	}
	
	//同步本地缓存的日志文件
	public void flush(){
		if(mDiskLruCache != null){
			
			try {
				mDiskLruCache.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//从内存缓存里取图片
	public Bitmap getMemoryCache(String key){
		if(key != null){
			Bitmap bitmap = mLruCache.get(key);
			return bitmap;
		}
		return null;
	}
	
	//加入到内存缓存里
	public void addMemoryCache(String key,Bitmap bitmap){
		if(getMemoryCache(key) == null){
			mLruCache.put(key, bitmap);
		}
	}
}
