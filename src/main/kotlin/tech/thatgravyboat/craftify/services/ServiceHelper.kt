package tech.thatgravyboat.craftify.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import gg.essential.universal.ChatColor
import gg.essential.universal.UChat
import gg.essential.universal.wrappers.UPlayer
import org.apache.commons.io.IOUtils
import tech.thatgravyboat.craftify.config.Config
import tech.thatgravyboat.craftify.ui.Player
import tech.thatgravyboat.craftify.utils.EssentialUtils
import tech.thatgravyboat.craftify.utils.ServerAddonHelper
import tech.thatgravyboat.craftify.utils.Utils
import tech.thatgravyboat.jukebox.api.events.EventType
import tech.thatgravyboat.jukebox.api.service.BaseService
import tech.thatgravyboat.jukebox.api.state.PlayingType
import tech.thatgravyboat.jukebox.api.state.Song
import tech.thatgravyboat.jukebox.api.state.SongState
import tech.thatgravyboat.jukebox.api.state.State
import tech.thatgravyboat.jukebox.impl.spotify.SpotifyService
import java.nio.charset.StandardCharsets

object ServiceHelper {

    private const val AUTH_API: String = "https://craftify.thatgravyboat.tech/api/v1/public/auth"
    private val badTokens = mutableSetOf<String>()
    private val GSON = Gson()
    private var lastFailedLogin = 0L

    fun BaseService.setup() {
        registerListener(EventType.UPDATE) {
            Player.updatePlayer(it.state)
        }
        registerListener(EventType.SONG_CHANGE) {
            Player.announceSong(it.state)
        }
        registerListener(EventType.VOLUME_CHANGE) {
            if (it.shouldNotify && Utils.isEssentialInstalled()) showVolumeNotification(it.volume)
        }
        if (Config.thisIsForTestingPacketsDoNotTurnOn) {
            ServerAddonHelper.setupServerAddon(this)
        }
        if (Config.thisIsForTestingEssentialPacketsDoNotTurnOn && Utils.isEssentialInstalled()) {
            EssentialUtils.setupServerAddon(this)
        }
    }

    fun setupSpotify(service: BaseService) {
        if (service is SpotifyService) {
            service.registerListener(EventType.SERVICE_UNAUTHORIZED) {
                Config.optionalRefresh()?.let {
                    loginToSpotify("refresh", it)
                    service.token = Config.token
                    service.restart()
                }
            }
        }
    }

    fun loginToSpotify(type: String, code: String) {
        if (System.currentTimeMillis() - lastFailedLogin < 10000) return
        if (badTokens.contains(code)) return
        var failed = false
        var reason = "Unknown error"
        try {
            Utils.post("$AUTH_API?type=$type&code=$code")?.let { response ->
                val data = try { GSON.fromJson(IOUtils.toString(response.inputStream, StandardCharsets.UTF_8), JsonObject::class.java) } catch (e: Exception) { null }
                if (response.success()) {
                    if (data?.get("success")?.asBoolean == true) {
                        val refreshToken = data.getOrNull("refresh_token")
                        val accessToken = data.getOrNull("access_token")
                        if (accessToken != null) {
                            Config.token = accessToken
                            Config.refreshToken = if (type != "refresh" && refreshToken == null) "" else refreshToken ?: Config.refreshToken
                            Config.modMode = 1
                            Config.markDirty()
                            Config.writeData()
                        } else {
                            failed = true
                        }
                    } else {
                        failed = true
                    }
                } else {
                    failed = true
                }

                if (failed) {
                    reason = data?.getOrNull("reason") ?: "Unknown error"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            failed = true
            reason = "Check the console for more information"
        }

        if (failed && lastFailedLogin < System.currentTimeMillis() - 600000) {
            if (UPlayer.hasPlayer()) {
                UChat.chat("${ChatColor.RED}Craftify > ${ChatColor.GRAY}Failed to login to Spotify: ${reason}, please try logging in again.")
            } else {
                println("Failed to login to Spotify: $reason, please try logging in again.")
            }
            lastFailedLogin = System.currentTimeMillis()
            badTokens.add(code)
        }
    }

    private fun JsonObject.getOrNull(key: String): String? {
        return if (has(key)) get(key).asString else null
    }

    private fun showVolumeNotification(volume: Int) {
        val image = when {
            volume <= 0 -> "https://i.imgur.com/v2a3Z8n.png"
            volume <= 30 -> "https://i.imgur.com/8L4av1O.png"
            volume <= 70 -> "https://i.imgur.com/tGJKxRr.png"
            else -> "https://i.imgur.com/1Ay43hi.png"
        }
        EssentialUtils.sendNotification(
            title = "Craftify",
            message = "The volume has been set to $volume%",
            image = image,
        )
    }

    fun createBisectAd(state: State) = State(
        state.player,
        Song(
            "Bisect Hosting Partner",
            listOf("use code '&agravy&r'"),
            "https://www.bisecthosting.com/images/logos/logo-app.png",
            "https://www.bisecthosting.com/gravy",
            PlayingType.AD
        ),
        SongState(0, 0, false)
    )
}