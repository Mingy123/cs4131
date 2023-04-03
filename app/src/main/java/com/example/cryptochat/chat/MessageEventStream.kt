package com.example.cryptochat.chat

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class MessageEventStream {
    private val eventStream = PublishSubject.create<Message>()

    fun emit(data: Message) {
        eventStream.onNext(data)
    }

    fun error(error: Throwable) {
        eventStream.onError(error)
    }

    fun observe(): Observable<Message> {
        return eventStream
    }
}