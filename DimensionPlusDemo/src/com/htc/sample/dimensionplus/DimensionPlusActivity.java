package com.htc.sample.dimensionplus;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.htc.lib1.duallens.DimensionPlusView;

public class DimensionPlusActivity extends Activity {
	final private String TAG = "DimensionPlusActivity";
	public static final String EXTRAS_FILE_NAME = "EXTRAS_FILE_NAME";
	private static final int REQUESTCODE = 1;
	
	private DimensionPlusView dimensionPlusView;
    
	String filename = "dimensionPlusSample.jpg"; // default image copied to root of sdcard
	private String root = Environment.getExternalStorageDirectory().toString();	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dimensionPlusView = (DimensionPlusView) findViewById(R.id.dimensionPlusView);

		String filepath = root+"/"+filename;

		final Intent intent = getIntent();
		if(intent.hasExtra(EXTRAS_FILE_NAME)) {
			filepath = intent.getStringExtra(EXTRAS_FILE_NAME);
			filename = filepath.substring(filepath.lastIndexOf("/")+1);
		}

		boolean filenotread = false;
        try {
            dimensionPlusView.loadDimensionPlusFile(filepath);
	    } catch (IOException e) {
	    	if(e.getMessage().contains("READFILE_ERROR")){	    		
	    		filenotread = true;
	    	} else {
	    		Toast.makeText(getBaseContext(), filename + " not supported", Toast.LENGTH_LONG).show();
	    		return;
	    	}
	    } catch (ArrayIndexOutOfBoundsException ae) {
	    	filenotread = true;
	    }
        if(filenotread) {
        	loadFromResource(filepath);
        }
        try {
            dimensionPlusView.loadDimensionPlusFile(filepath);
	    } catch (IOException e) {
	    	if(e.getMessage().contains("READFILE_ERROR")){	    		
	    		Toast.makeText(getBaseContext(), filename + " not found", Toast.LENGTH_LONG).show();
	    	} else {
	    		Toast.makeText(getBaseContext(), filename + " not supported", Toast.LENGTH_LONG).show();
	    		return;
	    	}
	    }
    }

    private void loadFromResource(String filepath) {
    	InputStream inputStream = null;
    	byte[] data = null;
        try {
            inputStream = getResources().openRawResource(R.raw.dimensionplussample);
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

	private void loadDimensionPlusFile(final String filepath) {
		try {
			dimensionPlusView.loadDimensionPlusFile(filepath);
	    } catch (Exception e) {
        	Toast.makeText(getBaseContext(),filename+"\n not found or unsupported", Toast.LENGTH_LONG).show();
	    }
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
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
    protected void onResume() {
        super.onResume();
        dimensionPlusView.onResume();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(mSenserEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dimensionPlusView.onPause();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(mSenserEventListener);
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        dimensionPlusView.release();
    }

    private SensorEventListener mSenserEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // gyro event
            float gravity1 = event.values[0];
            float gravity2 = event.values[1];

            int orientation = ((WindowManager) DimensionPlusActivity.this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                    .getRotation();
            switch (orientation) {
            case Surface.ROTATION_0:
                dimensionPlusView.gyro(-gravity2, gravity1);
                break;
            case Surface.ROTATION_90:
                dimensionPlusView.gyro(-gravity1, -gravity2);
                break;
            case Surface.ROTATION_180:
                dimensionPlusView.gyro(gravity2, -gravity1);
                break;
            case Surface.ROTATION_270:
                dimensionPlusView.gyro(gravity1, gravity2);
                break;
            default:
                dimensionPlusView.gyro(-gravity2, gravity1);
                break;
            }
            return;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

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
        		if(filepath!=null) {
        			loadDimensionPlusFile(filepath);
        		}            	
            }
        }
    }
}
