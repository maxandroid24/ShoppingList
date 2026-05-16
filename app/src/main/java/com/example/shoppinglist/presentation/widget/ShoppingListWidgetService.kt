package com.example.shoppinglist.presentation.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.ShoppingItemDao
import com.example.shoppinglist.domain.models.ShoppingGroup
import com.example.shoppinglist.domain.repositories.AuthRepository
import com.example.shoppinglist.domain.repositories.GroupRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ShoppingListWidgetService : RemoteViewsService() {

    @Inject
    lateinit var dao: ShoppingItemDao
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var groupRepository: GroupRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return ShoppingListRemoteViewsFactory(this.applicationContext, dao, authRepository, groupRepository, appWidgetId)
    }
}

class ShoppingListRemoteViewsFactory(
    private val context: Context,
    private val dao: ShoppingItemDao,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<Any> = emptyList()
    private var isProductMode = false

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val mode = prefs.getString("mode_$appWidgetId", ShoppingListWidgetProvider.MODE_GROUPS)
            val groupId = prefs.getString("groupId_$appWidgetId", null)
            
            isProductMode = mode == ShoppingListWidgetProvider.MODE_PRODUCTS
            
            if (isProductMode && groupId != null) {
                items = dao.getItemsForGroupSync(groupId)
            } else {
                // Fetch Groups from cache
                val groupsJson = prefs.getString("cached_groups_data", "[]") ?: "[]"
                val result = mutableListOf<ShoppingGroup>()
                try {
                    val array = org.json.JSONArray(groupsJson)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        result.add(ShoppingGroup(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            ownerId = "",
                            memberIds = List(obj.getInt("memberCount")) { "" }, // Dummy list for count
                            memberNames = emptyList(),
                            inviteCode = ""
                        ))
                    }
                } catch (e: Exception) {}
                items = result
            }
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) return RemoteViews(context.packageName, R.layout.widget_item)
        
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_item)
        
        if (item is com.example.shoppinglist.data.local.entities.ShoppingItemEntity) {
            views.setTextViewText(R.id.widgetItemName, item.name)
            views.setTextViewText(R.id.widgetItemQuantity, "${item.quantity}x")
            
            if (item.isBought) {
                views.setTextColor(R.id.widgetItemName, android.graphics.Color.GRAY)
            } else {
                views.setTextColor(R.id.widgetItemName, android.graphics.Color.parseColor("#191C1A"))
            }

            val fillInIntent = Intent()
            views.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent)
            
        } else if (item is ShoppingGroup) {
            views.setTextViewText(R.id.widgetItemName, item.name)
            views.setTextViewText(R.id.widgetItemQuantity, "${item.memberIds.size} members")
            views.setTextColor(R.id.widgetItemName, android.graphics.Color.parseColor("#191C1A"))
            
            val fillInIntent = Intent().apply {
                action = ShoppingListWidgetProvider.ACTION_SHOW_PRODUCTS
                putExtra(ShoppingListWidgetProvider.EXTRA_GROUP_ID, item.id)
                putExtra(ShoppingListWidgetProvider.EXTRA_GROUP_NAME, item.name)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickFillInIntent(R.id.widgetItemContainer, fillInIntent)
        }
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
