import java.util.*

object Genetic {
    const val SOLUTION_DEPTH = 5
    const val INITIAL_SOLUTIONS_COUNT = 50
    const val SOLUTION_SELECTION_COUNT = 10
    const val TIME_LIMIT = 30 //30ms, but it may be higher (until 50ms ultimate limit)

    private val random = Random()

    enum class Action {
        UP, DOWN, LEFT, RIGHT;
    }

    data class Solution(val actions: List<Action>, val score: Int)

    fun generateBestSolution(world: World): Solution {
        val startTime = System.nanoTime()
        val initialSolutions = generateInitialSolutions()
        var bestSolutions = initialSolutions

        while (haveTime(startTime)) {
            bestSolutions = improveSolutions(initialSolutions)
        }

        return bestSolutions.sortedByDescending { it.score }.first()
    }


    private fun haveTime(startTime: Long): Boolean {
        val nanoElapsed = System.nanoTime() - startTime
        return (nanoElapsed / 1_000_000) < TIME_LIMIT //conversion ns -> 盜 -> ms
    }

    private fun generateInitialSolutions(): List<Solution> = (0 until INITIAL_SOLUTIONS_COUNT).map { evaluate((0 until SOLUTION_DEPTH).map { getRandomAction() }) }

    private fun getRandomAction(): Action {
        val actions = Action.values()
        return actions[random.nextInt(actions.size)]
    }

    private fun improveSolutions(currentSolutions: List<Solution>): List<Solution> {
        val bestSolutionsFound = currentSolutions.sortedByDescending { it.score }.subList(0, SOLUTION_SELECTION_COUNT)

    }

    private fun evaluate(actions: List<Action>): Solution {
        val score = random.nextInt(250) //TODO simulate world behaviors
        return Solution(actions, score)
    }
}

object WorldSimulator {
    fun simulate(world: World, solution: Genetic.Solution): World {
        val newWorld = copyEntireWorld(world)
        val (map, myPlayer, otherPlayers, minions) = newWorld

        for (action in solution.actions) {
            simulateMyPlayerMove(world, action)
            simulateOtherPlayer(world)

        }
    }

    private fun copyEntireWorld(world: World): World {
        val myPlayer = MyPlayer(world.myPlayer.id, world.myPlayer.position.copy())
        val otherPlayers = world.otherPlayers.map { Player(it.id, it.position.copy(), it.health) }
        val minions = world.minions.map {
            val minion = when (it) {
                is Wanderer -> Wanderer(it.id, it.position.copy(), it.remainingTime)
                is Slasher -> Slasher(it.id, it.position.copy(), it.stateTurnRemaining)
                else -> error("What's that minion?")
            }
            minion.state = it.state
            minion.target = it.target
            minion
        }.toMutableList()

        return World(world.map, myPlayer, otherPlayers, minions, world.sanityLossLonely, world.sanityLossGroup, world.wandererSpawnTime, world.wandererLifeTime, world.turn)
    }

    private fun simulateMyPlayerMove(world: World, action: Genetic.Action) {
        val (map, myPlayer, otherPlayers, _, sanityLossLonely, sanityLossGroup) = world

        val case: MapCase = when (action) {
            Genetic.Action.UP -> map.get(myPlayer.position.up)
            Genetic.Action.DOWN -> map.get(myPlayer.position.down)
            Genetic.Action.LEFT -> map.get(myPlayer.position.left)
            Genetic.Action.RIGHT -> map.get(myPlayer.position.right)
        }
        myPlayer.hackMove(map, case)
        myPlayer.health -= if (otherPlayers.any { it.distanceTo(myPlayer) <= 2 }) sanityLossGroup else sanityLossLonely
    }

    private fun simulateOtherPlayer(world: World) {
        //TODO check if it's really useful to work on this behavior
        val (map, _, otherPlayers, _, sanityLossLonely, sanityLossGroup) = world

        otherPlayers.forEach { player ->
            player.health -= if (world.allPlayers.filter { it != player }.any { it.distanceTo(player) <= 2 }) sanityLossGroup else sanityLossLonely
        }
    }

    private fun simulateWanderers(world: World) {
        world.minions.filter { it is Wanderer }.map { it as Wanderer }.forEach { wanderer ->
            val pathByPlayer = world.allPlayers.associateBy({ it }, { PathFinder.findPath(world.map, wanderer, it) })
            val (target, path) = pathByPlayer.entries.sortedBy { it.value?.size ?: Int.MAX_VALUE }.first()

            wanderer.target = target
            wanderer.hackMove(world.map, path?.first()!!)
            if (wanderer.position == target.position) {
                world.minions.remove(wanderer)
                target.health -= 20 //TODO check if it's the good damage value
            }
            wanderer.remainingTime--
        }
    }

    private fun simulateSlashers(world: World) {
        world.minions.filter { it is Slasher }.map { it as Slasher }.forEach { slasher ->
        }
    }


    private fun Sprite.hackMove(map: Map, case: MapCase) {
        val path: List<MapCase>? = PathFinder.findPath(map, this.position, case.position)
        if (path != null) {
            this.position = path.first().position
        }
    }

    private fun Sprite.hackMove(map: Map, point: Point) {
        val path: List<MapCase>? = PathFinder.findPath(map, this.position, point)
        if (path != null) {
            this.position = path.first().position
        }
    }
}
