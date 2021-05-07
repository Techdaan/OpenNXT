package com.opennxt.net.game.handlers

import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.NoTimeout

object NoTimeoutHandler: GamePacketHandler<BasePlayer, NoTimeout> {
    override fun handle(context: BasePlayer, packet: NoTimeout) {
        if ((context.noTimeouts++ % 5) == 0)
            context.write(NoTimeout)
    }
}