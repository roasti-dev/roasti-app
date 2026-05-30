package dev.roasti.core.session.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val Service = "dev.roasti.auth"
private const val Account = "tokens"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainTokenStorage : TokenStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun readTokens(): TokensDto? = memScoped {
        val query = newReadQuery()
        try {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            val raw = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
                ?: return@memScoped null
            runCatching { json.decodeFromString<TokensDto>(raw) }.getOrNull()
        } finally {
            CFRelease(query)
        }
    }

    override suspend fun writeTokens(tokens: TokensDto) {
        val raw = json.encodeToString(TokensDto.serializer(), tokens)
        val data = (raw as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val dataRef = CFBridgingRetain(data)

        val findQuery = newBaseQuery()
        val exists = SecItemCopyMatching(findQuery, null) == errSecSuccess
        CFRelease(findQuery)

        if (exists) {
            val baseQuery = newBaseQuery()
            val attrs = newEmptyDict()
            CFDictionaryAddValue(attrs, kSecValueData, dataRef)
            SecItemUpdate(baseQuery, attrs)
            CFRelease(baseQuery)
            CFRelease(attrs)
        } else {
            val addQuery = newBaseQuery()
            CFDictionaryAddValue(addQuery, kSecValueData, dataRef)
            SecItemAdd(addQuery, null)
            CFRelease(addQuery)
        }

        if (dataRef != null) CFRelease(dataRef)
    }

    override suspend fun clearTokens() {
        val query = newBaseQuery()
        SecItemDelete(query)
        CFRelease(query)
    }

    private fun newBaseQuery(): CFMutableDictionaryRef {
        val dict = newEmptyDict()
        CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dict, kSecAttrService, CFBridgingRetain(Service as NSString))
        CFDictionaryAddValue(dict, kSecAttrAccount, CFBridgingRetain(Account as NSString))
        return dict
    }

    private fun newReadQuery(): CFMutableDictionaryRef {
        val dict = newBaseQuery()
        CFDictionaryAddValue(dict, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(dict, kSecMatchLimit, kSecMatchLimitOne)
        return dict
    }

    private fun newEmptyDict(): CFMutableDictionaryRef =
        CFDictionaryCreateMutable(
            allocator = kCFAllocatorDefault,
            capacity = 0,
            keyCallBacks = kCFTypeDictionaryKeyCallBacks.ptr,
            valueCallBacks = kCFTypeDictionaryValueCallBacks.ptr,
        ) ?: error("Failed to create CFMutableDictionary")

    @Suppress("UNUSED")
    private fun CFDictionaryRef.asRef(): CValuesRef<*>? = this
}
