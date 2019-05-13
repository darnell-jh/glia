package com.dhenry.glia.test

import org.assertj.core.api.Assertions.assertThat
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.support.converter.MessageConverter
import java.util.concurrent.CopyOnWriteArrayList

class ProducerEventListener(val messageConverter: MessageConverter) : MessageListener {

    private val _messages = CopyOnWriteArrayList<Message>()

    val messages: List<Message>
        get() = _messages.toList()

    fun getAllPayloads(): List<Any> {
        return messages.asSequence()
            .map { messageConverter.fromMessage(it) }
            .toList()
    }

    inline fun <reified T> getPayloads(): List<T> {
        return messages.asSequence()
            .map { messageConverter.fromMessage(it) }
            .filter { it is T }
            .map { it as T }
            .toList()
    }


    override fun onMessage(message: Message?) {
        if (message != null) _messages.add(message)
    }

    fun clearMessages() {
        _messages.clear()
    }

    fun hasMessages(count: Int = 1) = assertThat(_messages)
        .withFailMessage("Expected $count messages")
        .hasSize(count)

    inline fun <reified T> hasPayloads(count: Int = 1) {
        assertThat(getPayloads<T>()).hasSize(count)
    }

}