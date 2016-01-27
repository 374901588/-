package com.example.pictureselector;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pictureselector.ListImageDirPopupWindow.OnDirSelectedListener;
import com.example.pictureselector.bean.FolderBean;
import com.example.pictureselector.bean.ImageAdapter;

public class MainActivity extends Activity {	
	private static final int DATA_LOADED=0x110;
	
	private GridView mGridView;
	private List<String> mImgs;
	private ImageAdapter mImgAdapter;

	private RelativeLayout mBottonLy;
	private TextView mDirName;
	private TextView mDirCount;

	private File mCurrentDir;
	private int mMaxCount;

	private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

	private ProgressDialog mProgressDialog;
	
	private ListImageDirPopupWindow mPopupWindow;
	
	private Handler mHandler=new Handler() {
		public void handleMessage(android.os.Message msg) {
			if(msg.what==DATA_LOADED) {
				mProgressDialog.dismiss();
				
				//�����ݵ�View��
				data2View();
				
				initPopuWindow();
				
				Log.d("����3", "mHandler");
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		initDatas();
		initEvent();
	}

	protected void data2View() {
		if(mCurrentDir==null) {
			Toast.makeText(this, "δɨ�赽�κ�ͼƬ", Toast.LENGTH_SHORT).show();
			return;
		}
		
		mImgs=Arrays.asList(mCurrentDir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				if(filename.endsWith(".jpg")||filename.endsWith(".jpeg")||filename.endsWith(".png"))
					return true;
				return false;
			}
		}));
		mImgAdapter=new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
		mGridView.setAdapter(mImgAdapter);
		
		mDirCount.setText(""+mMaxCount);
		mDirName.setText(mCurrentDir.getName());
	}

	
	private void initEvent() {
		mBottonLy.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);//����popupWindow��ʾ���������Ķ���
				mPopupWindow.showAsDropDown(mBottonLy, 0, 0);//������ʾPopupWindow��λ��λ��ָ��View�����·���x,y��ʾ����ƫ����
				
				lightOff();
			}
		});
	}
	
	private void initPopuWindow() {
		mPopupWindow=new ListImageDirPopupWindow(this, mFolderBeans);
		
		//��popupWindow���� ʱ�򣬻�����Ļ�䰵����Ч������Ҫ����������popupWindow��ʧ��ʱ����¼���ʹ��Ļ���ԭ��������
		mPopupWindow.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				lightOn();
			}
		});
		
		mPopupWindow.setOnDirSelectedListener(new OnDirSelectedListener() {
			@Override
			public void onSeleted(FolderBean folderBean) {
				mCurrentDir=new File(folderBean.getDir());
				
				//����mImgsÿ�ζ��Ǹ��������ļ���������ͼƬȻ����GridView���������У�
				//�����������ļ�������ɾͼƬ֮��������popupWindow�д򿪸��ļ������ܹ�ˢ�����ݵ�
				//������������ͼƬ���ļ������ڳ���һ������ʱ��ɨ����ɵģ�
				//����֮���������������������ļ��������ļ���������ͼƬ�ǲ��ᱻɨ��ģ�������������
				//������δ�رճ����ʱ��ɾ���Ѿ�ɨ��ĵ����ļ��У�Ȼ���ٳ����д�PopupWindow��ѡ����ļ���(��Ϊ��ʱListView�����ݻ�ûˢ�����Ի���ʱ������PopupWindow��)�����쳣�˳�
				mImgs=Arrays.asList(mCurrentDir.list(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String filename) {
						if(filename.endsWith(".jpg")||filename.endsWith(".jpeg")||filename.endsWith(".png"))
							return true;
						return false;
					}
				}));				
				mImgAdapter=new ImageAdapter(MainActivity.this, mImgs, folderBean.getDir());
				mGridView.setAdapter(mImgAdapter);
				
				mDirCount.setText(mImgs.size()+"");
				mDirName.setText(folderBean.getName());
				
				mPopupWindow.dismiss();
				
				//�����������ں�̨��Ȼ���ڳ�����Ѿ����ڵ�Ŀ¼������ɾͼƬ���ؽ�����GridView��ʾ�������ǲ�����µģ�
				//������´򿪸�Ŀ¼����Ӧ���ļ���GridView��ʾ���ݻ���£�����PopupWindow�е�ListView���ļ�����ͼƬ�������������
				//����Ϊ�˸���ListView�����ݣ���Ҫ��������Ĵ���
				//����ΪListDirAdapter�������е�getView������ListViewһ���б䶯�ͻ����getView�ػ棩������ListView���ļ�����ͼƬ��������
				//���Բ���Ҫ�������ListDirAdapterʵ����notifyDataSetChanged����ˢ������
				for(FolderBean fb:mFolderBeans) {
					Log.d("����2", fb.getDir().substring(fb.getDir().lastIndexOf("/")+1)+" "+mCurrentDir.getName());
					if(fb.getDir().substring(fb.getDir().lastIndexOf("/")+1).equals(mCurrentDir.getName())) {
						if(fb.getCount()!=mImgs.size())
							fb.setCount(mImgs.size());
					}
				}
				
				for(int i=0;i<mFolderBeans.size();i++)
				Log.d("����", mFolderBeans.get(i).getName()+""+mFolderBeans.get(i).getCount());
			}
		});
	}
	
	/**
	 * ��������䰵
	 */
	private void lightOff() {
		WindowManager.LayoutParams lp=getWindow().getAttributes();//Attributes-����
		lp.alpha=0.3f;
		getWindow().setAttributes(lp);
	}
	
	/**
	 * �����������
	 */
	private void lightOn() {
		WindowManager.LayoutParams lp=getWindow().getAttributes();//Attributes-����
		lp.alpha=1.0f;
		getWindow().setAttributes(lp);
	}

	/**
	 * ����ContentProviderɨ���ֻ��е�����ͼƬ
	 */
	private void initDatas() {
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "��ǰ�洢�������ã�", Toast.LENGTH_SHORT).show();
			return;
		}

		mProgressDialog = ProgressDialog.show(this, null, "Loading...");

		new Thread() {
			public void run() {			
				Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				//Media.EXTERNAL_CONTENT_URI����The content:// style URI for the "primary" external storage volume��primary��ԭʼ�ģ���һλ
				//MediaStore�������androidϵͳ�ṩ��һ����ý�����ݿ�
				
				ContentResolver cr = MainActivity.this.getContentResolver();//�����ṩ��

				//������Ҫע��"=? or "�Ŀո��ܶ�
				Cursor cursor=cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE
						+ "=? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
						new String[] { "image/jpeg", "image/png" },
						MediaStore.Images.Media.DATE_MODIFIED);//���һ��������ʽ����ͼƬ��������Ϊ����
				
				Set<String> mDirPaths=new HashSet<String>();//���ڴ洢����ͼƬ���ļ��е�·��
				
				while(cursor.moveToNext()) {
					String path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
					File parentFile=new File(path).getParentFile();
					if(parentFile==null) continue;//������Ҫ�ж�һ�£���ʱ��parentFile�����Ϊnull���������Ȼ��֪������ԭ���п�������ΪͼƬ�����ص�ԭ��
					
					String dirPath=parentFile.getAbsolutePath();//�õ�����·��
					
					if(mDirPaths.contains(dirPath)) continue;//��ֹ�ظ�������ͬ�ļ����µ�ͼƬ
					else {
						mDirPaths.add(dirPath);
						FolderBean folderBean=new FolderBean();
						folderBean.setDir(dirPath);
						folderBean.setFirstImgPath(path);
						
						if(parentFile.list()==null) continue;
						//parentFile.list()����������ļ��������Ŀ¼�е��ļ������ַ������顣�������ļ�����һ��Ŀ¼������ǿյ�
						
						int picSize=parentFile.list(new FilenameFilter() {
							//���ù��ˣ���ֹ��ͼƬ������
							@Override
							public boolean accept(File dir, String filename) {
								if(filename.endsWith(".jpg")||filename.endsWith(".jpeg")||filename.endsWith(".png"))
									return true;
							return false;
							}
						}).length;
						
						folderBean.setCount(picSize);
						
						mFolderBeans.add(folderBean);
						
						if(picSize>mMaxCount) {
							mMaxCount=picSize;
							mCurrentDir=parentFile;
						}
					}		
				}
				cursor.close();
				
				//֪ͨHandlerɨ��ͼƬ���
				mHandler.sendEmptyMessage(DATA_LOADED);
			}
		}.start();
	}

	private void initView() {
		mGridView = (GridView) findViewById(R.id.id_gridview);
		mBottonLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
		mDirName = (TextView) findViewById(R.id.id_dir_name);
		mDirCount = (TextView) findViewById(R.id.id_dir_count);
	}
}
