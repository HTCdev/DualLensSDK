package com.htc.sample.dimensionplusgeometry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileChooserActivity extends ListActivity {

    private File root = Environment.getExternalStorageDirectory();	
	private List<String> item;
	private List<String> path;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getFolder(root.getAbsolutePath());
	}

	private void getFolder(String dirPath) {
		item = new ArrayList<String>();
		path = new ArrayList<String>();
		File f = new File(dirPath);
		File[] files = f.listFiles();
		if (!dirPath.equals(root.getAbsolutePath())) {
			item.add("../");
			path.add(f.getParent());
		}
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			path.add(file.getPath());
			if (file.isDirectory()) {
				item.add(file.getName() + "/");
			} else {
				item.add(file.getName());
			}
		}
		ArrayAdapter<String> fileList = new ArrayAdapter<String>(this, R.layout.row, item);
		setListAdapter(fileList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));
		if (file.isDirectory()) {
			if (file.canRead()) {
				getFolder(path.get(position));
			}
		} else {
			returnToDPGeometryActivity(file.getAbsolutePath());
		}
	}
    public void returnToDPGeometryActivity(String filename) {
    	final Intent returnIntent = new Intent();
        returnIntent.putExtra(DimensionPlusGeometryActivity.EXTRAS_FILE_NAME,filename);
        setResult(RESULT_OK,returnIntent);
        finish();
    }
}
