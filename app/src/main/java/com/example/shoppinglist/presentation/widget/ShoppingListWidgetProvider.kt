package com.example.shoppinglist.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        when (intent.action) {
            ACTION_SHOW_PRODUCTS -> {
                val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
                val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && groupId != null) {
                    setWidgetMode(context, appWidgetId, MODE_PRODUCTS, groupId, groupName)
                    val manager = AppWidgetManager.getInstance(context)
                    manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
                    updateAppWidget(context, manager, appWidgetId)
                }
            }
            ACTION_SHOW_GROUPS -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setWidgetMode(context, appWidgetId, MODE_GROUPS)
                    val manager = AppWidgetManager.getInstance(context)
                    manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListView)
                    updateAppWidget(context, manager, appWidgetId)
                }
            }
        }
    }

    companion object {
        const val ACTION_SHOW_PRODUCTS = "com.example.shoppinglist.ACTION_SHOW_PRODUCTS"
        const val ACTION_SHOW_GROUPS = "com.example.shoppinglist.ACTION_SHOW_GROUPS"
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        
        const val MODE_GROUPS = "mode_groups"
        const val MODE_PRODUCTS = "mode_products"

        private fun setWidgetMode(context: Context, widgetId: Int, mode: String, groupId: String? = null, groupName: String? = null) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("mode_$widgetId", mode)
                .putString("groupId_$widgetId", groupId)
                .putString("groupName_$widgetId", groupName)
                .apply()
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val mode = prefs.getString("mode_$appWidgetId", MODE_GROUPS)
            val groupName = prefs.getString("groupName_$appWidgetId", "My Groups")

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            
            val layoutId = if (minWidth > 200 || minHeight > 200) {
                R.layout.widget_shopping_list_4x4
            } else {
                R.layout.widget_shopping_list_2x2
            }

            val views = RemoteViews(context.packageName, layoutId)
            
            // Set Title
            views.setTextViewText(R.id.tvWidgetTitle, groupName)

            // RemoteAdapter setup
            val serviceIntent = Intent(context, ShoppingListWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widgetListView, serviceIntent)

            // Click Template (for opening the app)
            val appIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            
            // Item Click Template (for navigating inside widget)
            val itemClickIntent = Intent(context, ShoppingListWidgetProvider::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val itemClickPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 100, itemClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widgetListView, itemClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
