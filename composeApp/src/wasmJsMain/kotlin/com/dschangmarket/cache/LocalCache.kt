package com.dschangmarket.cache

import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise

// ── JS interop for IndexedDB ──

private external val indexedDB: dynamic

private external class IDBRequest : Promise<dynamic> {
    val result: dynamic
    val error: Any?
}

private external class IDBOpenDBRequest : Promise<dynamic> {
    val result: dynamic
    val error: Any?
}

private external class IDBDatabase {
    fun transaction(storeNames: String, mode: String): IDBTransaction
    fun close()
}

private external class IDBTransaction {
    val objectStore: (String) -> IDBObjectStore
    fun complete: Promise<Unit>
}

private external class IDBObjectStore {
    fun put(value: dynamic, key: String): IDBRequest
    fun get(key: String): IDBRequest
    fun delete(key: String): IDBRequest
    fun getAll(): IDBRequest
    fun count(): IDBRequest
    fun clear(): IDBRequest
}

private fun openDB(): Promise<IDBDatabase> {
    return Promise { resolve, reject ->
        val request = indexedDB.open("DschangMarketCache", 1)
        request.onerror = { reject(request.error) }
        request.onsuccess = { resolve(request.result as IDBDatabase) }
        request.onupgradeneeded = {
            val db = request.result as IDBDatabase
            if (!db.objectStoreNames.contains("messages")) {
                db.createObjectStore("messages", dynamic(keyPath = "id"))
            }
            if (!db.objectStoreNames.contains("media")) {
                db.createObjectStore("media", dynamic(keyPath = "url"))
            }
            if (!db.objectStoreNames.contains("api_cache")) {
                db.createObjectStore("api_cache", dynamic(keyPath = "key"))
            }
        }
    }
}

private fun putInStore(storeName: String, value: dynamic, key: String): Promise<Unit> {
    return openDB().then { db ->
        Promise { resolve, reject ->
            try {
                val tx = db.transaction(storeName, "readwrite")
                val store = tx.objectStore(storeName)
                val req = store.put(value, key)
                req.onerror = { reject(req.error) }
                tx.complete.then { resolve(Unit) }.catch { reject(it) }
            } catch (e: dynamic) {
                reject(e)
            } finally {
                db.close()
            }
        }
    }
}

private fun getFromStore(storeName: String, key: String): Promise<dynamic?> {
    return openDB().then { db ->
        Promise { resolve, reject ->
            try {
                val tx = db.transaction(storeName, "readonly")
                val store = tx.objectStore(storeName)
                val req = store.get(key)
                req.onerror = { reject(req.error) }
                req.onsuccess = { resolve(req.result) }
            } catch (e: dynamic) {
                reject(e)
            } finally {
                db.close()
            }
        }
    }
}

private fun deleteFromStore(storeName: String, key: String): Promise<Unit> {
    return openDB().then { db ->
        Promise { resolve, reject ->
            try {
                val tx = db.transaction(storeName, "readwrite")
                val store = tx.objectStore(storeName)
                val req = store.delete(key)
                req.onerror = { reject(req.error) }
                tx.complete.then { resolve(Unit) }.catch { reject(it) }
            } catch (e: dynamic) {
                reject(e)
            } finally {
                db.close()
            }
        }
    }
}

// ── Public API ──

object LocalCache {

    private const val LS_PREFIX = "dc_"
    private var useIndexedDB: Boolean = true

    init {
        try {
            // Test if IndexedDB is available
            useIndexedDB = js("typeof indexedDB !== 'undefined'") as Boolean
        } catch (_: dynamic) {
            useIndexedDB = false
        }
    }

    // ── Simple key-value (localStorage fallback) ──

    fun putString(key: String, value: String) {
        try {
            localStorage.setItem("$LS_PREFIX$key", value)
        } catch (_: dynamic) {}
    }

    fun getString(key: String): String? {
        return try {
            localStorage.getItem("$LS_PREFIX$key")
        } catch (_: dynamic) { null }
    }

    fun removeString(key: String) {
        try {
            localStorage.removeItem("$LS_PREFIX$key")
        } catch (_: dynamic) {}
    }

    // ── Messages cache ──

    fun cacheMessage(messageJson: String) {
        if (!useIndexedDB) return
        try {
            val obj = js("JSON.parse(messageJson)")
            obj.cached_at = js("Date.now()")
            putInStore("messages", obj, obj.id.toString()).catch { _ -> }
        } catch (_: dynamic) {}
    }

    fun cacheMessages(messagesJson: String) {
        if (!useIndexedDB) return
        try {
            val arr = js("JSON.parse(messagesJson)") as Array<dynamic>
            val now = js("Date.now()")
            arr.forEach { obj ->
                obj.cached_at = now
                putInStore("messages", obj, obj.id.toString()).catch { _ -> }
            }
        } catch (_: dynamic) {}
    }

    fun getCachedMessage(messageId: Int, callback: (String?) -> Unit) {
        if (!useIndexedDB) { callback(null); return }
        getFromStore("messages", messageId.toString()).then { result ->
            if (result != null) {
                callback(js("JSON.stringify(result)") as String)
            } else {
                callback(null)
            }
        }.catch { callback(null) }
    }

    // ── Media cache (track which URLs have been cached) ──

    fun markMediaCached(url: String, type: String = "image") {
        if (!useIndexedDB) return
        val entry = js("{ url: url, type: type, cached_at: Date.now() }")
        putInStore("media", entry, url).catch { _ -> }
    }

    fun isMediaCached(url: String, callback: (Boolean) -> Unit) {
        if (!useIndexedDB) { callback(false); return }
        getFromStore("media", url).then { result ->
            callback(result != null)
        }.catch { callback(false) }
    }

    // ── API Response cache (10 min) ──

    fun cacheApiResponse(endpoint: String, jsonResponse: String) {
        if (!useIndexedDB) return
        val entry = js("{ key: endpoint, data: jsonResponse, cached_at: Date.now() }")
        putInStore("api_cache", entry, endpoint).catch { _ -> }
    }

    fun getCachedApiResponse(endpoint: String, callback: (String?) -> Unit) {
        if (!useIndexedDB) { callback(null); return }
        getFromStore("api_cache", endpoint).then { result ->
            if (result != null) {
                val cachedAt = (result.cached_at as? Number)?.toLong() ?: 0
                val now = js("Date.now()") as Long
                if (now - cachedAt < 600_000) { // 10 min
                    callback(result.data as? String)
                } else {
                    deleteFromStore("api_cache", endpoint).catch { _ -> }
                    callback(null)
                }
            } else {
                callback(null)
            }
        }.catch { callback(null) }
    }

    // ── Conversational partner list (simple JSON in localStorage) ──

    fun cacheConversations(json: String) {
        putString("conversations", json)
    }

    fun getCachedConversations(): String? = getString("conversations")

    // ── Unread count ──

    fun cacheUnreadCount(count: Int) {
        putString("unread", count.toString())
    }

    fun getCachedUnreadCount(): Int = getString("unread")?.toIntOrNull() ?: 0
}
