package me.windy.ratpack.book

import groovy.transform.Immutable

@Immutable
class Book {
    String isbn
    Integer bookId
    String bookTitle
    Date bookDate
}