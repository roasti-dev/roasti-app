package dev.roasti.feature.brew.domain.model

enum class BrewStatus {
    BREWING,    // пользователь активно идёт по шагам (foreground-таймеры)
    WAITING,    // длинный шаг отпущен в фон, ждём срабатывания будильника
    COMPLETED,  // все шаги пройдены, уехало в Историю
    CANCELLED;  // пользователь отменил заваривание
}
