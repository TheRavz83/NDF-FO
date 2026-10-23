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

data class RoutePlace(val label: String, val latitude: Double, val longitude: Double)

/** Distance routière entre deux lieux : géocodage Nominatim puis itinéraire OSRM (OpenStreetMap). */
object RouteDistance {
    private const val USER_AGENT = "FoNotes-Android/1.0"
    private const val MAX_RESPONSE_CHARS = 200_000

    private data class Point(val latitude: String, val longitude: String)

    /** Distance routière en kilomètres (arrondie à 0,1 km) de [from] vers [to]. */
    suspend fun kilometers(
        from: String,
        to: String,
        fromPlace: RoutePlace? = null,
        toPlace: RoutePlace? = null
    ): BigDecimal = withContext(Dispatchers.IO) {
        val start = fromPlace?.let { Point(it.latitude.toString(), it.longitude.toString()) }
            ?: geocode(from)
        delay(1_100) // Nominatim : 1 requête par seconde maximum.
        val end = toPlace?.let { Point(it.latitude.toString(), it.longitude.toString()) }
            ?: geocode(to)
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
            "?overview=false"
        val json = JSONObject(get(url))
        val route = json.optJSONArray("routes")?.optJSONObject(0)
            ?: throw IllegalStateException("Aucun itinéraire routier trouvé.")
        BigDecimal(route.getDouble("distance"))
            .divide(BigDecimal(1000), 1, RoundingMode.HALF_UP)
    }

    suspend fun searchPlaces(query: String): List<RoutePlace> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val features = JSONObject(
            get("https://photon.komoot.io/api/?q=$encoded&limit=5&lang=fr")
        ).getJSONArray("features")
        buildList {
            for (index in 0 until features.length()) {
                val feature = features.getJSONObject(index)
                val properties = feature.getJSONObject("properties")
                val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val street = listOf("housenumber", "street")
                    .map { properties.optString(it).trim() }
                    .filter { it.isNotBlank() }.joinToString(" ")
                val locality = listOf("postcode", "city")
                    .map { properties.optString(it).trim() }
                    .filter { it.isNotBlank() }.joinToString(" ")
                val label = listOf(properties.optString("name"), street, locality,
                    properties.optString("country"))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                if (label.isNotBlank()) {
                    add(RoutePlace(label, coordinates.getDouble(1), coordinates.getDouble(0)))
                }
            }
        }.distinct()
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
