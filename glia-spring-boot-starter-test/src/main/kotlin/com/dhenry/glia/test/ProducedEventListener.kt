package com.dhenry.glia.test

import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.support.converter.MessageConverter
import java.util.concurrent.CopyOnWriteArrayList

class ProducedEventListener(private val messageConverter: MessageConverter) : MessageListener {
    private val messages = CopyOnWriteArrayList<Any>()

    fun getMessages(): List<Any> {
        return messages.toList()
    }

    override fun onMessage(message: Message?) {
        if (message != null) messages.add(messageConverter.fromMessage(message))
    }

    fun clearMessages() {
        messages.clear()
    }

    fun hasMessages(): Boolean = !messages.isEmpty()

    fun hasMessages(count: Int): Boolean = messages.size == count
}