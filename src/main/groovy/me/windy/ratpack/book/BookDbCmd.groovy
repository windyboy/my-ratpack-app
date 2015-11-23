package me.windy.ratpack.book

import com.google.inject.Inject
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixObservableCommand
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import ratpack.exec.Blocking

import static ratpack.rx.RxRatpack.observe
import static ratpack.rx.RxRatpack.observeEach

class BookDbCmd {
  private final Sql sql
    private static final HystrixCommandGroupKey hystrixCommandGroupKey = HystrixCommandGroupKey.Factory.asKey("sql-bookdb")

    @Inject
    public BookDbCmd(Sql sql) {
        this.sql = sql
    }

    rx.Observable<GroovyRowResult> getAll() {
        return new HystrixObservableCommand<GroovyRowResult>(
            HystrixObservableCommand.Setter.withGroupKey(hystrixCommandGroupKey).andCommandKey(HystrixCommandKey.Factory.asKey("getAll"))) {

            @Override
            protected rx.Observable<GroovyRowResult> construct() {
                observeEach(Blocking.get {
                    sql.rows("select book_id, book_title, book_date, isbn from book order by isbn")
                })
            }

            @Override
            protected String getCacheKey() {
                return "db-bookdb-all"
            }
        }.toObservable()
    }

    rx.Observable<GroovyRowResult> findById(final Long bookId) {
        return new HystrixObservableCommand<GroovyRowResult>(
            HystrixObservableCommand.Setter.withGroupKey(hystrixCommandGroupKey).andCommandKey(HystrixCommandKey.Factory.asKey("findById"))) {

            @Override
            protected rx.Observable<GroovyRowResult> construct() {
                observe(Blocking.get {
                    sql.firstRow("select book_id, book_title, book_date, isbn from book where book_id = ?",[bookId])
                })
            }

            @Override
            protected String getCacheKey() {
                return "db-bookdb-bookid-$bookId"
            }
        }.toObservable()
    }

    rx.Observable<GroovyRowResult> findByIsbn(final String isbn) {
        return new HystrixObservableCommand<GroovyRowResult>(
            HystrixObservableCommand.Setter.withGroupKey(hystrixCommandGroupKey).addCommandKey(HystrixCommandKey.Factory.asKey("findByIsbn"))) {
                @Override
                protected rx.Observable<GroovyRowResult> construct() {
                    observe(Blocking.get {
                        sql.firstRow("select book_id, book_title, book_date, isbn from book where isbn = ?",[isbn])
                    })
                }

                @Override
                protected String getCacheKey() {
                    return "db-bookdb-bookisbn-$isbn"
                }
            }.toObservable()
    }

    rx.Observable<GroovyRowResult> insert(final Book book){
        return new HystrixObservableCommand<GroovyRowResult>(
            HystrixObservableCommand.Setter.withGroupKey(hystrixCommandGroupKey).andCommandKey(HystrixCommandKey.Factory.asKey("insert"))) {

            @Override
            protected rx.Observable<GroovyRowResult> construct() {
                observe(Blocking.get {
                    sql.executeInsert("insert into book (book_title, book_date, isbn) values (:bookTitle, :bookDate, :isbn)",
                        [bookTitle: book.bookTitle, bookDate:book.bookDate, isbn:book.isbn])
                })
            }
        }.toObservable()
    }

}