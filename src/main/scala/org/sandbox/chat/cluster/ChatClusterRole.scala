package org.sandbox.chat.cluster

sealed trait ChatClusterRole { val name: String }
object ChatServiceRole extends ChatClusterRole { val name = "chatService" }
object ChatMsgPublisherRole extends ChatClusterRole { val name = "chatMsgPublisher" }
object BroadcastManagerRole extends ChatClusterRole { val name = "broadcastManager" }
object ParticipantAdministratorRole extends ChatClusterRole { val name = "participantAdmin" }
object HttpChatServiceRole extends ChatClusterRole { val name = "httpChatService" }
object ClusterMonitorRole extends ChatClusterRole { val name = "clusterMonitor" }
object ClusterReaperRole extends ChatClusterRole { val name = "clusterReaper" }

object ChatClusterRole {
  implicit def roleToString(role: ChatClusterRole): String = role.name
}