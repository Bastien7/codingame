import java.lang.Math.abs
import java.lang.Math.max
import java.lang.Math.min
import java.util.*


fun debugExplo(text: String) {
    if (exploratorLogActivated) System.err.println(text)
}

fun debug(text: String) {
    if (debugLogActivated) System.err.println(text)
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
    fun distanceTo(otherPoint: Point) = abs(otherPoint.x - this.x) + abs(otherPoint.y - this.y)

    val left get() = this + Vector(-1, 0)
    val right get() = this + Vector(1, 0)
    val up get() = this + Vector(0, 1)
    val down get() = this + Vector(0, -1)
}

data class Vector(val x: Int, val y: Int) {
    constructor(start: Point, end: Point) : this(end.x - start.x, end.y - start.y)

    val length get() = abs(x) + abs(y)
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
}

abstract class MapCase(val position: Point) {
    override fun toString() = "$javaClass: $position"
}

class Wall(position: Point) : MapCase(position)
open class EmptyCase(position: Point) : MapCase(position)
class Portal(position: Point) : EmptyCase(position)
class Shelter(position: Point, var energy: Int = 10) : EmptyCase(position)

fun createCase(x: Int, y: Int, symbol: Char): MapCase {
    val position = Point(x, y)
    return when (symbol) {
        '#' -> Wall(position)
        'w' -> Portal(position)
        'U' -> Shelter(position)
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
        if (point1.x != point2.x && point1.y != point2.y)
            return false

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

fun List<MapCase>.firstStep(sprite: Sprite): MapCase = this.firstOrNull { it.position.distanceTo(sprite.position) == 1 }
    ?: error("the sprite at ${sprite.position} cannot use the path ${this.map { it.position }.joinToString(", ")}")


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
}

open class Player(id: Int, position: Point, var health: Int = 250, val yelledBy: MutableSet<Player> = mutableSetOf()) : Sprite(id, position) {

    override fun update(data: SpriteInputs) {
        super.update(data)
        this.health = data.param0
    }
}

abstract class Minion(id: Int, position: Point, var state: State = State.SPAWNING) : Sprite(id, position) {
    enum class State(val stateId: Int) {
        SPAWNING(0), WANDERING(1), STALKING(2), RUSHING(3), STUNNED(4)
    }

    fun getState(stateId: Int): State = State.values().find { it.stateId == stateId }
        ?: error("State unknown: $stateId")

    open fun update(world: World, data: SpriteInputs) {
        super.update(data)
        this.state = getState(data.param1)
    }
}

class Wanderer(id: Int, position: Point, var remainingTime: Int = 0, state: State = State.SPAWNING, var target: Player? = null) : Minion(id, position, state) {
    override fun update(world: World, data: SpriteInputs) {
        super.update(world, data)
        this.remainingTime = data.param0
        this.target = world.allPlayers.filter { it.id == data.param2 }.firstOrNull()
    }
}

class Slasher(id: Int, position: Point, var stateTurnRemaining: Int, state: State = State.SPAWNING, var targetPosition: Point = position) : Minion(id, position, state) {
    override fun update(world: World, data: SpriteInputs) {
        super.update(world, data)
        this.stateTurnRemaining = data.param0
    }
}


/**
 *
 * EFFECTS
 *
 */
open class Effect(val launcher: Sprite, val target: Sprite)

class Yell(launcher: Sprite, target: Sprite) : Effect(launcher, target)

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

    // game loop
    while (true) {
        debug("The player decide to do something...")
        world.myPlayer.decide(world)
        debug("Update the world")
        world.update(input)
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
    fun initSlasher(data: SpriteInputs): Slasher = Slasher(data.id, Point(data.x, data.y), 5)
}

object EffectFactory {
    fun initYell(data: SpriteInputs, world: World): Yell {
        val launcher = world.allPlayers.first { it.id == data.param1 }
        val target = world.allPlayers.first { it.id == data.param2 }
        if (target is MyPlayer)
            target.yelledBy += launcher

        return Yell(launcher, target)
    }
}

data class World(
    val map: Map,
    val myPlayer: MyPlayer,
    val otherPlayers: List<Player>,
    val wanderers: MutableList<Wanderer>,
    val slashers: MutableList<Slasher>,
    val shelters: List<Shelter>,
    val sanityLossLonely: Int,
    val sanityLossGroup: Int,
    val wandererSpawnTime: Int,
    val wandererLifeTime: Int,
    var turn: Int = 0
) {
    val allPlayers get() = this.otherPlayers.plusElement(myPlayer)

    /**
     * Here you update the world and add some logic tags if necessary
     */
    fun update(input: Scanner) {
        val entityCount = input.nextInt() // the first given entity corresponds to your explorer

        myPlayer.update(readData(input))

        val dataList = (1 until entityCount).map { readData(input) }
        dataList.forEach { data ->
            when (data.entityType) {
                "EXPLORER" -> otherPlayers.first { it.id == data.id }.update(data)
                "WANDERER" -> {
                    val minionToUpdate = wanderers.firstOrNull { it.id == data.id }

                    if (minionToUpdate != null) {
                        minionToUpdate.update(this, data)
                    } else
                        wanderers.add(SpriteFactory.initWanderer(data))
                }
                "SLASHER" -> {
                    val minionToUpdate = slashers.firstOrNull { it.id == data.id }

                    if (minionToUpdate != null)
                        minionToUpdate.update(this, data)
                    else
                        slashers.add(SpriteFactory.initSlasher(data))
                }
                "EFFECT_YELL" -> EffectFactory.initYell(data, this)
            }
        }
        wanderers.removeAll(wanderers.filter { minion -> dataList.find { data -> data.id == minion.id } == null })

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

    val shelters = cases.flatten().filter { it is Shelter }.map { it as Shelter }

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
    val wanderers: MutableList<Wanderer> = (4 until entityCount).map { SpriteFactory.initWanderer(readData(input)) }.toMutableList()

    return World(map, myPlayer, otherPlayers, wanderers, mutableListOf(), shelters, sanityLossLonely, sanityLossGroup, wandererSpawnTime, wandererLifeTime)
}


/**
 *
 *
 * My player actions
 *
 *
 */
class MyPlayer(id: Int, position: Point, health: Int = 250, yelledBy: MutableSet<Player> = mutableSetOf()) : Player(id, position, health, yelledBy) {
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
        val bestSolution = Explorator.generateBestSolution(world)
        val destination = bestSolution.path.firstOrNull()

        val attackingWanderers = world.wanderers
            .filter {
                val distance = PathFinder.realDistanceTo(world.map, this, it)
                it.target?.position == this.position && distance > 2 && distance < 10
            }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 3 } //arbitrary value to check
            .sortedBy { it.position.distanceTo(this.position) }

        if (destination == null || destination == this.position) {
            if (health < 220 && !healActivated && !lightActivated && healthPack > 0) {
                activeHealh(world)
            } else if ((attackingWanderers.size >= 2 || attackingWanderers.isNotEmpty() && health < 100) && !lightActivated && !healActivated && lightRemaining > 0) {
                activeLight(world)
            } else {
                debug("don't move")
                dontMove()
            }
        } else {
            debug("move to $destination")
            moveTo(destination)
        }
/*
        world.wanderers
            .filter {
                val distance = PathFinder.realDistanceTo(world.map, this, it)
                it.target?.position == this.position && distance > 3 && distance < 10
            }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 3 } //arbitrary value to check
            .sortedBy { it.position.distanceTo(this.position) }
            .forEach { debug("wanderer ${it.id} is evil against me ${it.state}") }

        val attackingWanderers = world.wanderers
            .filter {
                val distance = PathFinder.realDistanceTo(world.map, this, it)
                it.target?.position == this.position && distance > 2 && distance < 10
            }
            .filter { it.state != Minion.State.SPAWNING }
            .filter { it.remainingTime > 3 } //arbitrary value to check
            .sortedBy { it.position.distanceTo(this.position) }

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
*/
        debug("decided")
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
    }
}


/**
 *
 *
 * EXPLORATOR
 *
 *
 */
object Explorator {
    private const val TIME_LIMIT = 20 //30ms, but it may be higher (until 50ms ultimate limit)

    data class Solution(val virtualWorld: World, val path: List<Point>)

    fun generateBestSolution(world: World): Solution {
        val initialSituation = Solution(world, listOf())

        var solutions: List<Solution> = listOf(initialSituation)
        var step = 0

        while (step < 5) {
            step++
            solutions = solutions.flatMap { computeSolutions(it) }
        }

        //solutions.forEach { solution -> debugExplo("${solution.virtualWorld.myPlayer.health}: " + solution.path.joinToString(", ")) }

        val maxHealth: Int = solutions.maxBy { it.virtualWorld.myPlayer.health }?.virtualWorld?.myPlayer?.health!!
        debugExplo("max expected health: $maxHealth")
        val bestSolution = solutions
            .filter { it.virtualWorld.myPlayer.health == maxHealth }
            .maxBy { it.virtualWorld.wanderers.filter { it.target?.position == world.myPlayer.position }.sumBy { it.distanceTo(world.myPlayer) } }!!

        //val bestSolution = solutions.sortedByDescending { it.virtualWorld.myPlayer.health }.first()
        debugExplo("My solution: ${bestSolution.virtualWorld.myPlayer.health}: " + bestSolution.path.joinToString(", "))
        return bestSolution

    }

    private fun computeSolutions(currentSolution: Solution): List<Solution> {
        val world = currentSolution.virtualWorld
        val currentPosition = world.myPlayer.position
        val nextPositions = listOf(currentPosition, currentPosition.left, currentPosition.up, currentPosition.right, currentPosition.down)
            .map { world.map.get(it) }.filter { it !is Wall }

        return nextPositions.map {
            Solution(WorldSimulator.simulate(world, it), currentSolution.path + it.position)
        }
    }
}


/**
 *
 *
 * SIMULATOR
 *
 *
 */
object WorldSimulator {
    fun simulate(world: World, destination: MapCase): World {
        val newWorld = copyEntireWorld(world)

        if (destination is EmptyCase)
            simulateMyPlayerMove(newWorld, destination)
        else
            error("why that's not an empty case? $destination")

        simulateOtherPlayer(newWorld)
        simulateWanderers(newWorld)
        simulateSlashers(newWorld)
        simulateShelters(world)

        return newWorld
    }

    private fun copyEntireWorld(world: World): World {
        val myOldPlayer = world.myPlayer
        val myPlayer = MyPlayer(myOldPlayer.id, myOldPlayer.position.copy(), myOldPlayer.health, myOldPlayer.yelledBy)
        val otherPlayers = world.otherPlayers.map { Player(it.id, it.position.copy(), it.health, it.yelledBy) }
        val players = otherPlayers + myPlayer
        val wanderers = world.wanderers.map {
            val target = players.find { player -> player.id == it.target?.id }
            Wanderer(it.id, it.position.copy(), it.remainingTime, it.state, target)
        }.toMutableList()
        val slashers = world.slashers.map {
            Slasher(it.id, it.position.copy(), it.stateTurnRemaining, it.state, it.targetPosition)
        }.toMutableList()
        val shelters = world.shelters.map { Shelter(it.position, it.energy) }

        return World(world.map, myPlayer, otherPlayers, wanderers, slashers, shelters, world.sanityLossLonely, world.sanityLossGroup, world.wandererSpawnTime, world.wandererLifeTime, world.turn)
    }

    private fun simulateMyPlayerMove(world: World, destination: EmptyCase) {
        val (_, myPlayer, otherPlayers, _, _, _, sanityLossLonely, sanityLossGroup) = world

        if (myPlayer.position.distanceTo(destination.position) > 1)
            error("The player should not try to go from ${myPlayer.position} to ${destination.position} in one step")

        myPlayer.position = destination.position
        myPlayer.health -= if (otherPlayers.any { it.distanceTo(myPlayer) <= 2 }) sanityLossGroup else sanityLossLonely

        if (destination is Shelter && destination.energy > 0)
            myPlayer.health += 5
    }


    private fun simulateOtherPlayer(world: World) {
        //TODO check if it's really useful to work on this behavior
        val (_, _, otherPlayers, _, _, _, sanityLossLonely, sanityLossGroup) = world

        otherPlayers.forEach { player ->
            player.health -= if (world.allPlayers.filter { it != player }.any { it.distanceTo(player) <= 2 }) sanityLossGroup else sanityLossLonely
        }
    }

    private fun simulateWanderers(world: World) {
        //debug("before wanderer ${world.myPlayer.health}")

        world.wanderers
            .filter { it.position.distanceTo(world.myPlayer.position) <= 5 }
            .forEach { wanderer ->
                if (wanderer.state == Minion.State.SPAWNING) {
                    wanderer.remainingTime--
                    if (wanderer.remainingTime == 0)
                        wanderer.state = Minion.State.WANDERING
                } else {
                    if (wanderer.position == world.myPlayer.position) {
                        world.wanderers.remove(wanderer)
                        world.myPlayer.health -= 20 //TODO check if it's the good damage value
                    } else {
                        if (wanderer.distanceTo(world.myPlayer) == 1) {
                            wanderer.position = world.myPlayer.position
                        } else if (wanderer.target == world.myPlayer || wanderer.target?.position == world.myPlayer.position) {
                            val path = PathFinder.findPath(world.map, wanderer, world.myPlayer)
                            if (path != null)
                                wanderer.hackMove(path)
                        }
                        if (wanderer.position == world.myPlayer.position) {
                            world.wanderers.remove(wanderer)
                            world.myPlayer.health -= 20 //TODO check if it's the good damage value
                        }
                    }
                    wanderer.remainingTime--
                }
            }
    }

    private fun simulateSlashers(world: World) {
        world.slashers.forEach { slasher ->
            if (slasher.state == Minion.State.STALKING && PathFinder.directLinePath(world.map, world.myPlayer, slasher)) {
                slasher.targetPosition = world.myPlayer.position
            }

            slasher.stateTurnRemaining--

            if (slasher.state == Minion.State.RUSHING)
                slasher.stateTurnRemaining = 0

            if (slasher.state == Minion.State.WANDERING && PathFinder.directLinePath(world.map, world.myPlayer, slasher)) {
                slasher.targetPosition = world.myPlayer.position
                slasher.state = Minion.State.STALKING
                slasher.stateTurnRemaining = 2
            } else if (slasher.stateTurnRemaining == 0) {
                when (slasher.state) {
                    Minion.State.SPAWNING, Minion.State.STALKING -> {
                        slasher.state = Minion.State.RUSHING
                        slasher.stateTurnRemaining = 1
                    }
                    Minion.State.RUSHING -> {
                        if (PathFinder.directLinePath(world.map, world.myPlayer, slasher)) {
                            slasher.position = slasher.targetPosition
                            world.myPlayer.health -= 20 //TODO check if it's the good value
                            slasher.state = Minion.State.STUNNED
                            slasher.stateTurnRemaining = 6
                        }
                    }
                    Minion.State.STUNNED -> slasher.state = Minion.State.WANDERING
                }
            }
        }
    }

    private fun simulateShelters(world: World) {
        world.shelters.forEach { shelter ->
            world.allPlayers.forEach { player ->
                if (shelter.position == player.position)
                    shelter.energy--
            }
        }
    }

    private fun Sprite.hackMove(path: List<MapCase>) {
        if (path.isNotEmpty()) {
            this.position = path.firstStep(this).position
        }
    }
}

const val debugLogActivated = true
const val exploratorLogActivated = true
