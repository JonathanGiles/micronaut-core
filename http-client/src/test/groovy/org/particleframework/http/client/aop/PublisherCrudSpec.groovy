/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.http.client.aop

import io.reactivex.Flowable
import io.reactivex.Maybe
import org.particleframework.context.ApplicationContext
import org.particleframework.core.async.publisher.Publishers
import org.particleframework.http.annotation.Controller
import org.particleframework.http.annotation.Delete
import org.particleframework.http.annotation.Get
import org.particleframework.http.annotation.Patch
import org.particleframework.http.annotation.Post
import org.particleframework.http.client.Client
import org.particleframework.runtime.server.EmbeddedServer
import org.reactivestreams.Publisher
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong

/**
 * @author graemerocher
 * @since 1.0
 */
class PublisherCrudSpec extends Specification {

    @Shared @AutoCleanup ApplicationContext context = ApplicationContext.run()
    @Shared EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    void "test CRUD operations on generated client that returns blocking responses"() {
        given:
        BookClient client = context.getBean(BookClient)

        when:
        Book book = Flowable.fromPublisher(client.get(99))
                            .firstElement()
                            .onErrorResumeNext(Maybe.empty())
                            .blockingGet()
        List<Book> books = Flowable.fromPublisher(client.list()).blockingFirst()

        then:
        book == null
        books.size() == 0

        when:
        book = Flowable.fromPublisher(client.save("The Stand")).blockingFirst()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1

        when:
        book = Flowable.fromPublisher(client.get(book.id)).blockingFirst()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1

        when:
        book = Flowable.fromPublisher(client.update(book.id, "The Shining")).blockingFirst()

        then:
        book != null
        book.title == "The Shining"
        book.id == 1

        when:
        Flowable.fromPublisher(client.delete(book.id)).blockingFirst()
        book = Flowable.fromPublisher(client.get(book.id))
                .firstElement()
                .onErrorResumeNext(Maybe.empty())
                .blockingGet()
        then:
        book == null
    }


    @Client('/publisher/books')
    static interface BookClient extends BookApi {
    }

    @Controller("/publisher/books")
    @Singleton
    static class BookController implements BookApi {

        Map<Long, Book> books = new LinkedHashMap<>()
        AtomicLong currentId = new AtomicLong(0)

        @Override
        Publisher<Book> get(Long id) {
            return Publishers.just(books.get(id))
        }

        @Override
        Publisher<List<Book>> list() {
            return Publishers.just(books.values().toList())
        }

        @Override
        Publisher<Book> delete(Long id) {
            return Publishers.just(books.remove(id))
        }

        @Override
        Publisher<Book> save(String title) {
            Book book = new Book(title: title, id:currentId.incrementAndGet())
            books[book.id] = book
            return Publishers.just(book)
        }

        @Override
        Publisher<Book> update(Long id, String title) {
            Book book = books[id]
            if(book != null) {
                book.title = title
            }
            return Publishers.just(book)
        }
    }

    static interface BookApi {

        @Get("/{id}")
        Publisher<Book> get(Long id)

        @Get('/')
        Publisher<List<Book>> list()

        @Delete("/{id}")
        Publisher<Book> delete(Long id)

        @Post('/')
        Publisher<Book> save(String title)

        @Patch("/{id}")
        Publisher<Book> update(Long id, String title)
    }


    static class Book {
        Long id
        String title
    }
}
