// ������� �޴��� ���� �ִ� �� ��� �ҷ�����
package com.example.yourapp.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** �� ������ ��� ������ Ŭ���� */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

/** ����Ʈ���� ��ġ�� ����� �� ����� �������� ��ƿ��Ƽ */
object AppUtil {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm: PackageManager = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = ArrayList<AppInfo>()

        for (app in installedApps) {
            // �ý��� ���� �ƴ�, ����ڰ� ���� ��ġ�� �۸� ���͸�
            if (app.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                val appName = app.loadLabel(pm).toString()
                val appIcon = app.loadIcon(pm)
                appList.add(AppInfo(name = appName, packageName = app.packageName, icon = appIcon))
            }
        }
        // �� �̸� ������ ����
        return appList.sortedBy { it.name }
    }
}