package com.mensinator.app.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

/**
 * Helper class to update widgets when period calculations change.
 * This ensures widgets are updated only when the period date calculation results in a new value.
 */
object PeriodCalculationWidgetUpdater : KoinComponent {
    
    private val context: Context by inject()
    
    @Volatile
    private var lastKnownPeriodDate: String? = null
    
    /**
     * Call this method when period calculation might have changed.
     * It will only update widgets if the calculated period date is different from the last known value.
     */
    fun updateWidgetsIfPeriodChanged(newPeriodDate: LocalDate?) {
        val newPeriodDateString = newPeriodDate?.toString() ?: "null"
        
        if (lastKnownPeriodDate != newPeriodDateString) {
            lastKnownPeriodDate = newPeriodDateString
            updateWidgets()
        }
    }
    
    private fun updateWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Do NOT emit to MidnightTrigger here.
                // Emitting before updateAll() could cause the active combine flow to recompose
                // with the OLD nextPeriod() value (the new value hasn't been emitted by the
                // nextPeriod() map block yet). updateAll() starts a fresh Glance composition that
                // re-runs calculateNextPeriod() from scratch via the dbWriteTrigger replay, so the
                // widget always gets the correct up-to-date data.
                // Update all widgets concurrently for better performance
                WidgetInstances.map { receiver ->
                    launch { receiver.glanceAppWidget.updateAll(context) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PeriodCalculationWidgetUpdater", "Failed to update widgets", e)
            }
        }
    }
}
