package org.sandbox.chat.cluster

object ChatClusterApp extends App {
    ChatServiceCluster.main(Array("2551"))
    ChatServiceCluster.main(Array.empty)

    ChatMsgPublisherCluster.main(Array("2552"))
    ChatMsgPublisherCluster.main(Array.empty)
}
