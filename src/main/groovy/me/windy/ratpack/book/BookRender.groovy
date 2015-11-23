package me.windy.ratpack.book

import ratpack.groovy.handling.GroovyContext
import ratpack.groovy.render.GroovyRendererSupport
import ratpack.jackson.Jackson

import static ratpack.groovy.Groovy.markupBuilder

class BookRenderer extends GroovyRendererSupport<Book> {

    @Override
    void render(GroovyContext context, Book book) throws Exception {
        context.byContent {
            json {
                context.render Jackson.json(book)
            }
            // xml {
            //     context.render markupBuilder("application/xml", "UTF-8") {
            //         delegate.book(isbn: book.isbn) {
            //             quantity book.quantity
            //             price book.price
            //             author book.author
            //             publisher book.publisher
            //         }
            //     }
            // }
        }
    }
}