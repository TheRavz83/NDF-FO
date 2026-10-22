package fr.notedefrais.app.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Distance routière entre deux lieux : géocodage Nominatim puis itinéraire OSRM (OpenStreetMap). */
object RouteDistance {
    private const val USER_AGENT = "FoNotes-Android/1.0"
    private const val MAX_RESPONSE_CHARS = 200_000

    private data class Point(val latitude: String, val longitude: String)

    /** Distance routière en kilomètres (arrondie à 0,1 km) de [from] vers [to]. */
    suspend fun kilometers(from: String, to: String): BigDecimal = withContext(Dispatchers.IO) {
        val start = geocode(from)
        delay(1_100) // Nominatim : 1 requête par seconde maximum.
        val end = geocode(to)
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
            "?overview=false"
        val json = JSONObject(get(url))
        val route = json.optJSONArray("routes")?.optJSONObject(0)
            ?: throw IllegalStateException("Aucun itinéraire routier trouvé.")
        BigDecimal(route.getDouble("distance"))
            .divide(BigDecimal(1000), 1, RoundingMode.HALF_UP)
    }

    private fun geocode(place: String): Point {
        val query = URLEncoder.encode(place.trim(), "UTF-8")
        val results = JSONArray(
            get("https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=1")
        )
        val first = results.optJSONObject(0)
            ?: throw IllegalStateException("Lieu introuvable : « ${place.trim()} ». Précisez l’adresse.")
        return Point(first.getString("lat"), first.getString("lon"))
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Service de calcul indisponible (${connection.responseCode}).")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(MAX_RESPONSE_CHARS + 1)
                var total = 0
                while (total < buffer.size) {
                    val read = reader.read(buffer, total, buffer.size - total)
                    if (read < 0) break
                    total += read
                }
                check(total <= MAX_RESPONSE_CHARS) { "Réponse trop volumineuse." }
                String(buffer, 0, total)
            }
        } finally {
            connection.disconnect()
        }
    }
}
