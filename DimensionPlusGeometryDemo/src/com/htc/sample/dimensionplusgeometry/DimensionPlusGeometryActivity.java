package com.htc.sample.dimensionplusgeometry;

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
//import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.htc.lib1.duallens.DimensionPlusUtility;
import com.htc.lib1.duallens.Geometry;

public class DimensionPlusGeometryActivity extends Activity {
    public static final String EXTRAS_FILE_NAME = "EXTRAS_FILE_NAME";
	final static private String TAG = "DimensionPlusGeometryDemo";
	private static final int REQUESTCODE = 1;
	private String filename = "dimensionPlusGeometrySample.jpg"; // default image copied to root of sdcard
	private ImageView image;
//	private OpenGLRenderer renderer;
	private ImageView origImage;
	private TextView textView;
	
	private File root = Environment.getExternalStorageDirectory();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		image = (ImageView) findViewById(R.id.imageViewMap);
		origImage = (ImageView) findViewById(R.id.imageView);
		textView = (TextView) findViewById(R.id.loadingtext);

		String filepath = root+"/"+filename;
		
		final Intent intent = getIntent();
		if(intent.hasExtra(EXTRAS_FILE_NAME)) {
			filepath = intent.getStringExtra(EXTRAS_FILE_NAME);
			filename = filepath.substring(filepath.lastIndexOf("/")+1);
		}
    	Bitmap bitmap = BitmapFactory.decodeFile(filepath);

    	if(bitmap==null) {
           	loadFromResource(filepath);
           	bitmap = BitmapFactory.decodeFile(filepath);
    	}    	
    	if(bitmap==null) {
        	textView.setText(filename+" not found!");
    	} else {
    		origImage.setImageBitmap(bitmap);
    	}
    	
		// Create a OpenGL view.
//		GLSurfaceView view =  (GLSurfaceView) findViewById(R.id.imageViewMap);				
		// Creating and attaching the renderer.
//		renderer = new OpenGLRenderer();
//		view.setRenderer(renderer);
   	
		exportGeometryFromFile(filepath);
	}

	private void exportGeometryFromFile(String filepath) {
		DimensionPlusUtility.ErrorMessage ret = DimensionPlusUtility.ErrorMessage.NO_ERROR;
		try {
			ret = DimensionPlusUtility.exportGeometryFromFile(this, filepath, exportListener) ;
		} catch (UnsatisfiedLinkError error) {
			Log.e(TAG,error.getMessage());
			textView.setText("Requires HTC One M8");
		}
		if(ret!= DimensionPlusUtility.ErrorMessage.NO_ERROR) {
			Log.e(TAG, " Error:"+ ret);
		}
	}

    private void loadFromResource(String filepath) {
    	InputStream inputStream = null;
    	byte[] data = null;
        try {
            inputStream = getResources().openRawResource(R.raw.dimensionplusgeometrysample);
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

	DimensionPlusUtility.OnFinishListener exportListener = new DimensionPlusUtility.OnFinishListener() {
		@Override
		public void onExportCompleted(Geometry ret) {
			Log.d(TAG,"onExportCompleted");
			if(ret == null){
				Log.e(TAG,"data is null");
			}
			else{
				Log.d(TAG,"recieved data");
				Log.d(TAG,"Geometry vertex length:" +ret.getVertices().array().length);
				Log.d(TAG,"Geometry texture coordinates length:" +ret.getTextureCoordinates().array().length);
				Log.d(TAG,"Geometry indices length:" +ret.getIndices().array().length);

				float[] vertexArray = ret.getVertices().array();
				int vertexArraySize = vertexArray.length;

				// visualize depth data through image view.
				int [] depthData = new int[vertexArraySize/4];
				for(int i=0; i<vertexArraySize/4 ; i++) {
					int aa = (int)(vertexArray[4*i+2] * 255.0f);
					depthData[(vertexArraySize/4-1)-i] = (  aa | aa<<8 | aa<<16 | 255<<24 );
				}
				Bitmap bmp = Bitmap.createBitmap( depthData, 320, 180, Config.ARGB_8888);
				image.setBackgroundColor(Color.WHITE);
				image.setImageBitmap(bmp);
/*
				// visualize depth data through opengl surface view.
				// create a 3d opengl model
				Model model = new Model(1, 1);
				model.setIndices(ret.getIndices().array());
				model.setVertices(ret.getVertices().array());
				model.setTextureCoordinates(ret.getTextureCoordinates().array());
				// Move and rotate the plane.
				model.z = -1.0f;
				model.rz = 0;
				model.ry = 195;
				model.rx = 195;				
//				model.loadBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.demo));
				// Add the plane to the renderer.
				renderer.addMesh(model);
*/

			}
		}
		@Override
		public void onSaveCompleted() {
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:;
            	startFileChooserActivity();
                break;
        }
        return true;
    }

	@Override
	protected void onPause() {
		super.onPause();
	}
	@Override
	protected void onResume() {
		super.onResume();
	}
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }

    public void startFileChooserActivity() {
        final Intent intent = new Intent(this, FileChooserActivity.class);
        startActivityForResult(intent, REQUESTCODE);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUESTCODE) {
            if(resultCode == RESULT_OK) {
            	String filepath = null;
        		if(intent.hasExtra(EXTRAS_FILE_NAME)) {
        			filepath = intent.getStringExtra(EXTRAS_FILE_NAME);
        			filename = filepath.substring(filepath.lastIndexOf("/")+1);
        		}
            	Bitmap bitmap = BitmapFactory.decodeFile(filepath);
            	if(bitmap==null) {
                	textView.setText(filename+" not found!");
            	} else {
            		origImage.setImageBitmap(bitmap);
            		image.setImageBitmap(null);
            	}
        		if(filepath!=null) {
        			exportGeometryFromFile(filepath);
        		}            	
            }
        }
    }

}
