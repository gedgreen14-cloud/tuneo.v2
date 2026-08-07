package com.tuneo.app.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/**
 * Client Supabase unique pour toute l'app (backend du feed social Tuneo).
 * Projet Supabase : "Tuneo".
 */
object SupabaseClientProvider {

    private const val SUPABASE_URL = "https://cgzfvxqzbpdaqbvqfbsy.supabase.co"
    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnemZ2eHF6YnBkYXFidnFmYnN5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU5NjIxMzIsImV4cCI6MjEwMTUzODEzMn0.hztAzwRjvP3SLG3Yyp4QCPkBBnZrF-C1oq5vmGOjUnE"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            // Les modèles envoient parfois des champs nullable (id, created_at, updated_at...)
            // à null : on ne veut pas les inclure dans le JSON envoyé, sinon Postgrest
            // écraserait les valeurs par défaut définies côté base (uuid_generate_v4(), now()...).
            defaultSerializer = KotlinXSerializer(
                Json {
                    encodeDefaults = true
                    explicitNulls = false
                    ignoreUnknownKeys = true
                }
            )

            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
