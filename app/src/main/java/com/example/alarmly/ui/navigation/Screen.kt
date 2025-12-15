package com.example.alarmly.ui.navigation

sealed class Screen(val route: String) {
    object AlarmList : Screen("alarm_list")
    object AlarmDetail : Screen("alarm_detail/{alarmId}") {
        fun createRoute(alarmId: Int?) = if (alarmId == null) "alarm_detail/new" else "alarm_detail/$alarmId"
    }
    object AlarmRinging : Screen("alarm_ringing/{alarmId}") {
        fun createRoute(alarmId: Int) = "alarm_ringing/$alarmId"
    }
}

