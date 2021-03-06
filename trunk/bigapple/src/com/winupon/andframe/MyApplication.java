/* 
 * @(#)MyApplication.java    Created on 2014-3-21
 * Copyright (c) 2014 ZDSoft Networks, Inc. All rights reserved.
 * $Id$
 */
package com.winupon.andframe;

import android.app.Application;

import com.winupon.andframe.bigapple.db.DBHelper;
import com.winupon.andframe.zzdemo.bitmap.AnBitmapUtilsFace2;

/**
 * 应用入口
 * 
 * @author xuan
 * @version $Revision: 1.0 $, $Date: 2014-3-21 下午4:21:58 $
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化数据库在这里
        DBHelper.init(1, "bigapple_test_database", this);

        // 图片加载器初始化
        // AnBitmapUtilsFace.init(this);

        AnBitmapUtilsFace2.init(this);
    }

}
