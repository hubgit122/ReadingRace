package ustc.ssqstone.readingrace;

import ustc.ssqstone.readingrace.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author Administrator
 * 
 */
public class MainActivity extends Activity
{
	private EditText editText1;
	private EditText editText2;
	private EditText editText3;
	private EditText editText4;
	private Button startButton;
	private Button stopButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.initView();
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		// stopService(new Intent("ustc.ssqstone.readingrace.UpdateReadingProgress"));
	}
	
	private void initView()
	{
		editText1 = (EditText) findViewById(R.id.editText1);
		editText2 = (EditText) findViewById(R.id.editText2);
		editText3 = (EditText) findViewById(R.id.editText3);
		editText4 = (EditText) findViewById(R.id.editText4);
		
		startButton = (Button) super.findViewById(R.id.b_begin);
		stopButton = (Button) super.findViewById(R.id.b_stop);
		
		startButton.setOnClickListener(clickListener);
		stopButton.setOnClickListener(clickListener);
	}
	
	private OnClickListener clickListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			switch (v.getId())
			{
			case R.id.b_begin:
				if (checkData())
				{
					saveData();
					startService(new Intent("ustc.ssqstone.readingrace.UpdateReadingProgress"));
					finish();
				}
				break;
			
			case R.id.b_stop:
				// startService(new Intent("ustc.ssqstone.readingrace.UpdateReadingProgress").putExtra("stop", true));
				
				stopService(new Intent("ustc.ssqstone.readingrace.UpdateReadingProgress"));
				finish();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onPause()
	{
		super.onPause();
		saveData();
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		editText1.setText(Integer.valueOf(intent.getIntExtra("current_page", Integer.valueOf(editText1.getText().toString()))));
		saveData();
		super.onNewIntent(intent);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		restoreData();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		saveData();
	}
	
	private void saveData()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("values", MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		
		editor.putInt("current_page", Integer.parseInt(editText1.getText().toString()));
		editor.putInt("end_page", Integer.parseInt(editText2.getText().toString()));
		editor.putInt("minute", Integer.parseInt(editText3.getText().toString()));
		editor.putInt("second", Integer.parseInt(editText4.getText().toString()));
		editor.commit();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		restoreData();
	}
	
	private void restoreData()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("values", MODE_PRIVATE);
		
		editText1.setText(Integer.valueOf(sharedPreferences.getInt("current_page", 1)).toString());
		editText2.setText(Integer.valueOf(sharedPreferences.getInt("end_page", 0)).toString());
		editText3.setText(Integer.valueOf(sharedPreferences.getInt("minute", 0)).toString());
		editText4.setText(Integer.valueOf(sharedPreferences.getInt("second", 30)).toString());
	}
	
	private boolean checkData()
	{
		return (Integer.parseInt(editText1.getText().toString()) > 0) && ((Integer.parseInt(editText3.getText().toString()) > 0) || (Integer.parseInt(editText4.getText().toString()) > 0));
	}
}
