package com.htc.sample.duallens;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.lib1.duallens.Constants;
import com.htc.lib1.duallens.DualLens;

public class DualLensActivity extends Activity {
    final static public String EXTRAS_FILE_NAME = "EXTRAS_FILE_NAME";
	final static private String TAG = "DualLensActivity";
	final static private int REQUESTCODE = 1;
	String filename = "duallenssample.jpg"; // default image copied to root of sdcard

	private Button button;
	private TextView errorText; 
	private ImageView image;
	private ImageView origImage;
	private DualLens dualLens;

	private int mStrength = 0;	
	private int [] mColorBar = null;
	
	private String root = Environment.getExternalStorageDirectory().toString();
	private String filepath;
	private boolean dualLensPrepared;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		dualLensPrepared = false;		
		image = (ImageView) findViewById(R.id.imageViewMap);
		origImage = (ImageView) findViewById(R.id.imageView);
		button = (Button) findViewById(R.id.button1);
		errorText = (TextView) findViewById(R.id.errorText); 
		
		filepath = root + "/"+filename;

		final Intent intent = getIntent();
		if(intent.hasExtra(EXTRAS_FILE_NAME)) {
			filepath = intent.getStringExtra(EXTRAS_FILE_NAME);
			filename = filepath.substring(filepath.lastIndexOf("/")+1);
		}
		
		loadFromResource(filepath); 
		Bitmap bitmap = BitmapFactory.decodeFile(filepath);
		
    	if(bitmap==null) {
        	Log.e(TAG,filename+" not found!");
        	errorText.setText(filename+" not found!");
    	} else {
    		origImage.setImageBitmap(bitmap);
    		addListenerOnButton();
    	}     
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.files:
            	startFileChooserActivity();
                break;
        }
        return true;
    }

	public void addListenerOnButton() {
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(dualLensPrepared) {
				    mStrength+=10;
				    if(mStrength>100) {
				    	mStrength = 0;
				    }
				    button.setText("strength "+mStrength);
				    dualLensPrepared=false;
					dualLens.setStrength(mStrength);
					try {
						dualLens.calculateBokeh();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void sleep(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void drawMask() {
		DualLens.Holder<byte[]> buf = dualLens.new Holder<byte[]>();
		DualLens.DataInfo datainfo = dualLens.getStrengthMap(buf);
	    int [] depthData = new int[datainfo.width * datainfo.height];
	    int leftByte;
        for(int i = 0; i < datainfo.width * datainfo.height; i++) {
            leftByte = buf.value[i] & 0x000000ff;
            depthData[i] = mColorBar[leftByte*500];
        }
	    Bitmap bmp = Bitmap.createBitmap( depthData, datainfo.width, datainfo.height, Config.ARGB_8888);
	    image.setImageBitmap(bmp);
	    image.setBackgroundColor(Color.WHITE);
	}
	
	
    private void loadFromResource(String filepath) {
    	
    	InputStream inputStream = null;
    	byte[] data = null;
        try {
            inputStream = getResources().openRawResource(R.raw.duallenssample);
            data = new byte[inputStream.available()];
            while (inputStream.read(data) != -1) {}
        } catch(IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(filepath);
			outStream.write(data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(outStream!=null) {
				try {
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
    }

	@Override
	protected void onResume() {
		super.onResume();
		try{
			Bitmap tmp = BitmapFactory.decodeStream(this.getAssets().open("coloridx.bmp"));
			mColorBar = new int [tmp.getHeight() * tmp.getWidth()];
			for (int i = 0; i < tmp.getHeight(); i++){
				for(int j = 0; j < tmp.getWidth(); j++){
					mColorBar[j+i*tmp.getWidth()] = tmp.getPixel(j, i);
				}
			}
		} catch (IOException e){
			Log.e("DualLensActivity", "IOException!");
		}
		if(filepath!=null) {
			createDualLensFromFile(filepath);
		}
	}
	private void createDualLensFromFile(String filepath) {
		dualLensPrepared = false;
		dualLens = new DualLens(DualLensActivity.this, filepath);
		dualLens.setOnCompletionListener(new DualLens.OnCompletionListener() {
			@Override
			public void onCompletion(DualLens arg0, int event, int extra, String path) {
				switch(event) {
					case Constants.TASK_COMPLETED_PREPARE:
						dualLensPrepared = false;
						if(extra == 0) {
							dualLens.setStrength(mStrength);
							try {
								dualLens.calculateBokeh();
							} catch (IllegalStateException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						break;
					case Constants.TASK_COMPLETED_BOKEH:
						dualLensPrepared = true;
			        	drawMask();
						break;
				}
			}
		});
		try{
			dualLens.prepare();
		} catch (IOException e){
			Log.w(TAG,"dualLens.prepare() failed with "+filepath+": "+e.getMessage());
		} catch (IllegalStateException e){
			Log.w(TAG,"dualLens.prepare() failed with "+filepath+": "+e.getMessage());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(dualLens!=null) {
			dualLensPrepared = false;
			dualLens.release();
			dualLens = null;
			filepath=null;
		}
	}
    public void startFileChooserActivity() {
        final Intent intent = new Intent(this, FileChooserActivity.class);
        startActivityForResult(intent, REQUESTCODE);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUESTCODE) {
            if(resultCode == RESULT_OK) {
        		if(intent.hasExtra(EXTRAS_FILE_NAME)) {
        			filepath = intent.getStringExtra(EXTRAS_FILE_NAME);
        			filename = filepath.substring(filepath.lastIndexOf("/")+1);
        		}
            	Bitmap bitmap = BitmapFactory.decodeFile(filepath);
            	if(bitmap==null) {
            		Log.e(TAG,filename+" not found!");
                	errorText.setText(filename+" not found!");
            	} else {
            		origImage.setImageBitmap(bitmap);
            		image.setImageBitmap(null);
            	}
        		if(filepath!=null) {
        			createDualLensFromFile(filepath);
        		}            	
            }
        }
    }


}
