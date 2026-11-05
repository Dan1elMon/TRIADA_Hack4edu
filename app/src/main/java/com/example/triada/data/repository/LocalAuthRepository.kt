package com.example.triada.data.repository

import androidx.room.withTransaction
import com.example.triada.data.local.*
import com.example.triada.domain.model.SchoolAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted, costed local credentials; no password or hash enters UI state or exported results. */
class LocalAuthRepository(private val db:LearningDatabase) {
    private val dao get()=db.extensionDao()
    private fun hash(password:String,salt:ByteArray,iterations:Int):ByteArray {
        val spec=PBEKeySpec(password.toCharArray(),salt,iterations,256)
        return try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded}finally{spec.clearPassword()}
    }
    private fun hex(data:ByteArray)=data.joinToString(""){"%02x".format(it)}
    private fun bytes(hex:String)=hex.chunked(2).map{it.toInt(16).toByte()}.toByteArray()
    private fun record(account:String,username:String,password:String):CredentialEntity {
        val salt=ByteArray(24).also{SecureRandom().nextBytes(it)};val cost=120_000
        return CredentialEntity(account,username,hex(salt),hex(hash(password,salt,cost)),cost)
    }
    suspend fun initialize()=withContext(Dispatchers.IO){db.withTransaction {
        val accounts=SchoolRepository(db).accounts().sortedBy{if(it.id=="demo-teacher")"" else it.id}
        val used=dao.usernames().toMutableSet()
        accounts.forEach { account -> if(dao.credential(account.id)==null){
            val username=(10000001..99999999).asSequence().map{it.toString()}.first{it !in used}
            dao.insertCredential(record(account.id,username,"admin"));used.add(username)
        }}
    }}
    suspend fun login(username:String,password:String):SchoolAccount?=withContext(Dispatchers.IO){
        if(!username.matches(Regex("[0-9]{8}")) || password.length !in 1..128)return@withContext null
        val row=dao.byUsername(username)
        // Perform the same expensive derivation for an unknown username.
        val candidate=hash(password,row?.salt?.let(::bytes) ?: ByteArray(24),row?.iterations ?: 120_000)
        if(row==null || !MessageDigest.isEqual(candidate,bytes(row.passwordHash))) null
        else SchoolRepository(db).accounts().first{it.id==row.accountId}
    }
    suspend fun change(account:String,current:String,username:String,password:String)=withContext(Dispatchers.IO){db.withTransaction {
        require(username.matches(Regex("[0-9]{8}")) && password.length in 5..128){"Usa 8 dígitos y una contraseña de 5 a 128 caracteres."}
        val old=requireNotNull(dao.credential(account))
        require(MessageDigest.isEqual(hash(current,bytes(old.salt),old.iterations),bytes(old.passwordHash))){"La contraseña actual no coincide."}
        require(dao.byUsername(username)?.accountId.let{it==null || it==account}){"Ese usuario ya existe."}
        dao.updateCredential(record(account,username,password))
    }}
}
