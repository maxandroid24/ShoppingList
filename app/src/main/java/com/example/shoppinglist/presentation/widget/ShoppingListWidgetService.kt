package com.example.shoppinglist.presentation.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.ShoppingItemDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingListWidgetService : RemoteViewsService() {

    @Inject
    lateinit var dao: ShoppingItemDao

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ShoppingListRemoteViewsFactory(this.applicationContext, dao)
    }
}

class ShoppingListRemoteViewsFactory(
    private val context: Context,
    private val dao: ShoppingItemDao
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<com.example.shoppinglist.data.local.entities.ShoppingItemEntity> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            val prefs = context.getSharedPreferences("mock_auth", Context.MODE_PRIVATE)
            val groupId = prefs.getString("activeGroupId", null)
            
            if (groupId != null) {
                items = dao.getItemsForGroupSync(groupId)
            } else {
                items = emptyList()
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) return RemoteViews(context.packageName, R.layout.widget_item)
        
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        
        views.setTextViewText(R.id.widgetItemName, item.name)
        views.setTextViewText(R.id.widgetItemQuantity, "${item.quantity}x")
        
        // Fill-in intent to satisfy the template in Provider
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widgetItemName, fillInIntent)
        
        // Strike through if bought
        if (item.isBought) {
            // RemoteViews has limited support for styling, but we can change color
            views.setTextColor(R.id.widgetItemName, android.graphics.Color.GRAY)
        } else {
            views.setTextColor(R.id.widgetItemName, android.graphics.Color.WHITE)
        }
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
