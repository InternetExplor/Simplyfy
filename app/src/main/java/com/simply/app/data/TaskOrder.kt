package com.simply.app.data

/**
 * Единый порядок задач для всех списков приложения.
 *
 * Раньше каждый экран сортировал по-своему: день — по сроку, проект — по сроку,
 * поиск — по названию, и одна и та же задача оказывалась то первой, то последней.
 * Теперь порядок один и читается как матрица: сначала то, что горит.
 */

/**
 * Вес квадранта. Срочность важнее важности: «срочно» надо делать сегодня,
 * «важно, но не срочно» подождёт до завтра. Неразобранное лежит в самом конце —
 * пока задача не в матрице, она не претендует на место в начале списка.
 */
fun matrixWeight(quadrant: Quadrant?): Int = when (quadrant) {
    Quadrant.DO -> 4        // срочно и важно
    Quadrant.DELEGATE -> 3  // срочно
    Quadrant.PLAN -> 2      // важно
    Quadrant.DROP -> 1      // не срочно
    null -> 0               // входящие: ещё не разобрано
}

/** Задачи без срока встают после всех датированных, а не перед ними. */
private val FAR_FUTURE: java.time.LocalDate = java.time.LocalDate.MAX

/**
 * Порядок задач: матрица, потом срок, потом ручное перетаскивание внутри
 * своей группы, потом время создания — чтобы список не переставлялся сам собой
 * при равенстве всего остального.
 */
val TaskOrder: Comparator<Task> = compareByDescending<Task> { matrixWeight(it.quadrant) }
    .thenBy { it.dueDate ?: FAR_FUTURE }
    .thenBy { it.sortIndex }
    .thenBy { it.createdAt }

fun List<Task>.inMatrixOrder(): List<Task> = sortedWith(TaskOrder)

/**
 * Задачи проектов, которые ещё в работе. Завершённый проект уносит своё
 * с глаз долой: из дня, матрицы, календаря, поиска и подборки для фокуса —
 * но не из самого проекта, там всё остаётся как было.
 */
fun AppData.activeTasks(): List<Task> {
    val closed = projects.filter { it.completed }.map { it.id }.toSet()
    if (closed.isEmpty()) return tasks
    return tasks.filterNot { it.projectId in closed }
}
