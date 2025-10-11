// 사용자의 휴대폰 내에 있는 앱 목록 불러오기
package com.example.yourapp.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** 앱 정보를 담는 데이터 클래스 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/** 스마트폰에 설치된 사용자 앱 목록을 가져오는 유틸리티 */
object AppUtil {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = ArrayList<AppInfo>()

        for (app in installedApps) {
            // 시스템 앱이 아닌, 사용자가 직접 설치한 앱만 필터링
            if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = app.loadLabel(pm).toString()
                val appIcon = app.loadIcon(pm)
                appList.add(AppInfo(name = appName, packageName = app.packageName, icon = appIcon))
            }
        }
        // 앱 이름 순으로 정렬
        return appList.sortedBy { it.name }
    }
}