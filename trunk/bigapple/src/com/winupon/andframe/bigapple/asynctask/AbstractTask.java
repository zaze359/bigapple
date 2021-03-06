/* 
 * @(#)AbstractTask.java    Created on 2013-2-17
 * Copyright (c) 2013 ZDSoft Networks, Inc. All rights reserved.
 * $Id$
 */
package com.winupon.andframe.bigapple.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import com.winupon.andframe.bigapple.asynctask.callback.AsyncTaskFailCallback;
import com.winupon.andframe.bigapple.asynctask.callback.AsyncTaskResultNullCallback;
import com.winupon.andframe.bigapple.asynctask.callback.AsyncTaskSuccessCallback;
import com.winupon.andframe.bigapple.asynctask.helper.Result;
import com.winupon.andframe.bigapple.bitmap.CompatibleAsyncTask;
import com.winupon.andframe.bigapple.utils.log.LogUtils;

/**
 * 耗时任务类的基类，采用了监听器设计模式，模板方法,注意这个任务类的实例只能在UI线程中被创建
 * 
 * @author xuan
 * @version $Revision: 1.0 $, $Date: 2013-2-17 下午4:32:49 $
 */
public abstract class AbstractTask<T> extends CompatibleAsyncTask<Object, Integer, Result<T>> {
    protected final Context context;
    private boolean isShowProgressDialog = true;// 默认显示
    private boolean isCancel = true;// 默认可以取消

    private String progressTitle = "请稍后...";// 默认提示信息

    private AsyncTaskSuccessCallback<T> asyncTaskSuccessCallback;// 成功回调
    private AsyncTaskFailCallback<T> asyncTaskFailCallback;// 失败回调
    private AsyncTaskResultNullCallback asyncTaskResultNullCallback;// result返回null的回调

    private ProgressDialog progressDialog;// 提示框

    public AbstractTask(Context context) {
        this.context = context;
    }

    public AbstractTask(Context context, boolean isShowProgressDialog) {
        this.context = context;
        this.isShowProgressDialog = isShowProgressDialog;
    }

    @Override
    protected void onPreExecute() {
        if (isShowProgressDialog) {
            if (null == progressDialog) {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(progressTitle);
                progressDialog.setCancelable(isCancel);
            }
            progressDialog.show();
        }
    }

    @Override
    protected Result<T> doInBackground(Object... objects) {
        Result<T> result = null;
        try {
            result = doHttpRequest(objects);
        }
        catch (Exception e) {
            LogUtils.e("", e);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Result<T> result) {
        if (isShowProgressDialog) {// 先隐藏提示框
            progressDialog.dismiss();
        }

        if (null == result) {
            if (null != asyncTaskResultNullCallback) {
                asyncTaskResultNullCallback.resultNullCallback();
            }
            return;
        }

        if (result.isSuccess()) {
            if (null != asyncTaskSuccessCallback) {
                asyncTaskSuccessCallback.successCallback(result);
            }
            else {
                if (!TextUtils.isEmpty(result.getMessage())) {
                    Toast.makeText(context, result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        else {
            if (null != asyncTaskFailCallback) {
                asyncTaskFailCallback.failCallback(result);
            }
            else {
                String errorMessage = result.getMessage();
                if (!TextUtils.isEmpty(errorMessage)) {
                    errorMessage = errorMessage.substring(errorMessage.indexOf(":") + 1, result.getMessage().length());
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    /**
     * 设置成功监听
     * 
     * @param asyncTaskSuccessCallback
     */
    public void setAsyncTaskSuccessCallback(AsyncTaskSuccessCallback<T> asyncTaskSuccessCallback) {
        this.asyncTaskSuccessCallback = asyncTaskSuccessCallback;
    }

    /**
     * 设置失败监听
     * 
     * @param asyncTaskFailCallback
     */
    public void setAsyncTaskFailCallback(AsyncTaskFailCallback<T> asyncTaskFailCallback) {
        this.asyncTaskFailCallback = asyncTaskFailCallback;
    }

    /**
     * 设置result返回null的回调
     * 
     * @param asyncTaskResultNullCallback
     */
    public void setAsyncTaskResultNullCallback(AsyncTaskResultNullCallback asyncTaskResultNullCallback) {
        this.asyncTaskResultNullCallback = asyncTaskResultNullCallback;
    }

    public boolean isShowProgressDialog() {
        return isShowProgressDialog;
    }

    public void setShowProgressDialog(boolean isShowProgressDialog) {
        this.isShowProgressDialog = isShowProgressDialog;
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }

    public boolean isCancel() {
        return isCancel;
    }

    public void setCancel(boolean isCancel) {
        this.isCancel = isCancel;
    }

    public String getProgressTitle() {
        return progressTitle;
    }

    public void setProgressTitle(String progressTitle) {
        this.progressTitle = progressTitle;
    }

    /**
     * http请求现在这里，需要子类自己实现
     * 
     * @param params
     * @return
     */
    protected abstract Result<T> doHttpRequest(Object... objects);

}
