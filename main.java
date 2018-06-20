import java.lang.Math.max
import java.lang.Math.min
import java.util.*
import kotlin.system.measureTimeMillis


fun debug(vararg objects: Any) {
    if (debugLogActivated) objects.map { System.err.println(it) }
}

fun debug(vararg cases: MapCase) {
    if (debugLogActivated) cases.map { System.err.println("${it.javaClass} (${it.position})") }
}


/**
 *
 *
 * CONSTANTS
 *
 *
 */
val HEAL_DURATION = 5
val TORCH_DURATION = 3

/**
 *
 *
 * PHYSIC
 *
 *
 */
data class Point(val x: Int, val y: Int) {
    operator fun plus(vector: Vector) = Point(x + vector.x, y + vector.y)
    operator fun minus(vector: Vector) = this + Vector(-vector.x, -vector.y)
    fun distanceTo(otherPoint: Point) = Vector(this, otherPoint).length

    val left get() = this + Vector(-1, 0)
    val right get() = this + Vector(1, 0)
    val up get() = this + Vector(0, 1)
    val down get() = this + Vector(0, -1)

    companion object {
        fun middle(point1: Point, point2: Point) = Point(Math.abs(point1.x - point2.x), Math.abs(point1.y - point2.y))
    }
}

data class Vector(val x: Int, val y: Int) {
    constructor(start: Point, end: Point) : this(end.x - start.x, end.y - start.y)

    val length get() = Math.abs(x) + Math.abs(y)
    operator fun plus(otherVector: Vector) = Vector(this.x + otherVector.x, this.y + otherVector.y)
}


/**
 *
 *
 * MAP
 *
 *
 */
data class Map(private val cases: List<List<MapCase>>, val width: Int, val height: Int) {
    fun get(x: Int, y: Int): MapCase = cases[max(min(y, height - 1), 0)][max(min(x, width - 1), 0)]
    fun get(position: Point): MapCase = get(position.x, position.y)

    fun getCasesAround(position: Point, radius: Int): List<MapCase> {
        return (position.x - radius..position.x + radius).flatMap { x ->
            (position.y - radius..position.y + radius).map { y ->
                get(x, y)
            }
        }.filter { it !is Wall }.filter { PathFinder.realDistanceTo(this, position, it.position) <= radius }
    }
}

abstract class MapCase(val position: Point) {
    override fun toString() = "$javaClass: $position"
}

class Wall(position: Point) : MapCase(position)
class Portal(position: Point) : MapCase(position)
class EmptyCase(position: Point) : MapCase(position)

fun createCase(x: Int, y: Int, symbol: Char): MapCase {
    val position = Point(x, y)
    return when (symbol) {
        '#' -> Wall(position)
        'w' -> Portal(position)
        else -> EmptyCase(position)
    }
}


/**
 *
 *
 * PATH FINDER
 *
 *
 */
object PathFinder {
    private data class Node(val case: MapCase, val cost: Int, val estimatedCost: Int, val previousNode: Node?)

    private fun getAdjacentCases(map: Map, case: MapCase): List<MapCase> {
        val position = case.position
        return listOf(map.get(position.left), map.get(position.right), map.get(position.down), map.get(position.up)).filter { it !is Wall }
    }

    private fun getAdjacentNodes(map: Map, end: Point, openList: MutableList<Node>, closedList: MutableList<Node>, node: Node): MutableList<Node> {
        return getAdjacentCases(map, node.case)
            .filter { case -> openList.find { node -> node.case == case } == null }
            .filter { case -> closedList.find { node -> node.case == case } == null }
            .map { Node(it, node.cost + 1, it.position.distanceTo(end), node) }
            .toMutableList()
    }

    private fun extractPath(latestNode: Node): List<MapCase> {
        val path: MutableList<MapCase> = mutableListOf()
        var currentNode: Node = latestNode
        while (currentNode.previousNode != null) {
            path.add(currentNode.case)
            currentNode = currentNode.previousNode!!
        }
        return path
    }

    fun findPath(map: Map, start: Sprite, end: Sprite): List<MapCase>? = findPath(map, start.position, end.position)
    fun findPath(map: Map, start: MapCase, end: MapCase): List<MapCase>? = findPath(map, start.position, end.position)
    fun findPath(map: Map, start: MapCase, end: Sprite): List<MapCase>? = findPath(map, start.position, end.position)
    fun findPath(map: Map, start: Sprite, end: MapCase): List<MapCase>? = findPath(map, start.position, end.position)

    fun findPath(map: Map, start: Point, end: Point): List<MapCase>? {
        if (start == end) return listOf()

        val closedList = mutableListOf<Node>()
        val firstNodes = getAdjacentNodes(map, end, mutableListOf(), closedList, Node(map.get(start), 0, start.distanceTo(end), null))

        return findPath(map, end, firstNodes, mutableListOf())
    }

    private fun findPath(map: Map, end: Point, openList: MutableList<Node>, closedList: MutableList<Node>): List<MapCase>? {
        var bestNode: Node? = null

        while (bestNode?.estimatedCost != 0 && !openList.isEmpty()) {
            bestNode = openList.sortedBy { it.cost + it.estimatedCost }.firstOrNull()
            if (bestNode == null) {
                return null
            }
            openList.remove(bestNode)
            closedList.add(bestNode)
            openList.addAll(getAdjacentNodes(map, end, openList, closedList, bestNode))
        }

        if (bestNode == null)
            return null
        else
            return extractPath(bestNode)
    }


    fun realDistanceTo(map: Map, start: Sprite, end: Sprite) = realDistanceTo(map, start.position, end.position)

    fun realDistanceTo(map: Map, start: Point, end: Point): Int {
        if (start == end) return 0

        val closedList = mutableListOf<Node>()
        val firstNodes = getAdjacentNodes(map, end, mutableListOf(), closedList, Node(map.get(start), 0, start.distanceTo(end), null))

        return realDistanceTo(map, end, firstNodes, mutableListOf())
    }

    private fun realDistanceTo(map: Map, end: Point, openList: MutableList<Node>, closedList: MutableList<Node>): Int {
        var bestNode: Node? = null

        while (bestNode?.estimatedCost != 0 && !openList.isEmpty()) {
            bestNode = openList.sortedBy { it.cost + it.estimatedCost }.firstOrNull()
            if (bestNode == null) {
                return Int.MAX_VALUE
            }
            openList.remove(bestNode)
            closedList.add(bestNode)
            openList.addAll(getAdjacentNodes(map, end, openList, closedList, bestNode))
        }

        if (bestNode == null)
            return Int.MAX_VALUE
        else
            return bestNode.cost
    }

    fun directLinePath(map: Map, player: Sprite, slasher: Sprite) = directLinePath(map, player.position, slasher.position)

    fun directLinePath(map: Map, point1: Point, point2: Point): Boolean {
        if (point1 == point2)
            return true

        listOf(Point::left, Point::right, Point::up, Point::down)
            .forEach { property ->
                var caseFound: MapCase = map.get(point2)

                while (caseFound !is Wall) {
                    val nextCase = map.get(property.get(caseFound.position))
                    if (nextCase.position == point1)
                        return true
                    else
                        caseFound = nextCase
                }
            }

        return false
    }
}


/**
 *
 *
 * SPRITES
 *
 *
 */
abstract class Sprite(val id: Int, var position: Point) {
    open fun update(data: SpriteInputs) {
        this.position = Point(data.x, data.y)
    }

    fun distanceTo(otherSprite: Sprite) = this.position.distanceTo(otherSprite.position)
    @Deprecated("Not reliable")
    fun Point.distanceFromSprites(sprites: List<Sprite>) = sprites.map { it.position.distanceTo(this) }.sumBy { it }
}

open class Player(id: Int, position: Point) : Sprite(id, position) {
    var health: Int = 250

    override fun update(data: SpriteInputs) {
        super.update(data)
        this.health = data.param0
    }
}

abstract class Minion(id: Int, position: Point, var state: State = State.SPAWNING, var target: Player? = null) : Sprite(id, position) {
    enum class State(val stateId: Int) {
        SPAWNING(0), WANDERING(1), STALKING(2), RUSHING(3), STUNNED(4)
    }

    fun getState(stateId: Int): State = State.values().find { it.stateId == stateId }
        ?: error("State unknown: $stateId")

    open fun update(world: World, data: SpriteInputs) {
        super.update(data)
        this.state = getState(data.param1)
        this.target = world.allPlayers.filter { it.id == data.param2 }.firstOrNull()
    }
}

class Wanderer(id: Int, position: Point, var remainingTime: Int = 0) : Minion(id, position) {
    override fun update(world: World, data: SpriteInputs) {
        super.update(world, data)
        this.remainingTime = data.param0
    }
}

class Slasher(id: Int, position: Point, var stateTurnRemaining: Int) : Minion(id, position) {
    override fun update(world: World, data: SpriteInputs) {
        super.update(world, data)
        this.stateTurnRemaining = data.param0
    }
}


/**
 *
 * Survive the wrath of Kutulu
 * Coded fearlessly by JohnnyYuge & nmahoude (ok we might have been a bit scared by the old god...but don't say anything)
 *
 **/
fun main(args: Array<String>) {
    val input = Scanner(System.`in`)

    debug("Initialize the world")
    val world = initWorld(input)
    debug("Done")

    debug(world.map.get(4, 1))
    debug(world.map.get(1, 4))
    debug(world.map.get(3, 2))

    // game loop
    while (true) {
        val time = measureTimeMillis {
            //debug("The player decide to do something...")
            world.myPlayer.decide(world)
            //debug("Update the world")
            world.update(input)
        }
        debug("Executed in $time ms")
    }
}


/**
 *
 *
 * INPUT MANAGEMENT
 *
 *
 */
data class SpriteInputs(val entityType: String, val id: Int, val x: Int, val y: Int, val param0: Int, val param1: Int, val param2: Int)

fun readData(input: Scanner) = SpriteInputs(input.next(), input.nextInt(), input.nextInt(), input.nextInt(), input.nextInt(), input.nextInt(), input.nextInt())

object SpriteFactory {
    fun initMyPlayer(data: SpriteInputs) = MyPlayer(data.id, Point(data.x, data.y))
    fun initPlayer(data: SpriteInputs) = Player(data.id, Point(data.x, data.y))
    fun initWanderer(data: SpriteInputs): Wanderer = Wanderer(data.id, Point(data.x, data.y), data.param0)
    fun initSlasher(data: SpriteInputs): Slasher = Slasher(data.id, Point(data.x, data.y), 6)
}

data class World(
    val map: Map,
    val myPlayer: MyPlayer,
    val otherPlayers: List<Player>,
    val minions: MutableList<Minion>,
    var turn: Int = 0
) {
    val allPlayers get() = this.otherPlayers.plusElement(myPlayer)

    /**
     * Here you update the world and add some logic tags if necessary
     */
    fun update(input: Scanner) {
        val entityCount = input.nextInt() // the first given entity corresponds to your explorer
        //debug("Update player")
        myPlayer.update(readData(input))

        //debug("Update other players and minions")
        val dataList = (1 until entityCount).map { readData(input) }

        dataList.forEach { data ->
            when (data.entityType) {
                "EXPLORER" -> otherPlayers.first { it.id == data.id }.update(data)
                "WANDERER" -> {
                    val minionToUpdate = minions.firstOrNull { it.id == data.id }

                    if (minionToUpdate != null) {
                        minionToUpdate.update(this, data)
                    } else
                        minions.add(SpriteFactory.initWanderer(data))
                }
                "SLASHER" -> {
                    val minionToUpdate = minions.firstOrNull { it.id == data.id }

                    if (minionToUpdate != null)
                        minionToUpdate.update(this, data)
                    else
                        minions.add(SpriteFactory.initSlasher(data))
                }
            }
        }

        minions.removeAll(minions.filter { minion -> dataList.find { data -> data.id == minion.id } == null })

        //otherPlayers.forEach { debug("Player id ${it.id} is at ${it.position}") }
        //minions.forEach { debug("Minion id ${it.id} is at ${it.position}, spawning: ${it.spawning}, TTL: ${it.remainingTime}") }

        turn++
        debug("World updated")
    }
}

fun initWorld(input: Scanner): World {
    //Get start inputs
    val width = input.nextInt()
    val height = input.nextInt()

    //Strange...
    if (input.hasNextLine()) {
        input.nextLine() //TODO something to do with that?
    }

    debug("Init the map")
    val cases: List<List<MapCase>> = (0 until height).map { y ->
        val line = input.nextLine()
        (0 until width).map { x -> createCase(x, y, line[x]) }
    }
    val map = Map(cases, width, height)
    debug("My map size is ", cases.size, 'x', cases.get(0).size)

    debug("Init world constant parameters")
    //TODO this might be stored somewhere in the World
    val sanityLossLonely = input.nextInt() // how much sanity you lose every turn when alone, always 3 until wood 1
    val sanityLossGroup = input.nextInt() // how much sanity you lose every turn when near another player, always 1 until wood 1
    val wandererSpawnTime = input.nextInt() // how many turns the wanderer take to spawn, always 3 until wood 1
    val wandererLifeTime = input.nextInt() // how many turns the wanderer is on map after spawning, always 40 until wood 1

    debug("Init players")
    val entityCount = input.nextInt() // the first given entity corresponds to your explorer
    val myPlayer: MyPlayer = SpriteFactory.initMyPlayer(readData(input))
    debug("My id is ${myPlayer.id}")
    val otherPlayers: List<Player> = (1..3).map { SpriteFactory.initPlayer(readData(input)) }

    debug("Init minions (${entityCount - 4}")
    val minions: MutableList<Minion> = (4 until entityCount).map { SpriteFactory.initWanderer(readData(input)) }.toMutableList()

    return World(map, myPlayer, otherPlayers, minions)
}


/**
 *
 *
 * My player actions
 *
 *
 */
class MyPlayer(id: Int, position: Point) : Player(id, position) {
    private var lightRemaining = 3
    private var healthPack = 2

    private var healActivated: Boolean = false
    private var healTurn: Int? = null

    private var lightActivated: Boolean = false
    private var lightTurn: Int? = null


    private fun activeHealh(world: World): Boolean {
        println("PLAN")
        healActivated = true
        healthPack--
        healTurn = world.turn
        return false
    }

    private fun activeLight(world: World): Boolean {
        println("LIGHT")
        lightActivated = true
        lightRemaining--
        lightTurn = world.turn
        return false
    }

    private fun move(x: Int, y: Int, talk: String? = null): Boolean {
        if (this.position.x != x || this.position.y != y) {
            println("MOVE $x $y $talk")
            return false
        } else {
            return true
        }
    }

    fun moveTo(point: Point, talk: String? = null): Boolean = move(point.x, point.y, talk)
    fun moveTo(case: MapCase, talk: String? = null): Boolean = moveTo(case.position, talk)
    fun dontMove(talk: String? = "") = println("WAIT $talk")

    /**
     * Here you decide what you want to do
     */
    fun decide(world: World) {
        updateEffectStates(world)

        world.minions
            .filter { it is Wanderer }.map { it as Wanderer } //Get only wanderers
            .filter {
                val distance = PathFinder.realDistanceTo(world.map, this, it)
                it.target?.position == this.position && distance > 3 && distance < 10
            }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 3 } //arbitrary value to check
            .sortedBy { it.position.distanceTo(this.position) }
            .forEach { debug("wanderer ${it.id} is evil against me ${it.state}") }

        val attackingWanderers = world.minions
            .filter { it is Wanderer }.map { it as Wanderer } //Get only wanderers
            .filter {
                val distance = PathFinder.realDistanceTo(world.map, this, it)
                it.target?.position == this.position && distance > 2 && distance < 10
            }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 3 } //arbitrary value to check
            .sortedBy { it.position.distanceTo(this.position) }

        val hasSlasherRisk = hasSlasherRisk(world)
        debug("Slasher on my way? $hasSlasherRisk")

        val contactWanderers = searchContactWandererRisk(world)
        debug("any contact risk wanderers? ${contactWanderers.isNotEmpty()} (${contactWanderers.size})")

        val targetWanderers = searchTargetingWandererRisk(world)
        debug("any contact wanderers coming around me? ${targetWanderers.isNotEmpty()} (${targetWanderers.size})")

        debug("decide")
        debug(health < 220, !healActivated, !lightActivated, healthPack > 0)
        var actionAvailable: Boolean = true

        if (actionAvailable && hasSlasherRisk(world)) {
            debug("STRATEGY: escapeSlashers")
            actionAvailable = escapeSlashers(world)
            debug("after escapeSlashers: $actionAvailable")
        }
        if (actionAvailable && contactWanderers.isNotEmpty()) {
            debug("STRATEGY: escapeContactWanderers")
            actionAvailable = escapeWanderers(world, contactWanderers)
            debug("after escapeContactWanderers: $actionAvailable")
        }
        if (actionAvailable && targetWanderers.isNotEmpty()) {
            debug("STRATEGY: escapeTargetingWanderers")
            actionAvailable = escapeWanderers(world, targetWanderers)
            debug("after escapeTargetingWanderers: $actionAvailable")
        }
        if (actionAvailable && health < 220 && !healActivated && !lightActivated && healthPack > 0) { //HEAL
            debug("STRATEGY: activeHealh")
            actionAvailable = activeHealh(world)
            debug("after activeHealh: $actionAvailable")
        }
        if (actionAvailable && (attackingWanderers.size >= 2 || attackingWanderers.isNotEmpty() && health < 100) && !lightActivated && !healActivated && lightRemaining > 0) { //LIGHT
            debug("STRATEGY: activeLight")
            actionAvailable = activeLight(world)
            debug("after activeLight: $actionAvailable")
        }
        if (actionAvailable && world.minions.isNotEmpty()) {
            debug("STRATEGY: avoidMinions")
            //avoidMinions(world) //TODO not yet working, check that if it can be useful (but it doesn't seems to be necessary to have a good score...)
            //dontMove("That's ok")
        }
        if (actionAvailable) {
            debug("STRATEGY: dontMove")
            dontMove("That's ok")
        }
        debug("decided")
        /*
        if (health < 220 && !healActivated && !lightActivated && healthPack > 0) { //HEAL
            activeHealh(world)
        } else if ((attackingWanderers.size >= 3 || attackingWanderers.isNotEmpty() && health < 100) && !lightActivated && !healActivated && lightRemaining > 0) { //LIGHT
            activeLight(world)
        } else if (!attackingWanderers.isEmpty()) { //ESCAPE
            globalEscape(world, attackingWanderers)
        } else //WAIT FOR FUN
            dontMove("That's ok")*/
    }


    private fun hasSlasherRisk(world: World, point: Point = this.position) = world.minions
        .filter { it is Slasher }.map { it as Slasher }
        .filter {
            //debug("$it.id ${it.state} (turn remaining: ${it.stateTurnRemaining}")
            debug("slasher? " + !(it.state == Minion.State.SPAWNING && it.stateTurnRemaining > 3))
            it.state != Minion.State.STUNNED && !(it.state == Minion.State.SPAWNING && it.stateTurnRemaining > 3)
        }
        .any { debug("slasher ${it.position} can hit $point? " + PathFinder.directLinePath(world.map, point, it.position))
            PathFinder.directLinePath(world.map, point, it.position)
        }

    private fun searchContactWandererRisk(world: World): List<Wanderer> {
        return world.minions
            .filter { it is Wanderer }.map { it as Wanderer } //Get only wanderers
            .filter { minion -> minion.position.distanceTo(this.position) == 1 || (minion.distanceTo(this) == 2 && minion.target?.position?.distanceTo(this.position) ?: Int.MAX_VALUE <= 1) }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 0 }
    }

    private fun searchTargetingWandererRisk(world: World): List<Wanderer> {
        return world.minions
            .filter { it is Wanderer }.map { it as Wanderer } //Get only wanderers
            .filter { minion -> minion.target?.position?.distanceTo(this.position) ?: Int.MAX_VALUE <= 2 }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 0 }
    }


    private fun escapeSlashers(world: World): Boolean {
        val wanderersAround = world.minions.filter { it is Wanderer }.filter { minion -> PathFinder.realDistanceTo(world.map, this, minion) < 5 }

        val escapeCase = world.map.getCasesAround(this.position, 3)
            .sortedBy { case -> wanderersAround.map { minion -> PathFinder.realDistanceTo(world.map, case.position, minion.position) }.sum() }
            .reversed()
            .firstOrNull { !hasSlasherRisk(world, it.position) }
/*
        world.map.getCasesAround(this.position, 3)
            .sortedBy { case -> wanderersAround.map { minion -> PathFinder.realDistanceTo(world.map, case.position, minion.position) }.sum() }
            .reversed()
            .filter { !hasSlasherRisk(world, it.position) }
            .forEach { debug("slasher escape possibility ${it.position} (danger: ${wanderersAround.map { minion -> PathFinder.realDistanceTo(world.map, it.position, minion.position) }.sum()})") }
*/
        val farEscapeCase = world.map.getCasesAround(this.position, 5).firstOrNull { !hasSlasherRisk(world, it.position) }

        if (escapeCase is MapCase) {
            return moveTo(escapeCase.position)
        } else if (farEscapeCase is MapCase) {
            return moveTo(farEscapeCase.position)
        } else {
            return true
            //dontMove("I don't know how to escape slashers :(")
        }
    }

    private fun escapeWanderers(world: World, wanderers: List<Wanderer>): Boolean {
        val bestEscapeCases = world.map.getCasesAround(this.position, 1)
            .filter { hasSlasherRisk(world, it.position) == false }
            .sortedBy { case -> wanderers.map { minion -> PathFinder.realDistanceTo(world.map, minion.position, case.position) }.sum() }
            .reversed()
        bestEscapeCases.forEach { debug("It's a escape possibility: ${it.position} (score: ${wanderers.map { minion -> PathFinder.realDistanceTo(world.map, minion.position, it.position) }.sum()}") }
        bestEscapeCases.forEach { debug("It's a escape possibility: ${it.position} (score: ${wanderers.map { minion -> PathFinder.realDistanceTo(world.map, minion.position, it.position) }.joinToString(", ")}") }

        return moveTo(bestEscapeCases.first())
    }

    private fun avoidMinions(world: World): Boolean {
        val bestEscapeCases = world.map.getCasesAround(this.position, 1)
            //.filter { hasSlasherRisk(world, it) == false }
            .sortedBy { case -> world.minions.map { minion -> PathFinder.realDistanceTo(world.map, minion.position, case.position) }.sum() }
            .reversed()
        bestEscapeCases.forEach { debug("It's a escape possibility: ${it.position} (score: ${world.minions.map { minion -> PathFinder.realDistanceTo(world.map, minion.position, it.position) }.sum()}") }
        debug("whats? ${bestEscapeCases.first()}")
        return moveTo(bestEscapeCases.first())
    }

    private fun updateEffectStates(world: World) {
        val healTurn: Int? = this.healTurn
        val torchDate: Int? = this.lightTurn

        if (healTurn != null && world.turn > healTurn + HEAL_DURATION) {
            healActivated = false
            this.healTurn = null
        }
        if (torchDate != null && world.turn > torchDate + TORCH_DURATION) {
            lightActivated = false
            this.lightTurn = null
        }
        //debug("Heal activated: $healActivated ($healTurn, $healthPack)", "Light activated: $lightActivated ($lightTurn, $lightRemaining)")
    }

    @Deprecated("Old strategy")
    private fun directEscape(world: World, minion: Wanderer) {
        debug("Threat is at ${minion.position} (TTL=${minion.remainingTime}, I'm at $position")
        val map = world.map

        val hisMovement = Vector(minion.position, this.position)

        val destination = listOf(
            map.get(this.position + hisMovement),
            map.get(position.left),
            map.get(position.down),
            map.get(position.right),
            map.get(position.up)
        )
            .filter {
                if (it.position != minion.position) debug("$it is not the minion")
                it.position != minion.position
            }.filter {
                if (it !is Wall) debug("$it is not a wall")
                it !is Wall
            }.filter {
                if (it.position.distanceTo(minion.position) > 1) debug("$it is not the next position of the minion")
                it.position.distanceTo(minion.position) > 1
            }.firstOrNull()?.position

        if (destination != null)
            this.moveTo(destination, "Fear!!")
        else
            dontMove("I don't know what to do, I'm blocked!")
    }

    @Deprecated("Old strategy")
    private fun globalEscape(world: World, minions: List<Wanderer>) {
        debug("There is ${minions.size} threats, I'm at $position")
        val map = world.map

        var groupedVector = Vector(0, 0)
        minions.forEach { minion -> groupedVector += Vector(minion.position, this.position) }


        listOf(
            map.get(position + Vector(-1, 0)),
            map.get(position + Vector(0, -1)),
            map.get(position + Vector(1, 0)),
            map.get(position + Vector(0, 1))
            //map.get(this.position + groupedVector)
        )
            .filter { minions.find { minion -> it.position != minion.position } != null }
            .filter { it !is Wall }
            .filter { minions.find { minion -> it.position.distanceTo(minion.position) <= 1 } == null }.distinct()
            .forEach {
                val distance = minions.map { minion -> PathFinder.findPath(world.map, it, minion) }.sumBy {
                    it?.size ?: Int.MAX_VALUE
                }
                debug("distance for ${it.position}: $distance")
            }

        val destination = listOf(
            map.get(position + Vector(-1, 0)),
            map.get(position + Vector(0, -1)),
            map.get(position + Vector(1, 0)),
            map.get(position + Vector(0, 1))
            //map.get(this.position + groupedVector)
        )
            .filter { minions.find { minion -> it.position != minion.position } != null }
            .filter { it !is Wall }
            .filter { minions.find { minion -> it.position.distanceTo(minion.position) <= 1 } == null }.distinct()
            .sortedBy {
                //debug("$it represents a distances sum: ${it.position.distanceFromSprite(minions)}")
                //it.position.distanceFromSprites(minions)
                val distance = minions.map { minion -> PathFinder.findPath(world.map, it, minion) }.sumBy {
                    it?.size ?: Int.MAX_VALUE
                }
                //debug("distance for minion $distance")
                distance
            }
            .reversed()
            .firstOrNull()?.position

        if (destination != null) {
            this.moveTo(destination, "Fear!!")
            //val bestPath = findBestPath(map, position, destination)
            //debug("The best path has size: ${bestPath?.size}")
            debug("Time: ${measureTimeMillis {
                PathFinder.findPath(map, map.get(1, 1), map.get(3, 1))
            }}")

        } else
            dontMove("I don't know what to do, I'm blocked!")
    }
}

/* TODO:
- avoid priority direct contact wanderer before medium far away wanderers
- when escape wanderes: avoid to go on slasher direction if possible
- torch when at least three enemies targets me

Bonus :
- heal when near to other player with lower health
 */


val debugLogActivated = true
