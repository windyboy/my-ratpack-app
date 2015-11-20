package me.windy.ratpack.book

import ratpack.groovy.handling.GroovyChainAction

import javax.inject.Inject

import static ratpack.jackson.Jackson.json
import static ratpack.jackson.Jackson.jsonNode
import static ratpack.rx.RxRatpack.observe

class BookRestEndpoint extends GroovyChainAction {
  private final BookService bookService

    @Inject
    BookRestEndpoint(BookService bookService) {
        this.bookService = bookService
    }

    @Override
    void execute() throws Exception {
      all {
          byMethod {
              get {
                  bookService.getAll().
                      toList().
                      subscribe { List<Book> books ->
                          render json(books)
                      }
              }
          }
      }
    }
}