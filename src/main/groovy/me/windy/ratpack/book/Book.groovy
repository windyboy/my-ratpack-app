package me.windy.ratpack.book

import groovy.transform.Immutable
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@Immutable
@JsonIgnoreProperties(ignoreUnknown = true)
class Book {
    String isbn
    @JsonProperty("id")
    Long bookId
    @JsonProperty("title")
    String bookTitle
    @JsonProperty("date")
    Date bookDate
}
