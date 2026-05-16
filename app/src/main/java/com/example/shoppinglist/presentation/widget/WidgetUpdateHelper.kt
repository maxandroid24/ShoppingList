package com.example.shoppinglist.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.shoppinglist.R

object WidgetUpdateHelper {
    fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, ShoppingListWidgetProvider::class.java)
        )
        
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widgetListView)
        
        for (appWidgetId in appWidgetIds) {
            ShoppingListWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
