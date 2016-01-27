package com.example.pictureselector.bean;

import java.util.HashSet;

import java.util.List;
import java.util.Set;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.pictureselector.R;
import com.example.pictureselector.util.ImageLoader;
import com.example.pictureselector.util.ImageLoader.Type;

public class ImageAdapter extends BaseAdapter {
	
	/**
	 * ��¼ͼƬ�Ƿ�ѡ��
	 * ���ı��ļ��е�ʱ�򣬻�����newһ��ImageAdapter
	 * ������������static���ڲ�ͬ�Ķ���乲������
	 * ʹ�ü�ʹ�Ǹı����ļ��У����ص�ԭ�����ļ���ʱ���Ѿ���ѡ�е�ͼƬ���ᱻȡ��
	 */
	private static Set<String> mSelectedImg=new HashSet<String>();
	
	private String mDirPath;
	private List<String> mImagePaths;
	private LayoutInflater mInflater;//���ڼ���item�Ĳ���
	
	private int mScreenWidth;//��Ļ�Ŀ��
	
	/**
	 * @param context
	 * @param mDatas:������ļ���������ͼƬ���ļ���
	 * @param dirPath��ͼƬ���ڵ��ļ��е�·��
	 * List�洢����ͼƬ���ļ�������ͼƬ��·�������ͼƬ�Ƚ϶࣬�洢·���Ļ��˷��ڴ�
	 */
	public ImageAdapter(Context context,List<String> mDatas,String dirPath) {		
		this.mDirPath=dirPath;
		mImagePaths=mDatas;
		mInflater=LayoutInflater.from(context);
		Log.d("ImageAdapter", "new ImageAdapter");
		
		WindowManager wm=(WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics=new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mScreenWidth=outMetrics.widthPixels;
	}

	@Override
	public int getCount() {
		return mImagePaths.size();
	}

	@Override
	public Object getItem(int position) {
		return mImagePaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
	    final ViewHolder viewHolder;
		if(convertView==null) {
			convertView=mInflater.inflate(R.layout.item_griview, parent,false);
			
			viewHolder=new ViewHolder();
			viewHolder.mImg=(ImageView) convertView.findViewById(R.id.id_item_image);
			viewHolder.mSelect=(ImageButton) convertView.findViewById(R.id.id_item_select);
			convertView.setTag(viewHolder);//ʹ���´ο���ֱ��getTag;
		}
		else {
			viewHolder=(ViewHolder) convertView.getTag();
		}
		
		//����״̬����ֹ��һ����ͼƬ�Լ���ʾ�Ѿ�ѡ��״̬ͼ��Ӱ��ڶ���ͼƬ����ʾ
		//��Ϊ�ڶ�����item�п����ǵ�һ��item�ĸ��ã������õ�item�е�imageView�����õ��ǵ�һ����ʾ��ͼ��
		//������imageViewȥsetTag�������������
		viewHolder.mImg.setImageResource(R.drawable.pictures_no);
		viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
		viewHolder.mImg.setColorFilter(null);
		
		viewHolder.mImg.setMaxWidth(mScreenWidth/3);
		//��Ϊ�Ѿ��ڲ���������һ����ʾ����ͼƬ�������������������mImg��MaxWidth�����д����ѡ��
		//�����Ϳ����Ż�ImageLoader�л�ȡImageView�Ŀ�ȵ��Ƕδ��루��ĳЩ������Ż�Ч���ǱȽ����Եģ�
		
		ImageLoader.getInstance(3, Type.LIFO).loadImage(
				mDirPath + "/" + mImagePaths.get(position),viewHolder.mImg);
		
		final String filePath=mDirPath+"/"+mImagePaths.get(position);
		
		viewHolder.mImg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d("viewHolder.mImg", "Clicked");
				
				//����Ѿ���ѡ��
				if(mSelectedImg.contains(filePath)) {
					mSelectedImg.remove(filePath);
					viewHolder.mImg.setColorFilter(null);
					viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
				}
				else {
					mSelectedImg.add(filePath);
					viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
					viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
				}
//				notifyDataSetChanged();
				//������ø÷�����ʹ���ڵ����Ļ��ʱ���������������������ֱ����onClick����set�������ñȽϺã��Ͳ�Ҫ���ø÷�����
				//notifyDataSetChanged����ͨ��һ���ⲿ�ķ���������������������ݸı�ʱ��Ҫǿ�Ƶ���getView��ˢ��ÿ��Item������
				//����ʵ�ֶ�̬��ˢ���б�Ĺ���
			}
		});
		
		//ÿ��GridView��״̬�ı�ʱ(�绬��GridView)������ѡ������һ���ļ��У�����Ҫ����getView���ػ���ͼ
		//�������������δ��룬GridView��״̬�ı�ʱ����ѡ������һ���ļ��У�ԭ���Ѿ���ѡ���ͼƬ�Ͳ�����ʾ��ѡ���Ч��
		//����ʵ���ϸ�ͼƬ�Ѿ���ѡ����
		if (mSelectedImg.contains(filePath)) {
			viewHolder.mImg.setColorFilter(Color
					.parseColor("#77000000"));
			viewHolder.mSelect
					.setImageResource(R.drawable.pictures_selected);
		}
		
		return convertView;//���ﷵ�صľ���GridView��itemΪitem_griview.xml�в��ֵĹؼ�
	}
	
	
	private class ViewHolder {
		ImageView mImg;
		ImageButton mSelect;
	}
}
