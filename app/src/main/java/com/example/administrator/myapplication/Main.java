package com.example.administrator.myapplication;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public class Main implements IXposedHookLoadPackage {
	private final String WECHAT_PACKAGE = "com.tencent.mm";
	private final String ALIPAY_PACKAGE = "com.eg.android.AlipayGphone";
	private boolean WECHAT_PACKAGE_ISHOOK = false;
	private boolean ALIPAY_PACKAGE_ISHOOK = false;
	
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
			throws Throwable {
		if (lpparam.appInfo == null || (lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM |
                ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
            return;
        }
		final String packageName = lpparam.packageName;
        final String processName = lpparam.processName;
        if (WECHAT_PACKAGE.equals(packageName)) {
    		try {
                XposedHelpers.findAndHookMethod(ContextWrapper.class, "attachBaseContext", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Context context = (Context) param.args[0];
                        ClassLoader appClassLoader = context.getClassLoader();
                        if(WECHAT_PACKAGE.equals(processName) && !WECHAT_PACKAGE_ISHOOK){
                        	WECHAT_PACKAGE_ISHOOK=true;
                        	//注册广播
                        	StartWechatReceived stratWechat=new StartWechatReceived();
                    		IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction("com.payhelper.wechat.start");
                            context.registerReceiver(stratWechat, intentFilter);
                        	XposedBridge.log("handleLoadPackage: " + packageName);
                        	Toast.makeText(context, "获取到wechat=>>classloader", Toast.LENGTH_LONG).show();
                        	new WechatHook().hook(appClassLoader,context);
                        }
                    }
                });
            } catch (Throwable e) {
                XposedBridge.log(e);
            }
        }else if(ALIPAY_PACKAGE.equals(packageName)){   
    		try {
                XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Context context = (Context) param.args[0];
                        ClassLoader appClassLoader = context.getClassLoader();
                        if(ALIPAY_PACKAGE.equals(processName) && !ALIPAY_PACKAGE_ISHOOK){
                        	ALIPAY_PACKAGE_ISHOOK=true;
                        	//注册广播
                        	StartAlipayReceived startAlipay=new StartAlipayReceived();
                    		IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction("com.payhelper.alipay.start");
                            context.registerReceiver(startAlipay, intentFilter);
                        	XposedBridge.log("handleLoadPackage: " + packageName);
                        	Toast.makeText(context, "获取到alipay=>>classloader", Toast.LENGTH_LONG).show();
                        	new AliPayHook().hook(appClassLoader,context);
                        }
                    }
                });
    		}catch (Throwable e) {
                XposedBridge.log(e);
            }
        }
	}
	
	
    class StartWechatReceived extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        	//XposedBridge.log("启动微信Activity");
        	try {
				Intent intent2=new Intent(context, XposedHelpers.findClass("com.tencent.mm.plugin.collect.ui.CollectCreateQRCodeUI", context.getClassLoader()));
				intent2.putExtra("mark", intent.getStringExtra("mark"));
				intent2.putExtra("money", intent.getStringExtra("money"));
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent2);
				//XposedBridge.log("启动微信成功");
			} catch (Exception e) {
				XposedBridge.log("启动微信失败?"+e.getMessage());
			}
        }
    }
    //自定义接受订单�?�知广播
    class StartAlipayReceived extends BroadcastReceiver {
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		//XposedBridge.log("启动支付宝Activity");
    		Intent intent2=new Intent(context, XposedHelpers.findClass("com.alipay.mobile.payee.ui.PayeeQRSetMoneyActivity", context.getClassLoader()));
    		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		intent2.putExtra("mark", intent.getStringExtra("mark"));
    		intent2.putExtra("money", intent.getStringExtra("money"));
    		context.startActivity(intent2);
    	}
    }
}
