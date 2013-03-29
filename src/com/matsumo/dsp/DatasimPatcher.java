package com.matsumo.dsp;

import java.lang.reflect.Method;
import java.util.Iterator;

import android.graphics.Color;
import android.telephony.ServiceState;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class DatasimPatcher implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	private static XSharedPreferences pref;

	@Override
	public void initZygote(StartupParam startupParam) {
		pref = new XSharedPreferences(DatasimPatcher.class.getPackage().getName());
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
//		XposedBridge.log("Loaded app: " + lpparam.packageName);

		//----
		// the status bar belongs to package com.android.systemui
/*		if (lpparam.packageName.equals("com.android.systemui")){
			XposedBridge.log("DatasimPatcher:hooked=com.android.systemui");
			try {
				Method updateClock =
					Class.forName("com.android.systemui.statusbar.policy.Clock", false, lpparam.classLoader)
					.getDeclaredMethod("updateClock");
				XposedBridge.hookMethod(updateClock, new XC_MethodHook(XCallback.PRIORITY_DEFAULT) {
					@Override
					protected void afterHookedMethod(
							MethodHookParam param) throws Throwable {
						// then change text and color
						try {
							TextView tv = (TextView) param.thisObject;
							String text = tv.getText().toString();
							tv.setText(text + " :)");
							tv.setTextColor(Color.RED);
						} catch (Exception e) {
							// replacing did not work.. but no reason to crash the VM! Log the error and go on.
							XposedBridge.log(e);
						}
					}
				});
			} catch (Exception e) {
				XposedBridge.log(e);
			}
		}*/
		//----

		if (!lpparam.packageName.equals("com.android.providers.telephony"))
			return;

//		XposedBridge.log("DatasimPatcher:we are in telephony!");

		pref.reload();
		final int mode = Integer.parseInt(pref.getString("mode", "0"));
//		XposedBridge.log("DatasimPatcher:mode="+mode);
		// http://bl.oov.ch/2012/01/b-mobile-sim.html
		// https://github.com/android/platform_frameworks_base/blob/jb-release/telephony/java/com/android/internal/telephony/gsm/GsmServiceStateTracker.java#L1229
		findAndHookMethod("com.android.internal.telephony.gsm.GsmServiceStateTracker",
							lpparam.classLoader,
							"regCodeToServiceState",
							int.class,
							new XC_MethodHook() {
/*			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called before the clock was updated by the original method
			}*/
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				// this will be called after the clock was updated by the original method
				final int code = (Integer)param.args[0];
				boolean hook = false;
				switch(mode){
				case 0:
					if(code == 3 || code == 13) hook = true;
					break;
				case 1:
					if(code == 2 || code == 12) hook = true;
					break;
				case 2:
					if(code == 0 || code == 10) hook = true;
					break;
				case 99:
					if(code == 2 || code == 3 || code == 12 || code == 13) hook = true;
					break;
				}
				if(hook){
//					XposedBridge.log("DatasimPatcher:patched="+code);
					param.setResult(ServiceState.STATE_IN_SERVICE);
				}
			}
		});

		final int emergency = Integer.parseInt(pref.getString("emergency", "0"));
//		XposedBridge.log("DatasimPatcher:emergency="+emergency);
		if(emergency > 0){
			// http://bl.oov.ch/2012/01/android-sim_19.html
			// https://github.com/android/platform_frameworks_base/blob/jb-release/telephony/java/com/android/internal/telephony/gsm/GsmServiceStateTracker.java#L603
			findAndHookMethod("com.android.internal.telephony.gsm.GsmServiceStateTracker",
					lpparam.classLoader,
					"handlePollStateResult",
					int.class,
					Object.class,
					new XC_MethodHook() {
/*				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					// this will be called before the clock was updated by the original method
				}*/
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					// this will be called after the clock was updated by the original method
					//TODO
				}
			});
		}
	}
}
