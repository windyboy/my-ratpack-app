package me.windy.ratpack.book

import groovy.transform.Immutable

@Immutable
class Book {
    String isbn
    Long bookId
    String bookTitle
    Date bookDate

    public Map asMap() {
      this.class.declaredFields.findAll { !it.synthetic }.collectEntries {
      [ (it.name):this."$it.name" ]
    }
  }
}