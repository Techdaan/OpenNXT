package com.opennxt.model.commands

import com.opennxt.model.messages.MessageReceiver
import com.opennxt.model.permissions.PermissionsHolder

interface CommandSender: PermissionsHolder, MessageReceiver {
}