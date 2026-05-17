package org.acoustixaudio.opiqo.mpvoverssh.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.acoustixaudio.opiqo.mpvoverssh.data.SshProfile
import java.util.Properties

class SshManager {
    private val jsch = JSch()

    suspend fun executeCommand(profile: SshProfile, command: String): Result<String> = withContext(Dispatchers.IO) {
        var session: Session? = null
        try {
            // Add private key if provided
            profile.privateKey?.let { key ->
                val passphraseBytes = profile.passphrase?.toByteArray()
                jsch.addIdentity("key", key.toByteArray(), null, passphraseBytes)
            }

            session = jsch.getSession(profile.username, profile.host, profile.port)
            
            val config = Properties()
            if (!profile.strictHostKeyChecking) {
                config["StrictHostKeyChecking"] = "no"
            }
            session.setConfig(config)

            if (profile.password != null && profile.privateKey == null) {
                session.setPassword(profile.password)
            }

            session.connect(10000) // 10 seconds timeout

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            
            val inputStream = channel.inputStream
            val errorStream = channel.errStream

            channel.connect()

            val output = StringBuilder()
            val error = StringBuilder()
            val buffer = ByteArray(1024)

            while (true) {
                while (inputStream.available() > 0) {
                    val i = inputStream.read(buffer, 0, 1024)
                    if (i < 0) break
                    output.append(String(buffer, 0, i))
                }
                while (errorStream.available() > 0) {
                    val i = errorStream.read(buffer, 0, 1024)
                    if (i < 0) break
                    error.append(String(buffer, 0, i))
                }
                if (channel.isClosed) {
                    if (inputStream.available() > 0) continue
                    break
                }
                delay(100)
            }

            val exitStatus = channel.exitStatus
            channel.disconnect()

            if (exitStatus == 0) {
                Result.success(output.toString())
            } else {
                Result.failure(Exception("Command failed with status $exitStatus: ${error.toString().ifEmpty { output.toString() }}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            session?.disconnect()
            // Important: Clear identities to avoid mixing up keys for different connections
            jsch.removeAllIdentity()
        }
    }
}
