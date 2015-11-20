package me.windy.ratpack.book

import groovy.json.JsonSlurper
import groovy.sql.GroovyRowResult
import groovy.util.logging.Slf4j
import rx.Observable

import javax.inject.Inject

import static rx.Observable.zip

@Slf4j
class BookService {
  private final BookDbCmd bookDbCmd

  @Inject
  BookService(BookDbCmd bookDbCmd) {
    this.bookDbCmd = bookDbCmd
  }

  Observable<List<Book>> getAll() {
        bookDbCmd.getAll().map{
          new Book(bookId: it.book_id, bookTitle: it.book_title, bookDate: it.book_date, isbn: it.isbn)
        }
    }
}