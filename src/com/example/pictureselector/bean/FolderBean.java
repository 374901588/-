package com.example.pictureselector.bean;

public class FolderBean {
	/**
	 * ��ǰ�ļ��е�·��
	 */
	private String dir;
	private String firstImgPath;
	/**
	 * ��ǰ�ļ��е�����
	 */
	private String name;
	/**
	 * ��ǰ�ļ�����ͼƬ������
	 */
	private int count;
	
	public FolderBean() {
		
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
		
		int lastIndexOf=this.dir.lastIndexOf("/");
		this.name=this.dir.substring(lastIndexOf+1);
	}

	public String getFirstImgPath() {
		return firstImgPath;
	}

	public void setFirstImgPath(String firstImgPath) {
		this.firstImgPath = firstImgPath;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public String getName() {
		return name;
	}
}
