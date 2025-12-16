package com.example.WeatherApp.api

data class APIWeather (
    var last_updated: String? = null,
    var temp_c : Double? = 0.0,
    var maxtemp_c: Double? = 0.0,
    var mintemp_c: Double? = 0.0,
    var condition : APICondition? = null
)

