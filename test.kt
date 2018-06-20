import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
    val cases = listOf(
        listOf(
            Wall(Point(0, 0)),
            Wall(Point(1, 0)),
            Wall(Point(2, 0)),
            Wall(Point(3, 0)),
            Wall(Point(4, 0)),
            Wall(Point(5, 0)),
            Wall(Point(6, 0)),
            Wall(Point(7, 0)),
            Wall(Point(8, 0))
        ),
        listOf(
            Wall(Point(0, 1)),
            EmptyCase(Point(1, 1)),
            EmptyCase(Point(2, 1)),
            EmptyCase(Point(3, 1)),
            Wall(Point(4, 1)),
            EmptyCase(Point(5, 1)),
            EmptyCase(Point(6, 1)),
            EmptyCase(Point(7, 1)),
            Wall(Point(8, 1))
        ),
        listOf(
            Wall(Point(0, 2)),
            EmptyCase(Point(1, 2)),
            Wall(Point(2, 2)),
            EmptyCase(Point(3, 2)),
            EmptyCase(Point(4, 2)),
            EmptyCase(Point(5, 2)),
            Wall(Point(6, 2)),
            EmptyCase(Point(7, 2)),
            Wall(Point(8, 2))
        ),
        listOf(
            Wall(Point(0, 3)),
            EmptyCase(Point(1, 3)),
            Wall(Point(2, 3)),
            Wall(Point(3, 3)),
            EmptyCase(Point(4, 3)),
            Wall(Point(5, 3)),
            Wall(Point(6, 3)),
            EmptyCase(Point(7, 3)),
            Wall(Point(8, 3))
        ),
        listOf(
            Wall(Point(0, 4)),
            EmptyCase(Point(1, 4)),
            EmptyCase(Point(2, 4)),
            EmptyCase(Point(3, 4)),
            EmptyCase(Point(4, 4)),
            EmptyCase(Point(5, 4)),
            EmptyCase(Point(6, 4)),
            EmptyCase(Point(7, 4)),
            Wall(Point(8, 4))
        ),
        listOf(
            Wall(Point(0, 5)),
            EmptyCase(Point(1, 5)),
            Wall(Point(2, 5)),
            Wall(Point(3, 5)),
            EmptyCase(Point(4, 5)),
            Wall(Point(5, 5)),
            Wall(Point(6, 5)),
            EmptyCase(Point(7, 5)),
            Wall(Point(8, 5))
        ),
        listOf(
            Wall(Point(0, 6)),
            EmptyCase(Point(1, 6)),
            Wall(Point(2, 6)),
            Wall(Point(3, 6)),
            EmptyCase(Point(4, 6)),
            Wall(Point(5, 6)),
            EmptyCase(Point(6, 6)),
            EmptyCase(Point(7, 6)),
            Wall(Point(8, 6))
        ),
        listOf(
            Wall(Point(0, 7)),
            EmptyCase(Point(1, 7)),
            EmptyCase(Point(2, 7)),
            EmptyCase(Point(3, 7)),
            EmptyCase(Point(4, 7)),
            EmptyCase(Point(5, 7)),
            EmptyCase(Point(6, 7)),
            Wall(Point(7, 7)),
            Wall(Point(8, 7))
        ),
        listOf(
            Wall(Point(0, 8)),
            Wall(Point(1, 8)),
            Wall(Point(2, 8)),
            Wall(Point(3, 8)),
            Wall(Point(4, 8)),
            Wall(Point(5, 8)),
            Wall(Point(6, 8)),
            Wall(Point(7, 8)),
            Wall(Point(8, 8))
        )
    )
    val map = Map(cases, 9, 9)

    //var bestPath: List<MapCase>? = null
    //bestPath = findBestPath(map, map.get(1, 1), map.get(5, 1))
    /*println("time: " + measureNanoTime {
        bestPath = findPath(map, map.get(1, 1), map.get(7, 6))
    })*/
    //bestPath?.forEach { println("${it.javaClass} ${it.position}") }
    //println(bestPath?.size)

    val player = Player(1, Point(3, 1))
    val slasher = Slasher(2, Point(5, 1), 10)
    println(PathFinder.directLinePath(map, player, slasher))

    //println(Minion.State.values().find { it.stateId == 2 } ?: error("State unknown: 1"))

    //println(PathFinder.realDistanceTo(map, Point(1, 1), Point(7, 1)))
}
