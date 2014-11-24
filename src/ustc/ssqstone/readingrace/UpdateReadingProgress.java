package ustc.ssqstone.readingrace;

import ustc.ssqstone.readingrace.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * @author Administrator
 */
public class UpdateReadingProgress extends Service
{
	private int timeStep;
	private int currentPage;
	private int endPage;
	private InformTimeOutTask informTimeOutTask;
	private ConditionVariable mConditionVariable;
	private final static int NOTIFICATION_ID = R.layout.notification;
	private Handler handler;
	private boolean started = false;
	private BroadcastReceiver screenOffBroadcastReceiver;
	private BroadcastReceiver screenOnBroadcastReceiver;
	private Thread updatingThread;
	private boolean halt = false;

	private class InformTimeOutTask implements Runnable
	{
		@Override
		public void run()
		{
			out: while (currentPage != endPage)
			{
				Message message = new Message();
				message.what = NOTIFICATION_ID;
				handler.sendMessage(message);

				for (int i = 0; i < timeStep; i += (halt ? 0 : 1))
				{
					if (mConditionVariable.block(1000))
					{
						break out;
					}
				}
			}
			UpdateReadingProgress.this.stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		readStatus();

		screenOffBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				halt = true;
			}
		};
		screenOnBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				halt = false;
			}
		};

		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
					case NOTIFICATION_ID:
						updateNotification();

						break;
					default:
						break;
				}
				super.handleMessage(msg);
			}
		};
	}

	@Override
	public void onStart(Intent intent, int startId)
	{
		super.onStart(intent, startId);
		if (intent.hasExtra("begin"))
		{
			currentPage -= 2;
			started = true;
			informTimeOutTask = new InformTimeOutTask();
			updatingThread = new Thread(null, informTimeOutTask,
					"updating notification");
			mConditionVariable = new ConditionVariable(false);
			updatingThread.start();
		}
		if (!intent.hasExtra("stop"))
		{
			startForeground(NOTIFICATION_ID, updateNotification());
		} else
		{
			stopSelf();
		}

		IntentFilter intentFilter;
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(screenOnBroadcastReceiver, intentFilter);

		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffBroadcastReceiver, intentFilter);
	}

	@Override
	public void onDestroy()
	{
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (currentPage == endPage)
		{
			Toast.makeText(getApplicationContext(), "计时完成, 你看完了没?",
					Toast.LENGTH_SHORT).show();
		}
		if (started)
		{
			currentPage -= 1;
			saveStatus();
			mConditionVariable.open();
			handler.removeMessages(NOTIFICATION_ID);
			// mNotificationManager.cancel(NOTIFICATION_ID + 1);
			// mNotificationManager.cancel(NOTIFICATION_ID + 2);
			// mNotificationManager.cancel(NOTIFICATION_ID + 3);
		}

		unregisterReceiver(screenOffBroadcastReceiver);
		unregisterReceiver(screenOnBroadcastReceiver);

		this.stopForeground(true);
		mNotificationManager.cancel(NOTIFICATION_ID);

		super.onDestroy();
	}

	private void readStatus()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("values",
				MODE_PRIVATE);
		timeStep = sharedPreferences.getInt("minute", 0) * 60
				+ sharedPreferences.getInt("second", 30);
		currentPage = sharedPreferences.getInt("current_page", 1);
		endPage = sharedPreferences.getInt("end_page", 0);
	}

	private void saveStatus()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("values",
				MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.remove("current_page");
		editor.putInt("current_page", currentPage);
		editor.commit();
	}

	/**
	 * 创建通知
	 */
	private Notification updateNotification()
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(new long[]{0,800},-1);
		
		Notification notification;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		RemoteViews contentView;
		Intent intent;
		PendingIntent contentIntent;

		if (started)
		{
			currentPage += 1;
			Bitmap icon = generatorPageNumIcon(
					getResIcon(getResources(), R.drawable.icon), currentPage);

			notification = new Notification(R.drawable.icon, "计时已开始",
					System.currentTimeMillis());
			notification.flags = Notification.FLAG_NO_CLEAR;
			contentView = new RemoteViews(this.getPackageName(),
					R.layout.notification);
			contentView.setTextViewText(R.id.text, "ReadingRace: "
					+ currentPage
					+ ((currentPage < endPage) ? (" to " + endPage +"  [running]") : ""));
			contentView.setImageViewBitmap(R.id.icon,
					getResIcon(getResources(), R.drawable.icon));
			notification.contentView = contentView;
			notification.number = currentPage;
			intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			contentIntent = PendingIntent.getActivity(this, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			notification.contentIntent = contentIntent;
			notification.number = currentPage;
			mNotificationManager.notify(NOTIFICATION_ID, notification);
			// notification = new Notification(R.drawable.num_0_g + currentPage
			// % 10, "页码个位", System.currentTimeMillis());
			// notification.flags = Notification.PRIORITY_MAX;
			// contentView = new RemoteViews(this.getPackageName(),
			// R.layout.notification);
			// contentView.setTextViewText(R.id.text, "个位: " + currentPage %
			// 10);
			// contentView.setImageViewBitmap(R.id.icon,
			// getResIcon(getResources(), R.drawable.icon)); //
			// .setImageViewBitmap(R.id.icon, icon);
			// notification.contentView = contentView;
			// intent = new Intent(this, MainActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
			// Intent.FLAG_ACTIVITY_SINGLE_TOP);
			// intent.putExtra("current_page", currentPage);
			// contentIntent = PendingIntent.getActivity(this, 0, intent,
			// PendingIntent.FLAG_UPDATE_CURRENT);
			// notification.contentIntent = contentIntent;
			// mNotificationManager.notify(NOTIFICATION_ID + 3, notification);
			//
			// notification = new Notification(R.drawable.num_0_g + currentPage
			// % 100 / 10, "页码十位", System.currentTimeMillis());
			// notification.flags = Notification.PRIORITY_MAX;
			// contentView = new RemoteViews(this.getPackageName(),
			// R.layout.notification);
			// contentView.setTextViewText(R.id.text, "十位: " + currentPage % 100
			// / 10);
			// contentView.setImageViewBitmap(R.id.icon,
			// getResIcon(getResources(), R.drawable.icon)); //
			// .setImageViewBitmap(R.id.icon, icon);
			// notification.contentView = contentView;
			// intent = new Intent(this, MainActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
			// Intent.FLAG_ACTIVITY_SINGLE_TOP);
			// intent.putExtra("current_page", currentPage);
			// contentIntent = PendingIntent.getActivity(this, 0, intent,
			// PendingIntent.FLAG_UPDATE_CURRENT);
			// notification.contentIntent = contentIntent;
			// mNotificationManager.notify(NOTIFICATION_ID + 2, notification);
			//
			// notification = new Notification(R.drawable.num_0_g + currentPage
			// / 100, "页码百位", System.currentTimeMillis());
			// notification.flags = Notification.PRIORITY_MAX;
			// contentView = new RemoteViews(this.getPackageName(),
			// R.layout.notification);
			// contentView.setTextViewText(R.id.text, "百位: " + currentPage /
			// 100);
			// contentView.setImageViewBitmap(R.id.icon, icon);
			// notification.contentView = contentView;
			// intent = new Intent(this, MainActivity.class);
			// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
			// Intent.FLAG_ACTIVITY_SINGLE_TOP);
			// intent.putExtra("current_page", currentPage);
			// contentIntent = PendingIntent.getActivity(this, 0, intent,
			// PendingIntent.FLAG_UPDATE_CURRENT);
			// notification.contentIntent = contentIntent;
			// mNotificationManager.notify(NOTIFICATION_ID + 1, notification);
			saveStatus();
		} else
		{
			notification = new Notification(R.drawable.icon,
					"计时随时可以启动, 请准备好后点击本通知", System.currentTimeMillis());
			// notification.flags = Notification.FLAG_NO_CLEAR;
			contentView = new RemoteViews(this.getPackageName(),
					R.layout.notification);
			contentView.setTextViewText(R.id.text, "ReadingRace: "
					+ currentPage
					+ ((currentPage < endPage) ? (" to " + endPage) : "")
					+ "[stopped now]");
			contentView.setImageViewBitmap(R.id.icon,
					getResIcon(getResources(), R.drawable.icon)); // .setImageViewBitmap(R.id.icon,
																	// icon);
			notification.contentView = contentView;
			notification.number = -1;
			intent = new Intent(this, UpdateReadingProgress.class);
			intent.putExtra("begin", true);
			contentIntent = PendingIntent.getService(this, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			notification.contentIntent = contentIntent;
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
		return notification;
	}

	/**
	 * 根据id获取一个图片
	 * 
	 * @param res
	 * @param resId
	 * @return
	 */
	private Bitmap getResIcon(Resources res, int resId)
	{
		Drawable icon = res.getDrawable(resId);
		if (icon instanceof BitmapDrawable)
		{
			BitmapDrawable bd = (BitmapDrawable) icon;
			return bd.getBitmap();
		} else
		{
			return null;
		}
	}

	// /**
	// * 在给定的图片的右上角加上页码。数量用蓝色表示
	// *
	// * @param icon
	// * 给定的图片
	// * @return 带页码的图片
	// */
	private Bitmap generatorPageNumIcon(Bitmap icon, int num)
	{
		// 初始化画布
		int iconSize = (int) getResources().getDimension(
				android.R.dimen.app_icon_size);
		Bitmap iconWithPageNum = Bitmap.createBitmap(iconSize, iconSize,
				Config.ARGB_8888);
		Canvas canvas = new Canvas(iconWithPageNum);

		// 拷贝图片
		Paint iconPaint = new Paint();
		iconPaint.setDither(true);// 防抖动
		iconPaint.setFilterBitmap(true);// 用来对Bitmap进行滤波处理，这样会有抗锯齿的效果
		Rect src = new Rect(0, 0, icon.getWidth(), icon.getHeight());
		Rect dst = new Rect(0, 0, iconSize, iconSize);
		canvas.drawBitmap(icon, src, dst, iconPaint);

		// 启用抗锯齿和使用设备的文本字距
		Paint countPaint = new Paint(Paint.ANTI_ALIAS_FLAG
				| Paint.DEV_KERN_TEXT_FLAG);
		countPaint.setColor(Color.RED);
		countPaint.setTextSize(20f);
		countPaint.setTypeface(Typeface.DEFAULT_BOLD);
		canvas.drawText(String.valueOf(num), iconSize - 40, 20, countPaint);

		return iconWithPageNum;
	}
}