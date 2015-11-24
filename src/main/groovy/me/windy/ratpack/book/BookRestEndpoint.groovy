package me.windy.ratpack.book

import ratpack.groovy.handling.GroovyChainAction

import javax.inject.Inject
import groovy.json.JsonOutput
import static ratpack.jackson.Jackson.json
import static ratpack.jackson.Jackson.jsonNode
import static ratpack.jackson.Jackson.fromJson
import static ratpack.rx.RxRatpack.observe
import java.text.SimpleDateFormat
import java.text.DateFormat

class BookRestEndpoint extends GroovyChainAction {
  private final BookService bookService
  private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    BookRestEndpoint(BookService bookService) {
        this.bookService = bookService
    }

    @Override
    void execute() throws Exception {
      get(":id") {
        def bookId = pathTokens["id"]
        bookService.findById(Long.parseLong(bookId)).
        single().
        subscribe {Book book ->
          if (book == null) {
              clientError 404
          } else {
              render book
          }
        }
      }

      all {
          byMethod {
            get {
              bookService.getAll().
              toList().
              subscribe { List<Book> books ->
                render json(books)
              }
            }

            post {
              parse(fromJson(Book.class)).
              observe().
              flatMap { input ->
                bookService.insert(input)
              }.
              single().
              flatMap {
                  bookService.findById(it)
              }.
              single().
              subscribe { book ->
                  render "$book.bookId"
              }
            }
        }
      }
    }
}
