package dev.roasti.features.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.MessageDigest
import kotlin.time.Clock

object RevokedTokenTable : Table("revoked_tokens") {
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val revokedAt = timestamp("revoked_at")
    override val primaryKey = PrimaryKey(tokenHash)
}

interface RevokedTokenRepository {
    suspend fun add(token: String)
    suspend fun isRevoked(token: String): Boolean
}

class RevokedTokenRepositoryImpl : RevokedTokenRepository {

    override suspend fun add(token: String): Unit = withContext(Dispatchers.IO) {
        val h = hash(token)
        transaction {
            val exists = RevokedTokenTable.selectAll()
                .where { RevokedTokenTable.tokenHash eq h }
                .count() > 0
            if (!exists) {
                RevokedTokenTable.insert {
                    it[tokenHash] = h
                    it[revokedAt] = Clock.System.now()
                }
            }
        }
    }

    override suspend fun isRevoked(token: String): Boolean = withContext(Dispatchers.IO) {
        transaction {
            RevokedTokenTable.selectAll()
                .where { RevokedTokenTable.tokenHash eq hash(token) }
                .count() > 0
        }
    }

    private fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
