package com.example.shoppinglist.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.shoppinglist.R
import com.example.shoppinglist.presentation.ui.main.MainActivity

class ShoppingListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            // Choose layout based on size
            val layoutId = if (minWidth > 200 || minHeight > 200) {
                R.layout.widget_shopping_list_4x4
            } else {
                R.layout.widget_shopping_list_2x2
            }

            val views = RemoteViews(context.packageName, layoutId)

            val intent = Intent(context, ShoppingListWidgetService::class.java)
            views.setRemoteAdapter(R.id.widgetListView, intent)

            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            
            // Set a template for items in the collection
            views.setPendingIntentTemplate(R.id.widgetListView, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
        }
    }
}
