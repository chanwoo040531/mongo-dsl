package com.example.kotlinmongo

import com.example.kotlinmongo.clazz.GroupType.SINGLE
import com.example.kotlinmongo.clazz.field
import com.example.kotlinmongo.collection.Author
import com.example.kotlinmongo.collection.Book
import com.example.kotlinmongo.collection.Status
import com.example.kotlinmongo.collection.Status.ACTIVE
import com.example.kotlinmongo.collection.Status.RETIREMENT
import com.example.kotlinmongo.extension.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import java.math.BigDecimal

@SpringBootTest
class GroupTest(
    @Autowired private val mongoTemplate: MongoTemplate,
) : StringSpec({
    beforeTest {
        mongoTemplate.dropCollection(Author::class.java)

        mongoTemplate.insert(
            Author.of(
                name = "John",
                age = 10,
                weight = 70.0,
                height = 170f,
                status = RETIREMENT,
                books = mutableListOf(
                    createBook("book1"),
                    createBook("book2"),
                ),
            )
        )
        mongoTemplate.insert(
            Author.of(
                name = "John",
                age = 20,
                weight = 80.0,
                height = 180f,
                status = ACTIVE,
                books = mutableListOf(
                    createBook("book3"),
                    createBook("book4"),
                ),
            )
        )
        mongoTemplate.insert(
            Author.of(
                name = "John",
                age = 30,
                weight = 90.0,
                height = 190f,
                status = ACTIVE,
                books = mutableListOf(
                    createBook("book5"),
                    createBook("book6"),
                ),
            )
        )
        mongoTemplate.insert(
            Author.of(
                name = "John",
                age = 40,
                weight = 100.0,
                height = 200f,
                status = ACTIVE,
                books = mutableListOf(
                    createBook("book7"),
                    createBook("book8"),
                ),
            )
        )
    }

    afterTest {
        mongoTemplate.dropCollection(Author::class.java)
    }

    "전체에 대한 count 를 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        }

        val count = mongoTemplate.count(document, Author::class)
        count shouldBe 4
    }

    "grouping 된 count 를 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        }

        val countOfGroup = mongoTemplate.count(document, Author::class).map {
            Status.valueOf(it.key) to it.value as Long
        }.toMap()

        countOfGroup shouldBe mapOf(ACTIVE to 3, RETIREMENT to 1)
    }

    "전체에 대한 합을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } sum {
            field(Author::age) alias "sum"
        }

        val sum = mongoTemplate.aggregate(document, Author::class)["sum"] as Long
        sum shouldBe 100
    }

    "grouping 된 필드에 대한 합을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        } sum {
            field(Author::age) alias "sum"
        }

        val sumOfGroup = mongoTemplate.aggregate(document, Author::class)
            .associate { it["_id"] to it["sum"] as Long }
            .mapKeys { Status.valueOf(it.key.toString()) }

        sumOfGroup shouldBe mapOf(ACTIVE to 90, RETIREMENT to 10)
    }

    "전체에 대한 평균을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } average {
            field(Author::age) alias "avg"
        }

        val avg = mongoTemplate.aggregate(document, Author::class)["avg"] as Double
        avg shouldBe 25.0
    }

    "grouping 된 평균을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        } average {
            field(Author::age) alias "avg"
        }

        val avgOfGroup = mongoTemplate.aggregate(document, Author::class)
            .associate { it["_id"] to it["avg"] as Double }
            .mapKeys { Status.valueOf(it.key.toString()) }

        avgOfGroup shouldBe mapOf(ACTIVE to 30.0, RETIREMENT to 10.0)
    }

    "전체에 대한 최대값을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } max {
            field(Author::age) alias "max"
        }

        val max = mongoTemplate.aggregate(document, Author::class)["max"] as Long
        max shouldBe 40
    }

    "mongoTemplate 을 이용 해서 grouping 된 총합을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        }

        val statusToTotalAge = mongoTemplate.sum(document, Author::age)
        statusToTotalAge shouldBe mapOf(ACTIVE to 90, RETIREMENT to 10)
    }

    "grouping 된 최대값을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        } max {
            field(Author::age) alias "max"
        }

        val maxOfGroup = mongoTemplate.aggregate(document, Author::class)
            .associate { it["_id"] to it["max"] as Long }
            .mapKeys { Status.valueOf(it.key.toString()) }

        maxOfGroup shouldBe mapOf(ACTIVE to 40, RETIREMENT to 10)
    }

    "전체에 대한 최소값을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } min {
            field(Author::age) alias "min"
        }

        val min = mongoTemplate.aggregate(document, Author::class)["min"] as Long
        min shouldBe 10
    }

    "grouping 된 최소값을 구할 수 있다" {
        val document = document {
            and { field(Author::name) eq "John" }
        } group {
            field(Author::status) by SINGLE
        } min {
            field(Author::age) alias "min"
        }

        val minOfGroup = mongoTemplate.aggregate(document, Author::class)
            .associate { it["_id"] to it["min"] as Long }
            .mapKeys { Status.valueOf(it.key.toString()) }

        minOfGroup shouldBe mapOf(ACTIVE to 20, RETIREMENT to 10)
    }

    "mongoDB에 데이터를 다른 타입으로 컨버팅 하고 연산을 할 수 있다." {
        val document = document {
            and { field(Author::name) eq "John" }
        } sum {
            field(Author::age) type BigDecimal::class alias "total"
        }

        val sum = mongoTemplate.aggregate(document, Author::class)["total"] as BigDecimal
        sum shouldBe 100.toBigDecimal()
    }
})

private val BigDecimal.roundOff: BigDecimal
    get() = this.setScale(0)

private fun createBook(
    title: String,
    price: Long = 10000L,
    isbn: String = "isbn",
    description: String? = null,
) =
    Book.of(
        title,
        price,
        isbn,
        description,
    )