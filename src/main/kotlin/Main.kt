
package org.example

import java.sql.DriverManager
import java.util.Properties

val url = "jdbc:postgresql://localhost:5432/song-library"
val connectionProps = Properties()

fun main() {
    connectionProps["user"] = "postgres"
    connectionProps["password"] = ""

    var running = true

    while (running) {
        println(
            "choose an operation:\n" +
                    "1 - albums\n" +
                    "2 - artists\n" +
                    "3 - categories\n" +
                    "4 - songs\n" +
                    "5 - songs by artist\n" +
                    "6 - artists with albums in given year\n" +
                    "7 - albums with songs in given category\n" +
                    "0 - exit"
        )

        when (readln().trim().toInt()) {
            1 -> album()
            2 -> artist()
            3 -> category()
            4 -> song()
            5 -> songsByArtist()
            6 -> artistsWithAlbumsInYear()
            7 -> albumsWithSongsInCategory()
            0 -> {
                println("seeya!")
                running = false
            }
            else -> println("invalid operation, please choose again")
        }
    }
}

fun album() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        connection.prepareStatement(
            "SELECT albumtitle, releaseyear FROM album"
        ).use { statement ->
            statement.executeQuery().use { rs ->
                var i = 0
                println("    albums:")
                while (rs.next()) {
                    println("${i++} - ${rs.getString("albumtitle")}, ${rs.getInt("releaseyear")}")
                }
            }
        }

        println("choose an operation: c u d e (create, update, delete, exit)")

        when (readln().trim()) {
            "c" -> {
                println("what is the name of the album?")
                val name = readln().trim()

                println("what is the release year of the album?")
                val year = readln().trim().toInt()

                connection.prepareStatement(
                    "INSERT INTO album VALUES (nextval('albumseq'), ?, ?)"
                ).use {
                    it.setString(1, name)
                    it.setInt(2, year)
                    it.executeUpdate()
                }
            }

            "u" -> {
                println("what is the name of the album you would like to update?")
                val oldName = readln().trim()

                println("choose an operation: y n (year, name)")

                when (readln().trim()) {
                    "y" -> {
                        println("what would you like to set the release year to?")
                        val year = readln().trim().toInt()

                        connection.prepareStatement(
                            "UPDATE album SET releaseyear = ? WHERE albumtitle = ?"
                        ).use {
                            it.setInt(1, year)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    "n" -> {
                        println("what is the new name you would like to use?")
                        val newName = readln().trim()

                        connection.prepareStatement(
                            "UPDATE album SET albumtitle = ? WHERE albumtitle = ?"
                        ).use {
                            it.setString(1, newName)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    else -> println("invalid operation, please try again")
                }
            }

            "d" -> {
                println("what is the name of the album you would like to delete?")
                val name = readln().trim()

                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        """
                        DELETE FROM song
                        WHERE songid IN (
                            SELECT i.songid FROM ison i
                            JOIN album a ON i.albumid = a.albumid
                            WHERE a.albumtitle = ?
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        """
                        DELETE FROM ison
                        WHERE albumid = (SELECT albumid FROM album WHERE albumtitle = ?)
                        """.trimIndent()
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM album WHERE albumtitle = ?"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }

            "e" -> return
            else -> println("invalid operation, please choose again")
        }
    }

    album()
}

fun artist() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        connection.prepareStatement("SELECT artistname FROM artist").use { statement ->
            statement.executeQuery().use { rs ->
                var i = 0
                println("    artists:")
                while (rs.next()) {
                    println("${i++} - ${rs.getString("artistname")}")
                }
            }
        }

        println("choose an operation: c u d e (create, update, delete, exit)")

        when (readln().trim()) {
            "c" -> {
                println("what is the name of the artist?")
                val name = readln().trim()

                connection.prepareStatement(
                    "INSERT INTO artist VALUES (nextval('artistseq'), ?)"
                ).use {
                    it.setString(1, name)
                    it.executeUpdate()
                }
            }

            "u" -> {
                println("what is the name of the artist you would like to update?")
                val oldName = readln().trim()

                println("what is the new name you would like to use?")
                val newName = readln().trim()

                connection.prepareStatement(
                    "UPDATE artist SET artistname = ? WHERE artistname LIKE ?"
                ).use {
                    it.setString(1, newName)
                    it.setString(2, "%$oldName%")
                    it.executeUpdate()
                }
            }

            "d" -> {
                println("what is the name of the artist you would like to delete?")
                val name = readln().trim()

                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        """
                        DELETE FROM song
                        WHERE songid IN (
                            SELECT p.songid FROM plays p
                            JOIN artist a ON p.artistid = a.artistid
                            WHERE a.artistname = ?
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM plays WHERE artistid = (SELECT artistid FROM artist WHERE artistname = ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM artist WHERE artistname = ?"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }

            "e" -> return
            else -> println("invalid operation, please choose again")
        }
    }

    artist()
}

fun category() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        connection.prepareStatement("SELECT categoryname FROM category").use { statement ->
            statement.executeQuery().use { rs ->
                var i = 0
                println("    categories:")
                while (rs.next()) {
                    println("${i++} - ${rs.getString("categoryname")}")
                }
            }
        }

        println("choose an operation: c u d e (create, update, delete, exit)")

        when (readln().trim()) {
            "c" -> {
                println("what is the name of the category?")
                val name = readln().trim()

                connection.prepareStatement(
                    "INSERT INTO category VALUES (nextval('categoryseq'), ?)"
                ).use {
                    it.setString(1, name)
                    it.executeUpdate()
                }
            }

            "u" -> {
                println("what is the name of the category you would like to update?")
                val oldName = readln().trim()

                println("what is the new name you would like to use?")
                val newName = readln().trim()

                connection.prepareStatement(
                    "UPDATE category SET categoryname = ? WHERE categoryname LIKE ?"
                ).use {
                    it.setString(1, newName)
                    it.setString(2, "%$oldName%")
                    it.executeUpdate()
                }
            }

            "d" -> {
                println("what is the name of the category you would like to delete?")
                val name = readln().trim()

                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        """
                        DELETE FROM song
                        WHERE songid IN (
                            SELECT i.songid FROM isin i
                            JOIN category c ON i.categoryid = c.categoryid
                            WHERE c.categoryname = ?
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM isin WHERE categoryid = (SELECT categoryid FROM category WHERE categoryname = ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM category WHERE categoryname = ?"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }

            "e" -> return
            else -> println("invalid operation, please choose again")
        }
    }

    category()
}


fun song() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        connection.prepareStatement("SELECT songtitle FROM song").use { statement ->
            statement.executeQuery().use { rs ->
                var i = 0
                println("    songs:")
                while (rs.next()) {
                    println("${i++} - ${rs.getString("songtitle")}")
                }
            }
        }

        println("choose an operation: c u d e (create, update, delete, exit)")

        when (readln().trim()) {
            "c" -> {
                println("what is the name of the song?")
                val name = readln().trim()

                println("what album is it on?")
                val album = readln().trim()

                println("who wrote the song?")
                val artist = readln().trim()

                println("what category is the song in?")
                val category = readln().trim()

                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        "INSERT INTO song VALUES (nextval('songseq'), ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        """
                        INSERT INTO ison
                        VALUES (
                            (SELECT albumid FROM album WHERE albumtitle LIKE ?),
                            (SELECT songid FROM song WHERE songtitle = ?)
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, "%$album%")
                        it.setString(2, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        """
                        INSERT INTO plays
                        VALUES (
                            (SELECT artistid FROM artist WHERE artistname LIKE ?),
                            (SELECT songid FROM song WHERE songtitle = ?)
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, "%$artist%")
                        it.setString(2, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        """
                        INSERT INTO isin
                        VALUES (
                            (SELECT categoryid FROM category WHERE categoryname LIKE ?),
                            (SELECT songid FROM song WHERE songtitle = ?)
                        )
                        """.trimIndent()
                    ).use {
                        it.setString(1, "%$category%")
                        it.setString(2, name)
                        it.executeUpdate()
                    }

                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }

            "u" -> {
                println("what is the name of the song you would like to update?")
                val oldName = readln().trim()

                println("choose an operation: al ar c n (album, artist, category, name)")

                when (readln().trim()) {
                    "al" -> {
                        println("what would you like to change the album to?")
                        val value = readln().trim()

                        connection.prepareStatement(
                            """
                            UPDATE ison SET albumid = (
                                SELECT albumid FROM album WHERE albumtitle = ?
                            )
                            WHERE songid = (
                                SELECT songid FROM song WHERE songtitle = ?
                            )
                            """.trimIndent()
                        ).use {
                            it.setString(1, value)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    "ar" -> {
                        println("what would you like to change the artist to?")
                        val value = readln().trim()

                        connection.prepareStatement(
                            """
                            UPDATE plays SET artistid = (
                                SELECT artistid FROM artist WHERE artistname = ?
                            )
                            WHERE songid = (
                                SELECT songid FROM song WHERE songtitle = ?
                            )
                            """.trimIndent()
                        ).use {
                            it.setString(1, value)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    "c" -> {
                        println("what would you like to change the category to?")
                        val value = readln().trim()

                        connection.prepareStatement(
                            """
                            UPDATE isin SET categoryid = (
                                SELECT categoryid FROM category WHERE categoryname = ?
                            )
                            WHERE songid = (
                                SELECT songid FROM song WHERE songtitle = ?
                            )
                            """.trimIndent()
                        ).use {
                            it.setString(1, value)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    "n" -> {
                        println("what is the new name you would like to use?")
                        val value = readln().trim()

                        connection.prepareStatement(
                            "UPDATE song SET songtitle = ? WHERE songtitle = ?"
                        ).use {
                            it.setString(1, value)
                            it.setString(2, oldName)
                            it.executeUpdate()
                        }
                    }

                    else -> println("invalid operation, please try again")
                }
            }

            "d" -> {
                println("what is the name of the song you would like to delete?")
                val name = readln().trim()

                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        "DELETE FROM isin WHERE songid = (SELECT songid FROM song WHERE songtitle = ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM plays WHERE songid = (SELECT songid FROM song WHERE songtitle = ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM ison WHERE songid = (SELECT songid FROM song WHERE songtitle = ?)"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.prepareStatement(
                        "DELETE FROM song WHERE songtitle = ?"
                    ).use {
                        it.setString(1, name)
                        it.executeUpdate()
                    }

                    connection.commit()
                } catch (e: Exception) {
                    connection.rollback()
                    throw e
                } finally {
                    connection.autoCommit = true
                }
            }

            "e" -> return
            else -> println("invalid operation, please choose again")
        }
    }

    song()
}

fun songsByArtist() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        println("which artist would you like to see a full list of songs for?")
        val inputArtist = readln().trim()

        connection.prepareStatement(
            """
            SELECT artistname, songtitle
            FROM plays p, artist a, song s
            WHERE p.artistid = a.artistid
            AND p.songid = s.songid
            AND p.artistid = (
                SELECT artistid FROM artist WHERE artistname LIKE ?
            )
            """.trimIndent()
        ).use { statement ->

            statement.setString(1, "%$inputArtist%")

            statement.executeQuery().use { rs ->
                var i = 0
                println("    results:")
                while (rs.next()) {
                    println(
                        "${i++} - ${rs.getString("songtitle")} by ${rs.getString("artistname")}"
                    )
                }
            }
        }
    }
}

fun artistsWithAlbumsInYear() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        println("which year would you like to see a full list of artists for?")
        val inputYear = readln().trim().toInt()

        connection.prepareStatement(
            """
            SELECT artistname
            FROM artist ar, plays p, ison i, album al
            WHERE ar.artistid = p.artistid
            AND p.songid = i.songid
            AND i.albumid = al.albumid
            AND al.releaseyear = ?
            """.trimIndent()
        ).use { statement ->

            statement.setInt(1, inputYear)

            statement.executeQuery().use { rs ->
                var i = 0
                println("    results:")
                while (rs.next()) {
                    println("${i++} - ${rs.getString("artistname")}")
                }
            }
        }
    }
}

fun albumsWithSongsInCategory() {
    DriverManager.getConnection(url, connectionProps).use { connection ->

        println("which category would you like to see a full list of albums for?")
        val inputCategory = readln().trim()

        connection.prepareStatement(
            """
            SELECT al.albumtitle, ar.artistname
            FROM album al, ison io, isin ii, category c, artist ar, plays p
            WHERE al.albumid = io.albumid
            AND io.songid = ii.songid
            AND ii.categoryid = c.categoryid
            AND io.songid = p.songid
            AND p.artistid = ar.artistid
            AND c.categoryid = (
                SELECT categoryid FROM category WHERE categoryname LIKE ?
            )
            """.trimIndent()
        ).use { statement ->

            statement.setString(1, "%$inputCategory%")

            statement.executeQuery().use { rs ->
                var i = 0
                println("    results:")
                while (rs.next()) {
                    println(
                        "${i++} - ${rs.getString("albumtitle")} by ${rs.getString("artistname")}"
                    )
                }
            }
        }
    }
}
