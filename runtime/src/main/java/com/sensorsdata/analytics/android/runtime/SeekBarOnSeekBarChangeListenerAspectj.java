package com.sensorsdata.analytics.android.runtime;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.SeekBar;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataThreadPool;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2016/11/29
 * SeekBar.OnSeekBarChangeListener 事件
 */

@Aspect
public class SeekBarOnSeekBarChangeListenerAspectj {
    private final static String TAG = SeekBarOnSeekBarChangeListenerAspectj.class.getCanonicalName();

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStartTrackingTouch(android.widget.SeekBar))")
    public void onStartTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStartTrackingTouch");
    }

    @After("execution(* android.widget.SeekBar.OnSeekBarChangeListener.onStopTrackingTouch(android.widget.SeekBar))")
    public void onStopTrackingTouchMethod(JoinPoint joinPoint) throws Throwable {
        actionAOP(joinPoint, "onStopTrackingTouch");
    }

    private void actionAOP(final JoinPoint joinPoint, final String action) {
        SensorsDataThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //关闭 AutoTrack
                    if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                        return;
                    }

                    //$AppClick 被过滤
                    if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                        return;
                    }

                    //基本校验
                    if (joinPoint == null || joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                        return;
                    }

                    //获取被点击的 View
                    View view = (View) joinPoint.getArgs()[0];
                    if (view == null) {
                        return;
                    }

                    //获取所在的 Context
                    Context context = view.getContext();
                    if (context == null) {
                        return;
                    }

                    //将 Context 转成 Activity
                    Activity activity = AopUtil.getActivityFromContext(context, view);

                    //Activity 被忽略
                    if (activity != null) {
                        if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                            return;
                        }
                    }

                    //View 被忽略
                    if (AopUtil.isViewIgnored(view)) {
                        return;
                    }

                    SeekBar seekBar = (SeekBar) view;

                    JSONObject properties = new JSONObject();

                    //ViewId
                    String idString = AopUtil.getViewId(view);
                    if (!TextUtils.isEmpty(idString)) {
                        properties.put(AopConstants.ELEMENT_ID, idString);
                    }

                    //Action
//                    properties.put(AopConstants.ELEMENT_ACTION, action);

                    //$screen_name & $title
                    if (activity != null) {
                        properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                        String activityTitle = AopUtil.getActivityTitle(activity);
                        if (!TextUtils.isEmpty(activityTitle)) {
                            properties.put(AopConstants.TITLE, activityTitle);
                        }
                    }

                    properties.put(AopConstants.ELEMENT_TYPE, "SeekBar");

                    //Content
                    properties.put(AopConstants.ELEMENT_CONTENT, String.valueOf(seekBar.getProgress()));

                    //fragmentName
                    AopUtil.getFragmentNameFromView(seekBar, properties);

                    //获取 View 自定义属性
                    JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
                    if (p != null) {
                        AopUtil.mergeJSONObject(p, properties);
                    }

                    SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
                } catch (Exception e) {
                    e.printStackTrace();
                    SALog.i(TAG, action + " AOP ERROR: " + e.getMessage());
                }
            }
        });
    }
}
