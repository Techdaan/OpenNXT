package com.opennxt.model.lobby

import com.opennxt.model.InterfaceHash
import com.opennxt.model.entity.BasePlayer
import com.opennxt.model.worldlist.WorldFlag
import com.opennxt.model.worldlist.WorldList
import com.opennxt.model.worldlist.WorldListEntry
import com.opennxt.model.worldlist.WorldListLocation
import com.opennxt.net.ConnectedClient
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.clientprot.WorldlistFetch
import com.opennxt.net.game.handlers.ClientCheatHandler
import com.opennxt.net.game.handlers.NoTimeoutHandler
import com.opennxt.net.game.handlers.WorldlistFetchHandler
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.*
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.game.serverprot.variables.ClientSetvarcLarge
import com.opennxt.net.game.serverprot.variables.ClientSetvarcSmall
import com.opennxt.net.game.serverprot.variables.ClientSetvarcstrSmall
import com.opennxt.net.game.serverprot.variables.ResetClientVarcache
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging
import kotlin.reflect.KClass

class LobbyPlayer(client: ConnectedClient) : BasePlayer(client) {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<in BasePlayer, out GamePacket>>()
    private val logger = KotlinLogging.logger { }

    val worldList = WorldList(
        arrayOf(
            WorldListEntry(
                id = 1,
                location = WorldListLocation(225, "Live"),
                flag = WorldFlag.createFlag(),
                activity = "Free To Play",
                host = "127.0.0.1",
                playercount = 69
            ),
            WorldListEntry(
                id = 2,
                location = WorldListLocation(225, "Live"),
                flag = WorldFlag.createFlag(WorldFlag.MEMBERS_ONLY),
                activity = "Members only",
                host = "127.0.0.1",
                playercount = 69
            ),
            WorldListEntry(
                id = 3,
                location = WorldListLocation(161, "Local"),
                flag = WorldFlag.createFlag(WorldFlag.VETERAN_WORLD),
                activity = "Veteran only",
                host = "127.0.0.1",
                playercount = 69
            ),
            WorldListEntry(
                id = 4,
                location = WorldListLocation(161, "Local"),
                flag = WorldFlag.createFlag(WorldFlag.BETA_WORLD),
                activity = "Beta world",
                host = "localhost",
                playercount = 69
            ),
            WorldListEntry(
                id = 5,
                location = WorldListLocation(161, "Local"),
                flag = WorldFlag.createFlag(WorldFlag.VIP_WORLD),
                activity = "VIP Only",
                host = "127.0.0.1",
                playercount = 69
            ),
        )
    )

    init {
        handlers[NoTimeout::class] = NoTimeoutHandler
        handlers[ClientCheat::class] = ClientCheatHandler
        handlers[WorldlistFetch::class] = WorldlistFetchHandler as GamePacketHandler<in BasePlayer, out GamePacket>
    }

    fun handleIncomingPackets() {
        val queue = client.incomingQueue
        while (true) {
            val packet = queue.poll() ?: return

            val handler = handlers[packet::class] as? GamePacketHandler<in BasePlayer, GamePacket>
            if (handler != null) {
                handler.handle(this, packet)
            } else {
                logger.info { "TODO: Handle incoming $packet" }
            }
        }
    }

    fun added() {
        client.write(ResetClientVarcache)
        TODORefactorThisClass.sendDefaultVarps(client)

        client.write(IfOpenTop(906))

        client.write(IfOpenSub(id=907, flag=true, parent=InterfaceHash(parent=906, component=65)))
        client.write(IfOpenSub(id=910, flag=true, parent=InterfaceHash(parent=906, component=66)))
        client.write(IfOpenSub(id=909, flag=true, parent=InterfaceHash(parent=906, component=67)))
        client.write(IfOpenSub(id=912, flag=true, parent=InterfaceHash(parent=906, component=69)))
        client.write(IfOpenSub(id=589, flag=true, parent=InterfaceHash(parent=906, component=68)))
        client.write(IfOpenSub(id=911, flag=true, parent=InterfaceHash(parent=906, component=70)))
        client.write(IfOpenSub(id=914, flag=true, parent=InterfaceHash(parent=906, component=128)))
        client.write(IfOpenSub(id=915, flag=true, parent=InterfaceHash(parent=906, component=129)))
        client.write(IfOpenSub(id=913, flag=true, parent=InterfaceHash(parent=906, component=130)))
        client.write(IfOpenSub(id=815, flag=true, parent=InterfaceHash(parent=906, component=137)))
        client.write(IfOpenSub(id=803, flag=true, parent=InterfaceHash(parent=906, component=132)))
        client.write(IfOpenSub(id=822, flag=true, parent=InterfaceHash(parent=906, component=133)))
        client.write(IfOpenSub(id=825, flag=true, parent=InterfaceHash(parent=906, component=115)))
        client.write(IfOpenSub(id=821, flag=true, parent=InterfaceHash(parent=906, component=116)))
        client.write(IfOpenSub(id=808, flag=true, parent=InterfaceHash(parent=906, component=114)))
        client.write(IfOpenSub(id=820, flag=true, parent=InterfaceHash(parent=906, component=134)))
        client.write(IfOpenSub(id=811, flag=true, parent=InterfaceHash(parent=906, component=131)))
        client.write(IfOpenSub(id=826, flag=true, parent=InterfaceHash(parent=906, component=82)))
        client.write(IfOpenSub(id=801, flag=true, parent=InterfaceHash(parent=906, component=36)))

        client.write(ClientSetvarcLarge(2771, 55004971))
        client.write(ClientSetvarcSmall(3496, 0))
        client.write(ClientSetvarcstrSmall(2508, ""))
        client.write(ClientSetvarcSmall(1027, 1))
        client.write(ClientSetvarcSmall(1034, 2))
        client.write(ClientSetvarcLarge(3699, 4096))

        client.write(RunClientScript(script = 7486, args = arrayOf(27002876, 52494341)))
        client.write(RunClientScript(script = 7486, args = arrayOf(27002876, 59637768)))

        client.write(IfOpenSub(id = 1322, flag = true, parent = InterfaceHash(parent = 906, component = 150)))
        client.write(IfOpenSub(id = 814, flag = true, parent = InterfaceHash(parent = 906, component = 37)))

        client.write(ClientSetvarcSmall(id = 4659, value = 0))
        client.write(ClientSetvarcLarge(id = 4660, value = 500))
        client.write(ClientSetvarcSmall(id = 1800, value = 0))
        client.write(ClientSetvarcLarge(id = 1648, value = 500))
        client.write(ClientSetvarcSmall(id = 4968, value = 0))
        client.write(ClientSetvarcSmall(id = 4969, value = 0))

        client.write(ClientSetvarcSmall(id = 3905, value = 0))
        client.write(ClientSetvarcSmall(id = 4266, value = 1))
        client.write(ClientSetvarcSmall(id = 4267, value = 110))
        client.write(ClientSetvarcLarge(id = 4660, value = 500))
        client.write(ClientSetvarcSmall(id = 4659, value = 0))

        // TODO http image
        client.write(ClientSetvarcSmall(id = 4263, value = -1))
        // TODO http image
        client.write(ClientSetvarcSmall(id = 4264, value = -1))
        // TODO http image
        client.write(ClientSetvarcSmall(id = 4265, value = -1))

        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    0,
                    16302,
                    1,
                    -1,
                    "This Week In RuneScape: Double XP LIVE & Improved Divination Training",
                    "This Week In RuneScape we're bringing you the new and improved Divination skill! Why not test it out during Double XP LIVE?",
                    "this-week-in-runescape-double-xp-live--improved-divination-training",
                    "04-May-2021",
                    1
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    1,
                    16301,
                    12,
                    -1,
                    "New & Improved Divination",
                    "A major update is coming to Divination next week. Click here to learn all about it!",
                    "new--improved-divination",
                    "29-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    2,
                    16293,
                    1,
                    -1,
                    "This Week In RuneScape: Dailies & Distractions & Diversions Week Begins!",
                    "It�s Dailies & Distractions & Diversions Week!",
                    "this-week-in-runescape-dailies--distractions--diversions-week-begins",
                    "26-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    3,
                    16282,
                    12,
                    -1,
                    "Double XP LIVE Returns Soon!",
                    "Double XP LIVE is coming again soon!",
                    "double-xp-live-returns-soon",
                    "23-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    4,
                    16291,
                    7,
                    -1,
                    "RuneScape On Mobile This Summer - A Message From Mod Warden",
                    "RuneScape is coming to mobile this Summer, and you can register today for free rewards!",
                    "runescape-on-mobile-this-summer---a-message-from-mod-warden",
                    "22-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    5,
                    16275,
                    1,
                    -1,
                    "This Week In RuneScape: Rex Matriarchs & Combat Week!",
                    "This week the Rex Matriarchs come roaring into the game with a new combat challenge for experienced fighters. Which is fitting, because it�s also Combat Week!",
                    "this-week-in-runescape-rex-matriarchs--combat-week",
                    "19-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    6,
                    16270,
                    1,
                    -1,
                    "This Week In RuneScape: Skilling Week Begins",
                    "This Week In RuneScape Awesome April begins, bringing with it Skilling Week!",
                    "this-week-in-runescape-skilling-week-begins",
                    "12-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    7,
                    16257,
                    3,
                    -1,
                    "Lockout Account Returns - Updates",
                    "Welcoming back The Returned.",
                    "lockout-account-returns---updates",
                    "08-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    8,
                    16258,
                    1,
                    -1,
                    "This Week In RuneScape: The RS20 mini-quest series continues!",
                    "This Week In RuneScape the Ninja Team returns for Strike 21. We've also got the next part of the RS20: Once Upon a Time miniquest series!",
                    "this-week-in-runescape-the-rs20-mini-quest-series-continues",
                    "05-Apr-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    9,
                    16243,
                    1,
                    -1,
                    "This Week In RuneScape: The Spring Festival Begins!",
                    "This Week In RuneScape marks the beginning of the Spring Festival!",
                    "this-week-in-runescape-the-spring-festival-begins",
                    "29-Mar-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    10,
                    16248,
                    3,
                    -1,
                    "Account Returning Begins & Making Things Right",
                    "An update on the Login Lockout situation, including the first details on the return of accounts and more.",
                    "account-returning-begins--making-things-right",
                    "26-Mar-2021",
                    0
                )
            )
        )
        client.write(
            RunClientScript(
                script = 10931,
                args = arrayOf(
                    11,
                    16219,
                    3,
                    -1,
                    "Login Lockout Daily Updates",
                    "This page is where we'll post the most recent news on the Login Lockout situation. Check back regularly for updates.",
                    "login-lockout-daily-updates",
                    "26-Mar-2021",
                    0
                )
            )
        )
        client.write(RunClientScript(script = 10936, args = emptyArray()))

        client.write(ChatFilterSettingsPrivatechat(0))
        client.write(FriendlistLoaded)
    }

    override fun tick() {
        // TODO Do lobby players even need to be ticked?
    }
}